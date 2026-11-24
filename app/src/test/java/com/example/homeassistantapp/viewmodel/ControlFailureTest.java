package com.example.homeassistantapp.viewmodel;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ControlFailureTest {

    @Test
    public void message_namesDeviceActionAndReason() {
        assertEquals("‘거실 전등’을 켜지 못했어요. 기기가 응답하지 않습니다.",
                ControlFailure.message("거실 전등", "ON", "기기가 응답하지 않습니다."));
        assertEquals("‘플러그’를 끄지 못했어요. 서버에 연결할 수 없어요.",
                ControlFailure.message("플러그", "OFF", ControlFailure.UNREACHABLE_SERVER));
    }

    @Test
    public void message_withoutNameOrReason() {
        assertEquals("기기를 켜지 못했어요.", ControlFailure.message(null, "ON", null));
        assertEquals("기기를 켜지 못했어요.", ControlFailure.message("", "ON", ""));
    }

    @Test
    public void keepsCommandForRetry() {
        ControlFailure failure = new ControlFailure("SN1", "플러그", "OFF", "x");
        assertEquals("SN1", failure.deviceId);
        assertEquals("OFF", failure.command);
    }

    @Test
    public void objectParticle_followsFinalConsonant() {
        assertEquals("을", ControlFailure.objectParticle("전등"));
        assertEquals("를", ControlFailure.objectParticle("플러그"));
        assertEquals("을(를)", ControlFailure.objectParticle("Plug 2"));
    }
}
