package com.example.homeassistantapp.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// 토큰은 모두 테스트용 가짜 값.
public class TokenFormatTest {

    @Test
    public void normalize_trimsSurroundingWhitespaceAndNewlines() {
        assertEquals("test-token-abc", TokenFormat.normalize("  test-token-abc\n"));
        assertEquals("test-token-abc", TokenFormat.normalize("\r\ntest-token-abc\t"));
        assertEquals("", TokenFormat.normalize(null));
    }

    @Test
    public void isValid_acceptsPrintableAscii() {
        assertTrue(TokenFormat.isValid("test-token_0123456789ABCDEF.~+/="));
        assertTrue(TokenFormat.isValid("!~"));
    }

    @Test
    public void isValid_rejectsEmptyAndNull() {
        assertFalse(TokenFormat.isValid(""));
        assertFalse(TokenFormat.isValid(null));
    }

    @Test
    public void isValid_rejectsInnerNewlineSpaceTabAndControl() {
        assertFalse(TokenFormat.isValid("test-token\nabc"));
        assertFalse(TokenFormat.isValid("test-token\rabc"));
        assertFalse(TokenFormat.isValid("test token"));
        assertFalse(TokenFormat.isValid("test\ttoken"));
        assertFalse(TokenFormat.isValid("test\u0000token"));
        assertFalse(TokenFormat.isValid("test\u007Ftoken"));
    }

    @Test
    public void isValid_rejectsNonAscii() {
        assertFalse(TokenFormat.isValid("토큰test"));
        assertFalse(TokenFormat.isValid("test\u00A0token"));
    }
}
