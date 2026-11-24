package com.example.homeassistantapp.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlainSecretMigrationTest {
    /** 평문 prefs 흉내 */
    private static final class FakePlain implements PlainSecretMigration.Source {
        String value;
        boolean present;
        FakePlain(String value) { this.value = value; this.present = value != null; }
        @Override public boolean has() { return present; }
        @Override public String get() { return value; }
        @Override public void remove() { present = false; value = null; }
    }

    /** 암호화 저장소 흉내. ok=false면 저장 실패 */
    private static final class FakeTarget implements PlainSecretMigration.Target {
        final boolean ok;
        String saved;
        int calls;
        FakeTarget(boolean ok) { this.ok = ok; }
        @Override public boolean save(String value) {
            calls++;
            if (ok) saved = value;
            return ok;
        }
    }

    @Test
    public void plainValue_movedThenRemoved() {
        FakePlain plain = new FakePlain("pass 'word'");
        FakeTarget target = new FakeTarget(true);
        assertEquals(PlainSecretMigration.Outcome.MIGRATED, PlainSecretMigration.run(plain, target));
        assertEquals("pass 'word'", target.saved);
        assertFalse(plain.has());
    }

    @Test
    public void saveFails_plainKeptForRetry() {
        FakePlain plain = new FakePlain("secret");
        FakeTarget failing = new FakeTarget(false);
        assertEquals(PlainSecretMigration.Outcome.KEPT_FOR_RETRY, PlainSecretMigration.run(plain, failing));
        assertTrue(plain.has());
        assertEquals("secret", plain.get());
        // 다음 실행에 성공하면 옮겨진다.
        FakeTarget next = new FakeTarget(true);
        assertEquals(PlainSecretMigration.Outcome.MIGRATED, PlainSecretMigration.run(plain, next));
        assertEquals("secret", next.saved);
        assertFalse(plain.has());
    }

    @Test
    public void nothingStored_noSaveCall() {
        FakePlain plain = new FakePlain(null);
        FakeTarget target = new FakeTarget(true);
        assertEquals(PlainSecretMigration.Outcome.NOTHING_TO_DO, PlainSecretMigration.run(plain, target));
        assertEquals(0, target.calls);
        assertNull(target.saved);
    }

    @Test
    public void emptyPassword_alsoMoved() {
        FakePlain plain = new FakePlain("");
        FakeTarget target = new FakeTarget(true);
        assertEquals(PlainSecretMigration.Outcome.MIGRATED, PlainSecretMigration.run(plain, target));
        assertEquals("", target.saved);
        assertFalse(plain.has());
    }

    @Test
    public void secondRun_afterMigration_doesNothing() {
        FakePlain plain = new FakePlain("x");
        PlainSecretMigration.run(plain, new FakeTarget(true));
        FakeTarget again = new FakeTarget(true);
        assertEquals(PlainSecretMigration.Outcome.NOTHING_TO_DO, PlainSecretMigration.run(plain, again));
        assertEquals(0, again.calls);
    }
}
