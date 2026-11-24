package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class EnergyResponse {
    @SerializedName("deviceId")
    public String deviceId;

    @SerializedName("state")
    public String state;

    @SerializedName("activePower_mW")
    public int activePowerMw;

    @SerializedName("totalEnergy_mWh")
    public long totalEnergyMwh;
}
