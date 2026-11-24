package com.example.homeassistantapp.view;

import androidx.annotation.ColorRes;

import com.example.homeassistantapp.R;

/** 기기 상태 하나로 화면 문구·칩 색·스위치 가능 여부를 정한다. 모르는 값(null 포함)은 "알 수 없음". */
public final class DeviceStateLabel {

    /** 상태 칩 색쌍. 라이트·다크 값은 colors.xml이 고른다. */
    public enum Tone {
        ON(R.color.state_on_bg, R.color.state_on_fg),
        OFF(R.color.state_off_bg, R.color.state_off_fg),
        OFFLINE(R.color.state_offline_bg, R.color.state_offline_fg),
        WARNING(R.color.state_unknown_bg, R.color.state_unknown_fg);

        @ColorRes public final int background;
        @ColorRes public final int foreground;

        Tone(@ColorRes int background, @ColorRes int foreground) {
            this.background = background;
            this.foreground = foreground;
        }
    }

    public enum Kind {
        ON("켜짐", Tone.ON, true),
        OFF("꺼짐", Tone.OFF, true),
        OFFLINE("오프라인", Tone.OFFLINE, false),
        UNKNOWN("알 수 없음", Tone.WARNING, false),
        // 요청 중에만 쓰는 클라이언트 값. 응답이 오면 덮어씌워진다.
        LOADING("확인 중…", Tone.WARNING, false);

        public final String text;
        public final Tone tone;
        public final boolean canToggle;

        Kind(String text, Tone tone, boolean canToggle) {
            this.text = text;
            this.tone = tone;
            this.canToggle = canToggle;
        }
    }

    private DeviceStateLabel() {}

    public static Kind kindOf(String state) {
        if (state == null) return Kind.UNKNOWN;
        switch (state) {
            case "ON": return Kind.ON;
            case "OFF": return Kind.OFF;
            case "OFFLINE": return Kind.OFFLINE;
            case "LOADING": return Kind.LOADING;
            default: return Kind.UNKNOWN;
        }
    }

    public static String textOf(String state) {
        return kindOf(state).text;
    }

    public static Tone toneOf(String state) {
        return kindOf(state).tone;
    }

    public static boolean canToggle(String state) {
        return kindOf(state).canToggle;
    }
}
