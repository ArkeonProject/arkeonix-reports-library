package com.empresa.reporter.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestCaseReport {

    private String          testName;
    private String          className;
    private String          methodName;
    private Instant         startTime;
    private Instant         endTime;
    private long            durationMs;
    private TestStatus      status;
    private String          exceptionMessage;
    private String          exceptionType;
    private String          failureOrigin;
    private String          stackTrace;
    private String          screenshotBase64;
    private String          browserInfo;
    // Enterprise fields
    private List<StepRecord>  steps;
    private FailureCategory   failureCategory;
    private FlakiStatus       flakiStatus;
    private List<String>      recentOutcomes;   // e.g. ["PASSED","FAILED","PASSED"]
    private int               retryCount;
    private boolean           passedOnRetry;

    // ── computed helpers ──────────────────────────────────────────────────────

    public String getSimpleClassName() {
        if (className == null) return "";
        int dot = className.lastIndexOf('.');
        return dot >= 0 ? className.substring(dot + 1) : className;
    }

    public String getFormattedStartTime() {
        if (startTime == null) return "";
        return LocalDateTime.ofInstant(startTime, ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String getFormattedDuration() {
        if (durationMs < 1000) return durationMs + "ms";
        return String.format("%.2fs", durationMs / 1000.0);
    }

    public boolean isFlaky() {
        return FlakiStatus.FLAKY.equals(flakiStatus);
    }

    public boolean hasSteps() {
        return steps != null && !steps.isEmpty();
    }

    public void addStep(String description) {
        addStep(new StepRecord(description, Instant.now(), StepStatus.PASSED, 0));
    }

    public void addStep(StepRecord step) {
        if (steps == null) steps = new ArrayList<>();
        steps.add(step);
    }

    // ── getters / setters ─────────────────────────────────────────────────────

    public String getTestName()                      { return testName; }
    public void   setTestName(String v)              { this.testName = v; }

    public String getClassName()                     { return className; }
    public void   setClassName(String v)             { this.className = v; }

    public String getMethodName()                    { return methodName; }
    public void   setMethodName(String v)            { this.methodName = v; }

    public Instant getStartTime()                    { return startTime; }
    public void    setStartTime(Instant v)           { this.startTime = v; }

    public Instant getEndTime()                      { return endTime; }
    public void    setEndTime(Instant v)             { this.endTime = v; }

    public long getDurationMs()                      { return durationMs; }
    public void setDurationMs(long v)                { this.durationMs = v; }

    public TestStatus getStatus()                    { return status; }
    public void       setStatus(TestStatus v)        { this.status = v; }

    public String getExceptionMessage()              { return exceptionMessage; }
    public void   setExceptionMessage(String v)      { this.exceptionMessage = v; }

    public String getExceptionType()                 { return exceptionType; }
    public void   setExceptionType(String v)         { this.exceptionType = v; }

    public String getFailureOrigin()                 { return failureOrigin; }
    public void   setFailureOrigin(String v)         { this.failureOrigin = v; }

    public String getStackTrace()                    { return stackTrace; }
    public void   setStackTrace(String v)            { this.stackTrace = v; }

    public String getScreenshotBase64()              { return screenshotBase64; }
    public void   setScreenshotBase64(String v)      { this.screenshotBase64 = v; }

    public String getBrowserInfo()                   { return browserInfo; }
    public void   setBrowserInfo(String v)           { this.browserInfo = v; }

    public List<StepRecord> getSteps()               { return steps != null ? steps : Collections.emptyList(); }
    public void             setSteps(List<StepRecord> v) { this.steps = v; }

    public FailureCategory getFailureCategory()      { return failureCategory; }
    public void            setFailureCategory(FailureCategory v) { this.failureCategory = v; }

    public FlakiStatus getFlakiStatus()              { return flakiStatus; }
    public void        setFlakiStatus(FlakiStatus v) { this.flakiStatus = v; }

    public List<String> getRecentOutcomes()          { return recentOutcomes != null ? recentOutcomes : Collections.emptyList(); }
    public void         setRecentOutcomes(List<String> v) { this.recentOutcomes = v; }

    public int     getRetryCount()                   { return retryCount; }
    public void    setRetryCount(int v)              { this.retryCount = v; }

    public boolean isPassedOnRetry()                 { return passedOnRetry; }
    public void    setPassedOnRetry(boolean v)       { this.passedOnRetry = v; }
}
