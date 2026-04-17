package com.loganalyser;

import com.loganalyser.analyser.LogAnalyser;
import com.loganalyser.analyser.impl.DefaultLogAnalyser;
import com.loganalyser.model.AnalysisResult;
import com.loganalyser.model.LogEntry;
import com.loganalyser.output.ResultPrinter;
import com.loganalyser.parser.LogFileReader;
import com.loganalyser.parser.impl.CombinedFormatLogLineParser;
import com.loganalyser.parser.impl.StreamingLogFileReader;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the log analysis pipeline for the command-line interface.
 *
 * <p>Accepts injectable {@link PrintStream} instances for output and errors so
 * the full pipeline can be exercised in tests without forking a process or
 * touching global state.
 *
 * <p>Usage: {@code java -jar log-analyser.jar <path-to-log-file>}
 */
public class LogAnalyserCli {

    private final PrintStream out;
    private final PrintStream err;

    public LogAnalyserCli(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    /**
     * Runs the analysis pipeline.
     *
     * @param args command-line arguments; expects exactly one element: the log file path
     * @return 0 on success, 1 on usage or I/O error
     */
    public int run(String[] args) {
        if (args == null || args.length == 0) {
            err.println("Usage: log-analyser <path-to-log-file>");
            return 1;
        }

        Path path = Path.of(args[0]);
        LogFileReader reader = new StreamingLogFileReader(new CombinedFormatLogLineParser(), err);
        LogAnalyser analyser = new DefaultLogAnalyser(err);
        ResultPrinter printer = new ResultPrinter(out);

        Optional<List<LogEntry>> entries = readEntries(reader, path);
        if (entries.isEmpty()) {
            return 1;
        }

        AnalysisResult analyse = analyser.analyse(entries.get());
        printer.print(analyse);
        return 0;
    }

    private Optional<List<LogEntry>> readEntries(LogFileReader reader, Path path) {
        try {
            return Optional.of(reader.readEntries(path));
        } catch (NoSuchFileException e) {
            err.println("Error: File not found: " + path);
            return Optional.empty();
        } catch (IOException e) {
            err.println("Error: Could not read file: " + e.getMessage());
            return Optional.empty();
        }
    }
}
