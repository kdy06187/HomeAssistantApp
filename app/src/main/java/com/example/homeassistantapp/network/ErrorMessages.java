package com.example.homeassistantapp.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

/** 서버 오류 본문({"result","message"} 또는 {"error"})에서 보여 줄 문구를 꺼낸다. */
public final class ErrorMessages {
    private ErrorMessages() {}

    /** message, 없으면 error. 둘 다 없거나 JSON이 아니면 null. */
    public static String fromJson(String body) {
        if (body == null || body.isEmpty()) return null;
        try {
            // 이 Gson 버전에는 정적 parseString이 없다.
            JsonElement root = new JsonParser().parse(body);
            if (!root.isJsonObject()) return null;
            JsonObject obj = root.getAsJsonObject();
            String message = stringOrNull(obj, "message");
            return message != null ? message : stringOrNull(obj, "error");
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 서버 문구가 없으면 "HTTP 코드". errorBody는 한 번만 읽을 수 있어 응답당 한 번만 부른다. */
    public static String describe(Response<?> response) {
        String fromServer = null;
        ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            try {
                fromServer = fromJson(errorBody.string());
            } catch (IOException ignored) {
            }
        }
        return fromServer != null ? fromServer : "HTTP " + response.code();
    }

    private static String stringOrNull(JsonObject obj, String key) {
        JsonElement value = obj.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }
}
