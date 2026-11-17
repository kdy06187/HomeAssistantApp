package com.example.homeassistantapp.network;

/**
 * 서버 주소 기본값을 한 곳에 모은다 (불일치 #3).
 *
 * 왜: 예전에는 MainActivity·RetrofitClient·SettingsFragment가 기본값을 따로 들고 있어서
 * 설정 화면만 `:8080`(DIY 기기 전용 TCP 포트)을 보여 줬다. 계약서 §1: 앱은 8000만 쓴다.
 *
 * 기본값은 설정을 한 번도 저장하지 않았을 때만 쓴다. 저장된 설정값이 항상 우선이다.
 * (서버 IP는 DHCP라 바뀔 수 있으므로 설정 화면에서 고쳐 쓰는 것이 기본 흐름이다.)
 */
public final class ServerDefaults {
    public static final String IP = "192.168.219.108";
    public static final String PORT = "8000";

    // SharedPreferences 이름과 키
    public static final String PREFS_NAME = "HomeAssistantPrefs";
    public static final String KEY_SERVER_IP = "SERVER_IP";
    public static final String KEY_SERVER_PORT = "SERVER_PORT";

    private ServerDefaults() {}
}
