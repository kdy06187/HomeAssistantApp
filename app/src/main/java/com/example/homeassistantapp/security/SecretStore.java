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
 * 비밀값 하나를 Android Keystore 키(AES-GCM)로 암호화해 prefs 한 칸에 저장한다. 키는 기기 밖으로 꺼낼 수 없다.
 * 저장 형식: "Base64(IV):Base64(암호문)". 값은 로그에 남기지 않는다.
 */
public final class SecretStore {
    private static final String TAG = "SecretStore";
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;

    private final String prefsName;
    private final String prefKey;
    private final String keyAlias;

    public SecretStore(String prefsName, String prefKey, String keyAlias) {
        this.prefsName = prefsName;
        this.prefKey = prefKey;
        this.keyAlias = keyAlias;
    }

    /**
     * 디스크에 쓴 것까지 확인되면 true(옮기기 뒤 원본을 지워도 되는지 판단에 쓴다).
     * 키가 망가져 암호화가 실패하면 키를 지우고 한 번 다시 만들어 시도한다(이 칸의 옛 암호문은 어차피 못 푼다).
     */
    public boolean save(Context context, String value) {
        if (encryptAndSave(context, value)) return true;
        deleteKey();
        return encryptAndSave(context, value);
    }

    private boolean encryptAndSave(Context context, String value) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            // Keystore 키는 IV를 직접 지정할 수 없어 cipher가 만든 IV를 함께 저장한다.
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            String stored = Base64.encodeToString(iv, Base64.NO_WRAP)
                    + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP);
            return prefs(context).edit().putString(prefKey, stored).commit();
        } catch (Exception e) {
            Log.e(TAG, "암호화 실패(" + prefKey + "): " + e.getClass().getSimpleName());
            return false;
        }
    }

    // 없거나 복호화할 수 없으면 null. 다른 기기로 백업을 복원하면 키가 없어 null이 된다.
    public String load(Context context) {
        String stored = prefs(context).getString(prefKey, null);
        if (stored == null) return null;

        String[] parts = stored.split(":", 2);
        if (parts.length != 2) return null;

        try {
            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);

            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(keyAlias)) return null;
            SecretKey key = (SecretKey) keyStore.getKey(keyAlias, null);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "복호화 실패(" + prefKey + "): " + e.getClass().getSimpleName());
            return null;
        }
    }

    public boolean has(Context context) {
        return prefs(context).contains(prefKey);
    }

    /** 암호문 그대로(저장 위치를 옮길 때만 쓴다. 같은 키 별칭이면 그대로 풀린다). */
    public String loadRaw(Context context) {
        return prefs(context).getString(prefKey, null);
    }

    public boolean saveRaw(Context context, String stored) {
        return prefs(context).edit().putString(prefKey, stored).commit();
    }

    public boolean remove(Context context) {
        return prefs(context).edit().remove(prefKey).commit();
    }

    private void deleteKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);
            if (keyStore.containsAlias(keyAlias)) keyStore.deleteEntry(keyAlias);
        } catch (Exception e) {
            Log.e(TAG, "키 삭제 실패(" + prefKey + "): " + e.getClass().getSimpleName());
        }
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        keyStore.load(null);
        if (keyStore.containsAlias(keyAlias)) {
            return (SecretKey) keyStore.getKey(keyAlias, null);
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(keyAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    private SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
    }
}
