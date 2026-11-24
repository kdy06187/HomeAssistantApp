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

    public List<EnergyLog> getLogs() {
        return logs;
    }

    /** state가 비어 있으면 fallback을 쓴다(null로 덮어쓰지 않게). */
    public String stateOr(String fallback) {
        return state != null ? state : fallback;
    }
}
