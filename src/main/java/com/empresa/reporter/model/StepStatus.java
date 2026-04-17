package com.empresa.reporter.model;

public enum StepStatus {
    PASSED, FAILED, SKIPPED, PENDING;

    public String getCssClass() { return name().toLowerCase(); }
}
