package com.empresa.reporter.history;

import com.empresa.reporter.model.FlakiStatus;

import java.util.List;
import java.util.Map;

public class FlakiDetector {

    private static final int MIN_RUNS = 3;

    public static FlakiStatus detect(String testName, Map<String, List<Boolean>> outcomes) {
        List<Boolean> history = outcomes.get(testName);
        if (history == null || history.size() < MIN_RUNS) return FlakiStatus.NEW;

        boolean hasPass = false, hasFail = false;
        for (Boolean b : history) {
            if (b)  hasPass = true;
            else    hasFail = true;
            if (hasPass && hasFail) return FlakiStatus.FLAKY;
        }
        return FlakiStatus.STABLE;
    }
}
