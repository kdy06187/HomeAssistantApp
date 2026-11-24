package com.example.homeassistantapp.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EnergyPollScheduleTest {

    @Test
    public void firstStart_fetchesImmediately() {
        assertEquals(0L, new EnergyPollSchedule().start(1_000L));
    }

    @Test
    public void whileVisible_nextRequestIn15Seconds() {
        EnergyPollSchedule s = new EnergyPollSchedule();
        s.start(0L);
        s.requestStarted();
        assertEquals(EnergyPollSchedule.INTERVAL_MS, s.finished(2_000L));
    }

    @Test
    public void afterStop_responseDoesNotSchedule() {
        EnergyPollSchedule s = new EnergyPollSchedule();
        s.start(0L);
        s.requestStarted();
        s.stop();
        assertEquals(EnergyPollSchedule.NONE, s.finished(2_000L));
    }

    @Test
    public void restartSoon_waitsOnlyTheRemainder() {
        EnergyPollSchedule s = new EnergyPollSchedule();
        s.start(0L);
        s.requestStarted();
        s.finished(10_000L);
        s.stop();
        assertEquals(11_000L, s.start(14_000L));
        s.stop();
        assertEquals(0L, s.start(40_000L));
    }

    @Test
    public void restartWhileInFlight_waitsForThatResponse() {
        EnergyPollSchedule s = new EnergyPollSchedule();
        s.start(0L);
        s.requestStarted();
        s.stop();
        assertEquals(EnergyPollSchedule.NONE, s.start(1_000L));
        assertEquals(EnergyPollSchedule.INTERVAL_MS, s.finished(2_000L));
    }

    @Test
    public void retry_onlyWhenVisibleAndIdle() {
        EnergyPollSchedule s = new EnergyPollSchedule();
        assertFalse(s.canFetchNow());
        s.start(0L);
        assertTrue(s.canFetchNow());
        s.requestStarted();
        assertFalse(s.canFetchNow());
    }

    @Test
    public void reset_forgetsLastRequest() {
        EnergyPollSchedule s = new EnergyPollSchedule();
        s.start(0L);
        s.requestStarted();
        s.finished(1_000L);
        s.stop();
        s.reset();
        assertEquals(0L, s.start(2_000L));
    }
}
