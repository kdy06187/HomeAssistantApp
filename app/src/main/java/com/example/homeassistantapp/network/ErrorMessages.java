package com.example.homeassistantapp.network;

import retrofit2.Response;

/** 실패 응답에서 보여 줄 문구. 서버 고정 문구를 쓰고, 없을 때만 상태코드별 앱 문구를 쓴다. */
public final class ErrorMessages {
    private ErrorMessages() {}

    public static String describe(ApiError error) {
        if (error.message != null && !error.message.isEmpty()) return error.message;
        return fallbackFor(error.httpCode);
    }

    /** errorBody를 읽으므로 응답당 한 번만 부른다. 분기도 해야 하면 ApiError.from을 먼저 쓴다. */
    public static String describe(Response<?> response) {
        return describe(ApiError.from(response));
    }

    static String fallbackFor(int httpCode) {
        switch (httpCode) {
            case 400: return "요청 형식이 올바르지 않습니다.";
            case 401: return "인증에 실패했습니다.";
            case 404: return "찾을 수 없습니다.";
            case 409: return "지금은 처리할 수 없는 요청입니다.";
            case 413: return "요청이 너무 큽니다.";
            case 500: return "서버 내부 오류입니다.";
            case 502: return "기기가 응답하지 않습니다.";
            default: return "HTTP " + httpCode;
        }
    }

    /** 401은 MainActivity가 토큰 안내를 한 번만 띄우므로, 요청마다 실패 토스트를 띄우지 않는다. */
    public static boolean isUnauthorized(int httpCode) {
        return httpCode == 401;
    }
}
