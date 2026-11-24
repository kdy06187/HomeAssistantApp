package com.example.homeassistantapp.viewmodel;

import com.example.homeassistantapp.network.ApiError;

/** 제어 응답을 화면 동작 단위로 나눈다. 상태코드와 오류 code만 본다. */
public enum ControlOutcome {
    /** 200: 바로 실행됨 */
    DONE,
    /** 202: 접수, delaySeconds 뒤 실행 */
    ACCEPTED,
    /** 409 COMMAND_BLOCKED: 드라이버가 막음 */
    BLOCKED,
    /** 502 DEVICE_UNREACHABLE: 서버가 OFFLINE으로 기록함 */
    UNREACHABLE,
    /** 404 DEVICE_NOT_FOUND: 없는·숨긴 기기 */
    DEVICE_GONE,
    /** 그 외 실패(400, 401, 500 등) */
    FAILED;

    public static ControlOutcome of(int httpCode, ApiError error) {
        if (httpCode == 202) return ACCEPTED;
        if (httpCode >= 200 && httpCode < 300) return DONE;
        if (httpCode == 409) return BLOCKED;
        if (httpCode == 502) return UNREACHABLE;
        if (httpCode == 404 && error != null && error.is("DEVICE_NOT_FOUND")) return DEVICE_GONE;
        return FAILED;
    }

    /** 202 안내 문구. 예: "명령을 접수했습니다. 약 45초 뒤 꺼집니다." */
    public static String acceptedMessage(String pendingCommand, Integer delaySeconds) {
        String when = delaySeconds != null && delaySeconds >= 0 ? "약 " + delaySeconds + "초 뒤" : "잠시 뒤";
        String what;
        if ("OFF".equals(pendingCommand)) what = "꺼집니다.";
        else if ("ON".equals(pendingCommand)) what = "켜집니다.";
        else what = "실행됩니다.";
        return "명령을 접수했습니다. " + when + " " + what;
    }

    /** 실패했을 때 보여 줄 상태. null이면 요청 전 상태로 되돌린다. */
    public String failedState() {
        return this == UNREACHABLE ? "OFFLINE" : null;
    }
}
