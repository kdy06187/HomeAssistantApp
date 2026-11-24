package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.homeassistantapp.dto.Device;

import org.junit.Test;

public class DeviceRowTest {

    @Test
    public void normalDevice_showsTypeAndStateAndIsEditable() {
        DeviceRow row = DeviceRow.of(HomeTileTest.device("MATTER", "OFFLINE"));
        assertEquals("Matter", row.typeLabel);
        assertEquals("오프라인", row.chipText);
        assertEquals(DeviceStateLabel.Tone.OFFLINE, row.tone);
        assertTrue(row.editable);
    }

    @Test
    public void tcpDiy_hasReadableTypeLabel() {
        assertEquals("직접 연결 (TCP_DIY)", DeviceRow.of(HomeTileTest.device("TCP_DIY", "ON")).typeLabel);
        assertEquals("ZIGBEE", DeviceRow.typeLabel("ZIGBEE"));
        assertEquals("", DeviceRow.typeLabel(null));
    }

    @Test
    public void pairingItem_cannotBeDeletedMovedOrOpened() {
        Device d = DeviceOrderTest.pairing();
        d.setType("MATTER");
        DeviceRow row = DeviceRow.of(d);
        assertTrue(row.pairing);
        assertFalse(row.editable);
        assertEquals("등록 중…", row.chipText);
    }
}
