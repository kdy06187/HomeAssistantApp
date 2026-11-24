package com.example.homeassistantapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ConnectionStateInterceptorTest {
    private final List<String> events = new ArrayList<>();

    private OkHttpClient client(Interceptor server) {
        return new OkHttpClient.Builder()
                .addInterceptor(new ConnectionStateInterceptor(new ConnectionStateInterceptor.Listener() {
                    @Override public void onResponse(int httpCode) { events.add("http " + httpCode); }
                    @Override public void onNetworkFailure() { events.add("fail"); }
                }))
                .addInterceptor(server)
                .build();
    }

    private static Interceptor answer(int code) {
        return chain -> new Response.Builder()
                .request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("x")
                .body(ResponseBody.create(MediaType.get("application/json"), "{}"))
                .build();
    }

    private static Request request() {
        return new Request.Builder().url("http://192.168.0.10:8000/api/devices").build();
    }

    @Test
    public void reportsResponseCode() throws IOException {
        client(answer(401)).newCall(request()).execute().close();
        client(answer(200)).newCall(request()).execute().close();
        assertEquals(List.of("http 401", "http 200"), events);
    }

    @Test
    public void reportsNetworkFailureAndRethrows() {
        Interceptor down = chain -> { throw new IOException("unexpected end of stream"); };
        try {
            client(down).newCall(request()).execute();
            fail();
        } catch (IOException expected) {
            assertEquals(List.of("fail"), events);
        }
    }
}
