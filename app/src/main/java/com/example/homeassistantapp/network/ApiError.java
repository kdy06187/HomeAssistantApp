package com.example.homeassistantapp.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * 실패 응답 {"error":{"code","message"}}. 분기는 code로, 화면 문구는 message로 한다.
 * 본문이 없거나 읽을 수 없으면(413 등) code·message가 null이고 httpCode만 있다.
 */
public final class ApiError {
    public final int httpCode;
    public final String code;
    public final String message;

    ApiError(int httpCode, String code, String message) {
        this.httpCode = httpCode;
        this.code = code;
        this.message = message;
    }

    /** errorBody는 한 번만 읽을 수 있으므로 응답당 한 번만 부른다. */
    public static ApiError from(Response<?> response) {
        String body = null;
        ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            try {
                body = errorBody.string();
            } catch (IOException ignored) {
            }
        }
        return parse(response.code(), body);
    }

    public static ApiError parse(int httpCode, String body) {
        if (body == null || body.isEmpty()) return new ApiError(httpCode, null, null);
        try {
            // 이 Gson 버전에는 정적 parseString이 없다.
            JsonElement root = new JsonParser().parse(body);
            if (!root.isJsonObject()) return new ApiError(httpCode, null, null);
            JsonElement error = root.getAsJsonObject().get("error");
            if (error != null && error.isJsonObject()) {
                JsonObject obj = error.getAsJsonObject();
                return new ApiError(httpCode, stringOrNull(obj, "code"), stringOrNull(obj, "message"));
            }
            // 앱이 직접 만든 401 등 {"error":"문구"} 모양도 문구만은 읽어 둔다.
            String text = error != null && error.isJsonPrimitive() ? error.getAsString() : null;
            return new ApiError(httpCode, null, text);
        } catch (RuntimeException e) {
            return new ApiError(httpCode, null, null);
        }
    }

    public boolean is(String expectedCode) {
        return expectedCode.equals(code);
    }

    private static String stringOrNull(JsonObject obj, String key) {
        JsonElement value = obj.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }
}
