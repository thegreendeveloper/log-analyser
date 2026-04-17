package com.loganalyser.parser;

import com.loganalyser.model.LogEntry;
import com.loganalyser.parser.impl.StreamingLogFileReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamingLogFileReaderTest {

    @TempDir
    Path tempDir;
    @Mock
    private LogLineParser mockParser;
    @Mock
    private PrintStream mockErr;

    @Test
    void validLines_returnsParsedEntries() throws IOException {
        LogEntry entry = new LogEntry("1.2.3.4", null, "/path/");
        when(mockParser.parse(anyString())).thenReturn(Optional.of(entry));
        Path file = tempDir.resolve("test.log");
        Files.writeString(file, "line1\nline2\n");

        List<LogEntry> results = new StreamingLogFileReader(mockParser, mockErr).readEntries(file);

        assertEquals(2, results.size());
    }

    @Test
    void blankLines_silentlySkipped_noWarningEmitted() throws IOException {
        when(mockParser.parse("")).thenReturn(Optional.empty());
        Path file = tempDir.resolve("test.log");
        Files.writeString(file, "\n\n");

        List<LogEntry> results = new StreamingLogFileReader(mockParser, mockErr).readEntries(file);

        assertTrue(results.isEmpty());
        verifyNoInteractions(mockErr);
    }

    @Test
    void nonBlankUnparseableLine_writesWarningToErr() throws IOException {
        when(mockParser.parse("bad line")).thenReturn(Optional.empty());
        Path file = tempDir.resolve("test.log");
        Files.writeString(file, "bad line\n");

        new StreamingLogFileReader(mockParser, mockErr).readEntries(file);

        verify(mockErr).println(contains("WARNING"));
        verify(mockErr).println(contains("bad line"));
    }

    @Test
    void mixedLines_returnsOnlyParsedEntries_warnsOnUnparseable() throws IOException {
        LogEntry entry = new LogEntry("1.2.3.4", null, "/path/");
        when(mockParser.parse("good line")).thenReturn(Optional.of(entry));
        when(mockParser.parse("bad line")).thenReturn(Optional.empty());
        Path file = tempDir.resolve("test.log");
        Files.writeString(file, "good line\nbad line\ngood line\n");

        List<LogEntry> results = new StreamingLogFileReader(mockParser, mockErr).readEntries(file);

        assertEquals(2, results.size());
        verify(mockErr).println(contains("WARNING"));
    }

    @Test
    void missingFile_throwsIOException() {
        Path missing = tempDir.resolve("does-not-exist.log");

        assertThrows(IOException.class, () -> new StreamingLogFileReader(mockParser, mockErr).readEntries(missing));
    }
}
