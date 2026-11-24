package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class EnergyLogResponse {
    @SerializedName("deviceId")
    public String deviceId;

    @SerializedName("state")
    public String state;

    @SerializedName("logs")
    private List<EnergyLog> logs;

    // 범위 안 기록이 limit보다 많아 오래된 쪽이 잘렸으면 true
    @SerializedName("truncated")
    public Boolean truncated;

    public List<EnergyLog> getLogs() {
        return logs;
    }

    public boolean isTruncated() {
        return Boolean.TRUE.equals(truncated);
    }

    /** state가 비어 있으면 fallback을 쓴다(null로 덮어쓰지 않게). */
    public String stateOr(String fallback) {
        return state != null ? state : fallback;
    }
}
