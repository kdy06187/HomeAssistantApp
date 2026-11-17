package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class ControlResponse {
    public static final String SUCCESS_MESSAGE = "명령 성공!";

    // 처리 결과가 아니라 기기 상태(ON/OFF/UNKNOWN)
    @SerializedName("status")
    public String status;
    @SerializedName("message")
    public String message;

    /** 서버는 명령이 실패해도 200을 주므로 message로 성공을 판단한다. */
    public boolean isCommandSucceeded() {
        return SUCCESS_MESSAGE.equals(message);
    }
}
