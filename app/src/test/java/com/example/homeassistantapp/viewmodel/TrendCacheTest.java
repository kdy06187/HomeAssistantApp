package com.example.homeassistantapp.viewmodel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TrendCacheTest {

    @Test
    public void firstTime_fetches() {
        assertTrue(new TrendCache().needsFetch("SN1", 0L));
    }

    @Test
    public void inFlight_isNotRequestedAgain() {
        TrendCache c = new TrendCache();
        c.started("SN1");
        assertFalse(c.needsFetch("SN1", 1L));
        assertTrue(c.needsFetch("SN2", 1L));
    }

    @Test
    public void success_isFreshForTenMinutes() {
        TrendCache c = new TrendCache();
        c.started("SN1");
        c.succeeded("SN1", 1_000L);
        assertFalse(c.needsFetch("SN1", 1_000L + TrendCache.FRESH_MS - 1));
        assertTrue(c.needsFetch("SN1", 1_000L + TrendCache.FRESH_MS));
    }

    @Test
    public void failure_retriesAfterOneMinute() {
        TrendCache c = new TrendCache();
        c.started("SN1");
        c.failed("SN1", 0L);
        assertFalse(c.needsFetch("SN1", TrendCache.RETRY_MS - 1));
        assertTrue(c.needsFetch("SN1", TrendCache.RETRY_MS));
    }

    @Test
    public void forget_fetchesAgain() {
        TrendCache c = new TrendCache();
        c.started("SN1");
        c.succeeded("SN1", 0L);
        c.forget("SN1");
        assertTrue(c.needsFetch("SN1", 1L));
    }
}
