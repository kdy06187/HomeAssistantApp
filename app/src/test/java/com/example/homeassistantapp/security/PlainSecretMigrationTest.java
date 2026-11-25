package com.example.homeassistantapp.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlainSecretMigrationTest {
    /** 예전 위치 흉내 */
    private static final class FakeLegacy implements PlainSecretMigration.Source {
        String value;
        boolean present;
        FakeLegacy(String value) { this.value = value; this.present = value != null; }
        @Override public boolean has() { return present; }
        @Override public String get() { return value; }
        @Override public void remove() { present = false; value = null; }
    }

    /** 새 저장소 흉내. ok=false면 저장 실패 */
    private static final class FakeTarget implements PlainSecretMigration.Target {
        final boolean ok;
        String saved;
        int calls;
        FakeTarget(boolean ok) { this.ok = ok; }
        FakeTarget(String existing) { this.ok = true; this.saved = existing; }
        @Override public boolean has() { return saved != null; }
        @Override public boolean save(String value) {
            calls++;
            if (ok) saved = value;
            return ok;
        }
    }

    @Test
    public void legacyValue_movedThenRemoved() {
        FakeLegacy legacy = new FakeLegacy("pass 'word'");
        FakeTarget target = new FakeTarget(true);
        assertEquals(PlainSecretMigration.Outcome.MIGRATED, PlainSecretMigration.run(legacy, target));
        assertEquals("pass 'word'", target.saved);
        assertFalse(legacy.has());
    }

    @Test
    public void saveFails_legacyKeptForRetry() {
        FakeLegacy legacy = new FakeLegacy("secret");
        FakeTarget failing = new FakeTarget(false);
        assertEquals(PlainSecretMigration.Outcome.KEPT_FOR_RETRY, PlainSecretMigration.run(legacy, failing));
        assertTrue(legacy.has());
        assertEquals("secret", legacy.get());
        FakeTarget next = new FakeTarget(true);
        assertEquals(PlainSecretMigration.Outcome.MIGRATED, PlainSecretMigration.run(legacy, next));
        assertEquals("secret", next.saved);
        assertFalse(legacy.has());
    }

    @Test
    public void nothingStored_noSaveCall() {
        FakeLegacy legacy = new FakeLegacy(null);
        FakeTarget target = new FakeTarget(true);
        assertEquals(PlainSecretMigration.Outcome.NOTHING_TO_DO, PlainSecretMigration.run(legacy, target));
        assertEquals(0, target.calls);
        assertNull(target.saved);
    }

    @Test
    public void emptyValue_discardedAsNone() {
        FakeLegacy legacy = new FakeLegacy("");
        FakeTarget target = new FakeTarget(true);
        assertEquals(PlainSecretMigration.Outcome.DISCARDED_EMPTY, PlainSecretMigration.run(legacy, target));
        assertEquals(0, target.calls);
        assertFalse(legacy.has());
    }

    @Test
    public void newStoreAlreadyHasValue_legacyDiscardedNotOverwritten() {
        // 평문 삭제가 실패해 남았어도, 이미 저장된 새 비밀번호를 옛 값으로 덮지 않는다.
        FakeLegacy legacy = new FakeLegacy("old");
        FakeTarget target = new FakeTarget("new-encrypted");
        assertEquals(PlainSecretMigration.Outcome.DISCARDED_STALE, PlainSecretMigration.run(legacy, target));
        assertEquals("new-encrypted", target.saved);
        assertEquals(0, target.calls);
        assertFalse(legacy.has());
    }

    @Test
    public void hasButNullValue_discarded() {
        FakeLegacy legacy = new FakeLegacy("x");
        legacy.value = null;
        assertEquals(PlainSecretMigration.Outcome.DISCARDED_EMPTY,
                PlainSecretMigration.run(legacy, new FakeTarget(true)));
    }

    @Test
    public void secondRun_afterMigration_doesNothing() {
        FakeLegacy legacy = new FakeLegacy("x");
        PlainSecretMigration.run(legacy, new FakeTarget(true));
        FakeTarget again = new FakeTarget(true);
        assertEquals(PlainSecretMigration.Outcome.NOTHING_TO_DO, PlainSecretMigration.run(legacy, again));
        assertEquals(0, again.calls);
    }
}
