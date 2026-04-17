package com.empresa.reporter.history;

import com.empresa.reporter.model.TestCaseReport;
import com.empresa.reporter.model.TestRunSummary;
import com.empresa.reporter.model.TestSuiteReport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class HistoryManager {

    private static final Logger log = LoggerFactory.getLogger(HistoryManager.class);
    private static final String HISTORY_SUBDIR = "history";

    private final Path historyDir;
    private final int  maxRuns;
    private final Gson gson;

    public HistoryManager(String outputDir, int maxRuns) {
        this.historyDir = Paths.get(outputDir, HISTORY_SUBDIR);
        this.maxRuns    = maxRuns;
        this.gson       = buildGson();
    }

    /** Returns the last {@code maxRuns} run summaries, oldest first. */
    public List<TestRunSummary> loadHistory() {
        List<Path> files = listRunFiles();
        List<TestRunSummary> result = new ArrayList<>();
        for (Path f : files) {
            try (Reader r = new FileReader(f.toFile())) {
                RunEntry entry = gson.fromJson(r, RunEntry.class);
                if (entry == null) continue;
                TestRunSummary s = new TestRunSummary();
                s.setRunTime(Instant.ofEpochMilli(entry.runTimeEpoch));
                s.setSuiteName(entry.suiteName);
                s.setTotal(entry.total);
                s.setPassed(entry.passed);
                s.setFailed(entry.failed);
                s.setSkipped(entry.skipped);
                s.setDurationMs(entry.durationMs);
                s.setBuildUrl(entry.buildUrl);
                result.add(s);
            } catch (Exception e) {
                log.debug("[selenium-reporter] Could not load history file {}: {}", f.getFileName(), e.getMessage());
            }
        }
        return result;
    }

    /**
     * Returns a map from testName → list of boolean outcomes (true=PASSED)
     * across the last {@code maxRuns} runs, to feed into FlakiDetector.
     */
    public Map<String, List<Boolean>> loadTestOutcomes() {
        List<Path> files = listRunFiles();
        Map<String, List<Boolean>> outcomes = new LinkedHashMap<>();
        for (Path f : files) {
            try (Reader r = new FileReader(f.toFile())) {
                RunEntry entry = gson.fromJson(r, RunEntry.class);
                if (entry == null || entry.testOutcomes == null) continue;
                entry.testOutcomes.forEach((name, status) ->
                        outcomes.computeIfAbsent(name, k -> new ArrayList<>())
                                .add("PASSED".equals(status)));
            } catch (Exception e) {
                log.debug("[selenium-reporter] Could not load outcomes from {}: {}", f.getFileName(), e.getMessage());
            }
        }
        return outcomes;
    }

    /** Persists the current run. Prunes oldest files if over the limit. */
    public void saveRun(TestSuiteReport report) {
        try {
            Files.createDirectories(historyDir);

            RunEntry entry = new RunEntry();
            entry.runTimeEpoch = report.getStartTime() != null
                    ? report.getStartTime().toEpochMilli()
                    : System.currentTimeMillis();
            entry.suiteName  = report.getSuiteName();
            entry.total      = report.getTotal();
            entry.passed     = (int) report.getPassed();
            entry.failed     = (int) report.getFailed();
            entry.skipped    = (int) report.getSkipped();
            entry.durationMs = report.getTotalDurationMs();
            if (report.getBuildInfo() != null) {
                entry.buildUrl = report.getBuildInfo().getBuildUrl();
            }
            entry.testOutcomes = new LinkedHashMap<>();
            for (TestCaseReport tc : report.getTestCases()) {
                if (tc.getTestName() != null) {
                    entry.testOutcomes.put(tc.getTestName(), tc.getStatus().name());
                }
            }

            String filename = "run_" + entry.runTimeEpoch + ".json";
            Path outPath = historyDir.resolve(filename);
            try (Writer w = new FileWriter(outPath.toFile())) {
                gson.toJson(entry, w);
            }
            log.debug("[selenium-reporter] History saved: {}", filename);

            pruneOldRuns();
        } catch (Exception e) {
            log.warn("[selenium-reporter] Failed to save history: {}", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Path> listRunFiles() {
        if (!Files.exists(historyDir)) return Collections.emptyList();
        try {
            List<Path> files = Files.list(historyDir)
                    .filter(p -> p.getFileName().toString().startsWith("run_")
                                 && p.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
            int from = Math.max(0, files.size() - maxRuns);
            return files.subList(from, files.size());
        } catch (IOException e) {
            log.debug("[selenium-reporter] Cannot list history dir: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void pruneOldRuns() {
        try {
            List<Path> all = Files.list(historyDir)
                    .filter(p -> p.getFileName().toString().startsWith("run_")
                                 && p.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
            while (all.size() > maxRuns) {
                Files.deleteIfExists(all.remove(0));
            }
        } catch (IOException e) {
            log.debug("[selenium-reporter] Cannot prune history: {}", e.getMessage());
        }
    }

    private static Gson buildGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Instant.class,
                        (JsonSerializer<Instant>) (src, t, ctx) -> ctx.serialize(src.toEpochMilli()))
                .registerTypeAdapter(Instant.class,
                        (JsonDeserializer<Instant>) (json, t, ctx) -> Instant.ofEpochMilli(json.getAsLong()))
                .create();
    }

    // ── Internal DTO ─────────────────────────────────────────────────────────

    private static class RunEntry {
        long                runTimeEpoch;
        String              suiteName;
        int                 total, passed, failed, skipped;
        long                durationMs;
        String              buildUrl;
        Map<String, String> testOutcomes;
    }
}
