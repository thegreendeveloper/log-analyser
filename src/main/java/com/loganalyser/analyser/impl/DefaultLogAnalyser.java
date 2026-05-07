package com.loganalyser.analyser.impl;

import com.loganalyser.analyser.LogAnalyser;
import com.loganalyser.model.AnalysisResult;
import com.loganalyser.model.LogEntry;
import com.loganalyser.model.RankedEntry;
import com.loganalyser.model.RankedList;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Default implementation of {@link LogAnalyser}.
 *
 * <p><b>URL handling:</b> URLs are normalised before counting: query strings
 * (everything from {@code ?} onwards) are stripped, and the result is lower-cased.
 * So {@code /Search?q=foo} and {@code /search?q=bar} both count as {@code /search}.
 * Absolute and relative URLs are still treated as distinct.
 *
 * <p><b>Tie handling:</b> exactly {@code TOP_N} entries are returned. If an entry
 * outside the top N shares the same count as the last-ranked entry, the corresponding
 * tie flag in {@link AnalysisResult} is set to {@code true}.
 *
 * <p><b>IP validation:</b> an IP is considered invalid if it does not match four
 * dot-separated decimal octets in the range 0–255 with no leading zeros. Invalid
 * IPs are still counted; a warning is emitted once per unique invalid address.
 *
 * <p>The {@link PrintStream} for warnings is injectable so tests can capture output
 * without redirecting {@code System.err}.
 */
public class DefaultLogAnalyser implements LogAnalyser {

    private static final int TOP_N = 3;

    private final PrintStream err;

    /**
     * @param err stream to write IP-validation warnings to (injectable for testability)
     */
    public DefaultLogAnalyser(PrintStream err) {
        this.err = err;
    }

    private static Map<String, Long> countBy(List<LogEntry> entries, Function<LogEntry, String> keyExtractor) {
        return entries.stream()
                .collect(Collectors.groupingBy(keyExtractor, Collectors.counting()));
    }

    private static String normaliseUrl(LogEntry entry) {
        return stripQueryString(entry.url()).toLowerCase();
    }

    private static String stripQueryString(String url) {
        int idx = url.indexOf('?');
        return idx >= 0 ? url.substring(0, idx) : url;
    }

    /**
     * Returns the top {@code topN} entries as {@link RankedEntry} objects with
     * 1-based ranks assigned. Passing {@code topN} as a parameter rather than
     * relying on the class constant means this method can be reused if the
     * desired limit ever needs to vary (e.g. top-5 URLs, top-3 IPs).
     */
    private static List<RankedEntry> topRanked(List<Map.Entry<String, Long>> sorted, int topN) {
        return IntStream.range(0, Math.min(topN, sorted.size()))
                .mapToObj(i -> toRankedEntry(i, sorted.get(i)))
                .toList();
    }

    private static RankedEntry toRankedEntry(int index, Map.Entry<String, Long> entry) {
        return new RankedEntry(index + 1, entry.getKey(), entry.getValue());
    }

    /**
     * Returns true if the entry immediately outside the top {@code topN} shares
     * the same count as the last entry inside it — i.e. the cut-off is ambiguous.
     */
    //HasTie has a bug here. It can return false in cases were rank 1 ties.
    private static boolean hasTie(List<Map.Entry<String, Long>> sorted, int topN) {
        if (sorted.size() <= topN) {
            return false;
        }
        return sorted.get(topN - 1).getValue().equals(sorted.get(topN).getValue());
    }

    /**
     * Sorts a count map by value descending, then by key ascending for deterministic
     * ordering when counts are equal (important for reliable test assertions).
     *
     * <p>Note: keys are sorted as plain strings. IP addresses such as
     * {@code 177.71.128.21} therefore sort before {@code 50.112.00.11} because
     * {@code '1' < '5'} — this is lexicographic comparison, not numeric ordering.
     */
    private static List<Map.Entry<String, Long>> sortDescending(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toList());
    }

    @Override
    public AnalysisResult analyse(List<LogEntry> entries) {
        Map<String, Long> ipCounts = countBy(entries, LogEntry::ipAddress);
        Map<String, Long> urlCounts = countBy(entries, DefaultLogAnalyser::normaliseUrl);

        warnInvalidIps(ipCounts.keySet());

        //Instead of sorting we could have used a priority queue of size N. We then avoid sorting the entire list and save computation time
        List<Map.Entry<String, Long>> sortedUrls = sortDescending(urlCounts);
        List<Map.Entry<String, Long>> sortedIps = sortDescending(ipCounts);

        return new AnalysisResult(
                ipCounts.size(),
                new RankedList(topRanked(sortedUrls, TOP_N), hasTie(sortedUrls, TOP_N)),
                new RankedList(topRanked(sortedIps, TOP_N), hasTie(sortedIps, TOP_N))
        );
    }

    private void warnInvalidIps(Set<String> ips) {
        ips.stream()
                .filter(ip -> !IpValidator.isValid(ip))
                .forEach(ip -> err.println("WARNING: Invalid IP address encountered: " + ip));
    }
}
