package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Device {
    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("type")
    public String type;
    @SerializedName("state")
    public String state;

    public String powerInfo = "전력 정보 없음";
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
    private List<EnergyLog> energyLogs;
    public void setState(String state) {
        this.state = state;
    }
    public String getPowerInfo() {
        return powerInfo;
    }

    public void setPowerInfo(String powerInfo) {
        this.powerInfo = powerInfo;
    }
    public List<EnergyLog> getEnergyLogs() {
        return energyLogs;
    }

    public void setEnergyLogs(List<EnergyLog> energyLogs) {
        this.energyLogs = energyLogs;
    }
}
