package com.example.homeassistantapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class UnauthorizedNoticeTest {

    @Test
    public void later401s_doNotNotifyAgain() {
        UnauthorizedNotice notice = new UnauthorizedNotice();
        assertTrue(notice.tryClaim());  // 목록 조회 401
        assertFalse(notice.tryClaim()); // 조금 뒤 상태 조회 401
        assertFalse(notice.tryClaim());
    }

    @Test
    public void afterSavingSettings_notifiesAgain() {
        UnauthorizedNotice notice = new UnauthorizedNotice();
        notice.tryClaim();
        notice.reset();
        assertTrue(notice.tryClaim());
        assertFalse(notice.tryClaim());
    }

    @Test
    public void simultaneous401s_notifyOnlyOnce() throws Exception {
        UnauthorizedNotice notice = new UnauthorizedNotice();
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger claimed = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            pool.execute(() -> {
                try {
                    start.await();
                    if (notice.tryClaim()) claimed.incrementAndGet();
                } catch (InterruptedException ignored) {
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(1, claimed.get());
    }
}
