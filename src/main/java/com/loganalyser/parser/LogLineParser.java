package com.loganalyser.parser;

import com.loganalyser.model.LogEntry;

import java.util.Optional;

/**
 * Parses a single raw log line into a {@link LogEntry}.
 *
 * <p>A malformed line is defined as any line that does not conform to the
 * Combined Log Format structure:
 * <pre>
 *   IP IDENT AUTH [TIMESTAMP] "METHOD URL PROTOCOL" STATUS SIZE "REFERER" "USER_AGENT"
 * </pre>
 *
 * <p>Specifically, a line is considered malformed if:
 * <ul>
 *   <li>It is null or blank</li>
 *   <li>It is missing the timestamp block enclosed in square brackets</li>
 *   <li>It is missing the request string enclosed in double quotes</li>
 *   <li>The request string does not contain at least METHOD, URL, and PROTOCOL tokens</li>
 * </ul>
 *
 * <p>Lines with extra trailing fields beyond the standard format are <em>not</em>
 * considered malformed — they are parsed normally and the extra fields are ignored.
 *
 * <p>Malformed lines are skipped rather than thrown, so callers receive a clean
 * list of valid entries and a single bad line cannot abort the entire analysis.
 */
public interface LogLineParser {

    /**
     * Attempts to parse a single raw log line.
     *
     * @param line a raw line from the log file; may be null
     * @return an {@link Optional} containing the parsed entry if the line is valid,
     * or {@link Optional#empty()} if the line is malformed or blank
     */
    Optional<LogEntry> parse(String line);
}
