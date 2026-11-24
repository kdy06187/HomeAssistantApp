package com.example.homeassistantapp.security;

/**
 * 평문으로 저장돼 있던 비밀값을 암호화 저장소로 옮긴다. 암호화 저장이 확실히 된 뒤에만 평문을 지우고,
 * 실패하면 평문을 남겨 다음 실행에 다시 시도한다(옮기다 값을 잃지 않게).
 */
public final class PlainSecretMigration {
    public interface Source {
        boolean has();
        String get();
        void remove();
    }

    public interface Target {
        /** 디스크에 쓴 것까지 확인되면 true */
        boolean save(String value);
    }

    public enum Outcome { NOTHING_TO_DO, MIGRATED, KEPT_FOR_RETRY }

    private PlainSecretMigration() {}

    public static Outcome run(Source plain, Target encrypted) {
        if (!plain.has()) return Outcome.NOTHING_TO_DO;
        String value = plain.get();
        if (value == null) value = "";
        if (!encrypted.save(value)) return Outcome.KEPT_FOR_RETRY;
        plain.remove();
        return Outcome.MIGRATED;
    }
}
