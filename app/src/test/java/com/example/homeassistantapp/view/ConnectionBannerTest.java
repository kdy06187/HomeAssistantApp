package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.homeassistantapp.network.ConnectionState;

import org.junit.Test;

public class ConnectionBannerTest {
    @Test
    public void ok_hidden() {
        assertFalse(ConnectionBanner.of(ConnectionState.Kind.OK, false).visible);
        assertFalse(ConnectionBanner.of(null, false).visible);
    }

    @Test
    public void unreachable_retry() {
        ConnectionBanner b = ConnectionBanner.of(ConnectionState.Kind.UNREACHABLE, false);
        assertTrue(b.visible);
        assertEquals(ConnectionBanner.Action.RETRY, b.action);
        assertEquals(ConnectionBanner.Action.RETRY, ConnectionBanner.of(ConnectionState.Kind.UNREACHABLE, true).action);
    }

    @Test
    public void tokenError_settingsButtonExceptOnSettingsTab() {
        ConnectionBanner other = ConnectionBanner.of(ConnectionState.Kind.TOKEN_ERROR, false);
        assertTrue(other.visible);
        assertEquals(ConnectionBanner.Action.OPEN_SETTINGS, other.action);
        ConnectionBanner settings = ConnectionBanner.of(ConnectionState.Kind.TOKEN_ERROR, true);
        assertTrue(settings.visible);
        assertEquals(ConnectionBanner.Action.NONE, settings.action);
    }
}
