package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class ControlRequest {
    @SerializedName("deviceId")
    public String deviceId;
    @SerializedName("command")
    public String command;

    public ControlRequest(String deviceId,String command) {
        this.deviceId = deviceId;
        this.command = command;
    }
}
