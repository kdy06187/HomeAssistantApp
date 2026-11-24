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
                "{\"deviceId\":\"SN12345\",\"state\":\"ON\",\"logs\":[{\"timestamp\":\"2026-11-16T00:20:00Z\",\"totalEnergy_mWh\":1048576}],\"truncated\":true}",
                EnergyLogResponse.class);
        assertEquals("SN12345", r.deviceId);
        assertEquals("ON", r.stateOr("OFF"));
        assertEquals(1048576L, r.getLogs().get(0).getTotalEnergy_mWh());
    }

    @Test
    public void summaryResponse_readsBuckets() {
        EnergyLogResponse r = gson.fromJson(
                "{\"deviceId\":\"SN12345\",\"state\":\"ON\",\"period\":\"week\",\"utcOffset\":\"+09:00\","
                        + "\"buckets\":[{\"start\":\"2026-11-01\",\"end\":\"2026-11-01\",\"energyUsed_mWh\":1200,\"sampleCount\":1440},"
                        + "{\"start\":\"2026-11-02\",\"end\":\"2026-11-08\",\"energyUsed_mWh\":0,\"sampleCount\":0}]}",
                EnergyLogResponse.class);
        assertEquals("week", r.period);
        assertEquals(2, r.buckets.size());
        assertEquals("2026-11-01", r.buckets.get(0).start);
        assertEquals(1200L, r.buckets.get(0).energyUsedMwh);
        assertEquals(0L, r.buckets.get(1).sampleCount);
    }

    @Test
    public void truncatedFlag() {
        assertTrue(gson.fromJson("{\"logs\":[],\"truncated\":true}", EnergyLogResponse.class).isTruncated());
        assertFalse(gson.fromJson("{\"logs\":[],\"truncated\":false}", EnergyLogResponse.class).isTruncated());
        assertFalse(gson.fromJson("{\"logs\":[]}", EnergyLogResponse.class).isTruncated());
    }

    @Test
    public void missingState_doesNotOverwriteWithNull() {
        EnergyLogResponse r = gson.fromJson("{\"logs\":[]}", EnergyLogResponse.class);
        assertEquals("ON", r.stateOr("ON"));
    }

    @Test
    public void oldDeviceStatusKey_isNotRead() {
        EnergyLogResponse r = gson.fromJson("{\"device_status\":\"ON\",\"logs\":[]}", EnergyLogResponse.class);
        assertEquals("OFF", r.stateOr("OFF"));
    }

    @Test
    public void largeTotalEnergy_parsedAs64Bit() {
        EnergyLogResponse r = gson.fromJson(
                "{\"state\":\"ON\",\"logs\":[{\"timestamp\":\"2026-11-16T00:20:00Z\",\"totalEnergy_mWh\":5000000000}]}",
                EnergyLogResponse.class);
        assertEquals(5000000000L, r.getLogs().get(0).getTotalEnergy_mWh());
    }
}
