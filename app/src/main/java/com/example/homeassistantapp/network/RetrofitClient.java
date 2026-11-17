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

    // ── 401 알림 (불일치 #2) ──
    // 왜 LiveData와 플래그를 같이 쓰는가: LiveData는 마지막 값을 새 관찰자에게 다시 준다.
    // 화면 회전으로 MainActivity가 다시 만들어지면 예전 401로 또 이동하게 되므로,
    // "아직 처리 안 한 401이 있는가"는 플래그로 따로 들고, 처리하면 내린다.
    private static final AtomicBoolean unauthorizedPending = new AtomicBoolean(false);
    private static final MutableLiveData<Boolean> unauthorizedSignal = new MutableLiveData<>();

    /** MainActivity가 관찰한다. 값이 오면 consumeUnauthorized()로 처리 여부를 확인한다. */
    public static LiveData<Boolean> getUnauthorizedSignal() {
        return unauthorizedSignal;
    }

    /** 처리하지 않은 401이 있으면 true를 돌려주고 플래그를 내린다(한 번만 true). */
    public static boolean consumeUnauthorized() {
        return unauthorizedPending.getAndSet(false);
    }

    private static void notifyUnauthorized() {
        // 여러 요청이 동시에 401을 받아도 화면 이동은 한 번만 한다.
        if (unauthorizedPending.compareAndSet(false, true)) {
            // OkHttp 백그라운드 스레드이므로 setValue가 아니라 postValue를 쓴다.
            unauthorizedSignal.postValue(true);
        }
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
                // 계약서 §4.2: 401이면 토큰 입력 화면으로 보낸다(불일치 #2).
                .addInterceptor(new UnauthorizedInterceptor(RetrofitClient::notifyUnauthorized))
                // 계약서 E3: 기기 등록만 읽기 타임아웃 120초 이상(불일치 #4).
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
            // 앱 최초 실행 시 아직 설정이 저장 안 되어 null인 경우 방어 로직 (기본값은 ServerDefaults 한 곳)
            rebuildClient(ServerDefaults.IP, ServerDefaults.PORT);
        }
        return api;
    }
}
