package com.example.homeassistantapp.view;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** 사용량(mWh) 글자와 차트 단위. 값 크기에 맞춰 mWh·Wh·kWh 중 하나를 고른다. */
public final class EnergyUnits {
    private EnergyUnits() {}

    public enum Unit {
        MWH("mWh", 1L), WH("Wh", 1_000L), KWH("kWh", 1_000_000L);

        public final String symbol;
        public final long divisor;

        Unit(String symbol, long divisor) {
            this.symbol = symbol;
            this.divisor = divisor;
        }

        public float scale(long mWh) {
            return (float) ((double) mWh / divisor);
        }
    }

    /** 가장 큰 값이 1 이상 1000 미만이 되는 단위. */
    public static Unit unitFor(long maxMwh) {
        if (maxMwh >= Unit.KWH.divisor) return Unit.KWH;
        if (maxMwh >= Unit.WH.divisor) return Unit.WH;
        return Unit.MWH;
    }

    /** 999 → "999 mWh", 1500 → "1.5 Wh", 2500000 → "2.5 kWh" */
    public static String format(long mWh) {
        return format(mWh, unitFor(mWh));
    }

    public static String format(long mWh, Unit unit) {
        DecimalFormat number = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
        return number.format((double) mWh / unit.divisor) + " " + unit.symbol;
    }
}
