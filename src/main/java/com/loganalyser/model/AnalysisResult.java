package com.loganalyser.model;

/**
 * The result of analysing a set of log entries.
 *
 * @param uniqueIpCount         number of distinct IP addresses seen
 * @param topUrls               top N most visited URLs as a {@link RankedList}; entries are
 *                              ordered by rank and carry visit counts; the tied flag is set
 *                              when a URL outside the top N shares the last-ranked count
 * @param mostActiveIpAddresses top N most active IP addresses as a {@link RankedList}; entries
 *                              are ordered by rank and carry request counts; the tied flag is
 *                              set when an IP outside the top N shares the last-ranked count
 */
public record AnalysisResult(
        int uniqueIpCount,
        RankedList topUrls,
        RankedList mostActiveIpAddresses
) {
}
