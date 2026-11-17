package com.example.homeassistantapp.network;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.Response;

/** 기기 등록은 페어링이 끝날 때까지 응답이 없어서, 이 경로만 읽기 타임아웃을 늘린다. */
public class CommissionTimeoutInterceptor implements Interceptor {
    public static final String COMMISSION_PATH = "/api/commission";
    public static final int COMMISSION_READ_TIMEOUT_SECONDS = 150; // 120초 이상 필요

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (COMMISSION_PATH.equals(chain.request().url().encodedPath())) {
            return chain.withReadTimeout(COMMISSION_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .proceed(chain.request());
        }
        return chain.proceed(chain.request());
    }
}
