package com.example.homeassistantapp.network;

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

    // LiveData는 새 관찰자에게 마지막 값을 다시 주므로(화면 회전 등),
    // 처리 여부는 플래그로 따로 관리해 401 이동이 한 번만 일어나게 한다.
    private static final AtomicBoolean unauthorizedPending = new AtomicBoolean(false);
    private static final MutableLiveData<Boolean> unauthorizedSignal = new MutableLiveData<>();

    public static LiveData<Boolean> getUnauthorizedSignal() {
        return unauthorizedSignal;
    }

    /** 처리하지 않은 401이 있으면 true를 돌려주고 플래그를 내린다. */
    public static boolean consumeUnauthorized() {
        return unauthorizedPending.getAndSet(false);
    }

    private static final UnauthorizedNotice unauthorizedNotice = new UnauthorizedNotice();

    /** 설정을 저장하면 다시 401 안내를 할 수 있게 한다. */
    public static void resetUnauthorizedNotice() {
        unauthorizedNotice.reset();
    }

    private static void notifyUnauthorized() {
        if (!unauthorizedNotice.tryClaim()) return;
        unauthorizedPending.set(true);
        // OkHttp 스레드에서 불리므로 postValue
        unauthorizedSignal.postValue(true);
    }

    public static void rebuildClient(String ip, String port) {
        String baseUrl = "http://" + ip + ":" + port + "/";

        Log.d("RetrofitClient", "생성된 Base URL: " + baseUrl);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                // 같은 LAN이라 연결은 금방 된다. 서버 PC가 꺼졌을 때 로딩이 1분씩 걸리지 않게 짧게 둔다.
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                // AuthInterceptor가 만든 401(형식이 틀린 토큰)도 받도록 401 감시를 바깥에 둔다.
                .addInterceptor(new UnauthorizedInterceptor(RetrofitClient::notifyUnauthorized))
                .addInterceptor(new AuthInterceptor(() -> apiToken))
                .addInterceptor(new CommissionTimeoutInterceptor())
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
            rebuildClient(ServerDefaults.IP, ServerDefaults.PORT);
        }
        return api;
    }
}
