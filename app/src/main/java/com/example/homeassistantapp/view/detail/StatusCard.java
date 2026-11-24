package com.example.homeassistantapp.view.detail;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.view.HomeTile;
import com.example.homeassistantapp.view.StateChip;
import com.google.android.material.materialswitch.MaterialSwitch;

/** 상태·제어 카드. 표시 값은 홈 타일과 같은 HomeTile 규칙을 쓴다(켜는 중 잠금 포함). */
public final class StatusCard {

    public interface Listener {
        void onToggle(boolean turnOn);
        void onRefresh();
    }

    private final TextView chip;
    private final MaterialSwitch toggle;
    private final ImageButton refresh;

    public StatusCard(View root, Listener listener) {
        chip = root.findViewById(R.id.tvDetailState);
        toggle = root.findViewById(R.id.switchDetail);
        refresh = root.findViewById(R.id.btnDetailRefresh);
        // isPressed로 사용자 조작만 받으므로 bind의 setChecked가 명령을 다시 보내지 않는다.
        toggle.setOnCheckedChangeListener((button, isChecked) -> {
            if (button.isPressed()) listener.onToggle(isChecked);
        });
        refresh.setOnClickListener(v -> listener.onRefresh());
    }

    public void bind(HomeTile tile) {
        StateChip.apply(chip, tile.chipText, tile.tone);
        toggle.setChecked(tile.switchChecked);
        toggle.setEnabled(tile.switchEnabled);
        toggle.setContentDescription(tile.name + " 켜기");
        refresh.setVisibility(tile.showsRefresh ? View.VISIBLE : View.GONE);
        refresh.setContentDescription(tile.name + " 상태 새로고침");
    }
}
