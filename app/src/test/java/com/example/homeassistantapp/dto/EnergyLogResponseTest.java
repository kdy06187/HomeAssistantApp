package com.example.homeassistantapp.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

public class EnergyLogResponseTest {
    private final Gson gson = new Gson();

    @Test
    public void normalResponse_usesServerState() {
        EnergyLogResponse r = gson.fromJson(
                "{\"device_status\":\"ON\",\"logs\":[{\"timestamp\":\"2026-11-16 09:30:00\",\"totalEnergy_mWh\":1048576}]}",
                EnergyLogResponse.class);
        assertFalse(r.hasError());
        assertEquals("ON", r.stateOr("OFF"));
        assertEquals(1048576L, r.getLogs().get(0).getTotalEnergy_mWh());
    }

    @Test
    public void errorResponse_keepsOriginalState() {
        // 이 API는 실패도 HTTP 200 + {"error"}로 온다.
        EnergyLogResponse r = gson.fromJson("{\"error\":\"등록되지 않은 기기입니다.\"}", EnergyLogResponse.class);
        assertTrue(r.hasError());
        assertEquals("OFF", r.stateOr("OFF"));
    }

    @Test
    public void missingStatus_doesNotOverwriteWithNull() {
        EnergyLogResponse r = gson.fromJson("{\"logs\":[]}", EnergyLogResponse.class);
        assertFalse(r.hasError());
        assertEquals("ON", r.stateOr("ON"));
    }

    @Test
    public void largeTotalEnergy_parsedAs64Bit() {
        EnergyLogResponse r = gson.fromJson(
                "{\"device_status\":\"ON\",\"logs\":[{\"timestamp\":\"2026-11-16 09:30:00\",\"totalEnergy_mWh\":5000000000}]}",
                EnergyLogResponse.class);
        assertEquals(5000000000L, r.getLogs().get(0).getTotalEnergy_mWh());
    }
}
