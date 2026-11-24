package com.example.homeassistantapp.viewmodel;

/** 기기 등록 한 번의 진행 상태. 바뀔 때마다 새 객체를 만든다. */
public final class CommissionStatus {
    public enum Phase {
        /** E3를 보냈고 아직 답이 없음 */
        SENDING,
        /** E8 PENDING */
        PENDING,
        /** E8 RUNNING */
        RUNNING,
        SUCCEEDED,
        /** E8 FAILED */
        FAILED,
        /** E3가 받아들여지지 않음(400·409·502·통신 실패 등). 입력을 고쳐 다시 보낼 수 있다 */
        REJECTED,
        /** 작업을 잃었거나 상한 초과: 목록으로 확인해야 함 */
        UNCONFIRMED
    }

    public final Phase phase;
    public final String deviceName;
    /** E3 202를 받은 시각(SystemClock.elapsedRealtime). 그 전에는 0 */
    public final long startedAtMs;
    public final boolean restored;
    /** 서버 오류 code(E8 error.code 또는 E3 오류 본문). 통신 실패면 null */
    public final String errorCode;
    public final String serverMessage;
    /** E3 거절일 때의 HTTP 코드. 통신 실패면 0 */
    public final int httpCode;

    private CommissionStatus(Phase phase, String deviceName, long startedAtMs, boolean restored,
                             String errorCode, String serverMessage, int httpCode) {
        this.phase = phase;
        this.deviceName = deviceName;
        this.startedAtMs = startedAtMs;
        this.restored = restored;
        this.errorCode = errorCode;
        this.serverMessage = serverMessage;
        this.httpCode = httpCode;
    }

    public static CommissionStatus sending(String deviceName) {
        return new CommissionStatus(Phase.SENDING, deviceName, 0, false, null, null, 0);
    }

    public static CommissionStatus rejected(String deviceName, int httpCode, String errorCode, String serverMessage) {
        return new CommissionStatus(Phase.REJECTED, deviceName, 0, false, errorCode, serverMessage, httpCode);
    }

    public CommissionStatus accepted(long startedAtMs) {
        return new CommissionStatus(Phase.PENDING, deviceName, startedAtMs, false, null, null, 0);
    }

    /** E8 state 값으로 진행 단계만 바꾼다. 모르는 값이면 그대로. */
    public CommissionStatus withServerState(String state) {
        Phase next = "RUNNING".equals(state) ? Phase.RUNNING : "PENDING".equals(state) ? Phase.PENDING : phase;
        if (next == phase) return this;
        return new CommissionStatus(next, deviceName, startedAtMs, false, null, null, 0);
    }

    public CommissionStatus succeeded(boolean restored) {
        return new CommissionStatus(Phase.SUCCEEDED, deviceName, startedAtMs, restored, null, null, 0);
    }

    public CommissionStatus failed(String errorCode, String serverMessage) {
        return new CommissionStatus(Phase.FAILED, deviceName, startedAtMs, false, errorCode, serverMessage, 0);
    }

    public CommissionStatus unconfirmed() {
        return new CommissionStatus(Phase.UNCONFIRMED, deviceName, startedAtMs, false, null, null, 0);
    }

    public boolean isActive() {
        return phase == Phase.SENDING || phase == Phase.PENDING || phase == Phase.RUNNING;
    }
}
