package com.example.homeassistantapp.viewmodel;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PowerOffRecheckTest {

    @Test
    public void delay_isServerSecondsPlusFiveSeconds() {
        assertEquals(50_000L, PowerOffRecheck.delayMs(45));
        assertEquals(15_000L, PowerOffRecheck.delayMs(10));
    }

    @Test
    public void zeroDelay_stillWaitsMargin() {
        assertEquals(5_000L, PowerOffRecheck.delayMs(0));
    }

    @Test
    public void missingOrNegativeDelay_usesFallback() {
        assertEquals(50_000L, PowerOffRecheck.delayMs(null));
        assertEquals(50_000L, PowerOffRecheck.delayMs(-3));
    }
}
