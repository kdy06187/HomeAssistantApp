package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.homeassistantapp.dto.Device;

import org.junit.Test;

public class HomeTileTest {

    static Device device(String type, String state) {
        Device d = DeviceOrderTest.device("SN1");
        d.setName("거실 플러그");
        d.setType(type);
        d.setState(state);
        return d;
    }

    @Test
    public void matterOn_showsPowerAndTotal() {
        Device d = device("MATTER", "ON");
        d.setEnergy(12.44, 1.054);
        HomeTile tile = HomeTile.of(d);
        assertTrue(tile.showsPower);
        assertEquals("12.4 W", tile.powerText);
        assertEquals("누적 1.05 kWh", tile.metaText);
        assertTrue(tile.switchChecked);
        assertTrue(tile.switchEnabled);
        assertEquals("켜짐", tile.chipText);
    }

    @Test
    public void matterWithoutEnergyYet_showsDash() {
        HomeTile tile = HomeTile.of(device("MATTER", "ON"));
        assertEquals("–", tile.powerText);
        assertEquals("누적 –", tile.metaText);
    }

    @Test
    public void matterOff_hidesStalePowerButKeepsTotal() {
        Device d = device("MATTER", "OFF");
        d.setEnergy(9.8, 0.32);
        HomeTile tile = HomeTile.of(d);
        assertEquals("–", tile.powerText);
        assertEquals("누적 0.32 kWh", tile.metaText);
        assertFalse(tile.switchChecked);
        assertTrue(tile.switchEnabled);
    }

    @Test
    public void offline_locksSwitchAndGivesReason() {
        HomeTile tile = HomeTile.of(device("TCP_DIY", "OFFLINE"));
        assertFalse(tile.switchEnabled);
        assertEquals("기기 응답 없음", tile.metaText);
        assertEquals(DeviceStateLabel.Tone.OFFLINE, tile.tone);
    }

    @Test
    public void tcpDiy_hasNoPowerLine() {
        HomeTile tile = HomeTile.of(device("TCP_DIY", "ON"));
        assertFalse(tile.showsPower);
        assertEquals("", tile.metaText);
        assertTrue(tile.opensDetail);
    }

    @Test
    public void pairingItem_hasNoSwitchAndDoesNotOpen() {
        Device d = DeviceOrderTest.pairing();
        d.setType("MATTER");
        HomeTile tile = HomeTile.of(d);
        assertTrue(tile.pairing);
        assertFalse(tile.switchVisible);
        assertFalse(tile.opensDetail);
        assertEquals("등록 중…", tile.chipText);
    }
}
