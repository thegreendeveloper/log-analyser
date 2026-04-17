package com.loganalyser.parser.impl;

import com.loganalyser.model.LogEntry;
import com.loganalyser.parser.LogFileReader;
import com.loganalyser.parser.LogLineParser;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link LogFileReader} that streams the file line by line.
 *
 * <p>Uses {@link Files#lines} to avoid loading the entire file into memory,
 * making it suitable for large log files. Each line is passed to the injected
 * {@link LogLineParser}; lines that return {@link Optional#empty()} are filtered
 * out of the result.
 *
 * <p>Non-blank lines that cannot be parsed are skipped and a warning is written
 * to the provided error stream. Blank lines are silently skipped.
 */
public class StreamingLogFileReader implements LogFileReader {

    private final LogLineParser lineParser;
    private final PrintStream err;

    /**
     * @param lineParser the parser to use for individual lines
     * @param err        stream to write parse warnings to (injectable for testability)
     */
    public StreamingLogFileReader(LogLineParser lineParser, PrintStream err) {
        this.lineParser = lineParser;
        this.err = err;
    }

    @Override
    public List<LogEntry> readEntries(Path path) throws IOException {
        try (var lines = Files.lines(path)) {
            return lines
                    .map(this::parseWithWarning)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        }
    }

    private Optional<LogEntry> parseWithWarning(String line) {
        Optional<LogEntry> result = lineParser.parse(line);
        if (result.isEmpty() && line != null && !line.isBlank()) {
            err.println("WARNING: Could not parse log line: " + line);
        }
        return result;
    }
}
