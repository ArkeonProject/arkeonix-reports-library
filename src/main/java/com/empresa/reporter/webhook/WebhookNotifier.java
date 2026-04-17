package com.empresa.reporter.webhook;

import com.empresa.reporter.model.TestSuiteReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class WebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotifier.class);

    private final String      webhookUrl;
    private final WebhookType type;
    private final boolean     notifyOnlyOnFailure;
    private final HttpClient  httpClient;

    public WebhookNotifier(String webhookUrl, WebhookType type, boolean notifyOnlyOnFailure) {
        this.webhookUrl          = webhookUrl;
        this.type                = type != null ? type : WebhookType.GENERIC_JSON;
        this.notifyOnlyOnFailure = notifyOnlyOnFailure;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void notify(TestSuiteReport report) {
        if (notifyOnlyOnFailure && report.getFailed() == 0) return;

        String payload;
        switch (type) {
            case SLACK:        payload = buildSlackPayload(report);   break;
            case TEAMS:        payload = buildTeamsPayload(report);   break;
            default:           payload = buildGenericPayload(report); break;
        }

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 400) {
                log.warn("[selenium-reporter] Webhook HTTP {}: {}", res.statusCode(), res.body());
            } else {
                log.info("[selenium-reporter] Webhook notified ({})", type);
            }
        } catch (Exception e) {
            log.warn("[selenium-reporter] Webhook failed: {}", e.getMessage());
        }
    }

    // ── Payload builders ──────────────────────────────────────────────────────

    private String buildSlackPayload(TestSuiteReport report) {
        boolean failed = report.getFailed() > 0;
        String icon    = failed ? ":x:" : ":white_check_mark:";
        String color   = failed ? "#f85149" : "#3fb950";
        String ciLink  = buildCiLink(report);

        return "{"
             + "\"attachments\":[{"
             + "\"color\":\"" + color + "\","
             + "\"blocks\":["
             + "{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\","
             +   "\"text\":\"" + icon + " *" + esc(report.getProjectName()) + "* — " + esc(report.getSuiteName()) + "\"}},"
             + "{\"type\":\"section\",\"fields\":["
             +   "{\"type\":\"mrkdwn\",\"text\":\"*Passed*\\n" + report.getPassed() + "\"},"
             +   "{\"type\":\"mrkdwn\",\"text\":\"*Failed*\\n" + report.getFailed() + "\"},"
             +   "{\"type\":\"mrkdwn\",\"text\":\"*Skipped*\\n" + report.getSkipped() + "\"},"
             +   "{\"type\":\"mrkdwn\",\"text\":\"*Duration*\\n" + esc(report.getFormattedTotalDuration()) + "\"}"
             + "]}"
             + (ciLink.isEmpty() ? "" : ",{\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"" + ciLink + "\"}}")
             + "]}]}";
    }

    private String buildTeamsPayload(TestSuiteReport report) {
        boolean failed = report.getFailed() > 0;
        String status  = failed ? "❌ FAILED" : "✅ PASSED";
        String color   = failed ? "f85149" : "3fb950";
        String ciLink  = buildCiLink(report);

        return "{"
             + "\"@type\":\"MessageCard\","
             + "\"@context\":\"http://schema.org/extensions\","
             + "\"themeColor\":\"" + color + "\","
             + "\"summary\":\"" + esc(report.getProjectName()) + " test results\","
             + "\"sections\":[{"
             + "\"activityTitle\":\"" + status + " " + esc(report.getProjectName()) + "\","
             + "\"activitySubtitle\":\"" + esc(report.getSuiteName()) + "\","
             + "\"facts\":["
             +   "{\"name\":\"Passed\",\"value\":\"" + report.getPassed() + "\"},"
             +   "{\"name\":\"Failed\",\"value\":\"" + report.getFailed() + "\"},"
             +   "{\"name\":\"Skipped\",\"value\":\"" + report.getSkipped() + "\"},"
             +   "{\"name\":\"Duration\",\"value\":\"" + esc(report.getFormattedTotalDuration()) + "\"}"
             + "]"
             + (ciLink.isEmpty() ? "" : ",\"potentialAction\":[{\"@type\":\"OpenUri\",\"name\":\"View Build\",\"targets\":[{\"os\":\"default\",\"uri\":\"" + esc(ciLink) + "\"}]}]")
             + "}]"
             + "}";
    }

    private String buildGenericPayload(TestSuiteReport report) {
        return "{"
             + "\"project\":\"" + esc(report.getProjectName()) + "\","
             + "\"suite\":\"" + esc(report.getSuiteName()) + "\","
             + "\"status\":\"" + (report.getFailed() > 0 ? "FAILED" : "PASSED") + "\","
             + "\"total\":" + report.getTotal() + ","
             + "\"passed\":" + report.getPassed() + ","
             + "\"failed\":" + report.getFailed() + ","
             + "\"skipped\":" + report.getSkipped() + ","
             + "\"passRatePct\":" + String.format("%.1f", report.getPassRateValue()) + ","
             + "\"durationMs\":" + report.getTotalDurationMs()
             + "}";
    }

    private String buildCiLink(TestSuiteReport report) {
        if (report.getBuildInfo() == null || report.getBuildInfo().getBuildUrl() == null) return "";
        return report.getBuildInfo().getBuildUrl();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
