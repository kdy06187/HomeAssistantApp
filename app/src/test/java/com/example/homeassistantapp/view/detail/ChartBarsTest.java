package com.example.homeassistantapp.view.detail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.homeassistantapp.dto.EnergyBucket;
import com.example.homeassistantapp.dto.EnergyLog;
import com.example.homeassistantapp.view.EnergyUnits;
import com.example.homeassistantapp.view.PeriodRange;

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

    private static EnergyBucket bucket(String start, String end, long mWh, long samples) {
        EnergyBucket b = new EnergyBucket();
        b.start = start;
        b.end = end;
        b.energyUsedMwh = mWh;
        b.sampleCount = samples;
        return b;
    }

    @Test
    public void fromBuckets_week_labelsAndEmptySlot() {
        List<EnergyBucket> buckets = Arrays.asList(
                bucket("2026-11-16", "2026-11-22", 9100, 10080),
                bucket("2026-11-23", "2026-11-24", 0, 0));
        ChartBars c = ChartBars.fromBuckets(buckets, PeriodRange.WEEK);
        assertEquals("주별 사용량 (Wh)", c.title);
        assertEquals("11/16", c.bars.get(0).label);
        assertEquals("11/16~11/22 · 9.1 Wh", c.describe(0));
        assertFalse(c.bars.get(1).hasData);
        assertEquals("11/23~11/24 · 기록 없음", c.describe(1));
        assertNull(c.note);
    }

    @Test
    public void fromBuckets_dayAndMonthLabels() {
        ChartBars day = ChartBars.fromBuckets(Arrays.asList(bucket("2026-11-24", "2026-11-24", 500, 3)), PeriodRange.DAY);
        assertEquals("일별 사용량 (mWh)", day.title);
        assertEquals("11/24 · 500 mWh", day.describe(0));

        ChartBars month = ChartBars.fromBuckets(Arrays.asList(bucket("2026-11-01", "2026-11-24", 2_500_000, 9)), PeriodRange.MONTH);
        assertEquals("11월", month.bars.get(0).label);
        assertEquals("2026년 11월 · 2.5 kWh", month.describe(0));
        assertTrue(month.note.startsWith("기록은 90일만"));
    }

    @Test
    public void fromBuckets_allEmpty_showsEmptyState() {
        ChartBars c = ChartBars.fromBuckets(Arrays.asList(bucket("2026-11-24", "2026-11-24", 0, 0)), PeriodRange.DAY);
        assertTrue(c.bars.isEmpty());
        assertEquals("이 기간에는 기록이 없어요.", c.emptyText);
        assertEquals("일별 사용량", c.title);
        assertTrue(ChartBars.fromBuckets(null, PeriodRange.DAY).bars.isEmpty());
    }

    @Test
    public void fromBuckets_badDate_doesNotCrash() {
        ChartBars c = ChartBars.fromBuckets(Arrays.asList(bucket("2026-13-01", null, 5, 1)), PeriodRange.WEEK);
        assertEquals("", c.bars.get(0).label);
    }

    @Test
    public void failed_keepsReason() {
        ChartBars c = ChartBars.failed("서버에 연결할 수 없어요.");
        assertEquals("기록을 불러오지 못했어요. 서버에 연결할 수 없어요.", c.emptyText);
        assertEquals("", c.describe(0));
    }
}
