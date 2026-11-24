package com.example.homeassistantapp.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WifiPasswordTest {
    @Test
    public void neverSaved_isEmptyAndUsable() {
        WifiPassword p = WifiPassword.of(false, null);
        assertEquals(WifiPassword.State.NONE, p.state);
        assertEquals("", p.value);
        assertTrue(p.isUsable());
    }

    @Test
    public void decrypted_ok() {
        WifiPassword p = WifiPassword.of(true, "pw");
        assertEquals(WifiPassword.State.OK, p.state);
        assertEquals("pw", p.value);
        assertTrue(WifiPassword.of(true, "").isUsable());
    }

    @Test
    public void storedButUnreadable_needsReentry() {
        WifiPassword p = WifiPassword.of(true, null);
        assertEquals(WifiPassword.State.UNREADABLE, p.state);
        assertNull(p.value);
        assertFalse(p.isUsable());
    }
}
