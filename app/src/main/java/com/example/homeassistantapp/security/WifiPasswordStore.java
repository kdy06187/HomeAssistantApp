package com.example.homeassistantapp.security;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Wi-Fi 비밀번호 보관. 암호문은 백업에서 빠지는 SecretPrefs에 둔다(키가 기기 밖으로 나가지 않으므로
 * 백업해 봐야 다른 기기에서는 읽을 수 없다). 예전 평문 값은 앱을 켤 때 옮기고 지운다.
 */
public final class WifiPasswordStore {
    public static final String SECRET_PREFS = "SecretPrefs";
    private static final String LEGACY_PREFS = "HomeAssistantPrefs";
    private static final String LEGACY_KEY = "WIFI_PASSWORD";
    private static final SecretStore STORE =
            new SecretStore(SECRET_PREFS, "WIFI_PASSWORD_ENC", "home_assistant_wifi_password_key");

    private WifiPasswordStore() {}

    public static boolean save(Context context, String password) {
        return STORE.save(context, password);
    }

    public static WifiPassword read(Context context) {
        boolean stored = STORE.has(context);
        if (!stored) {
            // 옮기기가 실패해 평문이 남아 있으면 다음 실행에 옮길 때까지 그 값을 쓴다.
            SharedPreferences legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE);
            if (legacy.contains(LEGACY_KEY)) return WifiPassword.of(true, legacy.getString(LEGACY_KEY, ""));
        }
        return WifiPassword.of(stored, stored ? STORE.load(context) : null);
    }

    /** MainActivity가 시작할 때 부른다. 실패하면 평문을 남기고 다음 실행에 다시 시도한다. */
    public static PlainSecretMigration.Outcome migrateLegacy(Context context) {
        SharedPreferences legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE);
        return PlainSecretMigration.run(new PlainSecretMigration.Source() {
            @Override public boolean has() { return legacy.contains(LEGACY_KEY); }
            @Override public String get() { return legacy.getString(LEGACY_KEY, ""); }
            @Override public void remove() { legacy.edit().remove(LEGACY_KEY).commit(); }
        }, value -> STORE.save(context, value));
    }
}
