package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

/** 제어 성공 본문. 성공 여부는 HTTP 상태코드로만 판단한다. */
public class ControlResponse {
    @SerializedName("deviceId")
    public String deviceId;

    // 202(지연 수락)이면 바뀌기 전 상태
    @SerializedName("state")
    public String state;
}
