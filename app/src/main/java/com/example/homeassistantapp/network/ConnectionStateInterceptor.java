package com.example.homeassistantapp.network;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * 모든 요청의 결과(응답 코드 또는 통신 실패)를 알린다. 응답·예외는 그대로 넘겨 각 콜백이 처리하게 한다.
 * OkHttp 백그라운드 스레드에서 불린다.
 */
public class ConnectionStateInterceptor implements Interceptor {
    public interface Listener {
        void onResponse(int httpCode);
        void onNetworkFailure();
    }

    private final Listener listener;

    public ConnectionStateInterceptor(Listener listener) {
        this.listener = listener;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response;
        try {
            response = chain.proceed(chain.request());
        } catch (IOException e) {
            // 화면을 떠나 취소한 요청은 서버 상태와 상관없다.
            if (!chain.call().isCanceled()) listener.onNetworkFailure();
            throw e;
        }
        listener.onResponse(response.code());
        return response;
    }
}
