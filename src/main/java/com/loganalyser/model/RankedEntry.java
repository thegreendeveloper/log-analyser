package com.loganalyser.model;

/**
 * A single entry in a ranked analysis result.
 *
 * <p>Pairs a value (URL or IP address) with its 1-based position in the ranking
 * and the count that determined that position. Carrying the count alongside the
 * value means callers can display or reason about the frequency without needing
 * to re-derive it from the original data.
 *
 * @param rank  1-based position in the ranking (1 = most frequent)
 * @param value the URL or IP address string, exactly as it appears after any
 *              normalisation applied by the analyser (e.g. query string stripped)
 * @param count the number of times this value was observed
 */
public record RankedEntry(int rank, String value, long count) {
}
