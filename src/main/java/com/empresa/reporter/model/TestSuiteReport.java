package com.empresa.reporter.model;

import com.empresa.reporter.core.CiBuildInfo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TestSuiteReport {

    private String              suiteName;
    private String              projectName;
    private Instant             startTime;
    private Instant             endTime;
    private long                totalDurationMs;
    private String              environment;
    private List<TestCaseReport> testCases = Collections.emptyList();
    // Enterprise
    private List<TestRunSummary> history   = Collections.emptyList();
    private CiBuildInfo          buildInfo;

    // ── aggregate computations ────────────────────────────────────────────────

    public int getTotal() { return testCases.size(); }

    public long getPassed() {
        return testCases.stream().filter(t -> t.getStatus() == TestStatus.PASSED).count();
    }

    public long getFailed() {
        return testCases.stream().filter(t -> t.getStatus() == TestStatus.FAILED).count();
    }

    public long getSkipped() {
        return testCases.stream().filter(t -> t.getStatus() == TestStatus.SKIPPED).count();
    }

    public long getFlakyCount() {
        return testCases.stream().filter(TestCaseReport::isFlaky).count();
    }

    public String getPassRate() {
        if (getTotal() == 0) return "0.0%";
        return String.format("%.1f%%", (double) getPassed() / getTotal() * 100);
    }

    public double getPassRateValue() {
        return getTotal() > 0 ? (double) getPassed() / getTotal() * 100 : 0;
    }

    public Map<String, Long> getFailureCategoryBreakdown() {
        return testCases.stream()
                .filter(t -> t.getStatus() == TestStatus.FAILED && t.getFailureCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getFailureCategory().getLabel(),
                        Collectors.counting()));
    }

    public String getFormattedStartTime() {
        if (startTime == null) return "";
        return LocalDateTime.ofInstant(startTime, ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getFormattedTotalDuration() {
        if (totalDurationMs < 1000) return totalDurationMs + "ms";
        long seconds = totalDurationMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds);
        return String.format("%.2fs", totalDurationMs / 1000.0);
    }

    // ── getters / setters ─────────────────────────────────────────────────────

    public String getSuiteName()                  { return suiteName; }
    public void   setSuiteName(String v)          { this.suiteName = v; }

    public String getProjectName()                { return projectName; }
    public void   setProjectName(String v)        { this.projectName = v; }

    public Instant getStartTime()                 { return startTime; }
    public void    setStartTime(Instant v)        { this.startTime = v; }

    public Instant getEndTime()                   { return endTime; }
    public void    setEndTime(Instant v)          { this.endTime = v; }

    public long getTotalDurationMs()              { return totalDurationMs; }
    public void setTotalDurationMs(long v)        { this.totalDurationMs = v; }

    public String getEnvironment()                { return environment; }
    public void   setEnvironment(String v)        { this.environment = v; }

    public List<TestCaseReport> getTestCases()    { return testCases; }
    public void setTestCases(List<TestCaseReport> v) { this.testCases = v; }

    public List<TestRunSummary> getHistory()      { return history; }
    public void setHistory(List<TestRunSummary> v) { this.history = v; }

    public CiBuildInfo getBuildInfo()             { return buildInfo; }
    public void        setBuildInfo(CiBuildInfo v){ this.buildInfo = v; }
}
