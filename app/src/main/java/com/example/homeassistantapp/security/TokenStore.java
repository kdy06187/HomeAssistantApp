package com.example.homeassistantapp.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * API 토큰을 Android Keystore 키(AES-GCM)로 암호화해 저장한다. 키는 기기 밖으로 꺼낼 수 없다.
 * 저장 형식: "Base64(IV):Base64(암호문)"
 */
public final class TokenStore {
    private static final String TAG = "TokenStore";
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "home_assistant_api_token_key";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;

    private static final String PREFS = "HomeAssistantPrefs";
    private static final String PREF_KEY = "API_TOKEN_ENC";

    private TokenStore() {}

    /** 성공하면 true. 토큰 값은 로그에 남기지 않는다. */
    public static boolean save(Context context, String token) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            // Keystore 키는 IV를 직접 지정할 수 없어 cipher가 만든 IV를 함께 저장한다.
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));

            String stored = Base64.encodeToString(iv, Base64.NO_WRAP)
                    + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP);
            prefs(context).edit().putString(PREF_KEY, stored).apply();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "토큰 암호화 실패: " + e.getClass().getSimpleName());
            return false;
        }
    }

    // 없거나 복호화할 수 없으면 null. 다른 기기로 백업을 복원하면 키가 없어 null이 된다.
    public static String load(Context context) {
        String stored = prefs(context).getString(PREF_KEY, null);
        if (stored == null) return null;

        String[] parts = stored.split(":", 2);
        if (parts.length != 2) return null;

        try {
            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);

            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_ALIAS)) return null;
            SecretKey key = (SecretKey) keyStore.getKey(KEY_ALIAS, null);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "토큰 복호화 실패: " + e.getClass().getSimpleName());
            return null;
        }
    }

    public static boolean hasToken(Context context) {
        return prefs(context).contains(PREF_KEY);
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
