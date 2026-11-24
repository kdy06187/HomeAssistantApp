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

    @Test
    public void turningOn_keepsRequestedPositionAndLocks() {
        Device d = device("MATTER", "LOADING");
        d.setPendingCommand("ON");
        HomeTile tile = HomeTile.of(d);
        assertTrue(tile.switchChecked);
        assertFalse(tile.switchEnabled);
        assertEquals("켜는 중…", tile.chipText);
        assertEquals(DeviceStateLabel.Tone.WARNING, tile.tone);
        assertFalse(tile.showsRefresh);
    }

    @Test
    public void turningOff_showsTurningOff() {
        Device d = device("TCP_DIY", "LOADING");
        d.setPendingCommand("OFF");
        HomeTile tile = HomeTile.of(d);
        assertFalse(tile.switchChecked);
        assertFalse(tile.switchEnabled);
        assertEquals("끄는 중…", tile.chipText);
    }

    @Test
    public void unknown_showsRefreshButton() {
        HomeTile tile = HomeTile.of(device("MATTER", "UNKNOWN"));
        assertTrue(tile.showsRefresh);
        assertFalse(tile.switchEnabled);
        assertEquals("알 수 없음", tile.chipText);
    }

    @Test
    public void loadingAndKnownStates_haveNoRefreshButton() {
        assertFalse(HomeTile.of(device("MATTER", "LOADING")).showsRefresh);
        assertFalse(HomeTile.of(device("MATTER", "ON")).showsRefresh);
        assertFalse(HomeTile.of(device("MATTER", "OFFLINE")).showsRefresh);
    }

    @Test
    public void emptyHome_onlyWhenListArrivedEmpty() {
        assertTrue(HomeTile.isEmptyHome(new java.util.ArrayList<>()));
        assertFalse(HomeTile.isEmptyHome(null));
        assertFalse(HomeTile.isEmptyHome(java.util.Collections.singletonList(DeviceOrderTest.pairing())));
    }
}
