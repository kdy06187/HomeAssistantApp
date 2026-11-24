package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

/**
 * 등록 작업 상태. E3 접수(202)와 E8 조회(200)가 같은 모양을 쓴다(E3에는 jobId·state만 온다).
 * jobId는 해석하지 않는다. 서버 13b·계약 14번 확정 뒤 모양을 다시 맞춘다.
 */
public class CommissionJobResponse {
    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";

    @SerializedName("jobId")
    public String jobId;

    @SerializedName("state")
    public String state;

    // 성공했을 때만 값이 있다.
    @SerializedName("deviceId")
    public String deviceId;

    // 90일 안에 삭제한 같은 기기를 다시 등록해 기록을 이어받았으면 true
    @SerializedName("restored")
    public Boolean restored;

    // 실패했을 때만 값이 있다.
    @SerializedName("error")
    public JobError error;

    public static class JobError {
        @SerializedName("code")
        public String code;

        @SerializedName("message")
        public String message;
    }

    public boolean isRestored() {
        return Boolean.TRUE.equals(restored);
    }
}
