package com.loganalyser.model;

/**
 * Represents a single successfully parsed HTTP log entry.
 *
 * <p>Using a record because a log entry is immutable value data —
 * no behaviour, no reason to mutate after parsing.
 *
 * @param ipAddress the IP address of the client
 * @param authUser  the authenticated username, or {@code null} if the request was unauthenticated
 * @param url       the requested URL, either absolute or relative as it appears in the log
 */
public record LogEntry(String ipAddress, String authUser, String url) {
}
