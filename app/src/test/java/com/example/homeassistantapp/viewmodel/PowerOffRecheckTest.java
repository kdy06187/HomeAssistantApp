package com.example.homeassistantapp.viewmodel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PowerOffRecheckTest {

    @Test
    public void successfulOff_rechecks() {
        assertTrue(PowerOffRecheck.shouldRecheck("OFF", true));
    }

    @Test
    public void onOrFailed_doesNotRecheck() {
        assertFalse(PowerOffRecheck.shouldRecheck("ON", true));
        assertFalse(PowerOffRecheck.shouldRecheck("OFF", false));
        assertFalse(PowerOffRecheck.shouldRecheck(null, true));
    }

    @Test
    public void delay_isLongerThanServerPowerCut() {
        assertTrue(PowerOffRecheck.DELAY_MS > 45_000L);
    }
}
