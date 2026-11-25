package com.example.homeassistantapp.security;

/**
 * 예전 위치에 있던 비밀값(평문 Wi-Fi 비밀번호, 옛 prefs의 토큰 암호문)을 새 저장소로 옮긴다.
 * 새 저장이 확실히 된 뒤에만 예전 값을 지우고, 실패하면 남겨 다음 실행에 다시 시도한다.
 * 새 저장소에 이미 값이 있으면 그쪽이 최신이므로 옮기지 않고 예전 값만 지운다.
 */
public final class PlainSecretMigration {
    public interface Source {
        boolean has();
        String get();
        void remove();
    }

    public interface Target {
        boolean has();

        /** 디스크에 쓴 것까지 확인되면 true */
        boolean save(String value);
    }

    public enum Outcome {
        NOTHING_TO_DO,
        MIGRATED,
        /** 새 저장소가 이미 있어 예전 값만 지움 */
        DISCARDED_STALE,
        /** 예전 값이 비어 있어 옮기지 않고 지움("없음"으로 본다) */
        DISCARDED_EMPTY,
        KEPT_FOR_RETRY
    }

    private PlainSecretMigration() {}

    public static Outcome run(Source legacy, Target target) {
        if (!legacy.has()) return Outcome.NOTHING_TO_DO;
        if (target.has()) {
            legacy.remove();
            return Outcome.DISCARDED_STALE;
        }
        String value = legacy.get();
        if (value == null || value.isEmpty()) {
            legacy.remove();
            return Outcome.DISCARDED_EMPTY;
        }
        if (!target.save(value)) return Outcome.KEPT_FOR_RETRY;
        legacy.remove();
        return Outcome.MIGRATED;
    }
}
