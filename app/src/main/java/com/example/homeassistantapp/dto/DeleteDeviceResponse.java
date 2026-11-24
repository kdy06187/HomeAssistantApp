package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

/** 삭제(숨김) 성공 본문. 시각은 UTC ISO 8601 문자열. */
public class DeleteDeviceResponse {
    @SerializedName("deviceId")
    public String deviceId;

    @SerializedName("deletedAt")
    public String deletedAt;

    // 이 시각 전에 같은 기기를 다시 등록하면 기록을 이어받는다.
    @SerializedName("restorableUntil")
    public String restorableUntil;

    // false면 숨김은 됐지만 기기 쪽 연결 해제가 실패했다.
    @SerializedName("unpaired")
    public Boolean unpaired;

    /** 값이 빠졌을 때는 경고하지 않는다(서버가 실패를 명시한 경우만). */
    public boolean needsDeviceReset() {
        return Boolean.FALSE.equals(unpaired);
    }
}
