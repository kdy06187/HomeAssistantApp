package com.example.homeassistantapp.network;

import java.io.IOException;
import java.util.function.Supplier;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** 모든 요청에 Bearer 토큰 헤더를 붙인다. 토큰은 요청 때마다 Supplier에서 읽어 바뀐 값이 바로 쓰인다. */
public class AuthInterceptor implements Interceptor {
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
        Request withAuth = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        return chain.proceed(withAuth);
    }
}
