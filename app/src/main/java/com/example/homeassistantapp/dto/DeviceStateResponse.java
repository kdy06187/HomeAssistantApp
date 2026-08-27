package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class DeviceStateResponse {
    @SerializedName("deviceId")
    public String deviceId;

    @SerializedName("state")
    public String state; // "ON", "OFF", "UNKNOWN"
}
