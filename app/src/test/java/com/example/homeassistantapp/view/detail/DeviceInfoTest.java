package com.example.homeassistantapp.view.detail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.homeassistantapp.dto.Device;

import org.junit.Test;

public class DeviceInfoTest {

    private static Device device(String id, String type, String ip) {
        Device d = new Device();
        d.setId(id);
        d.setType(type);
        d.ip = ip;
        return d;
    }

    @Test
    public void matter_hidesIpLine() {
        DeviceInfo info = DeviceInfo.of(device("SN1", "MATTER", null));
        assertEquals("종류  Matter", info.typeLine);
        assertEquals("기기 ID  SN1", info.idLine);
        assertNull(info.ipLine);
    }

    @Test
    public void tcpDiy_showsIp() {
        DeviceInfo info = DeviceInfo.of(device("TCP_A1B2C3D4E5F6", "TCP_DIY", "192.168.0.7"));
        assertEquals("종류  직접 연결 (TCP_DIY)", info.typeLine);
        assertEquals("IP 주소  192.168.0.7", info.ipLine);
    }

    @Test
    public void emptyIp_isHidden() {
        assertNull(DeviceInfo.of(device("TCP_X", "TCP_DIY", "")).ipLine);
    }
}
