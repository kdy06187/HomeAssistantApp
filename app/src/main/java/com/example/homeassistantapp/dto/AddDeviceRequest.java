package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class AddDeviceRequest {
    @SerializedName("protocolType")
    public String protocolType;
    @SerializedName("deviceName")
    public String deviceName;
    @SerializedName("payload")
    public String payload; // ip 주소 또는 Matter Pincode
    @SerializedName("ssid")
    public String ssid;

    @SerializedName("password")
    public String password;
    public AddDeviceRequest(String protocolType, String deviceName, String payload, String ssid, String password) {
        this.protocolType = protocolType;
        this.deviceName = deviceName;
        this.payload = payload;
        this.ssid = ssid;
        this.password = password;
    }

}
