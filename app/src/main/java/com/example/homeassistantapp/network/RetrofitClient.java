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

    // 서버 API 토큰(복호화된 값). 메모리에만 두고 디스크에는 TokenStore 가 암호화해서 저장한다.
    // volatile: OkHttp 는 백그라운드 스레드에서 인터셉터를 실행하므로, 메인 스레드에서 바꾼 값이 바로 보이게 한다.
    private static volatile String apiToken = null;

    /** 설정 저장이나 앱 시작 시 호출. 클라이언트를 다시 만들 필요 없이 다음 요청부터 적용된다. */
    public static void setApiToken(String token) {
        apiToken = token;
    }

    public static boolean hasApiToken() {
        return apiToken != null && !apiToken.isEmpty();
    }

    // 🌟 설정창에서 IP/Port를 변경할 때마다 호출되어 Base URL을 동적으로 갱신합니다.
    public static void rebuildClient(String ip, String port) {
        String baseUrl = "http://" + ip + ":" + port + "/";

        Log.d("RetrofitClient", "생성된 Base URL: " + baseUrl);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                // 계약서 §1.1: 모든 경로에 Bearer 토큰이 필요하다.
                .addInterceptor(new AuthInterceptor(() -> apiToken))
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
