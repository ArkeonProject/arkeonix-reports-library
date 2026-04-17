# selenium-reporter — Documentation

Automatic professional HTML report generator for Selenium WebDriver tests.  
Supports **TestNG**, **JUnit 5**, and **Cucumber** out of the box.

## Key features

- Single self-contained HTML file per run — no server, no dependencies to deploy
- Donut chart, duration bar chart, and pass-rate trend line chart
- Expandable failure rows with exception type, origin frame, stack trace, and inline screenshot
- Run history persisted as JSON — survives CI restarts when managed as an artifact
- Flaky test detection — flags tests that alternate between PASS and FAIL across runs
- Per-step log with timestamps and duration (Cucumber: automatic; TestNG/JUnit 5: manual API)
- Failure classification badges (Assertion, Element Not Found, Timeout, Stale Element)
- Webhook notifications for Slack, Microsoft Teams, or any HTTP endpoint
- CI build info bar — auto-detected from Jenkins, GitHub Actions, and GitLab CI
- Dark / light theme, sortable table columns

---

## Table of contents

| Document | What it covers |
|---|---|
| [getting-started.md](getting-started.md) | Installation, minimal working examples, first report |
| [configuration.md](configuration.md) | All `ReporterConfig` options with types, defaults, and examples |
| [ci-integration.md](ci-integration.md) | How to persist history across CI builds, cron dashboards, Jira + Slack integration |
| [architecture.md](architecture.md) | Internal design decisions relevant to contributors |
| [roadmap.md](roadmap.md) | Planned features in priority order |
