package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class EnergyLog {

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("totalEnergy_mWh")
    private long totalEnergy_mWh;

    // Getter & Setter
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public long getTotalEnergy_mWh() {
        return totalEnergy_mWh;
    }

    public void setTotalEnergy_mWh(long totalEnergy_mWh) {
        this.totalEnergy_mWh = totalEnergy_mWh;
    }
}
