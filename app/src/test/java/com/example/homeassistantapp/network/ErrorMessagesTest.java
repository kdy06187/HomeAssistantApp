package com.example.homeassistantapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class ErrorMessagesTest {

    private static Response<Object> errorResponse(int code, String body) {
        return Response.error(code, ResponseBody.create(MediaType.get("application/json"), body));
    }

    @Test
    public void messageKey_isRead() {
        assertEquals("허용되지 않은 명령입니다.",
                ErrorMessages.fromJson("{\"result\":\"error\",\"message\":\"허용되지 않은 명령입니다.\"}"));
    }

    @Test
    public void errorKey_isRead() {
        assertEquals("등록되지 않은 기기입니다.", ErrorMessages.fromJson("{\"error\":\"등록되지 않은 기기입니다.\"}"));
    }

    @Test
    public void unreadableBody_returnsNull() {
        assertNull(ErrorMessages.fromJson("<html>500</html>"));
        assertNull(ErrorMessages.fromJson("{\"result\":\"error\"}"));
        assertNull(ErrorMessages.fromJson("[1,2]"));
        assertNull(ErrorMessages.fromJson(""));
        assertNull(ErrorMessages.fromJson(null));
    }

    @Test
    public void describe_usesServerText() {
        assertEquals("등록되지 않은 기기입니다.",
                ErrorMessages.describe(errorResponse(400, "{\"error\":\"등록되지 않은 기기입니다.\"}")));
    }

    @Test
    public void describe_fallsBackToCode() {
        assertEquals("HTTP 500", ErrorMessages.describe(errorResponse(500, "oops")));
    }

    @Test
    public void onlyCode401_isUnauthorized() {
        org.junit.Assert.assertTrue(ErrorMessages.isUnauthorized(401));
        org.junit.Assert.assertFalse(ErrorMessages.isUnauthorized(400));
        org.junit.Assert.assertFalse(ErrorMessages.isUnauthorized(403));
        org.junit.Assert.assertFalse(ErrorMessages.isUnauthorized(200));
    }
}
