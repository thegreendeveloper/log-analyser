package com.loganalyser.analyser.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Package-private utility for validating IPv4 address strings.
 *
 * <p>An address is considered valid if it consists of four dot-separated decimal
 * octets, each in the range 0–255, with no leading zeros. Leading zeros are
 * flagged because some systems interpret them as octal (e.g. {@code 010} = 8),
 * making the address ambiguous.
 *
 * <h2>Why validation lives here and not in the parser</h2>
 *
 * <p>There are two distinct kinds of validation in this pipeline:
 * <ul>
 *   <li><b>Structural validation</b> — does the log line conform to the Combined
 *       Log Format? This is {@link com.loganalyser.parser.impl.CombinedFormatLogLineParser}'s
 *       responsibility. A line that cannot be structurally parsed is rejected
 *       entirely and never becomes a {@link com.loganalyser.model.LogEntry}.</li>
 *   <li><b>Semantic validation</b> — is the value we extracted meaningful? An IP
 *       address with leading zeros is structurally fine — the parser read it
 *       correctly from a well-formed log line. The question of whether the value
 *       itself is a valid IPv4 address is a semantic concern, not a structural one.
 *       That distinction belongs to the analysis layer, not the parsing layer.</li>
 * </ul>
 *
 * <p>Keeping semantic validation here avoids giving the parser two responsibilities
 * (extracting fields <em>and</em> warning about their content), and avoids
 * requiring the parser to accept an error stream it would otherwise not need.
 *
 * <p>This class is intentionally package-private — it is an implementation detail
 * of {@link DefaultLogAnalyser} and not part of the public API.
 */
class IpValidator {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$"
    );

    private IpValidator() {
    }

    static boolean isValid(String ip) {
        Matcher m = IPV4_PATTERN.matcher(ip);
        return m.matches() && allOctetsValid(m);
    }

    private static boolean allOctetsValid(Matcher m) {
        return IntStream.rangeClosed(1, 4)
                .mapToObj(m::group)
                .allMatch(IpValidator::isValidOctet);
    }

    private static boolean isValidOctet(String octet) {
        return hasNoLeadingZero(octet) && isInRange(octet);
    }

    private static boolean hasNoLeadingZero(String octet) {
        return octet.length() == 1 || !octet.startsWith("0");
    }

    private static boolean isInRange(String octet) {
        return Integer.parseInt(octet) <= 255;
    }
}
