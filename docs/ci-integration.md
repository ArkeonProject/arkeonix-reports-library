# CI / CD integration

This document explains how to keep run history alive across CI builds,
how to generate periodic dashboards, and how to route results to Jira and Slack.

---

## Local history (default)

Out of the box, `selenium-reporter` writes a compact JSON file to
`{outputDir}/history/` after every run.  
This works perfectly for local development and single-machine setups —
the trend chart and flaky-detection data accumulate automatically
with no extra configuration.

```
./test-reports/
├── report_20260418_143215.html
└── history/
    ├── run_1713450735000.json
    └── run_1713537135000.json
```

---

## CI/CD — Artifact-based history (recommended)

### The problem

Most CI systems start each build in a clean workspace.
The `history/` folder created during build N is discarded before build N+1 starts,
so flaky detection never sees more than one data point and the trend chart stays flat.

### The solution

Treat `history/` as a **persistent artifact**: upload it at the end of each build
and download it at the start of the next one.
The reporter will then read previous outcomes and produce accurate trend data.

---

### GitHub Actions

```yaml
name: Selenium Tests

on:
  push:
    branches: [main, develop]
  pull_request:

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      # Restore the history folder from the previous successful run.
      # continue-on-error: true so the first build (no artifact yet) does not fail.
      - name: Restore test history
        uses: actions/download-artifact@v4
        with:
          name: selenium-reporter-history
          path: test-reports/history
        continue-on-error: true

      - name: Run tests
        run: mvn test -Prun-selenium-tests

      # Always upload the report, even when tests fail.
      - name: Upload HTML report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: selenium-report-${{ github.run_number }}
          path: test-reports/report_*.html
          retention-days: 30

      # Overwrite the shared history artifact so the next build picks it up.
      - name: Save test history
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: selenium-reporter-history
          path: test-reports/history/
          retention-days: 90
          overwrite: true
```

> **How the overwrite works:** uploading an artifact with the same name (`selenium-reporter-history`)
> on every run replaces the previous version. The next build's download step always gets
> the latest accumulated history.

---

### Jenkins (Declarative Pipeline)

```groovy
pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Restore history') {
            steps {
                script {
                    // Copy history/ from the most recent build that archived it.
                    // Requires the Copy Artifact plugin.
                    copyArtifacts(
                        projectName: env.JOB_NAME,
                        filter: 'test-reports/history/**',
                        selector: lastSuccessful(),
                        optional: true   // first build has no artifact
                    )
                }
            }
        }

        stage('Run tests') {
            steps {
                sh 'mvn test -Prun-selenium-tests'
            }
        }
    }

    post {
        always {
            // Archive both the HTML report and the history folder.
            archiveArtifacts artifacts: 'test-reports/**', allowEmptyArchive: true

            // Optional: render the HTML report in the Jenkins UI.
            publishHTML(target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'test-reports',
                reportFiles: 'report_*.html',
                reportName: 'Selenium Report'
            ])
        }
    }
}
```

> The **Copy Artifact plugin** must be installed in Jenkins.  
> Install it via *Manage Jenkins → Plugins → Available → Copy Artifact*.

---

## Periodic historical dashboard (cron job)

For long-running projects you may want a single consolidated dashboard
that aggregates data from many past builds rather than just the last N runs.

The workflow below:

1. Downloads history artifacts from recent builds.
2. Runs `selenium-reporter:merge` to produce a `dashboard.html`.
3. Checks whether the merged results contain failures.
4. If yes: attaches the dashboard to a Jira issue and sends a Slack/Teams alert.
5. If no: exits silently.

> **Note:** `selenium-reporter:merge` and the JSON output file (`results.json`)
> are **planned features** — see [roadmap.md](roadmap.md).
> The workflow below is complete except for those two steps,
> which are marked with a `# ROADMAP` comment.

### GitHub Actions — scheduled dashboard

```yaml
name: Weekly Test Dashboard

on:
  schedule:
    - cron: '0 8 * * 1'   # Every Monday at 08:00 UTC
  workflow_dispatch:        # Allow manual trigger

jobs:
  dashboard:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      # Download the shared history artifact accumulated by the test workflow.
      - name: Download test history
        uses: actions/download-artifact@v4
        with:
          name: selenium-reporter-history
          path: test-reports/history
        continue-on-error: true

      # ROADMAP: merge all history files into a single dashboard.html.
      # Command not yet available — see roadmap.md item 1.
      - name: Generate historical dashboard
        run: |
          mvn selenium-reporter:merge \
            -DhistoryDir=test-reports/history \
            -Doutput=test-reports/dashboard.html

      # ROADMAP: check the JSON output for failures.
      # results.json is not yet produced — see roadmap.md item 2.
      - name: Check for failures in history
        id: check
        run: |
          if jq -e '.failed > 0' test-reports/results.json > /dev/null 2>&1; then
            echo "has_failures=true"  >> "$GITHUB_OUTPUT"
          else
            echo "has_failures=false" >> "$GITHUB_OUTPUT"
          fi

      # Upload dashboard regardless of pass/fail.
      - name: Upload dashboard artifact
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: selenium-dashboard-${{ github.run_number }}
          path: test-reports/dashboard.html
          retention-days: 90

      # Attach the dashboard HTML to a Jira issue when failures exist.
      - name: Attach dashboard to Jira
        if: steps.check.outputs.has_failures == 'true'
        run: |
          curl --silent --fail \
            --request POST \
            --header "Authorization: Basic ${{ secrets.JIRA_AUTH }}" \
            --header "X-Atlassian-Token: no-check" \
            --form "file=@test-reports/dashboard.html" \
            "${{ vars.JIRA_BASE_URL }}/rest/api/3/issue/${{ vars.JIRA_ISSUE_KEY }}/attachments"

      # Send a Slack notification with the Jira issue link.
      - name: Notify Slack
        if: steps.check.outputs.has_failures == 'true'
        run: |
          curl --silent --fail \
            --request POST \
            --header "Content-Type: application/json" \
            --data '{
              "text": "⚠️ *Test failures detected* in the weekly dashboard.\nJira: <${{ vars.JIRA_BASE_URL }}/browse/${{ vars.JIRA_ISSUE_KEY }}|${{ vars.JIRA_ISSUE_KEY }}>"
            }' \
            "${{ secrets.SLACK_WEBHOOK_URL }}"
```

**Required secrets and variables:**

| Name | Where to set | Value |
|---|---|---|
| `JIRA_AUTH` | Secret | Base64-encoded `user@example.com:api-token` |
| `SLACK_WEBHOOK_URL` | Secret | Incoming webhook URL from Slack app settings |
| `JIRA_BASE_URL` | Variable | e.g. `https://yourorg.atlassian.net` |
| `JIRA_ISSUE_KEY` | Variable | e.g. `QA-42` (issue to attach the dashboard to) |

---

### Jenkins — cron equivalent (Declarative Pipeline)

```groovy
pipeline {
    agent any

    triggers {
        // Run every Monday at 08:00
        cron('0 8 * * 1')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Download history') {
            steps {
                script {
                    // Copy the accumulated history from the test pipeline.
                    copyArtifacts(
                        projectName: 'selenium-tests',   // name of the test pipeline job
                        filter: 'test-reports/history/**',
                        selector: lastSuccessful(),
                        optional: true
                    )
                }
            }
        }

        // ROADMAP: selenium-reporter:merge not yet implemented — see roadmap.md item 1.
        stage('Generate dashboard') {
            steps {
                sh '''
                    mvn selenium-reporter:merge \
                      -DhistoryDir=test-reports/history \
                      -Doutput=test-reports/dashboard.html
                '''
            }
        }

        // ROADMAP: results.json not yet produced — see roadmap.md item 2.
        stage('Check failures and notify') {
            steps {
                script {
                    def hasFailed = sh(
                        script: "jq -e '.failed > 0' test-reports/results.json",
                        returnStatus: true
                    ) == 0

                    if (hasFailed) {
                        // Attach dashboard to Jira using the Jira plugin.
                        jiraAttachFile(
                            idOrKey: env.JIRA_ISSUE_KEY,
                            file: 'test-reports/dashboard.html',
                            site: 'jira'
                        )

                        // Send Slack notification using the Slack Notification plugin.
                        slackSend(
                            channel: '#qa-alerts',
                            color: 'danger',
                            message: "⚠️ Test failures in weekly dashboard. Jira: ${env.JIRA_ISSUE_KEY}"
                        )
                    }
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'test-reports/dashboard.html', allowEmptyArchive: true
        }
    }
}
```

**Required Jenkins plugins:**

- [Copy Artifact](https://plugins.jenkins.io/copyartifact/)
- [Jira](https://plugins.jenkins.io/jira/) — configured with a site named `jira`
- [Slack Notification](https://plugins.jenkins.io/slack/)
