package com.example.homeassistantapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

// 마지막 인터셉터가 요청을 붙잡고 가짜 응답을 돌려줘 서버 없이 시험한다. 토큰은 테스트용 가짜 값.
public class AuthInterceptorTest {

    private Request send(String token) throws Exception {
        AtomicReference<Request> captured = new AtomicReference<>();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(() -> token))
                .addInterceptor(chain -> {
                    captured.set(chain.request());
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create(MediaType.get("application/json"), "{}"))
                            .build();
                })
                .build();
        client.newCall(new Request.Builder().url("http://localhost:8000/api/devices").build())
                .execute().close();
        return captured.get();
    }

    @Test
    public void tokenPresent_addsBearerHeader() throws Exception {
        assertEquals("Bearer test-token-0123456789abcdef0123456789",
                send("test-token-0123456789abcdef0123456789").header("Authorization"));
    }

    @Test
    public void tokenMissing_omitsAuthorizationHeader() throws Exception {
        assertNull(send(null).header("Authorization"));
        assertNull(send("").header("Authorization"));
    }

    @Test
    public void tokenWithNewline_returns401WithoutCrashingOrSending() throws Exception {
        AtomicBoolean reachedServer = new AtomicBoolean(false);
        AtomicInteger unauthorizedCalls = new AtomicInteger();
        // RetrofitClient와 같은 순서: 401 감시가 바깥, 토큰 헤더가 안쪽
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new ConnectionStateInterceptor(new ConnectionStateInterceptor.Listener() {
                    @Override public void onResponse(int httpCode) { if (httpCode == 401) unauthorizedCalls.incrementAndGet(); }
                    @Override public void onNetworkFailure() {}
                }))
                .addInterceptor(new AuthInterceptor(() -> "test-token\nabc"))
                .addInterceptor(chain -> {
                    reachedServer.set(true);
                    return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                            .code(200).message("OK")
                            .body(ResponseBody.create(MediaType.get("application/json"), "{}")).build();
                })
                .build();

        Response response = client.newCall(
                new Request.Builder().url("http://localhost:8000/api/devices").build()).execute();

        assertEquals(401, response.code());
        assertEquals(AuthInterceptor.INVALID_TOKEN_BODY, response.body().string());
        assertEquals(1, unauthorizedCalls.get());
        assertFalse(reachedServer.get());
    }

    @Test
    public void tokenWithNonAscii_returns401() throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(() -> "토큰"))
                .addInterceptor(chain -> {
                    throw new AssertionError("서버로 보내면 안 된다");
                })
                .build();
        Response response = client.newCall(
                new Request.Builder().url("http://localhost:8000/api/devices").build()).execute();
        assertEquals(401, response.code());
        response.close();
    }

    @Test
    public void tokenChanged_nextRequestUsesNewToken() throws Exception {
        AtomicReference<String> token = new AtomicReference<>("old-token");
        AtomicReference<Request> captured = new AtomicReference<>();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(token::get))
                .addInterceptor(chain -> {
                    captured.set(chain.request());
                    return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                            .code(200).message("OK")
                            .body(ResponseBody.create(MediaType.get("application/json"), "{}")).build();
                })
                .build();
        Request req = new Request.Builder().url("http://localhost:8000/api/devices").build();

        client.newCall(req).execute().close();
        assertEquals("Bearer old-token", captured.get().header("Authorization"));

        token.set("new-token");
        client.newCall(req).execute().close();
        assertEquals("Bearer new-token", captured.get().header("Authorization"));
    }
}
