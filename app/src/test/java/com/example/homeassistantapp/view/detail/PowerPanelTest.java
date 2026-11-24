package com.example.homeassistantapp.view.detail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.viewmodel.EnergyReading;

import org.junit.Test;

import java.util.TimeZone;

public class PowerPanelTest {
    private static final TimeZone SEOUL = TimeZone.getTimeZone("Asia/Seoul");
    // 2026-11-24 00:05:07 UTC = 09:05:07 서울
    private static final long AT = 1795478707000L;

    private static Device matter(String state) {
        Device d = new Device();
        d.setId("SN1");
        d.setType("MATTER");
        d.setState(state);
        d.setEnergy(15.23, 1.048576);
        return d;
    }

    @Test
    public void on_showsWattsAndTotal() {
        PowerPanel p = PowerPanel.of(matter("ON"), EnergyReading.success("SN1", AT), SEOUL);
        assertEquals("15.2 W", p.powerText);
        assertEquals("누적 1.05 kWh", p.totalText);
        assertEquals("마지막 갱신 09:05:07 · 15초마다", p.updatedText);
        assertNull(p.bannerText);
    }

    @Test
    public void off_hidesWattsButKeepsTotal() {
        PowerPanel p = PowerPanel.of(matter("OFF"), null, SEOUL);
        assertEquals("–", p.powerText);
        assertEquals("누적 1.05 kWh", p.totalText);
        assertEquals("아직 받지 못했어요 · 15초마다", p.updatedText);
    }

    @Test
    public void failure_keepsLastSuccessTimeAndShowsBanner() {
        EnergyReading ok = EnergyReading.success("SN1", AT);
        EnergyReading failed = EnergyReading.failure(ok, "SN1", "기기가 응답하지 않습니다.");
        PowerPanel p = PowerPanel.of(matter("ON"), failed, SEOUL);
        assertEquals("마지막 갱신 09:05:07 · 15초마다", p.updatedText);
        assertEquals("전력을 불러오지 못했어요. 기기가 응답하지 않습니다.", p.bannerText);
    }

    @Test
    public void readingOfAnotherDevice_isIgnored() {
        EnergyReading other = EnergyReading.failure(null, "SN2", "x");
        PowerPanel p = PowerPanel.of(matter("ON"), other, SEOUL);
        assertNull(p.bannerText);
        assertEquals("아직 받지 못했어요 · 15초마다", p.updatedText);
    }

    @Test
    public void failureAfterOtherDevice_doesNotBorrowItsTime() {
        EnergyReading other = EnergyReading.success("SN2", AT);
        assertNull(EnergyReading.failure(other, "SN1", "x").lastSuccessAt);
    }
}
