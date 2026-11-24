package com.example.homeassistantapp.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CommissionStatusTest {
    @Test
    public void flow_sendingToSucceeded() {
        CommissionStatus s = CommissionStatus.sending("플러그");
        assertEquals(CommissionStatus.Phase.SENDING, s.phase);
        assertTrue(s.isActive());
        s = s.accepted(1_000L).withServerState("PENDING");
        assertEquals(CommissionStatus.Phase.PENDING, s.phase);
        assertEquals(1_000L, s.startedAtMs);
        s = s.withServerState("RUNNING");
        assertEquals(CommissionStatus.Phase.RUNNING, s.phase);
        s = s.succeeded(true);
        assertEquals(CommissionStatus.Phase.SUCCEEDED, s.phase);
        assertTrue(s.restored);
        assertFalse(s.isActive());
        assertEquals("플러그", s.deviceName);
        assertEquals(1_000L, s.startedAtMs);
    }

    @Test
    public void unknownOrSameState_keepsObject() {
        CommissionStatus running = CommissionStatus.sending("a").accepted(5L).withServerState("RUNNING");
        assertSame(running, running.withServerState("RUNNING"));
        assertSame(running, running.withServerState("WHATEVER"));
        assertSame(running, running.withServerState(null));
        // 접수 응답의 state가 비어 있어도 PENDING으로 시작한다.
        assertEquals(CommissionStatus.Phase.PENDING, CommissionStatus.sending("a").accepted(5L).withServerState(null).phase);
    }

    @Test
    public void failedAndRejectedKeepReasons() {
        CommissionStatus f = CommissionStatus.sending("a").accepted(5L).failed("COMMISSION_TIMEOUT", "시간 초과");
        assertEquals("COMMISSION_TIMEOUT", f.errorCode);
        assertEquals("시간 초과", f.serverMessage);
        assertFalse(f.isActive());
        CommissionStatus r = CommissionStatus.rejected("a", 409, "COMMISSION_BUSY", null);
        assertEquals(CommissionStatus.Phase.REJECTED, r.phase);
        assertEquals(409, r.httpCode);
        assertFalse(r.isActive());
        assertEquals(CommissionStatus.Phase.UNCONFIRMED, f.unconfirmed().phase);
    }
}
