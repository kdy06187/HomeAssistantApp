package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class AddDeviceRequest {
    @SerializedName("protocolType")
    public String protocolType;
    @SerializedName("deviceName")
    public String deviceName;
    @SerializedName("payload")
    public String payload; // ip 주소 또는 Matter Pincode

    public AddDeviceRequest(String protocolType, String deviceName, String payload) {
        this.protocolType = protocolType;
        this.deviceName = deviceName;
        this.payload = payload;
    }

}
