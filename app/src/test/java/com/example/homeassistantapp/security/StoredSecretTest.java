package com.example.homeassistantapp.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StoredSecretTest {
    @Test
    public void neverSaved_none() {
        StoredSecret s = StoredSecret.of(false, null);
        assertEquals(StoredSecret.State.NONE, s.state);
        assertEquals("", s.value);
        assertTrue(s.isUsable());
    }

    @Test
    public void emptyValue_countsAsNone() {
        assertEquals(StoredSecret.State.NONE, StoredSecret.of(true, "").state);
    }

    @Test
    public void decrypted_ok() {
        StoredSecret s = StoredSecret.of(true, "pw");
        assertEquals(StoredSecret.State.OK, s.state);
        assertEquals("pw", s.value);
    }

    @Test
    public void storedButUnreadable() {
        StoredSecret s = StoredSecret.of(true, null);
        assertEquals(StoredSecret.State.UNREADABLE, s.state);
        assertNull(s.value);
        assertFalse(s.isUsable());
    }
}
