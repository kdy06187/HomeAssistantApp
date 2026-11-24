package com.example.homeassistantapp.view.detail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.homeassistantapp.dto.EnergyLog;
import com.example.homeassistantapp.view.EnergyUnits;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class ChartBarsTest {
    private static final TimeZone SEOUL = TimeZone.getTimeZone("Asia/Seoul");

    private static EnergyLog log(String time, long mWh) {
        EnergyLog l = new EnergyLog();
        l.setTimestamp(time);
        l.setTotalEnergy_mWh(mWh);
        return l;
    }

    @Test
    public void fromLogs_intervalBarsWithLocalLabels() {
        List<EnergyLog> logs = Arrays.asList(
                log("2026-11-16T00:20:00Z", 1_040_000),
                log("2026-11-16T00:21:00Z", 1_041_500),
                log("2026-11-16T00:22:00Z", 1_042_000));
        ChartBars c = ChartBars.fromLogs(logs, false, SEOUL);
        assertEquals(2, c.bars.size());
        assertEquals("09:21", c.bars.get(0).label);
        assertEquals(1500, c.bars.get(0).energyMwh);
        assertEquals(EnergyUnits.Unit.WH, c.unit);
        assertEquals("구간별 사용량 (Wh)", c.title);
        assertEquals("09:22 · 0.5 Wh", c.describe(1));
        assertNull(c.emptyText);
        assertNull(c.note);
    }

    @Test
    public void fromLogs_counterResetCountsAsZero() {
        List<EnergyLog> logs = Arrays.asList(
                log("2026-11-16T00:20:00Z", 5000),
                log("2026-11-16T00:21:00Z", 100));
        assertEquals(0, ChartBars.fromLogs(logs, false, SEOUL).bars.get(0).energyMwh);
    }

    @Test
    public void fromLogs_lessThanTwo_showsEmptyText() {
        ChartBars c = ChartBars.fromLogs(Arrays.asList(log("2026-11-16T00:20:00Z", 1)), false, SEOUL);
        assertTrue(c.bars.isEmpty());
        assertTrue(c.emptyText.startsWith("기록이 쌓이는 중이에요"));
        assertEquals("구간별 사용량", c.title);
        assertEquals(c.emptyText, c.summary());
    }

    @Test
    public void fromLogs_nullAndTruncated() {
        ChartBars c = ChartBars.fromLogs(null, true, SEOUL);
        assertTrue(c.bars.isEmpty());
        assertEquals("최근 50건만 보여요.", c.note);
    }

    @Test
    public void summary_namesPeak() {
        List<EnergyLog> logs = new ArrayList<>(Arrays.asList(
                log("2026-11-16T00:20:00Z", 0),
                log("2026-11-16T00:21:00Z", 100),
                log("2026-11-16T00:22:00Z", 400)));
        ChartBars c = ChartBars.fromLogs(logs, false, SEOUL);
        assertEquals("구간별 사용량 (mWh), 막대 2개, 가장 많은 구간 09:22 · 300 mWh", c.summary());
    }

    @Test
    public void failed_keepsReason() {
        ChartBars c = ChartBars.failed("서버에 연결할 수 없어요.");
        assertEquals("기록을 불러오지 못했어요. 서버에 연결할 수 없어요.", c.emptyText);
        assertEquals("", c.describe(0));
    }
}
