package com.example.homeassistantapp.network;

import java.io.IOException;
import java.util.function.Supplier;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** 모든 요청에 Bearer 토큰 헤더를 붙인다. 토큰은 요청 때마다 Supplier에서 읽어 바뀐 값이 바로 쓰인다. */
public class AuthInterceptor implements Interceptor {
    static final String INVALID_TOKEN_BODY =
            "{\"error\": {\"code\": \"UNAUTHORIZED\", \"message\": \"저장된 API 토큰 형식이 올바르지 않습니다.\"}}";

    private final Supplier<String> tokenSupplier;

    public AuthInterceptor(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String token = tokenSupplier.get();

        // 토큰이 없으면 헤더 없이 보낸다(서버가 401로 답한다).
        if (token == null || token.isEmpty()) {
            return chain.proceed(original);
        }
        Request withAuth;
        try {
            withAuth = original.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
        } catch (IllegalArgumentException e) {
            // 줄바꿈 등 헤더에 못 넣는 문자가 저장돼 있으면 OkHttp 스레드에서 앱이 죽는다.
            // 서버에 보내지 않고 401을 만들어 돌려줘, 토큰 다시 입력 안내로 이어지게 한다.
            return invalidTokenResponse(original);
        }
        return chain.proceed(withAuth);
    }

    private static Response invalidTokenResponse(Request request) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .body(ResponseBody.create(MediaType.get("application/json; charset=utf-8"), INVALID_TOKEN_BODY))
                .build();
    }
}
