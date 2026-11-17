package com.example.homeassistantapp.network;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * 서버가 401을 돌려주면 알린다 (계약서 §4.2, 불일치 #2).
 *
 * 왜 인터셉터인가: 401은 어느 API에서나 올 수 있다. ViewModel의 콜백마다 검사하면 빠뜨리기 쉽고,
 * 인터셉터는 모든 응답을 한 곳에서 보므로 한 번만 짜면 된다.
 *
 * 응답 자체는 그대로 넘긴다. 각 화면의 콜백은 평소처럼 "실패"로 처리하고(LOADING 해제 등),
 * 화면 이동은 onUnauthorized를 받은 쪽(MainActivity)이 맡는다.
 * 주의: 이 메서드는 OkHttp 백그라운드 스레드에서 불린다.
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
