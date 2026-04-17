# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`selenium-reporter` — Maven library that auto-generates professional single-file HTML reports for Selenium WebDriver test suites. Supports TestNG (primary) and JUnit 5.

## Stack

- Java 11 (target), compiled with Maven
- Selenium WebDriver 4.15.0
- TestNG 7.8.0 (`ITestListener` + `ISuiteListener`)
- JUnit Jupiter 5.10.0
- Thymeleaf 3.1.2 (standalone, `ClassLoaderTemplateResolver`)
- Chart.js 4.4.0 via CDN (embedded in report)
- WebDriverManager 5.6.3 (test scope only, auto-manages ChromeDriver)
- SLF4J Simple for logging

## Commands

```bash
# Compile and install to local Maven repo (no browser needed)
mvn clean install

# Run the demo Selenium tests (requires Chrome installed)
mvn test -Prun-selenium-tests
```

The generated report lands at `./test-reports/report_YYYYMMDD_HHmmss.html`.

## Architecture

```text
config/ReporterConfig   — immutable builder-pattern config
model/                  — TestSuiteReport, TestCaseReport, TestStatus (enum)
core/ReportContext      — ThreadLocal<WebDriver> + global ReporterConfig; call setDriver() in @BeforeMethod
core/ScreenshotCapture  — casts WebDriver to TakesScreenshot → Base64
listener/TestNGReportListener       — ITestListener + ISuiteListener; generates report in onFinish(ISuite)
listener/JUnit5ReportExtension      — BeforeEach/AfterEach/AfterAll; generates report per test class in afterAll()
reporter/HtmlReporter   — loads Thymeleaf template from classpath, writes report_<ts>.html
resources/templates/report.html    — single-file HTML: CSS vars dark/light, Chart.js charts, sortable table, expandable failure rows
```

Key data flow:

1. `ReportContext.setDriver(driver)` called in test setup — stores driver in ThreadLocal
2. Listener catches pass/fail/skip events, captures screenshot + browser info from driver
3. `onFinish(ISuite)` builds `TestSuiteReport` and calls `HtmlReporter.generate()`
4. Thymeleaf inlines test data into the report template via `th:inline="javascript"` for chart data

## Template notes

- Data is passed to JS via `/*[[${variable}]]*/` inline syntax (Thymeleaf JavaScript inline mode)
- `th:block th:each` is used for paired test row + detail row in the table
- Stack traces use `th:text` inside `<pre>` to HTML-escape while preserving whitespace
- Screenshots embedded as `data:image/png;base64,...` in `<img src>`

## Errores aprendidos

- `Capabilities` is `org.openqa.selenium.Capabilities`, NOT `org.openqa.selenium.remote.Capabilities` in Selenium 4.x
- Methods shared between `core` and `listener` packages must be `public`, not package-private
