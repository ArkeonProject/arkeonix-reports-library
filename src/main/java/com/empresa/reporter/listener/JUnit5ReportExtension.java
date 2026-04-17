package com.empresa.reporter.listener;

import com.empresa.reporter.config.ReporterConfig;
import com.empresa.reporter.core.ReportContext;
import com.empresa.reporter.core.ScreenshotCapture;
import com.empresa.reporter.model.TestCaseReport;
import com.empresa.reporter.model.TestStatus;
import com.empresa.reporter.model.TestSuiteReport;
import com.empresa.reporter.reporter.HtmlReporter;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JUnit 5 extension. Register via {@code @ExtendWith(JUnit5ReportExtension.class)}.
 * For a suite-level report across all test classes, declare it as a static field:
 * <pre>
 *   {@literal @}RegisterExtension
 *   static JUnit5ReportExtension reporter = new JUnit5ReportExtension();
 * </pre>
 */
public class JUnit5ReportExtension implements BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private static final Logger log = LoggerFactory.getLogger(JUnit5ReportExtension.class);
    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(JUnit5ReportExtension.class);

    private final List<TestCaseReport> testCases = Collections.synchronizedList(new ArrayList<>());
    private final long suiteStartMillis = System.currentTimeMillis();

    @Override
    public void beforeEach(ExtensionContext context) {
        TestCaseReport tcr = new TestCaseReport();
        tcr.setTestName(context.getDisplayName());
        tcr.setClassName(context.getTestClass().map(Class::getName).orElse("Unknown"));
        tcr.setMethodName(context.getTestMethod().map(m -> m.getName()).orElse("unknown"));
        tcr.setStartTime(Instant.now());

        context.getStore(NS).put("startMs", System.currentTimeMillis());
        context.getStore(NS).put("tcr", tcr);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        TestCaseReport tcr = context.getStore(NS).get("tcr", TestCaseReport.class);
        if (tcr == null) return;

        long startMs = context.getStore(NS).getOrDefault("startMs", Long.class, System.currentTimeMillis());
        long endMs = System.currentTimeMillis();
        tcr.setEndTime(Instant.now());
        tcr.setDurationMs(endMs - startMs);

        TestStatus status = context.getExecutionException().isPresent() ? TestStatus.FAILED : TestStatus.PASSED;
        tcr.setStatus(status);

        context.getExecutionException().ifPresent(t -> {
            tcr.setExceptionMessage(t.getMessage());
            tcr.setStackTrace(stackTraceOf(t));
        });

        ReporterConfig cfg = ReportContext.getConfig();
        WebDriver driver = ReportContext.getDriver();
        if (driver != null) {
            boolean needed = (status == TestStatus.FAILED && cfg.isCaptureScreenshotOnFailure())
                    || (status == TestStatus.PASSED && cfg.isCaptureScreenshotOnSuccess());
            if (needed) {
                tcr.setScreenshotBase64(ScreenshotCapture.capture(driver));
            }
            if (driver instanceof RemoteWebDriver) {
                try {
                    Capabilities caps = ((RemoteWebDriver) driver).getCapabilities();
                    tcr.setBrowserInfo(caps.getBrowserName() + " " + caps.getBrowserVersion());
                } catch (Exception ignored) {}
            }
        }

        testCases.add(tcr);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (testCases.isEmpty()) return;

        long suiteEnd = System.currentTimeMillis();
        ReporterConfig cfg = ReportContext.getConfig();

        TestSuiteReport report = new TestSuiteReport();
        report.setSuiteName(context.getDisplayName());
        report.setProjectName(cfg.getProjectName());
        report.setStartTime(Instant.ofEpochMilli(suiteStartMillis));
        report.setEndTime(Instant.ofEpochMilli(suiteEnd));
        report.setTotalDurationMs(suiteEnd - suiteStartMillis);
        report.setEnvironment(System.getProperty("os.name") + " / Java " + System.getProperty("java.version"));
        report.setTestCases(new ArrayList<>(testCases));

        new HtmlReporter(cfg).generate(report);
        testCases.clear();
    }

    private String stackTraceOf(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
