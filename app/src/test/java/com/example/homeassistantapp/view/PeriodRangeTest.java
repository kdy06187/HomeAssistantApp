package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PeriodRangeTest {
    // 2026-11-24는 화요일
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

    @Test
    public void day_isLastSevenDaysIncludingToday() {
        PeriodRange r = PeriodRange.of(PeriodRange.DAY, TODAY);
        assertEquals("2026-11-18", r.fromText());
        assertEquals("2026-11-24", r.toText());
    }

    @Test
    public void week_startsOnMondaySevenWeeksBefore() {
        PeriodRange r = PeriodRange.of(PeriodRange.WEEK, TODAY);
        assertEquals("2026-10-05", r.fromText());
        assertEquals(DayOfWeek.MONDAY, r.from.getDayOfWeek());
        assertEquals("2026-11-24", r.toText());
    }

    @Test
    public void week_onMonday_thisWeekIsOneDay() {
        PeriodRange r = PeriodRange.of(PeriodRange.WEEK, LocalDate.of(2026, 11, 23));
        assertEquals("2026-10-05", r.fromText());
    }

    @Test
    public void month_startsOnFirstDayFiveMonthsBefore() {
        PeriodRange r = PeriodRange.of(PeriodRange.MONTH, TODAY);
        assertEquals("2026-06-01", r.fromText());
    }

    @Test
    public void crossesYearEnd() {
        LocalDate jan = LocalDate.of(2027, 1, 3);
        assertEquals("2026-12-28", PeriodRange.of(PeriodRange.DAY, jan).fromText());
        assertEquals("2026-08-01", PeriodRange.of(PeriodRange.MONTH, jan).fromText());
        assertEquals("2026-11-09", PeriodRange.of(PeriodRange.WEEK, jan).fromText());
    }

    @Test
    public void everyRange_isWithinServerLimitOf366Days() {
        LocalDate d = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < 800; i++, d = d.plusDays(1)) {
            for (String p : new String[]{PeriodRange.DAY, PeriodRange.WEEK, PeriodRange.MONTH}) {
                PeriodRange r = PeriodRange.of(p, d);
                long days = ChronoUnit.DAYS.between(r.from, r.to) + 1;
                assertTrue(p + " " + d, days >= 1 && days <= 366);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownPeriod_isRejected() {
        PeriodRange.of("year", TODAY);
    }
}
