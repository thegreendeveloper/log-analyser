package com.loganalyser.parser;

import com.loganalyser.model.LogEntry;
import com.loganalyser.parser.impl.CombinedFormatLogLineParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CombinedFormatLogLineParserTest {

    private static final String VALID_LINE =
            "177.71.128.21 - admin [10/Jul/2018:22:21:28 +0200] " +
                    "\"GET /intranet-analytics/ HTTP/1.1\" 200 3574 \"-\" \"Mozilla/5.0\"";
    private CombinedFormatLogLineParser parser;

    @BeforeEach
    void setUp() {
        parser = new CombinedFormatLogLineParser();
    }

    @Test
    void validLine_returnsEntry() {
        Optional<LogEntry> result = parser.parse(VALID_LINE);
        assertTrue(result.isPresent());
        assertEquals("177.71.128.21", result.get().ipAddress());
        assertEquals("admin", result.get().authUser());
        assertEquals("/intranet-analytics/", result.get().url());
    }

    @Test
    void unauthenticatedLine_authUserIsNull() {
        String line =
                "168.41.191.40 - - [09/Jul/2018:10:11:30 +0200] " +
                        "\"GET /faq/ HTTP/1.1\" 200 3574 \"-\" \"Mozilla/5.0\"";
        Optional<LogEntry> result = parser.parse(line);
        assertTrue(result.isPresent());
        assertNull(result.get().authUser());
    }

    @Test
    void nullLine_returnsEmpty() {
        assertTrue(parser.parse(null).isEmpty());
    }

    @Test
    void blankLine_returnsEmpty() {
        assertTrue(parser.parse("   ").isEmpty());
    }

    @Test
    void missingTimestamp_returnsEmpty() {
        String line = "177.71.128.21 - - \"GET /path/ HTTP/1.1\" 200 100";
        assertTrue(parser.parse(line).isEmpty());
    }

    @Test
    void missingRequestString_returnsEmpty() {
        String line = "177.71.128.21 - - [10/Jul/2018:22:21:28 +0200] 200 100";
        assertTrue(parser.parse(line).isEmpty());
    }

    @Test
    void extraTrailingFields_parsedSuccessfully() {
        String line =
                "72.44.32.10 - - [09/Jul/2018:15:48:07 +0200] " +
                        "\"GET / HTTP/1.1\" 200 3574 \"-\" \"Mozilla/5.0\" junk extra";
        Optional<LogEntry> result = parser.parse(line);
        assertTrue(result.isPresent());
        assertEquals("/", result.get().url());
    }

    @Test
    void urlWithQueryString_preservedAsIs() {
        String line =
                "1.2.3.4 - - [01/Jan/2020:00:00:00 +0000] " +
                        "\"GET /search?q=test HTTP/1.1\" 200 100 \"-\" \"Mozilla/5.0\"";
        Optional<LogEntry> result = parser.parse(line);
        assertTrue(result.isPresent());
        assertEquals("/search?q=test", result.get().url());
    }

    @Test
    void absoluteUrl_parsedCorrectly() {
        String line =
                "168.41.191.40 - - [09/Jul/2018:10:11:30 +0200] " +
                        "\"GET http://example.net/faq/ HTTP/1.1\" 200 3574 \"-\" \"Mozilla/5.0\"";
        Optional<LogEntry> result = parser.parse(line);
        assertTrue(result.isPresent());
        assertEquals("http://example.net/faq/", result.get().url());
    }
}
