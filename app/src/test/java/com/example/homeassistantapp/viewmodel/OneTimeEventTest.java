package com.example.homeassistantapp.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class OneTimeEventTest {

    @Test
    public void content_isGivenOnlyOnce() {
        OneTimeEvent<String> event = new OneTimeEvent<>("제어 실패: 명령 실패!");
        assertEquals("제어 실패: 명령 실패!", event.takeIfNotHandled());
        assertNull(event.takeIfNotHandled()); // 탭 전환 등으로 다시 전달돼도 다시 뜨지 않음
    }
}
