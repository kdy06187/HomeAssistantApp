package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class AddDeviceResponse {
    @SerializedName("result")
    public String result;
    @SerializedName("message")
    public String message;
}
