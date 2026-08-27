package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class EnergyResponse {
    @SerializedName("deviceId")
    public String deviceId;

    // 실시간 전력량 (밀리와트 단위)
    @SerializedName("activePower_mW")
    public int activePowerMw;

    // 누적 전력량 (밀리와트시 단위 - 값이 클 수 있으므로 long 사용)
    @SerializedName("totalEnergy_mWh")
    public long totalEnergyMwh;
}
