package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class QrPayloadTest {
    @Test
    public void matterQr_ok() {
        assertEquals(QrPayload.Check.OK, QrPayload.check("MT:Y3.13OTB00KA0648G00"));
        assertNull(QrPayload.rejectMessage(QrPayload.Check.OK));
    }

    @Test
    public void notMatter_rejected() {
        assertEquals(QrPayload.Check.NOT_MATTER, QrPayload.check("https://example.com"));
        assertEquals(QrPayload.Check.NOT_MATTER, QrPayload.check("mt:Y3.13OTB00"));
        assertNotNull(QrPayload.rejectMessage(QrPayload.Check.NOT_MATTER));
    }

    @Test
    public void emptyOrBadChars_rejected() {
        assertEquals(QrPayload.Check.EMPTY, QrPayload.check(""));
        assertEquals(QrPayload.Check.EMPTY, QrPayload.check(null));
        assertEquals(QrPayload.Check.BAD_CHARS, QrPayload.check("MT:"));
        assertEquals(QrPayload.Check.BAD_CHARS, QrPayload.check("MT:y3.abc"));
        assertEquals(QrPayload.Check.BAD_CHARS, QrPayload.check("MT:Y3 13"));
        assertEquals(QrPayload.Check.BAD_CHARS, QrPayload.check("MT:Y3\n13"));
    }

    @Test
    public void masked_showsOnlyFirstChars() {
        assertEquals("MT:Y3.…", QrPayload.masked("MT:Y3.13OTB00KA0648G00"));
        assertEquals("MT:Y3", QrPayload.masked("MT:Y3"));
        assertEquals("", QrPayload.masked(null));
    }
}
