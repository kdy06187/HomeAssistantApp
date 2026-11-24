package com.example.homeassistantapp.viewmodel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 홈 타일 추이선 요청을 줄이는 판단. 받은 값은 10분 동안 다시 묻지 않고,
 * 실패하면 1분 뒤에야 다시 묻는다(홈 목록은 자주 다시 그려지므로).
 */
public final class TrendCache {
    public static final long FRESH_MS = 10 * 60_000L;
    public static final long RETRY_MS = 60_000L;

    private final Map<String, Long> succeededAt = new HashMap<>();
    private final Map<String, Long> failedAt = new HashMap<>();
    private final Set<String> inFlight = new HashSet<>();

    public boolean needsFetch(String deviceId, long now) {
        if (inFlight.contains(deviceId)) return false;
        Long ok = succeededAt.get(deviceId);
        if (ok != null && now - ok < FRESH_MS) return false;
        Long failed = failedAt.get(deviceId);
        return failed == null || now - failed >= RETRY_MS;
    }

    public void started(String deviceId) {
        inFlight.add(deviceId);
    }

    public void succeeded(String deviceId, long now) {
        inFlight.remove(deviceId);
        failedAt.remove(deviceId);
        succeededAt.put(deviceId, now);
    }

    public void failed(String deviceId, long now) {
        inFlight.remove(deviceId);
        failedAt.put(deviceId, now);
    }

    /** 기기를 지우면 기록도 잊는다(같은 ID로 다시 등록하면 새로 묻게). */
    public void forget(String deviceId) {
        inFlight.remove(deviceId);
        succeededAt.remove(deviceId);
        failedAt.remove(deviceId);
    }
}
