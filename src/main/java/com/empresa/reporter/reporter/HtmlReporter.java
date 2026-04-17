package com.empresa.reporter.reporter;

import com.empresa.reporter.config.ReporterConfig;
import com.empresa.reporter.core.CiBuildInfo;
import com.empresa.reporter.core.FailureClassifier;
import com.empresa.reporter.history.FlakiDetector;
import com.empresa.reporter.history.HistoryManager;
import com.empresa.reporter.model.*;
import com.empresa.reporter.webhook.WebhookNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class HtmlReporter {

    private static final Logger log = LoggerFactory.getLogger(HtmlReporter.class);
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ReporterConfig config;
    private final TemplateEngine engine;

    public HtmlReporter(ReporterConfig config) {
        this.config = config;
        this.engine = buildEngine();
    }

    public File generate(TestSuiteReport report) {
        try {
            // 1. CI build info
            CiBuildInfo buildInfo = config.getCiBuildInfoOverride() != null
                    ? config.getCiBuildInfoOverride()
                    : CiBuildInfo.detect();
            report.setBuildInfo(buildInfo);

            // 2. History + flaky detection
            if (config.isHistoryEnabled()) {
                HistoryManager history = new HistoryManager(config.getOutputDir(), config.getHistoryLimit());
                Map<String, List<Boolean>> outcomes = history.loadTestOutcomes();

                for (TestCaseReport tc : report.getTestCases()) {
                    if (tc.getTestName() == null) continue;
                    FlakiStatus flaki = FlakiDetector.detect(tc.getTestName(), outcomes);
                    tc.setFlakiStatus(flaki);

                    List<Boolean> hist = outcomes.getOrDefault(tc.getTestName(), Collections.emptyList());
                    int from = Math.max(0, hist.size() - 5);
                    tc.setRecentOutcomes(hist.subList(from, hist.size()).stream()
                            .map(b -> b ? "PASSED" : "FAILED")
                            .collect(Collectors.toList()));
                }

                report.setHistory(history.loadHistory());
                history.saveRun(report);
            }

            // 3. Classify failures
            for (TestCaseReport tc : report.getTestCases()) {
                if (tc.getStatus() == TestStatus.FAILED && tc.getExceptionType() != null) {
                    tc.setFailureCategory(FailureClassifier.classify(tc));
                }
            }

            // 4. Write HTML
            Files.createDirectories(Paths.get(config.getOutputDir()));
            File out = new File(config.getOutputDir(),
                    "report_" + LocalDateTime.now().format(FILE_TS) + ".html");

            Context ctx = new Context(Locale.getDefault());
            ctx.setVariable("report",        report);
            ctx.setVariable("testLabels",    labelsFrom(report));
            ctx.setVariable("testDurations", durationsFrom(report));
            ctx.setVariable("testStatuses",  statusesFrom(report));
            ctx.setVariable("historyLabels",    historyLabelsFrom(report));
            ctx.setVariable("historyPassRates", historyPassRatesFrom(report));

            try (FileWriter writer = new FileWriter(out)) {
                engine.process("report", ctx, writer);
            }
            log.info("[selenium-reporter] Report generated: {}", out.getAbsolutePath());

            if (config.isOpenBrowserAfterReport()) openInBrowser(out);

            // 5. Webhook
            if (config.getWebhookUrl() != null && !config.getWebhookUrl().isEmpty()) {
                new WebhookNotifier(config.getWebhookUrl(), config.getWebhookType(),
                        config.isNotifyOnlyOnFailure()).notify(report);
            }

            return out;
        } catch (IOException e) {
            log.error("[selenium-reporter] Failed to generate report", e);
            throw new RuntimeException("Report generation failed", e);
        }
    }

    // ── Template engine ───────────────────────────────────────────────────────

    private TemplateEngine buildEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        TemplateEngine te = new TemplateEngine();
        te.setTemplateResolver(resolver);
        return te;
    }

    // ── Context helpers ───────────────────────────────────────────────────────

    private List<String> labelsFrom(TestSuiteReport r) {
        return r.getTestCases().stream().map(TestCaseReport::getTestName).collect(Collectors.toList());
    }

    private List<Long> durationsFrom(TestSuiteReport r) {
        return r.getTestCases().stream().map(TestCaseReport::getDurationMs).collect(Collectors.toList());
    }

    private List<String> statusesFrom(TestSuiteReport r) {
        return r.getTestCases().stream().map(t -> t.getStatus().name()).collect(Collectors.toList());
    }

    private List<String> historyLabelsFrom(TestSuiteReport r) {
        return r.getHistory().stream()
                .map(TestRunSummary::getFormattedRunTime)
                .collect(Collectors.toList());
    }

    private List<Double> historyPassRatesFrom(TestSuiteReport r) {
        return r.getHistory().stream()
                .map(TestRunSummary::getPassRateValue)
                .collect(Collectors.toList());
    }

    private void openInBrowser(File file) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(file.toURI());
            }
        } catch (IOException e) {
            log.warn("[selenium-reporter] Could not open browser: {}", e.getMessage());
        }
    }
}
