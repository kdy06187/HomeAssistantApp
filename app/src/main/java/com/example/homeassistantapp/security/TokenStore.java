package com.example.homeassistantapp.security;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * API 토큰 보관. 암호문은 백업에서 빠지는 SecretPrefs에 둔다. 예전 버전은 HomeAssistantPrefs에 두었으므로
 * 앱을 켤 때 암호문을 그대로 옮긴다(키 별칭이 같아 다시 암호화할 필요가 없다).
 */
public final class TokenStore {
    private static final String KEY_ALIAS = "home_assistant_api_token_key";
    private static final String PREF_KEY = "API_TOKEN_ENC";
    private static final String LEGACY_PREFS = "HomeAssistantPrefs";
    private static final SecretStore STORE = new SecretStore(WifiPasswordStore.SECRET_PREFS, PREF_KEY, KEY_ALIAS);

    private TokenStore() {}

    /** 성공하면 true. 토큰 값은 로그에 남기지 않는다. */
    public static boolean save(Context context, String token) {
        return STORE.save(context, token);
    }

    public static StoredSecret read(Context context) {
        boolean stored = STORE.has(context);
        return StoredSecret.of(stored, stored ? STORE.load(context) : null);
    }

    /** MainActivity가 시작할 때 부른다. */
    public static PlainSecretMigration.Outcome migrateLegacy(Context context) {
        SharedPreferences legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE);
        return PlainSecretMigration.run(new PlainSecretMigration.Source() {
            @Override public boolean has() { return legacy.contains(PREF_KEY); }
            @Override public String get() { return legacy.getString(PREF_KEY, null); }
            @Override public void remove() { legacy.edit().remove(PREF_KEY).commit(); }
        }, new PlainSecretMigration.Target() {
            @Override public boolean has() { return STORE.has(context); }
            @Override public boolean save(String value) { return STORE.saveRaw(context, value); }
        });
    }
}
