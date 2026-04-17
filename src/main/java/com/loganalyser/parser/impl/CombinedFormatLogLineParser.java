package com.loganalyser.parser.impl;

import com.loganalyser.model.LogEntry;
import com.loganalyser.parser.LogLineParser;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link LogLineParser} for the Apache/Nginx Combined Log Format.
 *
 * <p>An example of a valid log line:
 * <pre>
 *   177.71.128.21 - admin [10/Jul/2018:22:21:28 +0200] "GET /intranet-analytics/ HTTP/1.1" 200 3574 "-" "Mozilla/5.0..."
 * </pre>
 *
 * <p><b>Malformed line handling:</b> lines that do not match the expected structure
 * are skipped. This is intentional — a log file may be partially corrupted, and
 * the analyser should still produce useful output from the valid portion.
 * See {@link LogLineParser} for the full definition of what constitutes a malformed line.
 *
 * <p><b>Assumptions:</b>
 * <ul>
 *   <li>The IP address is the first whitespace-delimited token. Its format is not
 *       validated beyond this — the log file is the source of truth.</li>
 *   <li>The auth field may be {@code "-"} (unauthenticated) or a username such as
 *       {@code "admin"}. When {@code "-"}, {@link LogEntry#authUser()} is set to
 *       {@code null} so callers can use a simple null-check rather than knowing
 *       the log format convention.</li>
 *   <li>URLs may be absolute (e.g. {@code http://example.net/path/}) or relative
 *       (e.g. {@code /path/}). They are stored as-is without normalisation.</li>
 *   <li>Lines with extra trailing tokens beyond the standard fields are accepted —
 *       the regex matches on structure, not end-of-line.</li>
 * </ul>
 */
public class CombinedFormatLogLineParser implements LogLineParser {

    /**
     * Regex capturing:
     * <ol>
     *   <li>IP address — first non-whitespace token</li>
     *   <li>Auth user — third non-whitespace token (may be "-")</li>
     *   <li>HTTP method — first token inside the quoted request string (not used, but captured for clarity)</li>
     *   <li>URL — second token inside the quoted request string</li>
     * </ol>
     * The ident field (second token) and all fields after the request string
     * are deliberately not captured — they are not needed for the analysis.
     */
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^(\\S+)\\s+\\S+\\s+(\\S+)\\s+\\[.*?]\\s+\"(\\S+)\\s+(\\S+)\\s+\\S+\".*$"
    );

    @Override
    public Optional<LogEntry> parse(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = LOG_PATTERN.matcher(line.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String ip = matcher.group(1);
        String authRaw = matcher.group(2);
        String url = matcher.group(4);

        String authUser = "-".equals(authRaw) ? null : authRaw;
        return Optional.of(new LogEntry(ip, authUser, url));
    }
}
