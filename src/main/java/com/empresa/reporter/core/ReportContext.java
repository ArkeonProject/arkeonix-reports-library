package com.empresa.reporter.core;

import com.empresa.reporter.config.ReporterConfig;
import com.empresa.reporter.model.StepRecord;
import com.empresa.reporter.model.StepStatus;
import com.empresa.reporter.model.TestCaseReport;
import org.openqa.selenium.WebDriver;

import java.time.Instant;

/**
 * Thread-safe holder for per-test state.
 *
 * In your @BeforeMethod / @BeforeEach:
 *   ReportContext.setDriver(driver);
 *
 * Optional step logging from test code:
 *   ReportContext.logStep("Click submit button");
 *   ReportContext.logStepFailed("Verify title", new AssertionError("wrong title"));
 */
public class ReportContext {

    private static final ThreadLocal<WebDriver>      driverHolder      = new ThreadLocal<>();
    private static final ThreadLocal<TestCaseReport> currentTestHolder = new ThreadLocal<>();
    private static final ThreadLocal<Long>           stepStartHolder   = new ThreadLocal<>();
    private static volatile ReporterConfig config = ReporterConfig.builder().build();

    private ReportContext() {}

    // ── Driver ────────────────────────────────────────────────────────────────

    public static void setDriver(WebDriver driver) { driverHolder.set(driver); }
    public static WebDriver getDriver()            { return driverHolder.get(); }
    public static void removeDriver()              { driverHolder.remove(); }

    // ── Global config ─────────────────────────────────────────────────────────

    public static void setConfig(ReporterConfig cfg) { config = cfg; }
    public static ReporterConfig getConfig()          { return config; }

    // ── Current test (used internally by listeners) ───────────────────────────

    public static void setCurrentTest(TestCaseReport test) { currentTestHolder.set(test); }
    public static TestCaseReport getCurrentTest()          { return currentTestHolder.get(); }
    public static void removeCurrentTest()                 { currentTestHolder.remove(); }

    // ── Manual step logging API ───────────────────────────────────────────────

    /** Log a passed step with elapsed time since the last logStep call. */
    public static void logStep(String description) {
        addStep(description, StepStatus.PASSED);
    }

    /** Log a failed step (e.g. caught exception in a step helper). */
    public static void logStepFailed(String description) {
        addStep(description, StepStatus.FAILED);
    }

    private static void addStep(String description, StepStatus status) {
        TestCaseReport test = getCurrentTest();
        if (test == null) return;

        Instant now      = Instant.now();
        Long    prev     = stepStartHolder.get();
        long    durationMs = prev != null ? (System.currentTimeMillis() - prev) : 0;
        stepStartHolder.set(System.currentTimeMillis());

        test.addStep(new StepRecord(description, now, status, durationMs));
    }
}
