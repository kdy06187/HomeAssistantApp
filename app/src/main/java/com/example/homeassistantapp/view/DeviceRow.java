package com.example.homeassistantapp.view;

import com.example.homeassistantapp.dto.Device;

/** 기기 탭 목록 한 줄에 보일 값. */
public final class DeviceRow {
    public final String name;
    public final String typeLabel;
    public final String chipText;
    public final DeviceStateLabel.Tone tone;
    public final boolean pairing;
    /** 삭제·순서 바꾸기·상세 열기. 등록 중 임시 항목은 모두 막는다. */
    public final boolean editable;

    private DeviceRow(Device device) {
        name = device.getName();
        typeLabel = typeLabel(device.getType());
        pairing = DeviceOrder.isPairing(device);
        if (pairing) {
            chipText = "등록 중…";
            tone = DeviceStateLabel.Tone.OFF;
        } else {
            DeviceStateLabel.Kind kind = DeviceStateLabel.kindOf(device.getState());
            chipText = kind.text;
            tone = kind.tone;
        }
        editable = !pairing;
    }

    public static DeviceRow of(Device device) {
        return new DeviceRow(device);
    }

    static String typeLabel(String type) {
        if ("MATTER".equals(type)) return "Matter";
        if ("TCP_DIY".equals(type)) return "직접 연결 (TCP_DIY)";
        return type == null ? "" : type;
    }
}
