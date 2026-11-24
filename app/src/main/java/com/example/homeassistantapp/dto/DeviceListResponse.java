package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DeviceListResponse {
    @SerializedName("totalCount")
    public int totalCount;

    @SerializedName("devices")
    public List<Device> devices;
}
