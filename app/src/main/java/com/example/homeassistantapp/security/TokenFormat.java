package com.example.homeassistantapp.security;

/** API 토큰 입력값 정리와 형식 검사. HTTP 헤더에 넣을 수 없는 문자가 저장되지 않게 한다. */
public final class TokenFormat {
    private TokenFormat() {}

    /** 앞뒤 공백·줄바꿈을 지운다. null이면 빈 문자열. */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.trim();
    }

    /** 공백을 뺀 인쇄 가능한 ASCII(0x21~0x7E)로만 이루어져 있으면 true. 빈 값은 false. */
    public static boolean isValid(String token) {
        if (token == null || token.isEmpty()) return false;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c < 0x21 || c > 0x7E) return false;
        }
        return true;
    }
}
