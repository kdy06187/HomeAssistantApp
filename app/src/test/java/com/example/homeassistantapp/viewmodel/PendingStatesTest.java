package com.example.homeassistantapp.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PendingStatesTest {

    @Test
    public void failure_restoresStateBeforeRequest() {
        PendingStates states = new PendingStates();
        states.begin("A", "ON");
        assertEquals("ON", states.finish("A", null));
    }

    @Test
    public void success_showsServerState() {
        PendingStates states = new PendingStates();
        states.begin("A", "OFF");
        assertEquals("ON", states.finish("A", "ON"));
    }

    @Test
    public void overlappingRequests_keepLoadingUntilLastOneEnds() {
        PendingStates states = new PendingStates();
        states.begin("A", "ON");
        states.begin("A", PendingStates.LOADING); // 두 번째 요청은 LOADING일 때 시작
        assertEquals(PendingStates.LOADING, states.finish("A", null));
        assertEquals("ON", states.finish("A", null));
    }

    @Test
    public void overlappingFailures_inAnyOrder_neverEndInLoadingOrNull() {
        PendingStates states = new PendingStates();
        states.begin("A", "OFF");
        states.begin("A", PendingStates.LOADING);
        states.finish("A", null);
        String last = states.finish("A", null);
        assertEquals("OFF", last);
    }

    @Test
    public void newerServerState_winsOverOlderOne() {
        PendingStates states = new PendingStates();
        states.begin("A", "ON");
        states.begin("A", PendingStates.LOADING);
        states.finish("A", "OFF");
        assertEquals("OFF", states.finish("A", null));
    }

    @Test
    public void rememberedListState_isUsedOnFailure() {
        PendingStates states = new PendingStates();
        states.begin("A", "ON");
        states.remember("A", "OFFLINE"); // 요청 중에 목록을 새로 받음
        assertEquals("OFFLINE", states.finish("A", null));
    }

    @Test
    public void unknownDevice_restoresNull() {
        PendingStates states = new PendingStates();
        states.begin("A", null);
        assertNull(states.finish("A", null));
    }

    @Test
    public void finishWithoutBegin_doesNotStayLoading() {
        PendingStates states = new PendingStates();
        assertEquals("ON", states.finish("A", "ON"));
    }

    @Test
    public void devicesAreTrackedSeparately() {
        PendingStates states = new PendingStates();
        states.begin("A", "ON");
        states.begin("B", "OFF");
        assertEquals("OFF", states.finish("B", null));
        assertEquals("ON", states.finish("A", null));
    }
}
