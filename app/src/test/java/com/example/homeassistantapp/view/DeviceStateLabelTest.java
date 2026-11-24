package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.view.DeviceStateLabel.Kind;
import com.example.homeassistantapp.view.DeviceStateLabel.Tone;

import org.junit.Test;

public class DeviceStateLabelTest {

    @Test
    public void on_isOnAndToggleable() {
        assertEquals("켜짐", DeviceStateLabel.textOf("ON"));
        assertEquals(Tone.ON, DeviceStateLabel.toneOf("ON"));
        assertTrue(DeviceStateLabel.canToggle("ON"));
    }

    @Test
    public void off_isOffAndToggleable() {
        assertEquals("꺼짐", DeviceStateLabel.textOf("OFF"));
        assertEquals(Tone.OFF, DeviceStateLabel.toneOf("OFF"));
        assertTrue(DeviceStateLabel.canToggle("OFF"));
    }

    @Test
    public void offline_isOfflineAndLocked() {
        assertEquals("오프라인", DeviceStateLabel.textOf("OFFLINE"));
        assertEquals(Tone.OFFLINE, DeviceStateLabel.toneOf("OFFLINE"));
        assertFalse(DeviceStateLabel.canToggle("OFFLINE"));
    }

    @Test
    public void unknown_isShownRightAwayInWarningTone() {
        assertEquals("알 수 없음", DeviceStateLabel.textOf("UNKNOWN"));
        assertEquals(Tone.WARNING, DeviceStateLabel.toneOf("UNKNOWN"));
        assertFalse(DeviceStateLabel.canToggle("UNKNOWN"));
    }

    @Test
    public void unexpectedValues_areUnknown() {
        assertEquals(Kind.UNKNOWN, DeviceStateLabel.kindOf("STANDBY"));
        assertEquals(Kind.UNKNOWN, DeviceStateLabel.kindOf(""));
        assertEquals(Kind.UNKNOWN, DeviceStateLabel.kindOf(null));
        // 서버 값은 대문자로 온다. 소문자는 약속 밖이라 알 수 없음으로 둔다.
        assertEquals(Kind.UNKNOWN, DeviceStateLabel.kindOf("on"));
        assertFalse(DeviceStateLabel.canToggle(null));
    }

    @Test
    public void loading_isCheckingAndLocked() {
        assertEquals("확인 중…", DeviceStateLabel.textOf("LOADING"));
        assertEquals(Tone.WARNING, DeviceStateLabel.toneOf("LOADING"));
        assertFalse(DeviceStateLabel.canToggle("LOADING"));
    }

    @Test
    public void tones_pointToMatchingColorTokens() {
        assertEquals(R.color.state_on_bg, Tone.ON.background);
        assertEquals(R.color.state_on_fg, Tone.ON.foreground);
        assertEquals(R.color.state_off_bg, Tone.OFF.background);
        assertEquals(R.color.state_off_fg, Tone.OFF.foreground);
        assertEquals(R.color.state_offline_bg, Tone.OFFLINE.background);
        assertEquals(R.color.state_offline_fg, Tone.OFFLINE.foreground);
        assertEquals(R.color.state_unknown_bg, Tone.WARNING.background);
        assertEquals(R.color.state_unknown_fg, Tone.WARNING.foreground);
    }

    @Test
    public void everyKind_hasTextAndTone() {
        for (Kind kind : Kind.values()) {
            assertFalse(kind.name(), kind.text.isEmpty());
            assertTrue(kind.name(), kind.tone != null);
        }
    }
}
