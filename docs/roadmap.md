# Roadmap

Planned features in priority order.
Items marked **✅ Implemented** are already available in the current version.

---

## 1. `selenium-reporter:merge` command

**Status:** Planned

A Maven plugin goal (or standalone CLI) that reads multiple `history/` folders —
collected from different CI build artifacts — and produces a single consolidated
`dashboard.html` covering the full historical window.

This is the missing piece that makes the [periodic cron dashboard](ci-integration.md#periodic-historical-dashboard-cron-job)
flow fully automatic. Without it the cron job must be assembled manually.

**Planned interface:**

```bash
mvn selenium-reporter:merge \
  -DhistoryDir=aggregated-history/ \
  -Doutput=dashboard.html
```

---

## 2. JSON output (`results.json`)

**Status:** Planned

A machine-readable summary written alongside the HTML report after every run:

```json
{
  "status":      "FAILED",
  "total":       12,
  "passed":      10,
  "failed":      2,
  "skipped":     0,
  "passRatePct": 83.3,
  "durationMs":  45230,
  "failedTests": ["Login with invalid credentials", "Add item to cart"]
}
```

This enables CI scripts to check for failures without parsing HTML:

```bash
if jq -e '.failed > 0' test-reports/results.json; then
  echo "Tests failed — blocking merge"
  exit 1
fi
```

Also required by the cron dashboard workflow to decide whether to post to Jira.

---

## 3. S3 / GCS history backend

**Status:** Planned

Write and read `history/` directly from an object storage bucket instead of the local filesystem.
This eliminates the artifact upload/download steps from the CI pipeline entirely —
every build reads from and writes to the same bucket automatically.

**Planned configuration:**

```java
ReporterConfig.builder()
    .historyBackend("s3")
    .historyS3Bucket("my-org-test-reports")
    .historyS3Prefix("my-project/history/")
    .build();
```

AWS credentials would be picked up from the standard credential chain
(`AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` env vars, IAM role, `~/.aws/credentials`).
A GCS equivalent using Application Default Credentials is also planned.

---

## 4. Step log ✅ Implemented

**Status:** ✅ Available in the current version

Per-step log with timestamp and duration, shown in the detail panel of each test row.

- **Cucumber:** steps are recorded automatically from `TestStepStarted` / `TestStepFinished` events.
- **TestNG / JUnit 5:** use `ReportContext.logStep("description")` inside test methods.

See [configuration.md](configuration.md#manual-step-logging) for usage.

---

## 5. Flaky test detection ✅ Implemented

**Status:** ✅ Available in the current version

Tests that alternate between PASS and FAIL across runs are flagged as **FLAKY**.
Requires at least 3 historical data points before a verdict is issued.
Results appear as a flaky alert banner, a dedicated summary card, and per-row badges.

Depends on [history being persisted across CI builds](ci-integration.md).

---

## 6. Retry tracking ✅ Implemented

**Status:** ✅ Available in the current version

`TestCaseReport` exposes `retryCount` and `passedOnRetry` fields.
A **retry badge** is shown on any test that was re-run.

Integration with TestNG's `IRetryAnalyzer` to populate these fields automatically
is still pending; they can be set manually in the meantime.

---

## 7. Webhook notifications ✅ Implemented

**Status:** ✅ Available in the current version

Configurable POST to Slack, Microsoft Teams, or any HTTP endpoint on suite completion.
Supports `notifyOnlyOnFailure` to suppress noise on all-green runs.

See [configuration.md](configuration.md#full-option-reference) for configuration options.
