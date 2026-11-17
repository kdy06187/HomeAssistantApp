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

    // 이 API는 실패해도 HTTP 200에 {"error"}만 준다.
    @SerializedName("error")
    public String error;

    public String getDevice_status() {
        return device_status;
    }

    /** 본문에 error 키가 있으면 실패다. */
    public boolean hasError() {
        return error != null;
    }

    /** 오류이거나 device_status가 비어 있으면 fallback을 쓴다(null로 덮어쓰지 않게). */
    public String stateOr(String fallback) {
        if (hasError() || device_status == null) return fallback;
        return device_status;
    }
}
