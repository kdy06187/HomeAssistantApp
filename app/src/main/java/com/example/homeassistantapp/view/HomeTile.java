package com.example.homeassistantapp.view;

import com.example.homeassistantapp.dto.Device;

import java.util.List;
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
    /** 1칸 또는 2칸(한 줄 전체). */
    public final int span;
    /** 추이선 자리를 보일지. 선 값은 따로 받는다. */
    public final boolean showsTrend;

    private HomeTile(Device device, TileConfig config) {
        name = device.getName();
        TileConfig shown = config.effectiveFor(device.getType());
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
            span = 1;
            showsTrend = false;
            return;
        }

        String pending = device.getPendingCommand();
        if (pending != null) {
            // 응답이 올 때까지 사용자가 고른 위치에 두고 잠근다(되돌아갔다 다시 바뀌는 깜빡임 방지).
            chipText = "ON".equals(pending) ? "켜는 중…" : "끄는 중…";
            tone = DeviceStateLabel.Tone.WARNING;
            switchChecked = "ON".equals(pending);
            switchEnabled = false;
        } else {
            chipText = kind.text;
            tone = kind.tone;
            switchChecked = kind == DeviceStateLabel.Kind.ON;
            switchEnabled = kind.canToggle;
        }
        switchVisible = true;
        showsRefresh = pending == null && kind == DeviceStateLabel.Kind.UNKNOWN;
        showsPower = matter && shown.power;
        powerText = matter && kind == DeviceStateLabel.Kind.ON ? watts(device.getActivePowerW()) : NO_VALUE;
        if (kind == DeviceStateLabel.Kind.OFFLINE) {
            metaText = "기기 응답 없음";
        } else if (matter && shown.total) {
            metaText = "누적 " + kwh(device.getTotalEnergyKwh());
        } else {
            metaText = "";
        }
        opensDetail = true;
        span = shown.span;
        showsTrend = matter && shown.trend;
    }

    /** 꾸미기 설정 없이(기본값) 만든다. 상세 화면처럼 타일 모양과 상관없는 곳에서 쓴다. */
    public static HomeTile of(Device device) {
        return new HomeTile(device, TileConfig.DEFAULT);
    }

    public static HomeTile of(Device device, TileConfig config) {
        return new HomeTile(device, config == null ? TileConfig.DEFAULT : config);
    }

    /** 목록을 받았는데 기기가 하나도 없을 때만 빈 화면. 아직 못 받았으면(null) 빈 화면을 보이지 않는다. */
    public static boolean isEmptyHome(List<Device> devices) {
        return devices != null && devices.isEmpty();
    }

    static String watts(Double value) {
        return value == null ? NO_VALUE : String.format(Locale.ROOT, "%.1f W", value);
    }

    public static String kwh(Double value) {
        return value == null ? NO_VALUE : String.format(Locale.ROOT, "%.2f kWh", value);
    }
}
