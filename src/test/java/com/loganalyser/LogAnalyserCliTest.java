package com.loganalyser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogAnalyserCliTest {

    private static final String VALID_LINE =
            "177.71.128.21 - - [10/Jul/2018:22:21:28 +0200] " +
                    "\"GET /intranet-analytics/ HTTP/1.1\" 200 3574 \"-\" \"Mozilla/5.0\"";
    @TempDir
    Path tempDir;
    @Mock
    private PrintStream mockErr;
    private ByteArrayOutputStream outBytes;
    private PrintStream capturedOut;

    @BeforeEach
    void setUp() {
        outBytes = new ByteArrayOutputStream();
        capturedOut = new PrintStream(outBytes);
    }

    @Test
    void noArgs_returnsExitCode1_printsUsage() {
        int code = new LogAnalyserCli(capturedOut, mockErr).run(new String[]{});

        assertEquals(1, code);
        verify(mockErr).println(contains("Usage"));
    }

    @Test
    void nullArgs_returnsExitCode1() {
        int code = new LogAnalyserCli(capturedOut, mockErr).run(null);

        assertEquals(1, code);
    }

    @Test
    void fileNotFound_returnsExitCode1_printsError() {
        Path missing = tempDir.resolve("missing.log");

        int code = new LogAnalyserCli(capturedOut, mockErr).run(new String[]{missing.toString()});

        assertEquals(1, code);
        verify(mockErr).println(contains("not found"));
    }

    @Test
    void validFile_returnsExitCode0_outputsResult() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile, VALID_LINE + "\n");

        int code = new LogAnalyserCli(capturedOut, mockErr).run(new String[]{logFile.toString()});

        assertEquals(0, code);
        assertTrue(outBytes.toString().contains("Unique IP addresses: 1"));
        assertTrue(outBytes.toString().contains("/intranet-analytics/"));
    }

    @Test
    void malformedLines_warningOnStderr_validEntriesStillCounted() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile, VALID_LINE + "\nbad line here\n");

        int code = new LogAnalyserCli(capturedOut, mockErr).run(new String[]{logFile.toString()});

        assertEquals(0, code);
        verify(mockErr).println(contains("WARNING"));
        assertTrue(outBytes.toString().contains("Unique IP addresses: 1"));
    }

    @Test
    void allMalformedLines_returnsExitCode0_zeroIpCount() throws Exception {
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile, "not a log line\nalso not a log line\n");

        int code = new LogAnalyserCli(capturedOut, mockErr).run(new String[]{logFile.toString()});

        assertEquals(0, code);
        assertTrue(outBytes.toString().contains("Unique IP addresses: 0"));
        verify(mockErr, times(2)).println(contains("WARNING"));
    }

    @Test
    void sampleLogFile_fullIntegration() throws Exception {
        Path sampleLog = Path.of("logs/sample.log");
        if (!Files.exists(sampleLog)) {
            return;
        }

        int code = new LogAnalyserCli(capturedOut, mockErr).run(new String[]{sampleLog.toString()});

        assertEquals(0, code);
        String output = outBytes.toString();
        assertTrue(output.contains("Unique IP addresses: 11"));
        assertTrue(output.contains("168.41.191.40"));
        assertTrue(output.contains("/docs/manage-websites/"));
        verify(mockErr, atLeastOnce()).println(contains("WARNING"));
    }
}
