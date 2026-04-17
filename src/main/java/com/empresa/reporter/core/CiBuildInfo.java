package com.empresa.reporter.core;

public class CiBuildInfo {

    private final String ciSystem;
    private final String buildNumber;
    private final String buildUrl;
    private final String branch;
    private final String commitSha;

    private CiBuildInfo(String ciSystem, String buildNumber, String buildUrl, String branch, String commitSha) {
        this.ciSystem    = ciSystem;
        this.buildNumber = buildNumber;
        this.buildUrl    = buildUrl;
        this.branch      = branch;
        this.commitSha   = commitSha;
    }

    /** Detects CI environment from well-known env vars. Returns null when not in CI. */
    public static CiBuildInfo detect() {
        // Jenkins
        if (env("JENKINS_URL") != null || env("BUILD_URL") != null) {
            return new CiBuildInfo(
                    "Jenkins",
                    env("BUILD_NUMBER"),
                    env("BUILD_URL"),
                    env("GIT_BRANCH"),
                    abbrev(env("GIT_COMMIT")));
        }
        // GitHub Actions
        if ("true".equalsIgnoreCase(env("GITHUB_ACTIONS"))) {
            String repo  = env("GITHUB_REPOSITORY");
            String runId = env("GITHUB_RUN_ID");
            String url   = (repo != null && runId != null)
                    ? "https://github.com/" + repo + "/actions/runs/" + runId
                    : null;
            return new CiBuildInfo(
                    "GitHub Actions",
                    env("GITHUB_RUN_NUMBER"),
                    url,
                    env("GITHUB_REF_NAME"),
                    abbrev(env("GITHUB_SHA")));
        }
        // GitLab CI
        if (env("GITLAB_CI") != null) {
            return new CiBuildInfo(
                    "GitLab CI",
                    env("CI_PIPELINE_IID"),
                    env("CI_PIPELINE_URL"),
                    env("CI_COMMIT_REF_NAME"),
                    abbrev(env("CI_COMMIT_SHA")));
        }
        return null;
    }

    /** Factory for manual override via ReporterConfig. */
    public static CiBuildInfo of(String ciSystem, String buildNumber, String buildUrl, String branch, String commitSha) {
        return new CiBuildInfo(ciSystem, buildNumber, buildUrl, branch, commitSha);
    }

    public String getCiSystem()    { return ciSystem; }
    public String getBuildNumber() { return buildNumber; }
    public String getBuildUrl()    { return buildUrl; }
    public String getBranch()      { return branch; }
    public String getCommitSha()   { return commitSha; }

    private static String env(String name) { return System.getenv(name); }

    private static String abbrev(String sha) {
        return (sha != null && sha.length() > 7) ? sha.substring(0, 7) : sha;
    }
}
