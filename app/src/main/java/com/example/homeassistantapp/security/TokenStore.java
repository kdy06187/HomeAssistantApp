package com.example.homeassistantapp.security;

import android.content.Context;

/** API 토큰 보관. 암호화는 SecretStore가 하고, 저장 위치·키 이름은 예전과 같아 기존 토큰을 그대로 읽는다. */
public final class TokenStore {
    private static final SecretStore STORE =
            new SecretStore("HomeAssistantPrefs", "API_TOKEN_ENC", "home_assistant_api_token_key");

    private TokenStore() {}

    /** 성공하면 true. 토큰 값은 로그에 남기지 않는다. */
    public static boolean save(Context context, String token) {
        return STORE.save(context, token);
    }

    /** 없거나 복호화할 수 없으면 null. */
    public static String load(Context context) {
        return STORE.load(context);
    }

    public static boolean hasToken(Context context) {
        return STORE.has(context);
    }
}
