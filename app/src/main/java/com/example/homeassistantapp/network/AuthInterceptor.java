package com.example.homeassistantapp.network;

import java.io.IOException;
import java.util.function.Supplier;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 모든 요청에 "Authorization: Bearer <토큰>" 헤더를 붙인다 (계약서 §1.1, 불일치 #1).
 *
 * 왜 인터셉터인가: API 메서드마다 @Header 를 달면 하나라도 빠뜨릴 수 있다.
 * OkHttp 인터셉터는 나가는 모든 요청을 한 곳에서 가로채므로 빠짐없이 헤더가 붙는다.
 *
 * 왜 토큰 값 대신 Supplier 를 받는가: 설정에서 토큰을 바꾸면 클라이언트를 새로 만들지 않아도
 * 다음 요청부터 새 토큰이 쓰이게 하려고, 요청할 때마다 현재 값을 물어본다.
 */
public class AuthInterceptor implements Interceptor {
    private final Supplier<String> tokenSupplier;

    public AuthInterceptor(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String token = tokenSupplier.get();

        // 토큰이 없으면 헤더 없이 보낸다. 서버는 401로 답하고, 앱은 설정 화면으로 안내한다.
        // (토큰 값은 보안상 로그에 남기지 않는다.)
        if (token == null || token.isEmpty()) {
            return chain.proceed(original);
        }
        Request withAuth = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        return chain.proceed(withAuth);
    }
}
