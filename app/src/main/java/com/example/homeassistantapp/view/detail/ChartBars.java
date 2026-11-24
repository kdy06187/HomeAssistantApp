package com.example.homeassistantapp.view.detail;

import com.example.homeassistantapp.dto.EnergyBucket;
import com.example.homeassistantapp.dto.EnergyLog;
import com.example.homeassistantapp.view.EnergyLogTimeline;
import com.example.homeassistantapp.view.EnergyUnits;
import com.example.homeassistantapp.view.PeriodRange;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/** 상세 막대 차트에 그릴 값. 원본 기록이든 기간 집계든 이 모양으로 바꿔 차트는 그리기만 한다. */
public final class ChartBars {

    public static final class Bar {
        /** 축에 쓰는 짧은 글자. */
        public final String label;
        /** 막대를 눌렀을 때 쓰는 긴 글자(주는 "11/23~11/29"). */
        public final String detail;
        public final long energyMwh;
        /** 기록이 없는 구간이면 false(막대 없이 빈칸). */
        public final boolean hasData;

        Bar(String label, String detail, long energyMwh, boolean hasData) {
            this.label = label;
            this.detail = detail;
            this.energyMwh = energyMwh;
            this.hasData = hasData;
        }
    }

    public final List<Bar> bars;
    public final EnergyUnits.Unit unit;
    public final String title;
    /** 막대가 하나도 없을 때 대신 보일 문구. 막대가 있으면 null. */
    public final String emptyText;
    /** 차트 아래 안내(잘린 기록 등). 없으면 null. */
    public final String note;

    private ChartBars(List<Bar> bars, String titlePrefix, String emptyText, String note) {
        this.bars = Collections.unmodifiableList(bars);
        long max = 0;
        for (Bar bar : bars) max = Math.max(max, bar.energyMwh);
        this.unit = EnergyUnits.unitFor(max);
        this.title = bars.isEmpty() ? titlePrefix : titlePrefix + " (" + unit.symbol + ")";
        this.emptyText = bars.isEmpty() ? emptyText : null;
        this.note = note;
    }

    /** 원본 기록(누적값) → 이웃 기록 차이 막대. 라벨은 구간이 끝난 현지 시각. */
    public static ChartBars fromLogs(List<EnergyLog> logs, boolean truncated, TimeZone zone) {
        List<EnergyLog> ordered = EnergyLogTimeline.oldestFirst(logs);
        List<Long> usage = EnergyLogTimeline.intervalUsage(ordered);
        List<Bar> bars = new ArrayList<>();
        for (int i = 0; i < usage.size(); i++) {
            String label = EnergyLogTimeline.toLocalTimeLabel(ordered.get(i + 1).getTimestamp(), zone);
            bars.add(new Bar(label, label, usage.get(i), true));
        }
        return new ChartBars(bars, "구간별 사용량",
                "기록이 쌓이는 중이에요. 기록이 2개 이상 모이면 막대로 보여요.",
                truncated ? "최근 50건만 보여요." : null);
    }

    /** E7 집계 구간 → 막대. sampleCount가 0인 구간은 빈칸, 모든 구간이 비면 빈 상태. */
    public static ChartBars fromBuckets(List<EnergyBucket> buckets, String period) {
        List<Bar> bars = new ArrayList<>();
        boolean anyData = false;
        if (buckets != null) {
            for (EnergyBucket bucket : buckets) {
                LocalDate start = parse(bucket.start);
                LocalDate end = parse(bucket.end);
                boolean hasData = bucket.sampleCount > 0;
                anyData |= hasData;
                bars.add(new Bar(shortLabel(period, start), longLabel(period, start, end),
                        hasData ? bucket.energyUsedMwh : 0L, hasData));
            }
        }
        if (!anyData) bars.clear();
        String note = PeriodRange.MONTH.equals(period) ? "기록은 90일만 보관돼 오래된 달은 비어 있을 수 있어요." : null;
        return new ChartBars(bars, titleFor(period), "이 기간에는 기록이 없어요.", note);
    }

    private static String titleFor(String period) {
        if (PeriodRange.DAY.equals(period)) return "일별 사용량";
        if (PeriodRange.WEEK.equals(period)) return "주별 사용량";
        return "월별 사용량";
    }

    private static String shortLabel(String period, LocalDate start) {
        if (start == null) return "";
        if (PeriodRange.MONTH.equals(period)) return start.getMonthValue() + "월";
        return monthDay(start);
    }

    private static String longLabel(String period, LocalDate start, LocalDate end) {
        if (start == null) return "";
        if (PeriodRange.MONTH.equals(period)) return start.getYear() + "년 " + start.getMonthValue() + "월";
        if (PeriodRange.WEEK.equals(period) && end != null && !end.equals(start)) {
            return monthDay(start) + "~" + monthDay(end);
        }
        return monthDay(start);
    }

    private static String monthDay(LocalDate date) {
        return date.getMonthValue() + "/" + date.getDayOfMonth();
    }

    private static LocalDate parse(String date) {
        if (date == null) return null;
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static ChartBars failed(String reason) {
        return new ChartBars(new ArrayList<>(), "사용량", "기록을 불러오지 못했어요. " + reason, null);
    }

    /** 막대를 눌렀을 때 보일 글자. */
    public String describe(int index) {
        if (index < 0 || index >= bars.size()) return "";
        Bar bar = bars.get(index);
        return bar.detail + " · " + (bar.hasData ? EnergyUnits.format(bar.energyMwh, unit) : "기록 없음");
    }

    /** TalkBack이 차트 전체를 한 번에 읽을 요약. */
    public String summary() {
        if (bars.isEmpty()) return emptyText;
        int peak = 0;
        for (int i = 1; i < bars.size(); i++) {
            if (bars.get(i).energyMwh > bars.get(peak).energyMwh) peak = i;
        }
        return title + ", 막대 " + bars.size() + "개, 가장 많은 구간 " + describe(peak);
    }
}
