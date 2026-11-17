package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.homeassistantapp.dto.EnergyLog;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class EnergyLogTimelineTest {
    private static final TimeZone SEOUL = TimeZone.getTimeZone("Asia/Seoul"); // UTC+9

    private static EnergyLog log(String timestamp, long mWh) {
        EnergyLog l = new EnergyLog();
        l.setTimestamp(timestamp);
        l.setTotalEnergy_mWh(mWh);
        return l;
    }

    @Test
    public void utcTimestamp_isShownInLocalTime() {
        assertEquals("18:30", EnergyLogTimeline.toLocalTimeLabel("2026-11-16 09:30:00", SEOUL));
    }

    @Test
    public void utcTimestamp_crossesMidnight() {
        assertEquals("05:00", EnergyLogTimeline.toLocalTimeLabel("2026-11-16 20:00:00", SEOUL));
    }

    @Test
    public void badTimestamp_returnsEmpty() {
        assertEquals("", EnergyLogTimeline.toLocalTimeLabel("어제", SEOUL));
        assertEquals("", EnergyLogTimeline.toLocalTimeLabel(null, SEOUL));
    }

    @Test
    public void newestFirstLogs_areSortedOldestFirst() {
        List<EnergyLog> fromServer = Arrays.asList(
                log("2026-11-16 09:30:00", 300),
                log("2026-11-16 09:20:00", 200),
                log("2026-11-16 09:10:00", 100));
        List<EnergyLog> sorted = EnergyLogTimeline.oldestFirst(fromServer);
        assertEquals(100, sorted.get(0).getTotalEnergy_mWh());
        assertEquals(300, sorted.get(2).getTotalEnergy_mWh());
        // 원본 목록은 바꾸지 않는다
        assertEquals(300, fromServer.get(0).getTotalEnergy_mWh());
    }

    @Test
    public void cumulative_becomesIntervalUsage() {
        List<EnergyLog> logs = Arrays.asList(
                log("2026-11-16 09:00:00", 100),
                log("2026-11-16 09:10:00", 200),
                log("2026-11-16 09:20:00", 350));
        assertEquals(Arrays.asList(100L, 150L), EnergyLogTimeline.intervalUsage(logs));
    }

    @Test
    public void counterReset_becomesZero() {
        List<EnergyLog> logs = Arrays.asList(
                log("2026-11-16 09:00:00", 5000),
                log("2026-11-16 09:10:00", 30),
                log("2026-11-16 09:20:00", 80));
        assertEquals(Arrays.asList(0L, 50L), EnergyLogTimeline.intervalUsage(logs));
    }

    @Test
    public void oneOrNoLog_hasNoInterval() {
        assertTrue(EnergyLogTimeline.intervalUsage(Arrays.asList(log("2026-11-16 09:00:00", 100))).isEmpty());
        assertTrue(EnergyLogTimeline.intervalUsage(Arrays.asList()).isEmpty());
        assertTrue(EnergyLogTimeline.intervalUsage(null).isEmpty());
    }

    @Test
    public void nullOrEmpty_returnsEmptyList() {
        assertTrue(EnergyLogTimeline.oldestFirst(null).isEmpty());
        assertTrue(EnergyLogTimeline.oldestFirst(Arrays.asList()).isEmpty());
    }
}
