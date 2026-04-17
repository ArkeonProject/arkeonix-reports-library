# Configuration reference

All options are set through `ReporterConfig.builder()` and passed to `ReportContext.setConfig()` once,
typically in a `@BeforeClass` / `@BeforeAll` / `@BeforeSuite` method.

```java
ReportContext.setConfig(ReporterConfig.builder()
    .projectName("My App")
    .outputDir("./test-reports")
    .captureScreenshotOnFailure(true)
    .historyEnabled(true)
    .historyLimit(20)
    .webhookUrl("https://hooks.slack.com/services/...")
    .webhookType(WebhookType.SLACK)
    .notifyOnlyOnFailure(true)
    .build());
```

---

## Full option reference

| Option | Type | Default | Description |
|---|---|---|---|
| `projectName` | `String` | `"Selenium Test Suite"` | Project name shown in the report header |
| `outputDir` | `String` | `"./test-reports"` | Directory where HTML reports and the `history/` subfolder are written |
| `captureScreenshotOnFailure` | `boolean` | `true` | Capture a screenshot when a test fails and embed it in the report |
| `captureScreenshotOnSuccess` | `boolean` | `false` | Capture a screenshot when a test passes |
| `openBrowserAfterReport` | `boolean` | `false` | Open the generated HTML report in the default browser after the run |
| `historyEnabled` | `boolean` | `true` | Persist run data to `{outputDir}/history/` and use it for flaky detection and the trend chart |
| `historyLimit` | `int` | `20` | Maximum number of past runs to retain on disk; oldest files are pruned automatically |
| `webhookUrl` | `String` | `null` | HTTP endpoint to POST a notification to after every run. Leave `null` to disable |
| `webhookType` | `WebhookType` | `GENERIC_JSON` | Payload format: `SLACK`, `TEAMS`, or `GENERIC_JSON` |
| `notifyOnlyOnFailure` | `boolean` | `true` | Skip the webhook call when all tests pass |
| `ciBuildInfo` | `CiBuildInfo` | auto-detected | Override the CI metadata shown in the report header bar. Normally auto-detected from environment variables |

---

## Webhook types

| `WebhookType` | Payload format | Compatible with |
|---|---|---|
| `SLACK` | Block Kit `attachments` with fields | Slack Incoming Webhooks |
| `TEAMS` | Office 365 Connector `MessageCard` | Microsoft Teams Incoming Webhooks |
| `GENERIC_JSON` | Flat JSON object | Any HTTP endpoint |

### GENERIC_JSON payload shape

```json
{
  "project":     "My App",
  "suite":       "Smoke Tests",
  "status":      "FAILED",
  "total":       12,
  "passed":      10,
  "failed":      2,
  "skipped":     0,
  "passRatePct": 83.3,
  "durationMs":  45230
}
```

---

## CI build info auto-detection

`CiBuildInfo.detect()` is called automatically by `HtmlReporter` unless you supply an override.
When a supported CI environment is detected a build info bar is rendered at the top of the report.

| CI system | Environment variables read |
|---|---|
| Jenkins | `JENKINS_URL`, `BUILD_URL`, `BUILD_NUMBER`, `GIT_BRANCH`, `GIT_COMMIT` |
| GitHub Actions | `GITHUB_ACTIONS`, `GITHUB_RUN_NUMBER`, `GITHUB_RUN_ID`, `GITHUB_REPOSITORY`, `GITHUB_REF_NAME`, `GITHUB_SHA` |
| GitLab CI | `GITLAB_CI`, `CI_PIPELINE_IID`, `CI_PIPELINE_URL`, `CI_COMMIT_REF_NAME`, `CI_COMMIT_SHA` |

Manual override:

```java
ReportContext.setConfig(ReporterConfig.builder()
    .ciBuildInfo(CiBuildInfo.of(
        "My CI",
        "42",
        "https://ci.example.com/builds/42",
        "main",
        "abc1234"
    ))
    .build());
```

---

## Manual step logging

For TestNG and JUnit 5 tests you can add entries to the step log from inside the test code:

```java
// Logs a PASSED step with elapsed time since the previous call
ReportContext.logStep("Navigate to checkout");

// Logs a FAILED step (e.g. inside a catch block)
ReportContext.logStepFailed("Verify order total");
```

Cucumber users do not need to call these methods — Gherkin steps are recorded automatically.
