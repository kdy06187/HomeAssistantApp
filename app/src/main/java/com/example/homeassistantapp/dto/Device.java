package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class Device {
    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("type")
    public String type;
    @SerializedName("state")
    public String state;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setState(String state) {
        this.state = state;
    }
}
