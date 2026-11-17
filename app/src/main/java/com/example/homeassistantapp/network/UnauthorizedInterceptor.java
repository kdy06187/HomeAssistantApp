package com.example.homeassistantapp.network;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * 401 응답이면 onUnauthorized를 부른다. 응답은 그대로 넘겨 각 콜백이 실패 처리하게 한다.
 * OkHttp 백그라운드 스레드에서 불린다.
 */
public class UnauthorizedInterceptor implements Interceptor {
    private final Runnable onUnauthorized;

    public UnauthorizedInterceptor(Runnable onUnauthorized) {
        this.onUnauthorized = onUnauthorized;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        if (response.code() == 401) {
            onUnauthorized.run();
        }
        return response;
    }
}
