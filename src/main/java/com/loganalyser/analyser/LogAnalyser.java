package com.loganalyser.analyser;

import com.loganalyser.model.AnalysisResult;
import com.loganalyser.model.LogEntry;

import java.util.List;

/**
 * Analyses a list of parsed log entries and produces an {@link AnalysisResult}.
 */
public interface LogAnalyser {

    /**
     * @param entries the parsed log entries to analyse
     * @return the analysis result containing unique IP count, top URLs, and top IPs
     */
    AnalysisResult analyse(List<LogEntry> entries);
}
