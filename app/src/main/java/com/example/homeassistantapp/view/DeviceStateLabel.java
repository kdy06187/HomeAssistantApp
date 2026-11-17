package com.example.homeassistantapp.view;

/** 기기 상태를 화면 문구로 바꾼다. ON/OFF만 "연결됨", 모르는 값(null 포함)은 "알 수 없음". */
public final class DeviceStateLabel {
    public enum Kind { CONNECTED, OFFLINE, UNKNOWN }

    public static final String TEXT_CONNECTED = "연결됨";
    public static final String TEXT_OFFLINE = "오프라인";
    public static final String TEXT_UNKNOWN = "알 수 없음";

    private DeviceStateLabel() {}

    public static Kind kindOf(String state) {
        if ("ON".equals(state) || "OFF".equals(state)) return Kind.CONNECTED;
        if ("OFFLINE".equals(state)) return Kind.OFFLINE;
        return Kind.UNKNOWN;
    }

    public static String textOf(String state) {
        switch (kindOf(state)) {
            case CONNECTED: return TEXT_CONNECTED;
            case OFFLINE: return TEXT_OFFLINE;
            default: return TEXT_UNKNOWN;
        }
    }
}
