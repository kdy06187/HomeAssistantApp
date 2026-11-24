package com.example.homeassistantapp.view;

import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

/** 상태 칩(bg_state_chip 배경의 TextView)을 상태 색쌍으로 칠한다. */
public final class StateChip {
    private StateChip() {}

    public static void apply(TextView chip, String text, DeviceStateLabel.Tone tone) {
        chip.setText(text);
        chip.setTextColor(ContextCompat.getColor(chip.getContext(), tone.foreground));
        ViewCompat.setBackgroundTintList(chip,
                ColorStateList.valueOf(ContextCompat.getColor(chip.getContext(), tone.background)));
    }
}
