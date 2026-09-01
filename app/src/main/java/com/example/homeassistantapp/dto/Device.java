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

    public boolean isPairing;

    public boolean isPairing() {
        return isPairing;
    }

    public void setPairing(boolean pairing) {
        isPairing = pairing;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getState() {
        return state;
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
