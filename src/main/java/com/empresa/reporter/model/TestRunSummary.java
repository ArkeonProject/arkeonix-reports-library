package com.empresa.reporter.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TestRunSummary {

    private Instant runTime;
    private String  suiteName;
    private int     total;
    private int     passed;
    private int     failed;
    private int     skipped;
    private long    durationMs;
    private String  buildUrl;

    public double getPassRateValue() {
        return total > 0 ? (passed * 100.0 / total) : 0;
    }

    public String getFormattedRunTime() {
        if (runTime == null) return "";
        return LocalDateTime.ofInstant(runTime, ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
    }

    public Instant getRunTime()  { return runTime; }
    public void setRunTime(Instant runTime) { this.runTime = runTime; }

    public String getSuiteName() { return suiteName; }
    public void setSuiteName(String suiteName) { this.suiteName = suiteName; }

    public int getTotal()   { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getPassed()  { return passed; }
    public void setPassed(int passed) { this.passed = passed; }

    public int getFailed()  { return failed; }
    public void setFailed(int failed) { this.failed = failed; }

    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public String getBuildUrl() { return buildUrl; }
    public void setBuildUrl(String buildUrl) { this.buildUrl = buildUrl; }
}
