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

    public String getDevice_status() {
        return device_status;
    }
}
