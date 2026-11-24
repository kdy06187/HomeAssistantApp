package com.example.homeassistantapp.security;

/** 저장된 Wi-Fi 비밀번호를 읽은 결과. */
public final class WifiPassword {
    public enum State {
        /** 저장한 적 없음: 비밀번호 없는 Wi-Fi로 본다(빈 값) */
        NONE,
        OK,
        /** 저장돼 있지만 복호화할 수 없음(다른 기기로 복원 등): 다시 입력받아야 함 */
        UNREADABLE
    }

    public final State state;
    /** OK면 비밀번호, NONE이면 "", UNREADABLE이면 null */
    public final String value;

    private WifiPassword(State state, String value) {
        this.state = state;
        this.value = value;
    }

    public static WifiPassword of(boolean stored, String decrypted) {
        if (!stored) return new WifiPassword(State.NONE, "");
        if (decrypted == null) return new WifiPassword(State.UNREADABLE, null);
        return new WifiPassword(State.OK, decrypted);
    }

    public boolean isUsable() {
        return state != State.UNREADABLE;
    }
}
