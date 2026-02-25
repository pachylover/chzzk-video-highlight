package com.pachy.highlight.util.func;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilTest {

    @Test
    void sanitizeChat_nullInput_returnsNull() {
        assertNull(StringUtil.sanitizeChat(null));
    }

    @Test
    void sanitizeChat_removedNullCharacters() {
        String raw = "Hello\u0000World\u0000";
        String cleaned = StringUtil.sanitizeChat(raw);
        assertEquals("HelloWorld", cleaned);
    }

    @Test
    void sanitizeChat_trimsWhitespace() {
        String raw = "  foo bar  ";
        assertEquals("foo bar", StringUtil.sanitizeChat(raw));
    }

    @Test
    void sanitizeChat_noChangeForSafeString() {
        String raw = "normal message";
        assertEquals(raw, StringUtil.sanitizeChat(raw));
    }
}