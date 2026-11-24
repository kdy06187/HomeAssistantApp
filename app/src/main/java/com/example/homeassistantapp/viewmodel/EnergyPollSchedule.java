package com.example.homeassistantapp.viewmodel;

/**
 * 상세 화면 전력(E6) 주기 조회의 시간 계산. 화면이 보이는 동안만 돌고,
 * 요청이 끝난 뒤에 다음 요청을 예약하므로 응답이 느려도 요청이 겹치지 않는다.
 */
public final class EnergyPollSchedule {
    public static final long INTERVAL_MS = 15_000L;
    /** 지금은 예약하지 않는다(진행 중인 요청이 끝나면 예약하거나, 화면이 안 보임). */
    public static final long NONE = -1L;

    private boolean visible;
    private boolean inFlight;
    private long lastFinishedAt = -1L;

    /** 화면이 보이기 시작함. 첫 요청까지 기다릴 시간(ms) 또는 NONE. */
    public long start(long now) {
        visible = true;
        if (inFlight) return NONE;
        if (lastFinishedAt < 0) return 0L;
        // 회전처럼 잠깐 가렸다 다시 보이면 남은 시간만 기다린다.
        return Math.max(0L, INTERVAL_MS - (now - lastFinishedAt));
    }

    public void stop() {
        visible = false;
    }

    public boolean canFetchNow() {
        return visible && !inFlight;
    }

    public void requestStarted() {
        inFlight = true;
    }

    /** 응답(성공·실패)을 받음. 다음 요청까지 기다릴 시간 또는 NONE. */
    public long finished(long now) {
        inFlight = false;
        lastFinishedAt = now;
        return visible ? INTERVAL_MS : NONE;
    }

    /** 다른 기기 상세로 바뀜. 진행 중인 이전 요청의 결과는 호출하는 쪽이 버린다. */
    public void reset() {
        inFlight = false;
        lastFinishedAt = -1L;
    }
}
