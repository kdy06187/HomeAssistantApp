package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class EnergyResponse {
    @SerializedName("device_status")
    public String device_status;

    @SerializedName("activePower_mW")
    public int activePowerMw;

    @SerializedName("totalEnergy_mWh")
    public long totalEnergyMwh;
}
