package com.example.homeassistantapp.view.detail;

import com.example.homeassistantapp.dto.EnergyLog;
import com.example.homeassistantapp.view.EnergyLogTimeline;
import com.example.homeassistantapp.view.EnergyUnits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/** 상세 막대 차트에 그릴 값. 원본 기록이든 기간 집계든 이 모양으로 바꿔 차트는 그리기만 한다. */
public final class ChartBars {

    public static final class Bar {
        public final String label;
        public final long energyMwh;
        /** 기록이 없는 구간이면 false(막대 없이 빈칸). */
        public final boolean hasData;

        Bar(String label, long energyMwh, boolean hasData) {
            this.label = label;
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
            bars.add(new Bar(label, usage.get(i), true));
        }
        return new ChartBars(bars, "구간별 사용량",
                "기록이 쌓이는 중이에요. 기록이 2개 이상 모이면 막대로 보여요.",
                truncated ? "최근 50건만 보여요." : null);
    }

    public static ChartBars failed(String reason) {
        return new ChartBars(new ArrayList<>(), "사용량", "기록을 불러오지 못했어요. " + reason, null);
    }

    /** 막대를 눌렀을 때 보일 글자. */
    public String describe(int index) {
        if (index < 0 || index >= bars.size()) return "";
        Bar bar = bars.get(index);
        return bar.label + " · " + (bar.hasData ? EnergyUnits.format(bar.energyMwh, unit) : "기록 없음");
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
