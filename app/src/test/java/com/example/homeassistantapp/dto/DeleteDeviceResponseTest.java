package com.example.homeassistantapp.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

public class DeleteDeviceResponseTest {
    private final Gson gson = new Gson();

    private DeleteDeviceResponse parse(String unpaired) {
        return gson.fromJson("{\"deviceId\":\"SN12345\",\"deletedAt\":\"2026-11-17T03:00:00Z\","
                + "\"restorableUntil\":\"2027-02-15T03:00:00Z\"" + unpaired + "}", DeleteDeviceResponse.class);
    }

    @Test
    public void readsAllKeys() {
        DeleteDeviceResponse r = parse(",\"unpaired\":true");
        assertEquals("SN12345", r.deviceId);
        assertEquals("2026-11-17T03:00:00Z", r.deletedAt);
        assertEquals("2027-02-15T03:00:00Z", r.restorableUntil);
        assertFalse(r.needsDeviceReset());
    }

    @Test
    public void unpairedFalse_needsReset() {
        assertTrue(parse(",\"unpaired\":false").needsDeviceReset());
    }

    @Test
    public void unpairedMissingOrNull_noWarning() {
        assertFalse(parse("").needsDeviceReset());
        assertFalse(parse(",\"unpaired\":null").needsDeviceReset());
    }
}
