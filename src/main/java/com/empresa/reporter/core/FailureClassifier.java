package com.empresa.reporter.core;

import com.empresa.reporter.model.FailureCategory;
import com.empresa.reporter.model.TestCaseReport;

public class FailureClassifier {

    public static FailureCategory classify(TestCaseReport test) {
        String type    = test.getExceptionType();
        String message = test.getExceptionMessage() != null ? test.getExceptionMessage().toLowerCase() : "";

        if (type == null) return FailureCategory.UNEXPECTED_EXCEPTION;

        if (type.contains("AssertionError") || type.contains("AssertionFailedError")
                || type.contains("ComparisonFailure") || type.contains("SoftAssertionError")) {
            return FailureCategory.ASSERTION_FAILURE;
        }
        if (type.contains("NoSuchElementException") || type.contains("ElementNotFound")
                || type.contains("ElementClickInterceptedException")) {
            return FailureCategory.ELEMENT_NOT_FOUND;
        }
        if (type.contains("TimeoutException") || message.contains("timed out")
                || message.contains("timeout")) {
            return FailureCategory.TIMEOUT;
        }
        if (type.contains("StaleElementReferenceException")) {
            return FailureCategory.STALE_ELEMENT;
        }
        return FailureCategory.UNEXPECTED_EXCEPTION;
    }
}
