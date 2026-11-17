package com.example.homeassistantapp.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * 서버 오류 응답에서 사용자에게 보여 줄 문구를 꺼낸다 (계약서 §4.3, 불일치 #13).
 *
 * 왜 한 곳에 모았나: v1.0 오류 본문은 {"result","message"}와 {"error"} 두 모양이다(§6 I-03).
 * 서버 v2.0에서 모양을 하나로 정리하면 이 파일만 고치면 된다.
 */
public final class ErrorMessages {
    private ErrorMessages() {}

    /** JSON 본문에서 message, 없으면 error 값을 꺼낸다. 둘 다 없거나 JSON이 아니면 null. */
    public static String fromJson(String body) {
        if (body == null || body.isEmpty()) return null;
        try {
            // 이 프로젝트가 쓰는 Gson 버전에는 정적 parseString이 없어 인스턴스 방식을 쓴다.
            JsonElement root = new JsonParser().parse(body);
            if (!root.isJsonObject()) return null;
            JsonObject obj = root.getAsJsonObject();
            String message = stringOrNull(obj, "message");
            return message != null ? message : stringOrNull(obj, "error");
        } catch (RuntimeException e) {
            return null; // JSON이 아닌 본문(HTML 오류 페이지 등)
        }
    }

    /**
     * 실패한 Retrofit 응답의 오류 문구. 서버 문구가 없으면 "HTTP 코드"로 대신한다.
     * 주의: errorBody는 한 번만 읽을 수 있으므로 응답 하나에 한 번만 부른다.
     */
    public static String describe(Response<?> response) {
        String fromServer = null;
        ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            try {
                fromServer = fromJson(errorBody.string());
            } catch (IOException ignored) {
                // 본문을 읽지 못하면 코드만 보여 준다.
            }
        }
        return fromServer != null ? fromServer : "HTTP " + response.code();
    }

    private static String stringOrNull(JsonObject obj, String key) {
        JsonElement value = obj.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }
}
