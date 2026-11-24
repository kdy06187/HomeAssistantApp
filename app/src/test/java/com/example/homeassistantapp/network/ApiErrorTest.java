package com.example.homeassistantapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class ApiErrorTest {

    @Test
    public void standardBody_readsCodeAndMessage() {
        ApiError e = ApiError.parse(404,
                "{\"error\":{\"code\":\"DEVICE_NOT_FOUND\",\"message\":\"등록되지 않은 기기입니다.\"}}");
        assertEquals(404, e.httpCode);
        assertEquals("DEVICE_NOT_FOUND", e.code);
        assertEquals("등록되지 않은 기기입니다.", e.message);
        assertTrue(e.is("DEVICE_NOT_FOUND"));
        assertFalse(e.is("NOT_FOUND"));
    }

    @Test
    public void codeOnly_messageIsNull() {
        ApiError e = ApiError.parse(413, "{\"error\":{\"code\":\"PAYLOAD_TOO_LARGE\"}}");
        assertEquals("PAYLOAD_TOO_LARGE", e.code);
        assertNull(e.message);
    }

    @Test
    public void unknownCode_isKeptAsIs() {
        ApiError e = ApiError.parse(409, "{\"error\":{\"code\":\"SOMETHING_NEW\",\"message\":\"새 오류\"}}");
        assertEquals("SOMETHING_NEW", e.code);
        assertEquals("새 오류", e.message);
    }

    @Test
    public void stringErrorShape_readsMessageOnly() {
        ApiError e = ApiError.parse(401, "{\"error\":\"인증이 필요합니다.\"}");
        assertNull(e.code);
        assertEquals("인증이 필요합니다.", e.message);
    }

    @Test
    public void oldResultMessageShape_isNotRead() {
        // v1.0 모양 {"result","message"}는 더 이상 읽지 않는다.
        ApiError e = ApiError.parse(400, "{\"result\":\"error\",\"message\":\"잘못된 JSON 형식입니다.\"}");
        assertNull(e.code);
        assertNull(e.message);
    }

    @Test
    public void emptyOrBrokenBody_keepsHttpCodeOnly() {
        String[] bodies = {null, "", "<html>413</html>", "[1,2]", "{\"error\":[1]}", "{"};
        for (String body : bodies) {
            ApiError e = ApiError.parse(413, body);
            assertEquals(413, e.httpCode);
            assertNull(String.valueOf(body), e.code);
            assertNull(String.valueOf(body), e.message);
        }
    }

    @Test
    public void fromResponse_readsErrorBody() {
        Response<Object> response = Response.error(502, ResponseBody.create(MediaType.get("application/json"),
                "{\"error\":{\"code\":\"DEVICE_UNREACHABLE\",\"message\":\"기기가 응답하지 않습니다.\"}}"));
        ApiError e = ApiError.from(response);
        assertEquals(502, e.httpCode);
        assertEquals("DEVICE_UNREACHABLE", e.code);
    }
}
