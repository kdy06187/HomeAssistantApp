package com.example.homeassistantapp.view.detail;

import android.view.View;
import android.widget.TextView;

import com.example.homeassistantapp.R;

/** 기기 정보 카드(종류·ID·IP). */
public final class InfoCard {
    private final TextView type, id, ip;

    public InfoCard(View root) {
        type = root.findViewById(R.id.tvInfoType);
        id = root.findViewById(R.id.tvInfoId);
        ip = root.findViewById(R.id.tvInfoIp);
    }

    public void bind(DeviceInfo info) {
        type.setText(info.typeLine);
        id.setText(info.idLine);
        ip.setVisibility(info.ipLine == null ? View.GONE : View.VISIBLE);
        ip.setText(info.ipLine);
    }
}
