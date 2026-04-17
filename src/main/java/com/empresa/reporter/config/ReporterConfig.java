package com.empresa.reporter.config;

import com.empresa.reporter.core.CiBuildInfo;
import com.empresa.reporter.webhook.WebhookType;

public class ReporterConfig {

    private final String      projectName;
    private final String      outputDir;
    private final boolean     captureScreenshotOnFailure;
    private final boolean     captureScreenshotOnSuccess;
    private final boolean     openBrowserAfterReport;
    // History
    private final boolean     historyEnabled;
    private final int         historyLimit;
    // Webhook
    private final String      webhookUrl;
    private final WebhookType webhookType;
    private final boolean     notifyOnlyOnFailure;
    // CI override (auto-detected when null)
    private final CiBuildInfo ciBuildInfoOverride;

    private ReporterConfig(Builder b) {
        this.projectName               = b.projectName;
        this.outputDir                 = b.outputDir;
        this.captureScreenshotOnFailure = b.captureScreenshotOnFailure;
        this.captureScreenshotOnSuccess = b.captureScreenshotOnSuccess;
        this.openBrowserAfterReport    = b.openBrowserAfterReport;
        this.historyEnabled            = b.historyEnabled;
        this.historyLimit              = b.historyLimit;
        this.webhookUrl                = b.webhookUrl;
        this.webhookType               = b.webhookType;
        this.notifyOnlyOnFailure       = b.notifyOnlyOnFailure;
        this.ciBuildInfoOverride       = b.ciBuildInfoOverride;
    }

    public static Builder builder() { return new Builder(); }

    public String      getProjectName()                { return projectName; }
    public String      getOutputDir()                  { return outputDir; }
    public boolean     isCaptureScreenshotOnFailure()  { return captureScreenshotOnFailure; }
    public boolean     isCaptureScreenshotOnSuccess()  { return captureScreenshotOnSuccess; }
    public boolean     isOpenBrowserAfterReport()      { return openBrowserAfterReport; }
    public boolean     isHistoryEnabled()              { return historyEnabled; }
    public int         getHistoryLimit()               { return historyLimit; }
    public String      getWebhookUrl()                 { return webhookUrl; }
    public WebhookType getWebhookType()                { return webhookType; }
    public boolean     isNotifyOnlyOnFailure()         { return notifyOnlyOnFailure; }
    public CiBuildInfo getCiBuildInfoOverride()        { return ciBuildInfoOverride; }

    public static class Builder {
        private String      projectName               = "Selenium Test Suite";
        private String      outputDir                 = "./test-reports";
        private boolean     captureScreenshotOnFailure = true;
        private boolean     captureScreenshotOnSuccess = false;
        private boolean     openBrowserAfterReport    = false;
        private boolean     historyEnabled            = true;
        private int         historyLimit              = 20;
        private String      webhookUrl                = null;
        private WebhookType webhookType               = WebhookType.GENERIC_JSON;
        private boolean     notifyOnlyOnFailure       = true;
        private CiBuildInfo ciBuildInfoOverride       = null;

        public Builder projectName(String v)               { this.projectName = v;               return this; }
        public Builder outputDir(String v)                 { this.outputDir = v;                 return this; }
        public Builder captureScreenshotOnFailure(boolean v){ this.captureScreenshotOnFailure = v; return this; }
        public Builder captureScreenshotOnSuccess(boolean v){ this.captureScreenshotOnSuccess = v; return this; }
        public Builder openBrowserAfterReport(boolean v)   { this.openBrowserAfterReport = v;   return this; }
        public Builder historyEnabled(boolean v)           { this.historyEnabled = v;           return this; }
        public Builder historyLimit(int v)                 { this.historyLimit = v;             return this; }
        public Builder webhookUrl(String v)                { this.webhookUrl = v;               return this; }
        public Builder webhookType(WebhookType v)          { this.webhookType = v;              return this; }
        public Builder notifyOnlyOnFailure(boolean v)      { this.notifyOnlyOnFailure = v;      return this; }
        public Builder ciBuildInfo(CiBuildInfo v)          { this.ciBuildInfoOverride = v;      return this; }

        public ReporterConfig build() { return new ReporterConfig(this); }
    }
}
