package com.example.homeassistantapp.view;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

/** 일/주/월 차트의 조회 기간. 날짜 선택 없이 앱이 고정한다(일 7일·주 8주·월 6개월, 모두 오늘까지). */
public final class PeriodRange {
    public static final String DAY = "day";
    public static final String WEEK = "week";
    public static final String MONTH = "month";

    // 서버 집계 날짜가 +09:00 기준이라 "오늘"도 같은 기준으로 정한다.
    private static final ZoneOffset SERVER_OFFSET = ZoneOffset.ofHours(9);

    public final LocalDate from;
    public final LocalDate to;

    private PeriodRange(LocalDate from, LocalDate to) {
        this.from = from;
        this.to = to;
    }

    public static LocalDate today() {
        return LocalDate.now(SERVER_OFFSET);
    }

    public static PeriodRange of(String period, LocalDate today) {
        switch (period) {
            case DAY:
                return new PeriodRange(today.minusDays(6), today);
            case WEEK:
                // 서버의 주는 월요일 시작. 이번 주 포함 8주.
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                return new PeriodRange(monday.minusWeeks(7), today);
            case MONTH:
                return new PeriodRange(today.withDayOfMonth(1).minusMonths(5), today);
            default:
                throw new IllegalArgumentException("period: " + period);
        }
    }

    public String fromText() {
        return from.toString();
    }

    public String toText() {
        return to.toString();
    }
}
