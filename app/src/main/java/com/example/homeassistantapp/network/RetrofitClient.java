package com.example.homeassistantapp.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.homeassistantapp.api.HomeAssistantApi;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static HomeAssistantApi api = null;

    // 복호화된 토큰은 메모리에만 둔다. 인터셉터가 OkHttp 스레드에서 읽으므로 volatile.
    private static volatile String apiToken = null;

    /** 클라이언트를 다시 만들지 않아도 다음 요청부터 적용된다. */
    public static void setApiToken(String token) {
        apiToken = token;
    }

    public static boolean hasApiToken() {
        return apiToken != null && !apiToken.isEmpty();
    }

    // 연결 상태는 여기 한 곳에서만 바뀐다. 화면은 connectionState LiveData를 본다.
    private static final ConnectionState connection = new ConnectionState();
    private static final MutableLiveData<ConnectionState.Kind> connectionState =
            new MutableLiveData<>(ConnectionState.Kind.OK);

    public static LiveData<ConnectionState.Kind> getConnectionState() {
        return connectionState;
    }

    /** 토큰 오류로 들어간 뒤 아직 안내하지 않았으면 true를 돌려주고 내린다. */
    public static boolean consumeTokenNotice() {
        return connection.consumeTokenNotice();
    }

    /** 지금 토큰 오류 상태면 true. 설정 화면이 토큰 칸에 오류를 보인다. */
    public static boolean isTokenRejected() {
        return connection.get() == ConnectionState.Kind.TOKEN_ERROR;
    }

    /** 설정을 저장하면 새 값으로 다시 판단한다(배너를 내리고, 또 틀리면 다시 안내). */
    public static void onSettingsSaved() {
        connection.reset();
        connectionState.setValue(connection.get());
    }

    // OkHttp 스레드에서 불린다. 늦게 도착한 옛 값이 설정 저장 뒤 값을 덮지 않도록,
    // 메인 스레드에서 그 순간의 최신 상태를 읽어 내보낸다.
    private static Handler mainHandler;

    private static void publishLatest() {
        synchronized (RetrofitClient.class) {
            if (mainHandler == null) mainHandler = new Handler(Looper.getMainLooper());
        }
        mainHandler.post(() -> connectionState.setValue(connection.get()));
    }

    /** 이 세대 클라이언트의 결과만 상태에 반영하는 감시자. */
    private static ConnectionStateInterceptor.Listener listenerFor(int generation) {
        return new ConnectionStateInterceptor.Listener() {
            @Override
            public void onResponse(int httpCode) {
                if (connection.onResponse(generation, httpCode) != ConnectionState.Change.NONE) publishLatest();
            }

            @Override
            public void onNetworkFailure() {
                if (connection.onNetworkFailure(generation) != ConnectionState.Change.NONE) publishLatest();
            }
        };
    }

    public static void rebuildClient(String ip, String port) {
        String baseUrl = "http://" + ip + ":" + port + "/";

        Log.d("RetrofitClient", "생성된 Base URL: " + baseUrl);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                // 같은 LAN이라 연결은 금방 된다. 서버 PC가 꺼졌을 때 로딩이 1분씩 걸리지 않게 짧게 둔다.
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                // AuthInterceptor가 만든 401(형식이 틀린 토큰)도 받도록 상태 감시를 바깥에 둔다.
                .addInterceptor(new ConnectionStateInterceptor(listenerFor(connection.nextGeneration())))
                .addInterceptor(new AuthInterceptor(() -> apiToken))
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(HomeAssistantApi.class);
    }

    /**
     * 연결 테스트용 임시 클라이언트. 저장된 설정을 바꾸지 않고, 401이어도 전역 안내·탭 이동을 하지 않는다.
     * 주소가 URL로 만들 수 없는 값이면 IllegalArgumentException.
     */
    public static HomeAssistantApi createTestApi(String ip, String port, String token) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(() -> token))
                .build();
        return new Retrofit.Builder()
                .baseUrl("http://" + ip + ":" + port + "/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(HomeAssistantApi.class);
    }

    /** 지금 메모리에 있는 토큰(저장된 토큰을 복호화한 값). 연결 테스트가 칸이 비었을 때 쓴다. */
    public static String currentApiToken() {
        return apiToken;
    }

    public static HomeAssistantApi getApi() {
        if (api == null) {
            rebuildClient(ServerDefaults.IP, ServerDefaults.PORT);
        }
        return api;
    }
}
