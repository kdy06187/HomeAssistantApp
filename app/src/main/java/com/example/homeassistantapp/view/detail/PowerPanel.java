package com.example.homeassistantapp.view.detail;

import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.HomeTile;
import com.example.homeassistantapp.viewmodel.EnergyReading;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** 상세 전력 카드에 보일 글자. */
public final class PowerPanel {
    public final String powerText;
    public final String totalText;
    public final String updatedText;
    /** 실패 배너 문구. 없으면 null(배너 숨김). */
    public final String bannerText;

    private PowerPanel(Device device, EnergyReading reading, TimeZone zone) {
        // 꺼짐·오프라인일 때 "–"를 쓰는 규칙은 홈 타일과 같다.
        HomeTile tile = HomeTile.of(device);
        powerText = tile.powerText;
        totalText = "누적 " + HomeTile.kwh(device.getTotalEnergyKwh());

        boolean mine = reading != null && device.getId() != null && device.getId().equals(reading.deviceId);
        Long at = mine ? reading.lastSuccessAt : null;
        updatedText = (at == null ? "아직 받지 못했어요" : "마지막 갱신 " + clock(at, zone)) + " · 15초마다";
        bannerText = mine && reading.failure != null ? "전력을 불러오지 못했어요. " + reading.failure : null;
    }

    public static PowerPanel of(Device device, EnergyReading reading, TimeZone zone) {
        return new PowerPanel(device, reading, zone);
    }

    private static String clock(long epochMs, TimeZone zone) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
        format.setTimeZone(zone);
        return format.format(new Date(epochMs));
    }
}
