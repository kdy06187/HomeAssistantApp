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
 * 서버 API 토큰을 암호화해서 저장한다 (계약서 §1.1, 불일치 #1).
 *
 * 왜 Android Keystore + AES-GCM을 직접 쓰는가:
 * - 암호화 키는 Keystore 안에서 만들어지고 밖으로 꺼낼 수 없다. 그래서 SharedPreferences 파일이
 *   통째로 복사되어도(백업, 루팅 등) 키가 없으면 토큰을 읽을 수 없다.
 * - AES-GCM은 암호화와 함께 위변조 검사(인증 태그)도 해 준다. 파일이 조작되면 복호화가 실패한다.
 * - androidx.security의 EncryptedSharedPreferences는 지원 중단(deprecated)되었고, Tink는 이 값 하나를
 *   위해 넣기에는 큰 의존성이다. minSdk 26이면 Keystore API를 그대로 쓸 수 있다.
 *
 * 저장 형식: "Base64(IV):Base64(암호문)" 을 HomeAssistantPrefs 의 API_TOKEN_ENC 키에 넣는다.
 * IV(초기화 벡터)는 비밀이 아니지만 암호화할 때마다 새로 만들어야 하므로 암호문 옆에 같이 둔다.
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

    /** 토큰을 암호화해서 저장한다. 성공하면 true. 실패 이유는 로그에 남기되 토큰 값은 절대 남기지 않는다. */
    public static boolean save(Context context, String token) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            // 왜 IV를 직접 넣지 않는가: Keystore 키는 같은 IV 재사용을 막기 위해 IV를 스스로 만들게 되어 있다.
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

    /**
     * 저장된 토큰을 복호화해서 돌려준다. 없거나 읽을 수 없으면 null.
     * 예: 다른 기기로 백업이 복원되면 암호문은 있어도 Keystore 키가 없어서 복호화가 실패한다.
     *     이때는 "토큰 없음"으로 보고 설정 화면에서 다시 넣게 한다.
     */
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

    /** 토큰이 저장되어 있는지만 확인한다(복호화하지 않음). 설정 화면에서 "저장됨" 표시용. */
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
