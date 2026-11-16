package com.example.homeassistantapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 불일치 #1: 모든 요청에 Bearer 헤더가 붙는지 확인한다.
 * 실제 서버 없이 시험하려고, 마지막 인터셉터가 요청을 붙잡아 두고 가짜 응답을 돌려준다.
 * (테스트용 가짜 토큰이며 실제 토큰이 아니다.)
 */
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
    public void 토큰이_있으면_Bearer_헤더를_붙인다() throws Exception {
        assertEquals("Bearer test-token-0123456789abcdef0123456789",
                send("test-token-0123456789abcdef0123456789").header("Authorization"));
    }

    @Test
    public void 토큰이_없으면_헤더를_붙이지_않는다() throws Exception {
        assertNull(send(null).header("Authorization"));
        assertNull(send("").header("Authorization"));
    }

    @Test
    public void 토큰을_바꾸면_다음_요청부터_새_토큰을_쓴다() throws Exception {
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
