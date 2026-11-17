package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * 불일치 #8: ON/OFF만 "연결됨", OFFLINE은 "오프라인", 나머지는 "알 수 없음"(계약서 §1.2).
 */
public class DeviceStateLabelTest {

    // ON과 OFF는 기기가 응답한 상태라 연결됨이다
    @Test
    public void onAndOff_areConnected() {
        assertEquals("연결됨", DeviceStateLabel.textOf("ON"));
        assertEquals("연결됨", DeviceStateLabel.textOf("OFF"));
    }

    // OFFLINE은 오프라인이다
    @Test
    public void offline_isOffline() {
        assertEquals("오프라인", DeviceStateLabel.textOf("OFFLINE"));
    }

    // UNKNOWN, 처음 보는 값, null은 알 수 없음이다(앱이 죽지 않아야 한다)
    @Test
    public void unknownValues_areUnknown() {
        assertEquals("알 수 없음", DeviceStateLabel.textOf("UNKNOWN"));
        assertEquals("알 수 없음", DeviceStateLabel.textOf("STANDBY"));
        assertEquals("알 수 없음", DeviceStateLabel.textOf(null));
        assertEquals(DeviceStateLabel.Kind.UNKNOWN, DeviceStateLabel.kindOf(""));
    }
}
