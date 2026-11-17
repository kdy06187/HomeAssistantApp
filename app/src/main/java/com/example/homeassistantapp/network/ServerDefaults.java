package com.example.homeassistantapp.network;

/** 서버 주소 기본값. 설정을 저장한 적이 없을 때만 쓰고, 저장값이 항상 우선이다. */
public final class ServerDefaults {
    public static final String IP = "192.168.219.108";
    public static final String PORT = "8000";

    public static final String PREFS_NAME = "HomeAssistantPrefs";
    public static final String KEY_SERVER_IP = "SERVER_IP";
    public static final String KEY_SERVER_PORT = "SERVER_PORT";

    private ServerDefaults() {}
}
