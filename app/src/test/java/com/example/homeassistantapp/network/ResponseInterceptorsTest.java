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

// 마지막 인터셉터가 가짜 응답을 돌려줘 서버 없이 401 감시를 시험한다.
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

    @Test
    public void status401_notifies() throws Exception {
        assertEquals(1, countUnauthorized(401));
    }

    @Test
    public void otherStatus_doesNotNotify() throws Exception {
        assertEquals(0, countUnauthorized(200));
        assertEquals(0, countUnauthorized(400));
    }
}
