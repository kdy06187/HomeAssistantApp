package com.example.homeassistantapp.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;

import org.junit.Test;

public class ControlResponseTest {
    private final Gson gson = new Gson();

    @Test
    public void successBody_readsDeviceIdAndState() {
        ControlResponse r = gson.fromJson("{\"deviceId\":\"TCP_A1B2C3D4E5F6\",\"state\":\"OFF\"}", ControlResponse.class);
        assertEquals("TCP_A1B2C3D4E5F6", r.deviceId);
        assertEquals("OFF", r.state);
    }

    @Test
    public void acceptedBody_readsStateBeforeChange_andIgnoresExtraFields() {
        ControlResponse r = gson.fromJson(
                "{\"deviceId\":\"SN12345\",\"state\":\"ON\",\"pendingCommand\":\"OFF\",\"delaySeconds\":45}",
                ControlResponse.class);
        assertEquals("ON", r.state);
    }

    @Test
    public void oldStatusKey_isNotRead() {
        ControlResponse r = gson.fromJson("{\"status\":\"ON\",\"message\":\"명령 성공!\"}", ControlResponse.class);
        assertNull(r.state);
    }
}
