package com.example.homeassistantapp.network;

import android.util.Log;

import com.example.homeassistantapp.api.HomeAssistantApi;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static HomeAssistantApi api = null;

    // 🌟 설정창에서 IP/Port를 변경할 때마다 호출되어 Base URL을 동적으로 갱신합니다.
    public static void rebuildClient(String ip, String port) {
        String baseUrl = "http://" + ip + ":" + port + "/";

        Log.d("RetrofitClient", "생성된 Base URL: " + baseUrl);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(HomeAssistantApi.class);
    }

    public static HomeAssistantApi getApi() {
        if (api == null) {
            // 앱 최초 실행 시 아직 설정이 저장 안 되어 null인 경우 방어 로직 (기본 IP 유지)
            rebuildClient("192.168.219.108", "8000");
        }
        return api;
    }
}
