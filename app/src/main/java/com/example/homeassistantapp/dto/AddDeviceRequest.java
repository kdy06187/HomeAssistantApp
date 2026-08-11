package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class AddDeviceRequest {
    @SerializedName("type")
    public String type;
    @SerializedName("name")
    public String name;
    @SerializedName("payload")
    public String payload; // ip 주소 또는 Matter Pincode

    public AddDeviceRequest(String type, String name, String payload) {
        this.type = type;
        this.name = name;
        this.payload = payload;
    }

}
