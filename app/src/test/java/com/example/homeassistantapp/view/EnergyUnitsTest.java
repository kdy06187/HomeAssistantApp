package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EnergyUnitsTest {

    @Test
    public void format_picksUnitBySize() {
        assertEquals("999 mWh", EnergyUnits.format(999));
        assertEquals("1.5 Wh", EnergyUnits.format(1500));
        assertEquals("2.5 kWh", EnergyUnits.format(2_500_000));
        assertEquals("0 mWh", EnergyUnits.format(0));
    }

    @Test
    public void format_roundsToTwoDecimals() {
        assertEquals("1.23 Wh", EnergyUnits.format(1234));
        assertEquals("1 kWh", EnergyUnits.format(1_000_000));
    }

    @Test
    public void unitFor_boundaries() {
        assertEquals(EnergyUnits.Unit.MWH, EnergyUnits.unitFor(999));
        assertEquals(EnergyUnits.Unit.WH, EnergyUnits.unitFor(1000));
        assertEquals(EnergyUnits.Unit.WH, EnergyUnits.unitFor(999_999));
        assertEquals(EnergyUnits.Unit.KWH, EnergyUnits.unitFor(1_000_000));
    }

    @Test
    public void format_inGivenUnit_forSharedChartAxis() {
        assertEquals("0.15 Wh", EnergyUnits.format(150, EnergyUnits.Unit.WH));
        assertEquals(1.5f, EnergyUnits.Unit.WH.scale(1500), 0.0001f);
    }
}
