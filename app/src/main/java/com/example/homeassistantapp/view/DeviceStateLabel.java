package com.example.homeassistantapp.view;

/**
 * 기기 상태 문자열을 화면에 보여 줄 말로 바꾼다 (계약서 §1.2, 불일치 #8).
 *
 * 왜 따로 뺐는가: 예전에는 LOADING·OFFLINE이 아니면 전부 "연결됨"으로 표시해서
 * UNKNOWN이나 처음 보는 값도 정상처럼 보였다. 판정을 화면 코드에서 떼어 내면 단위 테스트로 확인할 수 있다.
 */
public final class DeviceStateLabel {
    public enum Kind { CONNECTED, OFFLINE, UNKNOWN }

    public static final String TEXT_CONNECTED = "연결됨";
    public static final String TEXT_OFFLINE = "오프라인";
    public static final String TEXT_UNKNOWN = "알 수 없음";

    private DeviceStateLabel() {}

    /** ON/OFF만 "연결됨"이다. 기기가 응답했다는 뜻이기 때문이다. 그 밖의 값(null 포함)은 "알 수 없음". */
    public static Kind kindOf(String state) {
        if ("ON".equals(state) || "OFF".equals(state)) return Kind.CONNECTED;
        if ("OFFLINE".equals(state)) return Kind.OFFLINE;
        return Kind.UNKNOWN;
    }

    public static String textOf(String state) {
        switch (kindOf(state)) {
            case CONNECTED: return TEXT_CONNECTED;
            case OFFLINE: return TEXT_OFFLINE;
            default: return TEXT_UNKNOWN;
        }
    }
}
