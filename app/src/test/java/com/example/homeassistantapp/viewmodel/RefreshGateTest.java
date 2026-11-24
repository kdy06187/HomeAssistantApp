package com.example.homeassistantapp.viewmodel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RefreshGateTest {

    @Test
    public void manyNotFoundAtOnce_claimOnlyOnce() {
        RefreshGate gate = new RefreshGate();
        assertTrue(gate.tryClaim());
        assertFalse(gate.tryClaim());
        assertFalse(gate.tryClaim());
    }

    @Test
    public void afterRelease_canClaimAgain() {
        RefreshGate gate = new RefreshGate();
        assertTrue(gate.tryClaim());
        gate.release();
        assertTrue(gate.tryClaim());
    }
}
