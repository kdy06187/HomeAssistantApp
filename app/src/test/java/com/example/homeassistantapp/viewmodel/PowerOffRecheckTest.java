package com.example.homeassistantapp.viewmodel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 불일치 #15: 성공한 OFF 뒤에만 상태를 다시 확인하고, 지연은 서버 전원 차단(45초)보다 길다.
 */
public class PowerOffRecheckTest {

    // 성공한 OFF는 다시 확인한다
    @Test
    public void successfulOff_rechecks() {
        assertTrue(PowerOffRecheck.shouldRecheck("OFF", true));
    }

    // ON이나 실패한 명령은 다시 확인하지 않는다
    @Test
    public void onOrFailed_doesNotRecheck() {
        assertFalse(PowerOffRecheck.shouldRecheck("ON", true));
        assertFalse(PowerOffRecheck.shouldRecheck("OFF", false));
        assertFalse(PowerOffRecheck.shouldRecheck(null, true));
    }

    // 지연은 계약서의 45초 전원 차단보다 길다
    @Test
    public void delay_isLongerThanServerPowerCut() {
        assertTrue(PowerOffRecheck.DELAY_MS > 45_000L);
    }
}
