package com.example.homeassistantapp.network;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 불일치 #2(401 알림)와 #4(E3만 읽기 타임아웃 120초 이상)를 실제 서버 없이 확인한다.
 * 마지막 인터셉터가 가짜 응답을 돌려주고, 그때 적용된 타임아웃을 기록한다.
 */
public class ResponseInterceptorsTest {

    private static Response fake(Interceptor.Chain chain, int code) {
        return new Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("fake")
                .body(ResponseBody.create(MediaType.get("application/json"), "{}"))
                .build();
    }

    private static int countUnauthorized(int responseCode) throws Exception {
        AtomicInteger calls = new AtomicInteger();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new UnauthorizedInterceptor(calls::incrementAndGet))
                .addInterceptor(chain -> fake(chain, responseCode))
                .build();
        client.newCall(new Request.Builder().url("http://localhost:8000/api/devices").build())
                .execute().close();
        return calls.get();
    }

    // 401이면 알린다
    @Test
    public void status401_notifies() throws Exception {
        assertEquals(1, countUnauthorized(401));
    }

    // 200, 400은 알리지 않는다
    @Test
    public void otherStatus_doesNotNotify() throws Exception {
        assertEquals(0, countUnauthorized(200));
        assertEquals(0, countUnauthorized(400));
    }

    private static int readTimeoutFor(String path) throws Exception {
        AtomicInteger seenMillis = new AtomicInteger();
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(new CommissionTimeoutInterceptor())
                .addInterceptor(chain -> {
                    seenMillis.set(chain.readTimeoutMillis());
                    return fake(chain, 200);
                })
                .build();
        client.newCall(new Request.Builder().url("http://localhost:8000" + path).build())
                .execute().close();
        return seenMillis.get();
    }

    // 기기 등록(E3)은 읽기 타임아웃이 120초 이상이다
    @Test
    public void commission_usesLongReadTimeout() throws Exception {
        int millis = readTimeoutFor("/api/commission");
        assertEquals(150_000, millis);
        assertEquals(true, millis >= 120_000);
    }

    // 다른 경로는 기본 60초 그대로다
    @Test
    public void otherPath_keepsDefaultReadTimeout() throws Exception {
        assertEquals(60_000, readTimeoutFor("/api/devices"));
    }
}
