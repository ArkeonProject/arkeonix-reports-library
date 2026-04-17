package com.empresa.reporter.model;

public enum TestStatus {
    PASSED, FAILED, SKIPPED;

    public String getCssClass() {
        return name().toLowerCase();
    }
}
