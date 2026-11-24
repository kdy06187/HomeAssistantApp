package com.example.homeassistantapp.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.homeassistantapp.api.HomeAssistantApi;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    // LiveData는 새 관찰자에게 마지막 값을 다시 주므로(화면 회전 등),
    // 처리 여부는 플래그로 따로 관리해 토큰 오류로 바뀐 순간의 설정 탭 이동이 한 번만 일어나게 한다.
    private static final AtomicBoolean unauthorizedPending = new AtomicBoolean(false);
    private static final MutableLiveData<Boolean> unauthorizedSignal = new MutableLiveData<>();

    public static LiveData<ConnectionState.Kind> getConnectionState() {
        return connectionState;
    }

    public static LiveData<Boolean> getUnauthorizedSignal() {
        return unauthorizedSignal;
    }

    /** 처리하지 않은 토큰 오류 진입이 있으면 true를 돌려주고 플래그를 내린다. */
    public static boolean consumeUnauthorized() {
        return unauthorizedPending.getAndSet(false);
    }

    /** 지금 토큰 오류 상태면 true. 설정 화면이 토큰 칸에 오류를 보인다. */
    public static boolean isTokenRejected() {
        return connection.get() == ConnectionState.Kind.TOKEN_ERROR;
    }

    /** 설정을 저장하면 새 값으로 다시 판단한다(배너를 내리고, 또 틀리면 다시 안내). */
    public static void onSettingsSaved() {
        unauthorizedPending.set(false);
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

    private static final ConnectionStateInterceptor.Listener connectionListener =
            new ConnectionStateInterceptor.Listener() {
                @Override
                public void onResponse(int httpCode) {
                    if (!connection.onResponse(httpCode)) return;
                    publishLatest();
                    if (connection.get() == ConnectionState.Kind.TOKEN_ERROR) {
                        unauthorizedPending.set(true);
                        unauthorizedSignal.postValue(true);
                    }
                }

                @Override
                public void onNetworkFailure() {
                    if (connection.onNetworkFailure()) publishLatest();
                }
            };

    public static void rebuildClient(String ip, String port) {
        String baseUrl = "http://" + ip + ":" + port + "/";

        Log.d("RetrofitClient", "생성된 Base URL: " + baseUrl);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                // 같은 LAN이라 연결은 금방 된다. 서버 PC가 꺼졌을 때 로딩이 1분씩 걸리지 않게 짧게 둔다.
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                // AuthInterceptor가 만든 401(형식이 틀린 토큰)도 받도록 상태 감시를 바깥에 둔다.
                .addInterceptor(new ConnectionStateInterceptor(connectionListener))
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
