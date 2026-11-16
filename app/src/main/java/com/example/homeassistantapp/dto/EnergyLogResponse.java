package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class EnergyLogResponse {
    @SerializedName("logs")
    private List<EnergyLog> logs;

    @SerializedName("device_status")
    public String device_status;

    public List<EnergyLog> getLogs() {
        return logs;
    }

    // 계약서 E7 / §6 I-02: 없는 기기거나 서버 내부 오류여도 HTTP 200에 {"error": "..."} 만 온다.
    // 그래서 HTTP 코드만으로는 실패를 알 수 없고, 이 필드가 있는지도 봐야 한다.
    @SerializedName("error")
    public String error;

    public String getDevice_status() {
        return device_status;
    }

    /** 계약서 §4.1: 2xx 이면서 본문에 error 키가 없어야 성공이다. */
    public boolean hasError() {
        return error != null;
    }

    /**
     * 기기 상태로 쓸 값을 고른다(불일치 #7).
     * 왜: 오류 응답에는 device_status 가 없어서(null) 그대로 넣으면 화면에서 null 비교로 앱이 죽었다.
     * 오류이거나 상태가 비어 있으면 원래 상태(fallback)를 그대로 유지한다.
     */
    public String stateOr(String fallback) {
        if (hasError() || device_status == null) return fallback;
        return device_status;
    }
}
