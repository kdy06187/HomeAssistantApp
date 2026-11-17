package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class ControlResponse {
    // 계약서 E2: 서버가 명령을 실제로 해냈을 때만 이 문구를 준다.
    public static final String SUCCESS_MESSAGE = "명령 성공!";

    // E2에서는 처리 결과가 아니라 "기기 상태"다(ON/OFF/UNKNOWN 등).
    @SerializedName("status")
    public String status;
    @SerializedName("message")
    public String message;

    /**
     * 명령이 성공했는지 본다 (계약서 §4.1, 불일치 #5).
     * 왜 HTTP 코드만으로는 안 되는가: E2는 기기가 응답하지 않아도 200에 "명령 실패!"를 준다(§6 I-01).
     * v2.0에서 실패를 4xx로 바꾸면 이 판정만 고치면 된다.
     */
    public boolean isCommandSucceeded() {
        return SUCCESS_MESSAGE.equals(message);
    }
}
