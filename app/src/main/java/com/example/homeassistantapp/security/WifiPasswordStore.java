package com.example.homeassistantapp.security;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Wi-Fi 비밀번호 보관. 암호문은 백업에서 빠지는 SecretPrefs에 둔다. 예전 평문 값은 앱을 켤 때 옮기고 지운다.
 * 암호화가 실패하면 평문을 백업에 들어가는 HomeAssistantPrefs에서 SecretPrefs의 대기 칸으로 옮겨 두고 다음에 다시 한다.
 */
public final class WifiPasswordStore {
    public static final String SECRET_PREFS = "SecretPrefs";
    private static final String LEGACY_PREFS = "HomeAssistantPrefs";
    private static final String LEGACY_KEY = "WIFI_PASSWORD";
    private static final String PENDING_KEY = "WIFI_PASSWORD_PENDING";
    private static final SecretStore STORE =
            new SecretStore(SECRET_PREFS, "WIFI_PASSWORD_ENC", "home_assistant_wifi_password_key");

    private WifiPasswordStore() {}

    /** 성공하면 남아 있던 평문(예전 칸·대기 칸)도 지운다. 안 지우면 옛 값이 남는다. */
    public static boolean save(Context context, String password) {
        if (!STORE.save(context, password)) return false;
        legacy(context).edit().remove(LEGACY_KEY).commit();
        secret(context).edit().remove(PENDING_KEY).commit();
        return true;
    }

    public static StoredSecret read(Context context) {
        if (STORE.has(context)) return StoredSecret.of(true, STORE.load(context));
        // 옮기기가 실패해 평문이 남아 있으면 다음 실행에 옮길 때까지 그 값을 쓴다.
        String pending = plainLeft(context);
        return StoredSecret.of(pending != null, pending);
    }

    /** MainActivity가 시작할 때 부른다. */
    public static void migrateLegacy(Context context) {
        SharedPreferences legacy = legacy(context);
        SharedPreferences secret = secret(context);
        PlainSecretMigration.Target target = new PlainSecretMigration.Target() {
            @Override public boolean has() { return STORE.has(context); }
            @Override public boolean save(String value) { return STORE.save(context, value); }
        };
        PlainSecretMigration.run(source(secret, PENDING_KEY), target);
        PlainSecretMigration.Outcome fromLegacy = PlainSecretMigration.run(source(legacy, LEGACY_KEY), target);
        if (fromLegacy == PlainSecretMigration.Outcome.KEPT_FOR_RETRY) {
            // 암호화는 실패했지만 평문이 백업에 들어가지 않게 백업 제외 파일로 옮긴다.
            String value = legacy.getString(LEGACY_KEY, "");
            if (secret.edit().putString(PENDING_KEY, value).commit()) legacy.edit().remove(LEGACY_KEY).commit();
        }
    }

    private static String plainLeft(Context context) {
        SharedPreferences secret = secret(context);
        if (secret.contains(PENDING_KEY)) return secret.getString(PENDING_KEY, "");
        SharedPreferences legacy = legacy(context);
        return legacy.contains(LEGACY_KEY) ? legacy.getString(LEGACY_KEY, "") : null;
    }

    private static PlainSecretMigration.Source source(SharedPreferences prefs, String key) {
        return new PlainSecretMigration.Source() {
            @Override public boolean has() { return prefs.contains(key); }
            @Override public String get() { return prefs.getString(key, null); }
            @Override public void remove() { prefs.edit().remove(key).commit(); }
        };
    }

    private static SharedPreferences legacy(Context context) {
        return context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE);
    }

    private static SharedPreferences secret(Context context) {
        return context.getSharedPreferences(SECRET_PREFS, Context.MODE_PRIVATE);
    }
}
