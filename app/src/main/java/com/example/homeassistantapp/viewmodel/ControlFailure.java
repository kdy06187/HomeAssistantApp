package com.example.homeassistantapp.viewmodel;

/** 제어 실패 한 건. 화면은 message를 Snackbar로 보이고 "다시 시도"에서 같은 명령을 다시 보낸다. */
public final class ControlFailure {
    public static final String UNREACHABLE_SERVER = "서버에 연결할 수 없어요.";

    public final String deviceId;
    public final String command;
    public final String message;

    public ControlFailure(String deviceId, String deviceName, String command, String reason) {
        this.deviceId = deviceId;
        this.command = command;
        this.message = message(deviceName, command, reason);
    }

    /** 예: "‘거실 전등’을 켜지 못했어요. 기기가 응답하지 않습니다." */
    static String message(String deviceName, String command, String reason) {
        boolean hasName = deviceName != null && !deviceName.isEmpty();
        String name = hasName ? "‘" + deviceName + "’" + objectParticle(deviceName) : "기기를";
        String action = "OFF".equals(command) ? "끄지" : "켜지";
        String head = name + " " + action + " 못했어요.";
        return reason == null || reason.isEmpty() ? head : head + " " + reason;
    }

    /** 마지막 글자에 받침이 있으면 "을", 없으면 "를". 한글이 아니면(영문·숫자) 둘 다 쓴다. */
    static String objectParticle(String word) {
        char last = word.charAt(word.length() - 1);
        if (last < 0xAC00 || last > 0xD7A3) return "을(를)";
        return (last - 0xAC00) % 28 == 0 ? "를" : "을";
    }
}
