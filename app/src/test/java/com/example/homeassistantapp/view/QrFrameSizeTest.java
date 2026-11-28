package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QrFrameSizeTest {
    @Test
    public void portrait_plentyOfSpace_keepsDefault() {
        assertEquals(720, QrFrameSize.of(720, 1080, 1500, 48));
    }

    @Test
    public void landscape_shortGap_shrinksToFit() {
        // 세로 빈 곳 500px에서 위아래 여백 48px씩 빼면 404px
        assertEquals(404, QrFrameSize.of(720, 2000, 500, 48));
    }

    @Test
    public void narrowWidth_limitsToo() {
        assertEquals(504, QrFrameSize.of(720, 600, 1500, 48));
    }

    @Test
    public void noRoom_zeroNotNegative() {
        assertEquals(0, QrFrameSize.of(720, 2000, 60, 48));
    }
}
