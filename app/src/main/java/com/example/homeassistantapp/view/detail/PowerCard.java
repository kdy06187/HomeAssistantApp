package com.example.homeassistantapp.view.detail;

import android.view.View;
import android.widget.TextView;

import com.example.homeassistantapp.R;

/** 전력 카드(현재 W·누적·마지막 갱신)와 그 위의 실패 배너. 값만 바꾸고 카드를 흐리게 하지 않는다. */
public final class PowerCard {
    private final View root, banner;
    private final TextView power, total, updated, bannerText;

    public PowerCard(View root, Runnable onRetry) {
        this.root = root;
        banner = root.findViewById(R.id.powerBanner);
        bannerText = root.findViewById(R.id.tvPowerBanner);
        power = root.findViewById(R.id.tvDetailPower);
        total = root.findViewById(R.id.tvDetailTotal);
        updated = root.findViewById(R.id.tvDetailUpdated);
        root.findViewById(R.id.btnPowerRetry).setOnClickListener(v -> onRetry.run());
    }

    public void setVisible(boolean visible) {
        root.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void bind(PowerPanel panel) {
        power.setText(panel.powerText);
        total.setText(panel.totalText);
        updated.setText(panel.updatedText);
        banner.setVisibility(panel.bannerText == null ? View.GONE : View.VISIBLE);
        bannerText.setText(panel.bannerText);
    }
}
