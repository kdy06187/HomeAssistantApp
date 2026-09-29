package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

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

    @SerializedName("driverTypes")
    public List<Integer> driverType;
    public AddDeviceRequest(String protocolType, String deviceName, String payload, String ssid, String password, List<Integer> driverType) {
        this.protocolType = protocolType;
        this.deviceName = deviceName;
        this.payload = payload;
        this.ssid = ssid;
        this.password = password;
        this.driverType = driverType;
    }

}
