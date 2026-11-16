package com.example.homeassistantapp.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * 불일치 #7: E7 응답(계약서 v1.0 §3 E7)을 Retrofit 과 같은 Gson 으로 읽어 상태 처리를 확인한다.
 */
public class EnergyLogResponseTest {
    private final Gson gson = new Gson();

    // 정상 응답이면 서버 상태를 쓴다
    @Test
    public void normalResponse_usesServerState() {
        EnergyLogResponse r = gson.fromJson(
                "{\"device_status\":\"ON\",\"logs\":[{\"timestamp\":\"2026-11-16 09:30:00\",\"totalEnergy_mWh\":1048576}]}",
                EnergyLogResponse.class);
        assertFalse(r.hasError());
        assertEquals("ON", r.stateOr("OFF"));
        assertEquals(1048576L, r.getLogs().get(0).getTotalEnergy_mWh());
    }

    // 오류 응답이면 원래 상태를 유지한다
    @Test
    public void errorResponse_keepsOriginalState() {
        // 계약서 E7: 없는 기기·서버 오류도 HTTP 200 + {"error"}
        EnergyLogResponse r = gson.fromJson("{\"error\":\"등록되지 않은 기기입니다.\"}", EnergyLogResponse.class);
        assertTrue(r.hasError());
        assertEquals("OFF", r.stateOr("OFF"));
    }

    // 상태가 비어 있으면 null로 덮어쓰지 않는다
    @Test
    public void missingStatus_doesNotOverwriteWithNull() {
        EnergyLogResponse r = gson.fromJson("{\"logs\":[]}", EnergyLogResponse.class);
        assertFalse(r.hasError());
        assertEquals("ON", r.stateOr("ON"));
    }

    // 큰 누적값도 64비트로 읽는다
    @Test
    public void largeTotalEnergy_parsedAs64Bit() {
        EnergyLogResponse r = gson.fromJson(
                "{\"device_status\":\"ON\",\"logs\":[{\"timestamp\":\"2026-11-16 09:30:00\",\"totalEnergy_mWh\":5000000000}]}",
                EnergyLogResponse.class);
        assertEquals(5000000000L, r.getLogs().get(0).getTotalEnergy_mWh());
    }
}
