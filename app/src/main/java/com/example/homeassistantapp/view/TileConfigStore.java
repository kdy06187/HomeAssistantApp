package com.example.homeassistantapp.view;

import android.content.Context;
import android.content.SharedPreferences;

/** 기기별 타일 설정 저장소. 순서(DashboardPrefs)와 파일을 나눠 따로 정리할 수 있게 한다. */
public final class TileConfigStore {
    public static final String PREFS_NAME = "TilePrefs";

    private final SharedPreferences prefs;

    public TileConfigStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String keyFor(String deviceId) {
        return "tile." + deviceId;
    }

    public TileConfig load(String deviceId) {
        return TileConfig.fromJson(prefs.getString(keyFor(deviceId), null));
    }

    public void save(String deviceId, TileConfig config) {
        prefs.edit().putString(keyFor(deviceId), config.toJson()).apply();
    }

    public void remove(String deviceId) {
        prefs.edit().remove(keyFor(deviceId)).apply();
    }
}
