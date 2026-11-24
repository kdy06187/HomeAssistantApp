package com.example.homeassistantapp.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.Collections;

// 서버 응답 키 이름을 고정한다. 키가 바뀌면 Gson이 조용히 null을 넣으므로 여기서 잡는다.
public class DtoKeysTest {
    private final Gson gson = new Gson();

    @Test
    public void deviceList_readsCamelCaseKeys() {
        DeviceListResponse r = gson.fromJson("{\"totalCount\":2,\"devices\":["
                + "{\"deviceId\":\"TCP_A1B2C3D4E5F6\",\"name\":\"거실 전등\",\"protocolType\":\"TCP_DIY\",\"state\":\"ON\",\"ip\":\"192.168.0.50\"},"
                + "{\"deviceId\":\"SN12345\",\"name\":\"스마트 플러그\",\"protocolType\":\"MATTER\",\"state\":\"OFF\",\"ip\":null}]}",
                DeviceListResponse.class);
        assertEquals(2, r.totalCount);
        Device tcp = r.devices.get(0);
        assertEquals("TCP_A1B2C3D4E5F6", tcp.getId());
        assertEquals("거실 전등", tcp.getName());
        assertEquals("TCP_DIY", tcp.getType());
        assertEquals("ON", tcp.getState());
        assertEquals("192.168.0.50", tcp.ip);
        Device matter = r.devices.get(1);
        assertEquals("MATTER", matter.getType());
        assertNull(matter.ip);
    }

    @Test
    public void deviceList_oldKeysAreNotRead() {
        DeviceListResponse r = gson.fromJson(
                "{\"status\":\"success\",\"total_count\":1,\"devices\":[{\"id\":\"ARD_1\",\"type\":\"TCP_DIY\"}]}",
                DeviceListResponse.class);
        assertEquals(0, r.totalCount);
        assertNull(r.devices.get(0).getId());
        assertNull(r.devices.get(0).getType());
    }

    @Test
    public void energy_readsStateAndUnits() {
        EnergyResponse r = gson.fromJson(
                "{\"deviceId\":\"SN12345\",\"state\":\"ON\",\"activePower_mW\":15230,\"totalEnergy_mWh\":5000000000}",
                EnergyResponse.class);
        assertEquals("SN12345", r.deviceId);
        assertEquals("ON", r.state);
        assertEquals(15230, r.activePowerMw);
        assertEquals(5000000000L, r.totalEnergyMwh);
    }

    @Test
    public void deviceState_readsKeys() {
        DeviceStateResponse r = gson.fromJson("{\"deviceId\":\"SN12345\",\"state\":\"OFFLINE\"}", DeviceStateResponse.class);
        assertEquals("SN12345", r.deviceId);
        assertEquals("OFFLINE", r.state);
    }

    @Test
    public void requests_useContractKeys() {
        assertEquals("{\"deviceId\":\"SN12345\",\"command\":\"OFF\"}",
                gson.toJson(new ControlRequest("SN12345", "OFF")));
        assertEquals("{\"deviceId\":\"SN12345\"}", gson.toJson(new DeleteDeviceRequest("SN12345")));
        String add = gson.toJson(new AddDeviceRequest("MATTER", "플러그", "34970112332", "MyWiFi", "pw",
                Collections.singletonList(1)));
        assertEquals("{\"protocolType\":\"MATTER\",\"deviceName\":\"플러그\",\"payload\":\"34970112332\","
                + "\"ssid\":\"MyWiFi\",\"password\":\"pw\",\"driverTypes\":[1]}", add);
    }
}
