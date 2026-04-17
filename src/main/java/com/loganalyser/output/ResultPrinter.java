package com.loganalyser.output;

import com.loganalyser.model.AnalysisResult;
import com.loganalyser.model.RankedEntry;
import com.loganalyser.model.RankedList;

import java.io.PrintStream;
import java.util.List;

/**
 * Formats an {@link AnalysisResult} and writes it to a {@link PrintStream}.
 *
 * <p>The stream is injected rather than hard-coded to {@code System.out} so that
 * callers (including tests) can redirect output without touching global state.
 */
public class ResultPrinter {

    private final PrintStream out;

    public ResultPrinter(PrintStream out) {
        this.out = out;
    }

    public void print(AnalysisResult result) {
        out.println("=== Log Analysis Result ===");
        out.println();

        out.println("Unique IP addresses: " + result.uniqueIpCount());
        out.println();

        printRankedList("Top 3 most visited URLs", result.topUrls(), "visit", "visits");
        out.println();

        printRankedList("Top 3 most active IP addresses", result.mostActiveIpAddresses(), "request", "requests");
    }

    private static String tieNote(List<RankedEntry> entries) {
        long lastCount = entries.get(entries.size() - 1).count();
        int tieStartRank = firstRankWithCount(entries, lastCount);
        return " (NOTE: tie from position " + tieStartRank + " - multiple entries share this count)";
    }

    private static int firstRankWithCount(List<RankedEntry> entries, long count) {
        return entries.stream()
                .filter(e -> e.count() == count)
                .findFirst()
                .map(RankedEntry::rank)
                .orElse(entries.size());
    }

    private void printRankedList(String heading, RankedList list, String singular, String plural) {
        String tieNote = list.tied() ? tieNote(list.entries()) : "";
        out.println(heading + tieNote + ":");
        printAllEntries(list.entries(), singular, plural);
    }

    private void printAllEntries(List<RankedEntry> entries, String singular, String plural) {
        entries.forEach(entry -> printEntry(entry, singular, plural));
    }

    private void printEntry(RankedEntry entry, String singular, String plural) {
        String unit = entry.count() == 1 ? singular : plural;
        out.printf("  %d. %s (%d %s)%n", entry.rank(), entry.value(), entry.count(), unit);
    }
}
