package com.loganalyser.output;

import com.loganalyser.model.AnalysisResult;
import com.loganalyser.model.RankedList;

import java.io.PrintStream;

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

    private void printRankedList(String heading, RankedList list, String singular, String plural) {
        String tieNote = list.tied()
                ? " (NOTE: tie at position 3 — more entries share this count)"
                : "";
        out.println(heading + tieNote + ":");
        list.entries().forEach(entry -> out.printf(
                "  %d. %s (%d %s)%n",
                entry.rank(), entry.value(), entry.count(),
                entry.count() == 1 ? singular : plural
        ));
    }
}
