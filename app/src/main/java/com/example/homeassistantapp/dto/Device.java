package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class Device {
    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("type")
    public String type;
}
