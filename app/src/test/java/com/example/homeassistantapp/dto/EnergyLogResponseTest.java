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

    @Test
    public void 정상_응답이면_서버_상태를_쓴다() {
        EnergyLogResponse r = gson.fromJson(
                "{\"device_status\":\"ON\",\"logs\":[{\"timestamp\":\"2026-11-16 09:30:00\",\"totalEnergy_mWh\":1048576}]}",
                EnergyLogResponse.class);
        assertFalse(r.hasError());
        assertEquals("ON", r.stateOr("OFF"));
        assertEquals(1048576L, r.getLogs().get(0).getTotalEnergy_mWh());
    }

    @Test
    public void 오류_응답이면_원래_상태를_유지한다() {
        // 계약서 E7: 없는 기기·서버 오류도 HTTP 200 + {"error"}
        EnergyLogResponse r = gson.fromJson("{\"error\":\"등록되지 않은 기기입니다.\"}", EnergyLogResponse.class);
        assertTrue(r.hasError());
        assertEquals("OFF", r.stateOr("OFF"));
    }

    @Test
    public void 상태가_비어_있으면_null로_덮어쓰지_않는다() {
        EnergyLogResponse r = gson.fromJson("{\"logs\":[]}", EnergyLogResponse.class);
        assertFalse(r.hasError());
        assertEquals("ON", r.stateOr("ON"));
    }

    @Test
    public void 큰_누적값도_64비트로_읽는다() {
        EnergyLogResponse r = gson.fromJson(
                "{\"device_status\":\"ON\",\"logs\":[{\"timestamp\":\"2026-11-16 09:30:00\",\"totalEnergy_mWh\":5000000000}]}",
                EnergyLogResponse.class);
        assertEquals(5000000000L, r.getLogs().get(0).getTotalEnergy_mWh());
    }
}
