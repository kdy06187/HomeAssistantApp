package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DeviceStateLabelTest {

    @Test
    public void onAndOff_areConnected() {
        assertEquals("연결됨", DeviceStateLabel.textOf("ON"));
        assertEquals("연결됨", DeviceStateLabel.textOf("OFF"));
    }

    @Test
    public void offline_isOffline() {
        assertEquals("오프라인", DeviceStateLabel.textOf("OFFLINE"));
    }

    @Test
    public void unknownValues_areUnknown() {
        assertEquals("알 수 없음", DeviceStateLabel.textOf("UNKNOWN"));
        assertEquals("알 수 없음", DeviceStateLabel.textOf("STANDBY"));
        assertEquals("알 수 없음", DeviceStateLabel.textOf(null));
        assertEquals(DeviceStateLabel.Kind.UNKNOWN, DeviceStateLabel.kindOf(""));
    }
}
