package com.example.homeassistantapp.view;

import com.example.homeassistantapp.dto.Device;

import java.util.Locale;

/** 홈 타일 하나에 보일 값. Android 없이 기기 값만으로 정해 단위 테스트한다. */
public final class HomeTile {
    public static final String NO_VALUE = "–";

    public final String name;
    public final boolean pairing;
    public final String chipText;
    public final DeviceStateLabel.Tone tone;
    public final boolean switchVisible;
    public final boolean switchChecked;
    public final boolean switchEnabled;
    public final boolean showsRefresh;
    public final boolean showsPower;
    public final String powerText;
    public final String metaText;
    public final boolean opensDetail;

    private HomeTile(Device device) {
        name = device.getName();
        pairing = DeviceOrder.isPairing(device);
        DeviceStateLabel.Kind kind = DeviceStateLabel.kindOf(device.getState());
        boolean matter = "MATTER".equals(device.getType());

        if (pairing) {
            chipText = "등록 중…";
            tone = DeviceStateLabel.Tone.OFF;
            switchVisible = false;
            switchChecked = false;
            switchEnabled = false;
            showsRefresh = false;
            showsPower = false;
            powerText = NO_VALUE;
            metaText = "";
            opensDetail = false;
            return;
        }

        chipText = kind.text;
        tone = kind.tone;
        switchVisible = true;
        switchChecked = kind == DeviceStateLabel.Kind.ON;
        switchEnabled = kind.canToggle;
        showsRefresh = false;
        showsPower = matter;
        powerText = matter && kind == DeviceStateLabel.Kind.ON ? watts(device.getActivePowerW()) : NO_VALUE;
        if (kind == DeviceStateLabel.Kind.OFFLINE) {
            metaText = "기기 응답 없음";
        } else if (matter) {
            metaText = "누적 " + kwh(device.getTotalEnergyKwh());
        } else {
            metaText = "";
        }
        opensDetail = true;
    }

    public static HomeTile of(Device device) {
        return new HomeTile(device);
    }

    static String watts(Double value) {
        return value == null ? NO_VALUE : String.format(Locale.ROOT, "%.1f W", value);
    }

    static String kwh(Double value) {
        return value == null ? NO_VALUE : String.format(Locale.ROOT, "%.2f kWh", value);
    }
}
