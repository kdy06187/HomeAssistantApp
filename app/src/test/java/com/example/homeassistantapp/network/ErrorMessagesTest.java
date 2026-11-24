package com.example.homeassistantapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class ErrorMessagesTest {

    private static Response<Object> errorResponse(int code, String body) {
        return Response.error(code, ResponseBody.create(MediaType.get("application/json"), body));
    }

    @Test
    public void describe_usesServerMessage() {
        assertEquals("기기 드라이버가 명령을 막았습니다.", ErrorMessages.describe(errorResponse(409,
                "{\"error\":{\"code\":\"COMMAND_BLOCKED\",\"message\":\"기기 드라이버가 명령을 막았습니다.\"}}")));
    }

    @Test
    public void describe_withoutMessage_usesStatusFallback() {
        assertEquals("요청이 너무 큽니다.", ErrorMessages.describe(errorResponse(413, "")));
        assertEquals("기기가 응답하지 않습니다.",
                ErrorMessages.describe(errorResponse(502, "{\"error\":{\"code\":\"DEVICE_UNREACHABLE\"}}")));
        assertEquals("서버 내부 오류입니다.", ErrorMessages.describe(errorResponse(500, "oops")));
    }

    @Test
    public void describe_emptyMessage_usesStatusFallback() {
        assertEquals("찾을 수 없습니다.",
                ErrorMessages.describe(ApiError.parse(404, "{\"error\":{\"code\":\"NOT_FOUND\",\"message\":\"\"}}")));
    }

    @Test
    public void unknownStatus_fallsBackToHttpCode() {
        assertEquals("HTTP 503", ErrorMessages.describe(errorResponse(503, "")));
    }

    @Test
    public void everyContractStatus_hasFallback() {
        int[] codes = {400, 401, 404, 409, 413, 500, 502};
        for (int code : codes) {
            assertFalse(String.valueOf(code), ErrorMessages.fallbackFor(code).startsWith("HTTP "));
        }
    }

    @Test
    public void onlyCode401_isUnauthorized() {
        assertTrue(ErrorMessages.isUnauthorized(401));
        assertFalse(ErrorMessages.isUnauthorized(400));
        assertFalse(ErrorMessages.isUnauthorized(403));
        assertFalse(ErrorMessages.isUnauthorized(200));
    }
}
