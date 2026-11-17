package com.example.homeassistantapp.network;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * 기기 등록(E3) 요청만 읽기 타임아웃을 길게 잡는다 (계약서 E3, 불일치 #4).
 *
 * 왜 전체 타임아웃을 늘리지 않는가: E3은 BLE 스캔·페어링이 끝날 때까지 응답이 오지 않아
 * 120초 이상 기다려야 한다. 하지만 목록 조회 같은 다른 요청까지 그만큼 기다리면
 * 서버가 멈췄을 때 사용자가 2분 넘게 빈 화면을 보게 된다. 그래서 이 경로만 늘린다.
 */
public class CommissionTimeoutInterceptor implements Interceptor {
    public static final String COMMISSION_PATH = "/api/commission";
    public static final int COMMISSION_READ_TIMEOUT_SECONDS = 150; // 계약서: 120초 이상

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (COMMISSION_PATH.equals(chain.request().url().encodedPath())) {
            return chain.withReadTimeout(COMMISSION_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .proceed(chain.request());
        }
        return chain.proceed(chain.request());
    }
}
