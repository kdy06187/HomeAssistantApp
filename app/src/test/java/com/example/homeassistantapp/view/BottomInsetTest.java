package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BottomInsetTest {
    @Test
    public void tabShown_noKeyboard_nothing() {
        // 하단 탭이 제스처 바 여백을 스스로 가진다.
        assertEquals(0, BottomInset.of(true, 200, 48, 0));
    }

    @Test
    public void tabShown_keyboardAboveTab_onlyTheDifference() {
        assertEquals(700, BottomInset.of(true, 200, 48, 900));
        assertEquals(0, BottomInset.of(true, 200, 48, 150));
    }

    @Test
    public void fullScreen_gestureBarOrKeyboard_whicheverIsTaller() {
        assertEquals(48, BottomInset.of(false, 200, 48, 0));
        assertEquals(900, BottomInset.of(false, 200, 48, 900));
    }
}
