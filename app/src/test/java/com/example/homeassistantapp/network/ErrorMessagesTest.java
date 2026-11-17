package com.example.homeassistantapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * 불일치 #13: 오류 문구를 message 또는 error 키에서 읽는다(계약서 §4.3). #12의 E6 400 문구도 여기서 나온다.
 */
public class ErrorMessagesTest {

    private static Response<Object> errorResponse(int code, String body) {
        return Response.error(code, ResponseBody.create(MediaType.get("application/json"), body));
    }

    // {"result","message"} 모양에서는 message를 읽는다 (E2·E3·E4)
    @Test
    public void messageKey_isRead() {
        assertEquals("허용되지 않은 명령입니다.",
                ErrorMessages.fromJson("{\"result\":\"error\",\"message\":\"허용되지 않은 명령입니다.\"}"));
    }

    // {"error"} 모양에서는 error를 읽는다 (E6, 401)
    @Test
    public void errorKey_isRead() {
        assertEquals("등록되지 않은 기기입니다.", ErrorMessages.fromJson("{\"error\":\"등록되지 않은 기기입니다.\"}"));
    }

    // JSON이 아니거나 키가 없으면 null
    @Test
    public void unreadableBody_returnsNull() {
        assertNull(ErrorMessages.fromJson("<html>500</html>"));
        assertNull(ErrorMessages.fromJson("{\"result\":\"error\"}"));
        assertNull(ErrorMessages.fromJson("[1,2]"));
        assertNull(ErrorMessages.fromJson(""));
        assertNull(ErrorMessages.fromJson(null));
    }

    // E6 400 응답이면 서버 문구를 돌려준다 (#12)
    @Test
    public void describe_usesServerText() {
        assertEquals("등록되지 않은 기기입니다.",
                ErrorMessages.describe(errorResponse(400, "{\"error\":\"등록되지 않은 기기입니다.\"}")));
    }

    // 서버 문구가 없으면 HTTP 코드로 대신한다
    @Test
    public void describe_fallsBackToCode() {
        assertEquals("HTTP 500", ErrorMessages.describe(errorResponse(500, "oops")));
    }
}
