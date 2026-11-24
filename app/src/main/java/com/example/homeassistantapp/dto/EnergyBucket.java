package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

/** E7 집계 모드의 구간 하나. 날짜는 +09:00 기준 "YYYY-MM-DD"(포함). */
public class EnergyBucket {
    @SerializedName("start")
    public String start;

    @SerializedName("end")
    public String end;

    @SerializedName("energyUsed_mWh")
    public long energyUsedMwh;

    // 0이면 그 구간에 기록이 없다(사용량 0과 다름).
    @SerializedName("sampleCount")
    public long sampleCount;
}
