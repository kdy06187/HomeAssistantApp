package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;

import com.example.homeassistantapp.dto.Device;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

public class ListScreenStateTest {
    @Test
    public void neverLoaded_waitingOrFailed() {
        assertEquals(ListScreenState.LOADING, ListScreenState.of(null, false));
        assertEquals(ListScreenState.LOAD_FAILED, ListScreenState.of(null, true));
    }

    @Test
    public void serverSaidZero_isEmpty_notFailed() {
        assertEquals(ListScreenState.EMPTY, ListScreenState.of(new ArrayList<>(), false));
        // 0개를 받은 뒤 새로고침이 실패해도 "불러오지 못함"으로 바꾸지 않는다(마지막 결과 + 배너).
        assertEquals(ListScreenState.EMPTY, ListScreenState.of(new ArrayList<>(), true));
    }

    @Test
    public void hasDevices_contentEvenAfterLaterFailure() {
        assertEquals(ListScreenState.CONTENT, ListScreenState.of(Collections.singletonList(new Device()), false));
        assertEquals(ListScreenState.CONTENT, ListScreenState.of(Collections.singletonList(new Device()), true));
    }

    @Test
    public void onlyPairingItem_isContent() {
        // 등록 중 임시 항목만 있어도 목록 자리에 그 항목을 보인다.
        assertEquals(ListScreenState.CONTENT, ListScreenState.of(Collections.singletonList(DeviceOrderTest.pairing()), true));
    }
}
