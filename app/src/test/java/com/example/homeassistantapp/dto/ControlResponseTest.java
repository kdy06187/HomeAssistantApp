package com.example.homeassistantapp.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

public class ControlResponseTest {
    private final Gson gson = new Gson();

    @Test
    public void successMessage_isSucceeded() {
        ControlResponse r = gson.fromJson("{\"status\":\"ON\",\"message\":\"명령 성공!\"}", ControlResponse.class);
        assertTrue(r.isCommandSucceeded());
        assertEquals("ON", r.status);
    }

    @Test
    public void failureMessageWith200_isNotSucceeded() {
        ControlResponse r = gson.fromJson("{\"status\":\"UNKNOWN\",\"message\":\"명령 실패!\"}", ControlResponse.class);
        assertFalse(r.isCommandSucceeded());
        assertEquals("UNKNOWN", r.status);
    }

    @Test
    public void missingMessage_isNotSucceeded() {
        ControlResponse r = gson.fromJson("{\"status\":\"ON\"}", ControlResponse.class);
        assertFalse(r.isCommandSucceeded());
    }
}
