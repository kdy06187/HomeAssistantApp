package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class ControlResponse {
    @SerializedName("status")
    public String status;
    @SerializedName("message")
    public String message;
}
