package com.example.homeassistantapp.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * 불일치 #5: E2 성공 판정은 HTTP 200만으로 하지 않고 message == "명령 성공!"까지 본다(계약서 E2, §4.1).
 */
public class ControlResponseTest {
    private final Gson gson = new Gson();

    // 명령 성공 응답이면 성공이다
    @Test
    public void successMessage_isSucceeded() {
        ControlResponse r = gson.fromJson("{\"status\":\"ON\",\"message\":\"명령 성공!\"}", ControlResponse.class);
        assertTrue(r.isCommandSucceeded());
        assertEquals("ON", r.status);
    }

    // 200으로 와도 "명령 실패!"면 실패다 (§6 I-01)
    @Test
    public void failureMessageWith200_isNotSucceeded() {
        ControlResponse r = gson.fromJson("{\"status\":\"UNKNOWN\",\"message\":\"명령 실패!\"}", ControlResponse.class);
        assertFalse(r.isCommandSucceeded());
        assertEquals("UNKNOWN", r.status);
    }

    // message가 없으면 실패로 본다
    @Test
    public void missingMessage_isNotSucceeded() {
        ControlResponse r = gson.fromJson("{\"status\":\"ON\"}", ControlResponse.class);
        assertFalse(r.isCommandSucceeded());
    }
}
