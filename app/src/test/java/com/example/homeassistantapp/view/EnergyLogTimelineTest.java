package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.homeassistantapp.dto.EnergyLog;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

/**
 * 불일치 #9(UTC → 현지 시간), #10(최신순 → 오래된 순)을 확인한다.
 */
public class EnergyLogTimelineTest {
    private static final TimeZone SEOUL = TimeZone.getTimeZone("Asia/Seoul"); // UTC+9

    private static EnergyLog log(String timestamp, long mWh) {
        EnergyLog l = new EnergyLog();
        l.setTimestamp(timestamp);
        l.setTotalEnergy_mWh(mWh);
        return l;
    }

    // 서버 UTC 09:30은 서울 시간 18:30이다
    @Test
    public void utcTimestamp_isShownInLocalTime() {
        assertEquals("18:30", EnergyLogTimeline.toLocalTimeLabel("2026-11-16 09:30:00", SEOUL));
    }

    // 날짜가 넘어가도 시간이 맞다(UTC 20:00 → 서울 다음 날 05:00)
    @Test
    public void utcTimestamp_crossesMidnight() {
        assertEquals("05:00", EnergyLogTimeline.toLocalTimeLabel("2026-11-16 20:00:00", SEOUL));
    }

    // 읽을 수 없는 값은 빈 문자열이다
    @Test
    public void badTimestamp_returnsEmpty() {
        assertEquals("", EnergyLogTimeline.toLocalTimeLabel("어제", SEOUL));
        assertEquals("", EnergyLogTimeline.toLocalTimeLabel(null, SEOUL));
    }

    // 서버가 최신순으로 보내도 오래된 순으로 바꾼다
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

    // null이나 빈 목록이어도 죽지 않는다
    @Test
    public void nullOrEmpty_returnsEmptyList() {
        assertTrue(EnergyLogTimeline.oldestFirst(null).isEmpty());
        assertTrue(EnergyLogTimeline.oldestFirst(Arrays.asList()).isEmpty());
    }
}
