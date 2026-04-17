package com.loganalyser.output;

import com.loganalyser.model.AnalysisResult;
import com.loganalyser.model.RankedEntry;
import com.loganalyser.model.RankedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ResultPrinterTest {

    private ByteArrayOutputStream outBytes;
    private ResultPrinter printer;

    private static RankedList emptyList() {
        return new RankedList(List.of(), false);
    }

    @BeforeEach
    void setUp() {
        outBytes = new ByteArrayOutputStream();
        printer = new ResultPrinter(new PrintStream(outBytes));
    }

    @Test
    void print_includesUniqueIpCount() {
        AnalysisResult result = new AnalysisResult(7, emptyList(), emptyList());

        printer.print(result);

        assertTrue(outBytes.toString().contains("Unique IP addresses: 7"));
    }

    @Test
    void print_includesUrlEntryWithRankAndCount() {
        AnalysisResult result = new AnalysisResult(1,
                new RankedList(List.of(new RankedEntry(1, "/home/", 5)), false),
                emptyList());

        printer.print(result);

        String output = outBytes.toString();
        assertTrue(output.contains("1."));
        assertTrue(output.contains("/home/"));
        assertTrue(output.contains("5"));
    }

    @Test
    void print_includesIpEntryWithRankAndCount() {
        AnalysisResult result = new AnalysisResult(1,
                emptyList(),
                new RankedList(List.of(new RankedEntry(1, "1.2.3.4", 3)), false));

        printer.print(result);

        String output = outBytes.toString();
        assertTrue(output.contains("1."));
        assertTrue(output.contains("1.2.3.4"));
        assertTrue(output.contains("3"));
    }

    @Test
    void print_urlVisitCount_singularForOne() {
        AnalysisResult result = new AnalysisResult(1,
                new RankedList(List.of(new RankedEntry(1, "/page/", 1)), false),
                emptyList());

        printer.print(result);

        assertTrue(outBytes.toString().contains("1 visit)"));
    }

    @Test
    void print_urlVisitCount_pluralForMany() {
        AnalysisResult result = new AnalysisResult(1,
                new RankedList(List.of(new RankedEntry(1, "/page/", 4)), false),
                emptyList());

        printer.print(result);

        assertTrue(outBytes.toString().contains("4 visits)"));
    }

    @Test
    void print_ipRequestCount_singularForOne() {
        AnalysisResult result = new AnalysisResult(1,
                emptyList(),
                new RankedList(List.of(new RankedEntry(1, "1.2.3.4", 1)), false));

        printer.print(result);

        assertTrue(outBytes.toString().contains("1 request)"));
    }

    @Test
    void print_ipRequestCount_pluralForMany() {
        AnalysisResult result = new AnalysisResult(1,
                emptyList(),
                new RankedList(List.of(new RankedEntry(1, "1.2.3.4", 3)), false));

        printer.print(result);

        assertTrue(outBytes.toString().contains("3 requests)"));
    }

    @Test
    void print_urlTieNote_appearsWhenTied() {
        AnalysisResult result = new AnalysisResult(1, new RankedList(List.of(), true), emptyList());

        printer.print(result);

        assertTrue(outBytes.toString().contains("tie"));
    }

    @Test
    void print_urlTieNote_absentWhenNotTied() {
        AnalysisResult result = new AnalysisResult(1,
                new RankedList(List.of(new RankedEntry(1, "/page/", 2)), false),
                emptyList());

        printer.print(result);

        assertFalse(outBytes.toString().contains("tie"));
    }

    @Test
    void print_ipTieNote_appearsWhenTied() {
        AnalysisResult result = new AnalysisResult(1, emptyList(), new RankedList(List.of(), true));

        printer.print(result);

        assertTrue(outBytes.toString().contains("tie"));
    }

    @Test
    void print_ipTieNote_absentWhenNotTied() {
        AnalysisResult result = new AnalysisResult(1,
                emptyList(),
                new RankedList(List.of(new RankedEntry(1, "1.2.3.4", 2)), false));

        printer.print(result);

        assertFalse(outBytes.toString().contains("tie"));
    }
}
