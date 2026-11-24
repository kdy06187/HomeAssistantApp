package com.example.homeassistantapp.view.detail;

import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.DeviceRow;

/** 상세의 기기 정보 카드에 보일 글자. */
public final class DeviceInfo {
    public final String typeLine;
    public final String idLine;
    /** IP가 없으면(Matter) null. 줄을 숨긴다. */
    public final String ipLine;

    private DeviceInfo(Device device) {
        typeLine = "종류  " + DeviceRow.of(device).typeLabel;
        idLine = "기기 ID  " + (device.getId() == null ? "" : device.getId());
        String ip = device.ip;
        ipLine = ip == null || ip.isEmpty() ? null : "IP 주소  " + ip;
    }

    public static DeviceInfo of(Device device) {
        return new DeviceInfo(device);
    }
}
