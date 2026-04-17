package com.loganalyser.parser;

import com.loganalyser.model.LogEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads a log file from disk and returns its successfully parsed entries.
 *
 * <p>Malformed lines are skipped transparently — see {@link LogLineParser} for
 * the definition of what constitutes a malformed line. The returned list contains
 * only entries that were successfully parsed; callers do not need to filter further.
 *
 * <p>Separating file I/O from line parsing keeps both concerns independently
 * testable: parser tests operate on plain strings with no filesystem dependency,
 * while file reader tests can use a small temporary file without caring about
 * parsing logic.
 */
public interface LogFileReader {

    /**
     * Reads all lines from the given path and returns the successfully parsed entries.
     *
     * @param path path to the log file
     * @return list of successfully parsed log entries; never null, may be empty
     * @throws IOException if the file cannot be read or does not exist
     */
    List<LogEntry> readEntries(Path path) throws IOException;
}
