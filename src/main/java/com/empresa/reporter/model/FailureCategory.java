package com.empresa.reporter.model;

public enum FailureCategory {
    ASSERTION_FAILURE,
    ELEMENT_NOT_FOUND,
    TIMEOUT,
    STALE_ELEMENT,
    UNEXPECTED_EXCEPTION;

    public String getLabel() {
        switch (this) {
            case ASSERTION_FAILURE:   return "Assertion";
            case ELEMENT_NOT_FOUND:   return "Element Not Found";
            case TIMEOUT:             return "Timeout";
            case STALE_ELEMENT:       return "Stale Element";
            default:                  return "Exception";
        }
    }

    public String getCssClass() {
        switch (this) {
            case ASSERTION_FAILURE:   return "cat-assertion";
            case ELEMENT_NOT_FOUND:   return "cat-element";
            case TIMEOUT:             return "cat-timeout";
            case STALE_ELEMENT:       return "cat-stale";
            default:                  return "cat-exception";
        }
    }
}
