# Architecture

This document explains the design decisions that are not obvious from reading the code.
It is aimed at contributors and anyone integrating the library into a custom test framework.

---

## Why screenshots are captured in `TestStepFinished`, not `TestCaseFinished`

The Cucumber event lifecycle for a failing scenario is:

```
TestCaseStarted
  TestStepStarted  (step 1)
  TestStepFinished (step 1 — PASSED)
  TestStepStarted  (step 2)
  TestStepFinished (step 2 — FAILED)   ← driver is still alive here
  TestStepStarted  (@After hook)
  TestStepFinished (@After hook — driver.quit() was called inside)
TestCaseFinished                        ← driver is already closed
```

By the time `TestCaseFinished` fires, the `@After` hook has already called `driver.quit()`.
Any screenshot attempt at that point returns null or throws a `SessionNotCreatedException`.

`CucumberReportPlugin` therefore registers a `TestStepFinished` handler and captures the
screenshot there, while the driver session is still open.
The `TestCaseFinished` handler only adds a **fallback** for failures that originate inside
`@Before` hooks, which are not preceded by a `TestStepFinished` event.

**For TestNG and JUnit 5** this problem does not arise: `@AfterMethod` / `@AfterEach` runs
after the extension's `afterEach()` callback, so the driver is still alive when the listener
finishes processing the test result.

---

## ThreadLocal WebDriver via `ReportContext`

`ReportContext` stores the `WebDriver` instance in a `ThreadLocal<WebDriver>`.

```
Thread A ──► ReportContext.setDriver(driverA) ──► screenshot uses driverA
Thread B ──► ReportContext.setDriver(driverB) ──► screenshot uses driverB
```

This is mandatory for parallel test execution: each thread runs its own scenario
against its own browser session, and the reporter must capture from the correct one.

**Required call sequence in every test setup/teardown:**

```java
// @Before / @BeforeMethod / @BeforeEach
ReportContext.setDriver(driver);   // bind this thread's driver

// @After / @AfterMethod / @AfterEach
ReportContext.removeDriver();      // unbind BEFORE driver.quit()
driver.quit();
```

Calling `removeDriver()` **after** `driver.quit()` is safe but unnecessary.
Calling it **before** is the correct order: it prevents the reporter from attempting
a screenshot on a closed session in any edge-case code path that runs between the two calls.

Failing to call `removeDriver()` at all causes a `ThreadLocal` memory leak in long-running
processes (e.g. application servers running tests via an embedded engine).

---

## History format

Each run is persisted as a single JSON file in `{outputDir}/history/`.
The filename encodes the run's start time in epoch milliseconds, which makes
files naturally sortable by name without parsing their contents:

```
history/
├── run_1713450735000.json
├── run_1713537135000.json
└── run_1713623535000.json
```

### JSON schema

```json
{
  "runTimeEpoch": 1713450735000,
  "suiteName":   "Smoke Tests",
  "total":       12,
  "passed":      10,
  "failed":      2,
  "skipped":     0,
  "durationMs":  45230,
  "buildUrl":    "https://ci.example.com/builds/42",
  "testOutcomes": {
    "Login with valid credentials":   "PASSED",
    "Login with invalid credentials": "FAILED",
    "Add item to cart":               "PASSED"
  }
}
```

| Field | Type | Description |
|---|---|---|
| `runTimeEpoch` | `long` | Unix timestamp (ms) of the suite start |
| `suiteName` | `String` | Value of `TestSuiteReport.suiteName` |
| `total` | `int` | Total number of test cases |
| `passed` | `int` | Number of PASSED tests |
| `failed` | `int` | Number of FAILED tests |
| `skipped` | `int` | Number of SKIPPED tests |
| `durationMs` | `long` | Total wall-clock duration of the suite in milliseconds |
| `buildUrl` | `String` \| `null` | Link to the CI build, if available |
| `testOutcomes` | `Map<String, String>` | Per-test result: key is `testName`, value is `"PASSED"`, `"FAILED"`, or `"SKIPPED"` |

### Flaky detection logic

`FlakiDetector.detect(testName, outcomes)` requires at least **3 historical data points**
before issuing a verdict. With fewer runs every test is classified as `NEW`.

A test is classified `FLAKY` if its outcome list contains at least one `true` (PASSED)
and at least one `false` (FAILED). Otherwise it is `STABLE`.

### Third-party tooling

The JSON format is intentionally simple so that external tools can produce or consume it.
To inject history from another system, write correctly formatted `run_*.json` files
into the `history/` folder before running the tests.
`HistoryManager` reads all files whose names match `run_*.json` in lexicographic order,
which corresponds to chronological order given the epoch-millisecond filename prefix.

---

## Package structure

```
com.empresa.reporter
├── config/       ReporterConfig (immutable, builder pattern)
├── core/         ReportContext (ThreadLocals), ScreenshotCapture,
│                 CiBuildInfo (CI env-var detection), FailureClassifier
├── history/      HistoryManager (JSON read/write/prune), FlakiDetector
├── listener/     TestNGReportListener, JUnit5ReportExtension, CucumberReportPlugin
├── model/        TestSuiteReport, TestCaseReport, TestStatus, StepRecord,
│                 StepStatus, FlakiStatus, FailureCategory, TestRunSummary
├── reporter/     HtmlReporter (Thymeleaf orchestration)
└── webhook/      WebhookNotifier, WebhookType
```

`HtmlReporter.generate()` is the central orchestration point:

1. Detects CI build info from environment variables.
2. Loads previous run outcomes from `HistoryManager`.
3. Sets `FlakiStatus` and `recentOutcomes` on each `TestCaseReport`.
4. Sets `FailureCategory` on each failed `TestCaseReport`.
5. Saves the current run to `HistoryManager`.
6. Renders the Thymeleaf template to a `.html` file.
7. Fires the webhook (if configured).
