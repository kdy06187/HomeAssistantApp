package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class DeleteDeviceRequest {
    @SerializedName("deviceId")
    public String deviceId;
    public DeleteDeviceRequest(String deviceId) {
        this.deviceId = deviceId;
    }
}
