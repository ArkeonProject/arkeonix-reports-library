package com.empresa.reporter.listener;

import com.empresa.reporter.config.ReporterConfig;
import com.empresa.reporter.core.ReportContext;
import com.empresa.reporter.core.ScreenshotCapture;
import com.empresa.reporter.model.*;
import com.empresa.reporter.reporter.HtmlReporter;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cucumber plugin that generates an HTML report after the full test run.
 *
 * Register in your runner class:
 * <pre>
 *   {@literal @}CucumberOptions(plugin = "com.empresa.reporter.listener.CucumberReportPlugin")
 * </pre>
 *
 * In your Cucumber Hooks:
 * <pre>
 *   {@literal @}Before
 *   public void setUp() {
 *       driver = new ChromeDriver();
 *       ReportContext.setDriver(driver);
 *   }
 *   {@literal @}After
 *   public void tearDown() {
 *       ReportContext.removeDriver();
 *       driver.quit();
 *   }
 * </pre>
 */
public class CucumberReportPlugin implements ConcurrentEventListener {

    private static final Logger log = LoggerFactory.getLogger(CucumberReportPlugin.class);

    private final List<TestCaseReport>           testCases      = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Long>              stepStartTimes = new ConcurrentHashMap<>();
    private volatile long suiteStartMillis;

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class,   this::onRunStarted);
        publisher.registerHandlerFor(TestCaseStarted.class,  this::onCaseStarted);
        publisher.registerHandlerFor(TestStepStarted.class,  this::onStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::onStepFinished);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class,  this::onRunFinished);
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    private void onRunStarted(TestRunStarted event) {
        suiteStartMillis = System.currentTimeMillis();
        testCases.clear();
        log.info("[selenium-reporter] Cucumber run started");
    }

    private void onCaseStarted(TestCaseStarted event) {
        TestCaseReport tcr = new TestCaseReport();
        tcr.setTestName(event.getTestCase().getName());
        tcr.setClassName(featureNameFrom(event.getTestCase().getUri().toString()));
        tcr.setMethodName(event.getTestCase().getName());
        tcr.setStartTime(Instant.now());
        ReportContext.setCurrentTest(tcr);
    }

    private void onStepStarted(TestStepStarted event) {
        if (!(event.getTestStep() instanceof PickleStepTestStep)) return;
        PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
        String key = stepKey(step);
        stepStartTimes.put(key, System.currentTimeMillis());
    }

    /**
     * Bug 1 fix: screenshot captured here while driver is still alive.
     * Bug 2 fix: structured failure info extracted from step result.
     * Step log: Gherkin steps recorded with timing and status.
     */
    private void onStepFinished(TestStepFinished event) {
        TestCaseReport tcr = ReportContext.getCurrentTest();
        if (tcr == null) return;

        // Record step in step log (only Gherkin steps, not hook steps)
        if (event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
            String key = stepKey(step);
            Long startMs = stepStartTimes.remove(key);
            long durationMs = startMs != null ? (System.currentTimeMillis() - startMs) : 0;

            String keyword = step.getStep().getKeyword().trim();
            String text    = step.getStep().getText();
            String desc    = keyword + " " + text;

            StepStatus stepStatus = mapStepStatus(event.getResult().getStatus());
            tcr.addStep(new StepRecord(desc, Instant.now(), stepStatus, durationMs));
        }

        if (event.getResult().getStatus() != Status.FAILED) return;

        // Bug 1: screenshot while driver is still alive
        ReporterConfig cfg = ReportContext.getConfig();
        if (cfg.isCaptureScreenshotOnFailure() && tcr.getScreenshotBase64() == null) {
            WebDriver driver = ReportContext.getDriver();
            if (driver != null) {
                tcr.setScreenshotBase64(ScreenshotCapture.capture(driver));
            }
        }

        // Bug 2: structured failure info
        Throwable cause = event.getResult().getError();
        if (cause != null && tcr.getExceptionMessage() == null) {
            tcr.setExceptionType(cause.getClass().getSimpleName());
            tcr.setExceptionMessage(cause.getMessage());
            tcr.setStackTrace(stackTraceOf(cause));

            StackTraceElement origin = Arrays.stream(cause.getStackTrace())
                    .filter(f -> !f.getClassName().startsWith("org.junit")
                              && !f.getClassName().startsWith("io.cucumber")
                              && !f.getClassName().startsWith("sun.")
                              && !f.getClassName().startsWith("java.")
                              && !f.getClassName().startsWith("jdk."))
                    .findFirst()
                    .orElse(cause.getStackTrace().length > 0 ? cause.getStackTrace()[0] : null);

            if (origin != null) {
                String simpleClass = origin.getClassName();
                int dot = simpleClass.lastIndexOf('.');
                if (dot >= 0) simpleClass = simpleClass.substring(dot + 1);
                tcr.setFailureOrigin(simpleClass + "." + origin.getMethodName()
                        + "(" + origin.getFileName() + ":" + origin.getLineNumber() + ")");
            }
        }
    }

    private void onCaseFinished(TestCaseFinished event) {
        TestCaseReport tcr = ReportContext.getCurrentTest();
        if (tcr == null) {
            tcr = new TestCaseReport();
            tcr.setTestName(event.getTestCase().getName());
            tcr.setClassName(featureNameFrom(event.getTestCase().getUri().toString()));
            tcr.setStartTime(Instant.now());
        }

        long endMs   = System.currentTimeMillis();
        long startMs = tcr.getStartTime() != null ? tcr.getStartTime().toEpochMilli() : endMs;
        tcr.setEndTime(Instant.now());
        tcr.setDurationMs(endMs - startMs);

        Result result = event.getResult();
        tcr.setStatus(mapStatus(result.getStatus()));

        // Fallback: set exception info if not captured in onStepFinished (e.g. @Before hook failures)
        if (tcr.getExceptionMessage() == null && result.getError() != null) {
            Throwable cause = result.getError();
            tcr.setExceptionType(cause.getClass().getSimpleName());
            tcr.setExceptionMessage(cause.getMessage());
            tcr.setStackTrace(stackTraceOf(cause));
        }

        // Screenshot on success
        ReporterConfig cfg = ReportContext.getConfig();
        WebDriver driver   = ReportContext.getDriver();
        if (tcr.getStatus() == TestStatus.PASSED && cfg.isCaptureScreenshotOnSuccess()
                && tcr.getScreenshotBase64() == null && driver != null) {
            tcr.setScreenshotBase64(ScreenshotCapture.capture(driver));
        }

        captureBrowserInfo(tcr);
        testCases.add(tcr);
        ReportContext.removeCurrentTest();
    }

    private void onRunFinished(TestRunFinished event) {
        long suiteEnd = System.currentTimeMillis();
        ReporterConfig config = ReportContext.getConfig();

        TestSuiteReport report = new TestSuiteReport();
        report.setSuiteName(System.getProperty("cucumber.suite.name", "Cucumber Suite"));
        report.setProjectName(config.getProjectName());
        report.setStartTime(Instant.ofEpochMilli(suiteStartMillis));
        report.setEndTime(Instant.ofEpochMilli(suiteEnd));
        report.setTotalDurationMs(suiteEnd - suiteStartMillis);
        report.setEnvironment(System.getProperty("os.name") + " / Java " + System.getProperty("java.version"));
        report.setTestCases(new ArrayList<>(testCases));

        new HtmlReporter(config).generate(report);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TestStatus mapStatus(Status cucumberStatus) {
        switch (cucumberStatus) {
            case PASSED:  return TestStatus.PASSED;
            case FAILED:  return TestStatus.FAILED;
            default:      return TestStatus.SKIPPED;
        }
    }

    private StepStatus mapStepStatus(Status status) {
        switch (status) {
            case PASSED:  return StepStatus.PASSED;
            case FAILED:  return StepStatus.FAILED;
            case PENDING: return StepStatus.PENDING;
            default:      return StepStatus.SKIPPED;
        }
    }

    private String stepKey(PickleStepTestStep step) {
        return Thread.currentThread().getId() + "_" + step.getId();
    }

    private void captureBrowserInfo(TestCaseReport tcr) {
        WebDriver driver = ReportContext.getDriver();
        if (!(driver instanceof RemoteWebDriver)) return;
        try {
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            tcr.setBrowserInfo(caps.getBrowserName() + " " + caps.getBrowserVersion());
        } catch (Exception ignored) {}
    }

    /** "classpath:features/login/Login.feature" → "login/Login" */
    private String featureNameFrom(String uri) {
        String name = uri;
        int colon = name.lastIndexOf(':');
        if (colon >= 0) name = name.substring(colon + 1);
        if (name.endsWith(".feature")) name = name.substring(0, name.length() - 8);
        if (name.startsWith("/features/")) name = name.substring(10);
        return name;
    }

    private String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
