package com.empresa.reporter.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class StepRecord {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final String description;
    private final Instant timestamp;
    private final StepStatus status;
    private final long durationMs;

    public StepRecord(String description, Instant timestamp, StepStatus status, long durationMs) {
        this.description = description;
        this.timestamp   = timestamp;
        this.status      = status;
        this.durationMs  = durationMs;
    }

    public String getDescription() { return description; }
    public Instant getTimestamp()  { return timestamp; }
    public StepStatus getStatus()  { return status; }
    public long getDurationMs()    { return durationMs; }

    public String getFormattedTimestamp() {
        if (timestamp == null) return "";
        return LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()).format(TS_FMT);
    }

    public String getFormattedDuration() {
        if (durationMs <= 0) return "";
        if (durationMs < 1000) return durationMs + "ms";
        return String.format("%.2fs", durationMs / 1000.0);
    }

    public String getCssClass() { return status != null ? status.getCssClass() : "skipped"; }
}
