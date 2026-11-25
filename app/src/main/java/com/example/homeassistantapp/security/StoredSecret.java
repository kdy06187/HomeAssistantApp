package com.example.homeassistantapp.security;

/** 저장된 비밀값(토큰·Wi-Fi 비밀번호)을 읽은 결과. 화면은 이 상태 하나로 "없음/저장됨/다시 입력"을 정한다. */
public final class StoredSecret {
    public enum State {
        /** 저장한 적 없음(또는 빈 값) */
        NONE,
        OK,
        /** 저장돼 있지만 복호화할 수 없음(다른 기기로 복원, 키 손상 등): 다시 입력받아야 함 */
        UNREADABLE
    }

    public final State state;
    /** OK면 값, NONE이면 "", UNREADABLE이면 null */
    public final String value;

    private StoredSecret(State state, String value) {
        this.state = state;
        this.value = value;
    }

    public static StoredSecret of(boolean stored, String decrypted) {
        if (!stored) return new StoredSecret(State.NONE, "");
        if (decrypted == null) return new StoredSecret(State.UNREADABLE, null);
        if (decrypted.isEmpty()) return new StoredSecret(State.NONE, "");
        return new StoredSecret(State.OK, decrypted);
    }

    public boolean isUsable() {
        return state != State.UNREADABLE;
    }
}
