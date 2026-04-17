package com.loganalyser.analyser.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class IpValidatorTest {

    @Test
    void standardAddresses_valid() {
        assertTrue(IpValidator.isValid("192.168.1.1"));
        assertTrue(IpValidator.isValid("0.0.0.0"));
        assertTrue(IpValidator.isValid("255.255.255.255"));
        assertTrue(IpValidator.isValid("10.0.0.1"));
    }

    @Test
    void leadingZerosInOctet_invalid() {
        assertFalse(IpValidator.isValid("50.112.00.11"));
        assertFalse(IpValidator.isValid("01.02.03.04"));
        assertFalse(IpValidator.isValid("192.168.01.1"));
    }

    @Test
    void octetOutOfRange_invalid() {
        assertFalse(IpValidator.isValid("256.1.1.1"));
        assertFalse(IpValidator.isValid("1.1.1.999"));
    }

    @Test
    void wrongFormat_invalid() {
        assertFalse(IpValidator.isValid("example.com"));
        assertFalse(IpValidator.isValid("1.2.3"));
        assertFalse(IpValidator.isValid("1.2.3.4.5"));
        assertFalse(IpValidator.isValid(""));
    }
}
