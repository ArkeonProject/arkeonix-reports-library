# selenium-reporter

Automatic professional HTML report generator for Selenium WebDriver tests.  
Supports **TestNG**, **JUnit 5**, and **Cucumber** out of the box.

---

## Features

- Single self-contained HTML file per suite run
- Donut chart (pass/fail/skip ratio) + duration bar chart + **pass rate trend line chart**
- Expandable failure rows with exception type, origin frame, stack trace, and inline screenshot
- Base64-embedded screenshots — no external files needed
- **Run history & flaky detection** — marks tests that alternate between PASS and FAIL across runs
- **Step log with timestamps** — per-step duration and status inside each test's detail row
- **Webhook notifications** — Slack, Microsoft Teams, or generic JSON
- **CI build info bar** — auto-detected from Jenkins, GitHub Actions, and GitLab CI
- **Failure classification** — Assertion / Element Not Found / Timeout / Stale Element badges
- Dark / light theme toggle, sortable columns
- Zero configuration to get started

---

## Quick start

### 1. Add as a local Maven dependency

```bash
mvn clean install -DskipTests
```

Then in your project's `pom.xml`:
```xml
<dependency>
    <groupId>com.empresa</groupId>
    <artifactId>selenium-reporter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## TestNG integration

```java
@Listeners(TestNGReportListener.class)
public class LoginTest {

    private WebDriver driver;

    @BeforeClass
    public static void configReporter() {
        ReportContext.setConfig(ReporterConfig.builder()
            .projectName("My App")
            .outputDir("./test-reports")
            .captureScreenshotOnFailure(true)
            .build());
    }

    @BeforeMethod
    public void setUp() {
        driver = new ChromeDriver();
        ReportContext.setDriver(driver);   // required for screenshots
    }

    @AfterMethod
    public void tearDown() {
        ReportContext.removeDriver();
        driver.quit();
    }

    @Test
    public void testLoginSuccess() {
        ReportContext.logStep("Navigate to login page");
        driver.get("https://example.com/login");

        ReportContext.logStep("Submit credentials");
        // ... interactions ...

        ReportContext.logStep("Verify dashboard loaded");
        // ... assertions ...
    }
}
```

> Register via `testng.xml` instead of `@Listeners` if preferred:
> ```xml
> <listeners>
>     <listener class-name="com.empresa.reporter.listener.TestNGReportListener"/>
> </listeners>
> ```

---

## JUnit 5 integration

```java
@ExtendWith(JUnit5ReportExtension.class)
public class LoginTest {

    private WebDriver driver;

    @BeforeAll
    static void configReporter() {
        ReportContext.setConfig(ReporterConfig.builder()
            .projectName("My App")
            .outputDir("./test-reports")
            .captureScreenshotOnFailure(true)
            .build());
    }

    @BeforeEach
    void setUp() {
        driver = new ChromeDriver();
        ReportContext.setDriver(driver);
    }

    @AfterEach
    void tearDown() {
        ReportContext.removeDriver();
        driver.quit();
    }

    @Test
    void testLogin() { /* ... */ }
}
```

For a **suite-level** report that aggregates results across multiple test classes:

```java
@RegisterExtension
static JUnit5ReportExtension reporter = new JUnit5ReportExtension();
```

---

## Cucumber integration

Register the plugin in your runner class or `junit-platform.properties`:

```java
@CucumberOptions(plugin = "com.empresa.reporter.listener.CucumberReportPlugin")
public class RunnerTest {}
```

```properties
# junit-platform.properties
cucumber.plugin=com.empresa.reporter.listener.CucumberReportPlugin
```

In your hooks, give the plugin access to the driver:

```java
@Before
public void setUp() {
    driver = new ChromeDriver();
    ReportContext.setDriver(driver);
}

@After
public void tearDown() {
    ReportContext.removeDriver();
    driver.quit();
}
```

Gherkin steps are recorded automatically — keyword, text, per-step duration, and pass/fail status all appear in the report's step log.

Override the suite name (defaults to `"Cucumber Suite"`):

```bash
mvn test -Dcucumber.suite.name="Smoke Tests"
```

---

## Step log (manual, TestNG / JUnit 5)

Call `ReportContext.logStep()` anywhere inside a test to add entries to the step log:

```java
@Test
public void checkoutFlow() {
    ReportContext.logStep("Add item to cart");
    cart.addItem("SKU-001");

    ReportContext.logStep("Proceed to checkout");
    checkout.open();

    ReportContext.logStep("Enter payment details");
    checkout.fillPayment(card);
}
```

Each call records the description, timestamp, and elapsed time since the previous step.  
Use `ReportContext.logStepFailed("description")` to mark a step as failed (e.g. inside a catch block).

---

## Run history & flaky detection

Enabled by default. After every run a compact JSON file is saved to `{outputDir}/history/`.  
On subsequent runs the library:

1. Compares each test's outcome against the last N runs.
2. Marks a test **FLAKY** if it has both PASSED and FAILED results in that window (minimum 3 runs required before a verdict is given).
3. Shows a **flaky alert banner** at the top of the report and a dedicated **Flaky** summary card.
4. Renders up to 5 colored history dots per test row.
5. Draws a **pass rate trend line chart** from the stored history.

History files are pruned automatically — only the most recent `historyLimit` runs are kept.

```java
ReporterConfig.builder()
    .historyEnabled(true)     // default: true
    .historyLimit(20)         // default: 20 runs
    .build();
```

---

## Webhook notifications

Send a notification to Slack, Microsoft Teams, or any HTTP endpoint after every run:

```java
ReporterConfig.builder()
    .webhookUrl("https://hooks.slack.com/services/T.../B.../...")
    .webhookType(WebhookType.SLACK)
    .notifyOnlyOnFailure(true)   // default: true — skip notification on all-green runs
    .build();
```

| `webhookType`        | Payload format                            |
|----------------------|-------------------------------------------|
| `SLACK`              | Slack Block Kit `attachments` + fields    |
| `TEAMS`              | Office 365 Connector `MessageCard`        |
| `GENERIC_JSON`       | Simple flat JSON with pass/fail counts    |

The notification is sent after the HTML file is written, so a webhook failure never blocks the report.

---

## CI / CD build info

When running inside a supported CI system, a build info bar is automatically shown at the top of the report with the build number, branch, commit SHA, and a link back to the pipeline run.

| CI system       | Detected via                             |
|-----------------|------------------------------------------|
| Jenkins         | `JENKINS_URL` or `BUILD_URL` env var     |
| GitHub Actions  | `GITHUB_ACTIONS=true` env var            |
| GitLab CI       | `GITLAB_CI` env var                      |

Override or supply values manually:

```java
ReporterConfig.builder()
    .ciBuildInfo(CiBuildInfo.of("My CI", "42", "https://ci.example.com/42", "main", "abc1234"))
    .build();
```

---

## Configuration reference

| Method                              | Default                    | Description                                      |
|-------------------------------------|----------------------------|--------------------------------------------------|
| `projectName(String)`               | `"Selenium Test Suite"`    | Shown in the report header                       |
| `outputDir(String)`                 | `"./test-reports"`         | Directory for HTML reports and history           |
| `captureScreenshotOnFailure(boolean)` | `true`                   | Screenshot on `FAILED` tests                     |
| `captureScreenshotOnSuccess(boolean)` | `false`                  | Screenshot on `PASSED` tests                     |
| `openBrowserAfterReport(boolean)`   | `false`                    | Auto-open report in browser after generation     |
| `historyEnabled(boolean)`           | `true`                     | Enable run history and flaky detection           |
| `historyLimit(int)`                 | `20`                       | Maximum number of past runs to retain            |
| `webhookUrl(String)`                | `null`                     | HTTP endpoint for post-run notification          |
| `webhookType(WebhookType)`          | `GENERIC_JSON`             | Payload format: `SLACK`, `TEAMS`, `GENERIC_JSON` |
| `notifyOnlyOnFailure(boolean)`      | `true`                     | Skip webhook when all tests pass                 |
| `ciBuildInfo(CiBuildInfo)`          | auto-detected              | Override CI metadata shown in the report bar     |

---

## Running the demo test

Requires **Google Chrome** installed.

```bash
mvn test -Prun-selenium-tests
```

The report is saved to `./test-reports/report_YYYYMMDD_HHmmss.html`.  
One test is intentionally failing to demonstrate screenshot capture and failure reporting.

---

## Output structure

```
./test-reports/
├── report_20260418_143215.html    ← self-contained report
└── history/
    ├── run_1713450735000.json     ← per-run history entry
    └── run_1713537135000.json
```

---

## How it works

1. `ReportContext.setDriver(driver)` stores the driver in a `ThreadLocal` — listeners pick it up from any thread.
2. The listener collects a `TestCaseReport` per test (timing, status, exception, steps, screenshot, browser info).
3. `HtmlReporter.generate()` orchestrates: loads history → detects flaky tests → classifies failures → renders the Thymeleaf template → saves history → fires the webhook.
4. The output is a fully self-contained HTML file: all CSS, JS, charts, and screenshots are inlined.
