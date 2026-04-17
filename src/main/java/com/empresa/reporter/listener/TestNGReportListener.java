package com.empresa.reporter.listener;

import com.empresa.reporter.config.ReporterConfig;
import com.empresa.reporter.core.ReportContext;
import com.empresa.reporter.core.ScreenshotCapture;
import com.empresa.reporter.model.TestCaseReport;
import com.empresa.reporter.model.TestStatus;
import com.empresa.reporter.model.TestSuiteReport;
import com.empresa.reporter.reporter.HtmlReporter;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TestNG listener. Register via {@code @Listeners(TestNGReportListener.class)}
 * on your test class, or in testng.xml under {@code <listeners>}.
 */
public class TestNGReportListener implements ITestListener, ISuiteListener {

    private static final Logger log = LoggerFactory.getLogger(TestNGReportListener.class);

    private final List<TestCaseReport> testCases = Collections.synchronizedList(new ArrayList<>());
    private volatile long suiteStartMillis;
    private volatile String suiteName = "Test Suite";

    // ── ISuiteListener ────────────────────────────────────────────────────────

    @Override
    public void onStart(ISuite suite) {
        suiteStartMillis = System.currentTimeMillis();
        suiteName = suite.getName();
        testCases.clear();
        log.info("[selenium-reporter] Suite started: {}", suiteName);
    }

    @Override
    public void onFinish(ISuite suite) {
        long suiteEnd = System.currentTimeMillis();

        TestSuiteReport report = new TestSuiteReport();
        report.setSuiteName(suiteName);
        report.setProjectName(ReportContext.getConfig().getProjectName());
        report.setStartTime(Instant.ofEpochMilli(suiteStartMillis));
        report.setEndTime(Instant.ofEpochMilli(suiteEnd));
        report.setTotalDurationMs(suiteEnd - suiteStartMillis);
        report.setEnvironment(System.getProperty("os.name") + " / Java " + System.getProperty("java.version"));
        report.setTestCases(new ArrayList<>(testCases));

        new HtmlReporter(ReportContext.getConfig()).generate(report);
    }

    // ── ITestListener ─────────────────────────────────────────────────────────

    @Override
    public void onTestStart(ITestResult result) {
        TestCaseReport tcr = new TestCaseReport();
        tcr.setTestName(result.getName());
        tcr.setClassName(result.getTestClass().getName());
        tcr.setMethodName(result.getMethod().getMethodName());
        tcr.setStartTime(Instant.ofEpochMilli(result.getStartMillis()));
        ReportContext.setCurrentTest(tcr);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        finishTest(result, TestStatus.PASSED);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        finishTest(result, TestStatus.FAILED);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        finishTest(result, TestStatus.SKIPPED);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        finishTest(result, TestStatus.FAILED);
    }

    @Override
    public void onStart(ITestContext context) {}

    @Override
    public void onFinish(ITestContext context) {}

    // ── helpers ───────────────────────────────────────────────────────────────

    private void finishTest(ITestResult result, TestStatus status) {
        TestCaseReport tcr = ReportContext.getCurrentTest();
        if (tcr == null) {
            tcr = new TestCaseReport();
            tcr.setTestName(result.getName());
            tcr.setClassName(result.getTestClass().getName());
            tcr.setMethodName(result.getMethod().getMethodName());
            tcr.setStartTime(Instant.ofEpochMilli(result.getStartMillis()));
        }

        tcr.setEndTime(Instant.ofEpochMilli(result.getEndMillis()));
        tcr.setDurationMs(result.getEndMillis() - result.getStartMillis());
        tcr.setStatus(status);

        if (result.getThrowable() != null) {
            tcr.setExceptionMessage(result.getThrowable().getMessage());
            tcr.setStackTrace(stackTraceOf(result.getThrowable()));
        }

        captureScreenshot(tcr, status);
        captureBrowserInfo(tcr);

        testCases.add(tcr);
        ReportContext.removeCurrentTest();
    }

    private void captureScreenshot(TestCaseReport tcr, TestStatus status) {
        ReporterConfig cfg = ReportContext.getConfig();
        WebDriver driver = ReportContext.getDriver();
        if (driver == null) return;

        boolean needed = (status == TestStatus.FAILED && cfg.isCaptureScreenshotOnFailure())
                || (status == TestStatus.PASSED && cfg.isCaptureScreenshotOnSuccess());
        if (needed) {
            tcr.setScreenshotBase64(ScreenshotCapture.capture(driver));
        }
    }

    private void captureBrowserInfo(TestCaseReport tcr) {
        WebDriver driver = ReportContext.getDriver();
        if (!(driver instanceof RemoteWebDriver)) return;
        try {
            Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
            tcr.setBrowserInfo(caps.getBrowserName() + " " + caps.getBrowserVersion());
        } catch (Exception ignored) {}
    }

    private String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
