package com.example.homeassistantapp.viewmodel;

/**
 * 202(지연 수락)를 받으면 서버가 알려 준 delaySeconds 뒤에 상태를 한 번 다시 확인한다.
 * 실행 직후 기기 응답이 늦을 수 있어 여유를 더한다.
 */
public final class PowerOffRecheck {
    static final long MARGIN_MS = 5_000L;
    // delaySeconds가 없거나 잘못 오면, 지금 알려진 가장 긴 지연(PC 드라이버 45초)을 쓴다.
    static final int FALLBACK_DELAY_SECONDS = 45;

    private PowerOffRecheck() {}

    public static long delayMs(Integer delaySeconds) {
        int seconds = delaySeconds == null || delaySeconds < 0 ? FALLBACK_DELAY_SECONDS : delaySeconds;
        return seconds * 1000L + MARGIN_MS;
    }
}
