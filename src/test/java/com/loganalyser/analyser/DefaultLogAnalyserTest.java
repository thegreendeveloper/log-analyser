package com.loganalyser.analyser;

import com.loganalyser.analyser.impl.DefaultLogAnalyser;
import com.loganalyser.model.AnalysisResult;
import com.loganalyser.model.LogEntry;
import com.loganalyser.model.RankedEntry;
import com.loganalyser.model.RankedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DefaultLogAnalyserTest {

    @Mock
    private PrintStream mockErr;

    private DefaultLogAnalyser analyser;

    /**
     * Extracts just the value strings from a RankedList for order/equality assertions.
     */
    private static List<String> values(RankedList list) {
        return list.entries().stream().map(RankedEntry::value).toList();
    }

    private static LogEntry e(String ip, String url) {
        return new LogEntry(ip, null, url);
    }

    @BeforeEach
    void setUp() {
        analyser = new DefaultLogAnalyser(mockErr);
    }

    @Test
    void emptyInput_returnsZeroes() {
        AnalysisResult result = analyser.analyse(List.of());

        assertEquals(0, result.uniqueIpCount());
        assertTrue(result.topUrls().entries().isEmpty());
        assertTrue(result.mostActiveIpAddresses().entries().isEmpty());
        assertFalse(result.topUrls().tied());
        assertFalse(result.mostActiveIpAddresses().tied());
    }

    @Test
    void uniqueIpCount_deduplicatesCorrectly() {
        List<LogEntry> entries = List.of(
                e("1.2.3.4", "/a"),
                e("1.2.3.4", "/b"),
                e("5.6.7.8", "/a")
        );

        assertEquals(2, analyser.analyse(entries).uniqueIpCount());
    }

    @Test
    void urlMatching_caseInsensitive() {
        List<LogEntry> entries = List.of(
                e("1.2.3.4", "/Docs/Page/"),
                e("1.2.3.4", "/docs/page/"),
                e("1.2.3.4", "/DOCS/PAGE/")
        );

        AnalysisResult result = analyser.analyse(entries);

        assertEquals(1, result.topUrls().entries().size());
        assertEquals("/docs/page/", result.topUrls().entries().get(0).value());
        assertEquals(3L, result.topUrls().entries().get(0).count());
    }

    @Test
    void urlsWithQueryStrings_strippedBeforeComparison() {
        List<LogEntry> entries = List.of(
                e("1.2.3.4", "/search?q=foo"),
                e("1.2.3.4", "/search?q=bar"),
                e("1.2.3.4", "/search")
        );

        AnalysisResult result = analyser.analyse(entries);

        assertEquals(List.of("/search"), values(result.topUrls()));
        assertEquals(3L, result.topUrls().entries().get(0).count());
    }

    @Test
    void urlWithoutQueryString_unchanged() {
        List<LogEntry> entries = List.of(
                e("1.2.3.4", "/path/"),
                e("1.2.3.4", "/path/")
        );

        assertEquals(List.of("/path/"), values(analyser.analyse(entries).topUrls()));
    }

    @Test
    void topUrls_returnedInDescendingOrder() {
        List<LogEntry> entries = List.of(
                e("1.2.3.4", "/a"),
                e("1.2.3.4", "/b"), e("1.2.3.4", "/b"),
                e("1.2.3.4", "/c"), e("1.2.3.4", "/c"), e("1.2.3.4", "/c")
        );

        AnalysisResult result = analyser.analyse(entries);

        assertEquals(List.of("/c", "/b", "/a"), values(result.topUrls()));
        assertFalse(result.topUrls().tied());
    }

    @Test
    void topUrls_ranksAreOneBasedAndSequential() {
        List<LogEntry> entries = List.of(
                e("1.2.3.4", "/a"),
                e("1.2.3.4", "/b"), e("1.2.3.4", "/b"),
                e("1.2.3.4", "/c"), e("1.2.3.4", "/c"), e("1.2.3.4", "/c")
        );

        List<RankedEntry> topUrls = analyser.analyse(entries).topUrls().entries();

        assertEquals(1, topUrls.get(0).rank());
        assertEquals(2, topUrls.get(1).rank());
        assertEquals(3, topUrls.get(2).rank());
    }

    @Test
    void topUrls_countsReflectActualFrequency() {
        List<LogEntry> entries = List.of(
                e("1.2.3.4", "/a"),
                e("1.2.3.4", "/b"), e("1.2.3.4", "/b"),
                e("1.2.3.4", "/c"), e("1.2.3.4", "/c"), e("1.2.3.4", "/c")
        );

        List<RankedEntry> topUrls = analyser.analyse(entries).topUrls().entries();

        assertEquals(3L, topUrls.get(0).count());
        assertEquals(2L, topUrls.get(1).count());
        assertEquals(1L, topUrls.get(2).count());
    }

    @Test
    void topUrlsFewerThanThree_returnsAll() {
        List<LogEntry> entries = List.of(
                e("1.2.3.4", "/a"),
                e("1.2.3.4", "/b")
        );

        assertEquals(2, analyser.analyse(entries).topUrls().entries().size());
    }

    @Test
    void topUrlsTied_flaggedWhenFourthSharesRankThreeCount() {
        List<LogEntry> entries = List.of(
                e("ip", "/a"), e("ip", "/a"), e("ip", "/a"),
                e("ip", "/b"), e("ip", "/b"),
                e("ip", "/c"),
                e("ip", "/d")
        );

        AnalysisResult result = analyser.analyse(entries);

        assertTrue(result.topUrls().tied());
        assertEquals(3, result.topUrls().entries().size());
    }

    @Test
    void allUrlsHaveSameCount_tieStartsAtPositionOne() {
        List<LogEntry> entries = List.of(
                e("ip1", "/a"),
                e("ip2", "/b"),
                e("ip3", "/c"),
                e("ip4", "/d")
        );

        AnalysisResult result = analyser.analyse(entries);

        assertTrue(result.topUrls().tied());
        assertEquals(1, result.topUrls().entries().get(0).rank());
    }

    @Test
    void topUrlsNotTied_whenFourthHasDifferentCount() {
        List<LogEntry> entries = List.of(
                e("ip", "/a"), e("ip", "/a"), e("ip", "/a"),
                e("ip", "/b"), e("ip", "/b"),
                e("ip", "/c"), e("ip", "/c"),
                e("ip", "/d")
        );

        assertFalse(analyser.analyse(entries).topUrls().tied());
    }

    @Test
    void topIps_returnedInDescendingOrder() {
        List<LogEntry> entries = List.of(
                e("ip1", "/a"),
                e("ip2", "/a"), e("ip2", "/a"),
                e("ip3", "/a"), e("ip3", "/a"), e("ip3", "/a")
        );

        AnalysisResult result = analyser.analyse(entries);

        assertEquals(List.of("ip3", "ip2", "ip1"), values(result.mostActiveIpAddresses()));
    }

    @Test
    void topIps_ranksAreOneBasedAndSequential() {
        List<LogEntry> entries = List.of(
                e("ip1", "/a"),
                e("ip2", "/a"), e("ip2", "/a"),
                e("ip3", "/a"), e("ip3", "/a"), e("ip3", "/a")
        );

        List<RankedEntry> topIps = analyser.analyse(entries).mostActiveIpAddresses().entries();

        assertEquals(1, topIps.get(0).rank());
        assertEquals(2, topIps.get(1).rank());
        assertEquals(3, topIps.get(2).rank());
    }

    @Test
    void topIps_countsReflectActualFrequency() {
        List<LogEntry> entries = List.of(
                e("ip1", "/a"),
                e("ip2", "/a"), e("ip2", "/a"),
                e("ip3", "/a"), e("ip3", "/a"), e("ip3", "/a")
        );

        List<RankedEntry> topIps = analyser.analyse(entries).mostActiveIpAddresses().entries();

        assertEquals(3L, topIps.get(0).count());
        assertEquals(2L, topIps.get(1).count());
        assertEquals(1L, topIps.get(2).count());
    }

    @Test
    void topIpsTied_flaggedWhenFourthSharesRankThreeCount() {
        List<LogEntry> entries = List.of(
                e("ip1", "/a"), e("ip1", "/a"), e("ip1", "/a"), e("ip1", "/a"),
                e("ip2", "/a"), e("ip2", "/a"), e("ip2", "/a"),
                e("ip3", "/a"), e("ip3", "/a"), e("ip3", "/a"),
                e("ip4", "/a"), e("ip4", "/a"), e("ip4", "/a")
        );

        assertTrue(analyser.analyse(entries).mostActiveIpAddresses().tied());
    }

    @Test
    void topIpsNotTied_exactlyThreeDistinctCounts() {
        List<LogEntry> entries = List.of(
                e("ip1", "/a"), e("ip1", "/a"), e("ip1", "/a"),
                e("ip2", "/a"), e("ip2", "/a"),
                e("ip3", "/a")
        );

        assertFalse(analyser.analyse(entries).mostActiveIpAddresses().tied());
    }

    @Test
    void invalidIp_leadingZerosInOctet_warningEmitted() {
        analyser.analyse(List.of(e("50.112.00.11", "/a")));

        verify(mockErr).println(contains("WARNING"));
        verify(mockErr).println(contains("50.112.00.11"));
    }

    @Test
    void invalidIpWarning_deduplicatedAcrossMultipleRequests() {
        analyser.analyse(List.of(
                e("50.112.00.11", "/a"),
                e("50.112.00.11", "/b"),
                e("50.112.00.11", "/c")
        ));

        verify(mockErr, times(1)).println(contains("50.112.00.11"));
    }

    @Test
    void validIp_noWarningEmitted() {
        analyser.analyse(List.of(e("192.168.1.1", "/a")));

        verifyNoInteractions(mockErr);
    }

    @Test
    void hostnameInsteadOfIp_warningEmitted() {
        analyser.analyse(List.of(e("example.com", "/a")));

        verify(mockErr).println(contains("WARNING"));
    }

    @Test
    void ipv6Address_warningEmitted() {
        analyser.analyse(List.of(e("::1", "/a")));

        verify(mockErr).println(contains("WARNING"));
        verify(mockErr).println(contains("::1"));
    }

    @Test
    void sampleLogData_correctResults() {
        List<LogEntry> entries = List.of(
                e("177.71.128.21", "/intranet-analytics/"),
                e("168.41.191.40", "http://example.net/faq/"),
                e("168.41.191.41", "/this/page/does/not/exist/"),
                e("168.41.191.40", "http://example.net/blog/category/meta/"),
                e("177.71.128.21", "/blog/2018/08/survey-your-opinion-matters/"),
                e("168.41.191.9", "/docs/manage-users/"),
                e("168.41.191.40", "/blog/category/community/"),
                e("168.41.191.34", "/faq/"),
                e("177.71.128.21", "/docs/manage-websites/"),
                e("50.112.00.28", "/faq/how-to-install/"),
                e("50.112.00.11", "/asset.js"),
                e("72.44.32.11", "/to-an-error"),
                e("72.44.32.10", "/"),
                e("168.41.191.9", "/docs/"),
                e("168.41.191.43", "/moved-permanently"),
                e("168.41.191.43", "/temp-redirect"),
                e("168.41.191.40", "/docs/manage-websites/"),
                e("168.41.191.34", "/faq/how-to/"),
                e("72.44.32.10", "/translations/"),
                e("79.125.00.21", "/newsletter/"),
                e("50.112.00.11", "/hosting/"),
                e("72.44.32.10", "/download/counter/"),
                e("50.112.00.11", "/asset.css")
        );

        AnalysisResult result = analyser.analyse(entries);

        assertEquals(11, result.uniqueIpCount());

        RankedEntry topUrl = result.topUrls().entries().get(0);
        assertEquals(1, topUrl.rank());
        assertEquals("/docs/manage-websites/", topUrl.value());
        assertEquals(2L, topUrl.count());
        assertTrue(result.topUrls().tied());
        assertEquals(3, result.topUrls().entries().size());

        RankedEntry topIp = result.mostActiveIpAddresses().entries().get(0);
        assertEquals(1, topIp.rank());
        assertEquals("168.41.191.40", topIp.value());
        assertEquals(4L, topIp.count());
        assertTrue(result.mostActiveIpAddresses().tied());
        assertEquals(3, result.mostActiveIpAddresses().entries().size());
    }
}
