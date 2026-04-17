package com.empresa.reporter.model;

public enum FlakiStatus {
    STABLE,
    FLAKY,
    NEW;

    public String getLabel() {
        switch (this) {
            case FLAKY:  return "Flaky";
            case STABLE: return "Stable";
            default:     return "New";
        }
    }

    public String getCssClass() {
        switch (this) {
            case FLAKY:  return "badge-flaky";
            case STABLE: return "badge-stable";
            default:     return "badge-new";
        }
    }
}
