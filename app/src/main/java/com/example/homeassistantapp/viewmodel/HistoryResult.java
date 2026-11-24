package com.example.homeassistantapp.viewmodel;

import com.example.homeassistantapp.dto.EnergyLogResponse;

/** 상세 차트용 E7 결과. period가 null이면 원본 기록(최근 50건). */
public final class HistoryResult {
    public final String deviceId;
    public final String period;
    /** 성공이면 응답, 실패면 null. */
    public final EnergyLogResponse response;
    /** 실패 이유. 성공이면 null. */
    public final String failure;

    HistoryResult(String deviceId, String period, EnergyLogResponse response, String failure) {
        this.deviceId = deviceId;
        this.period = period;
        this.response = response;
        this.failure = failure;
    }

    public boolean isFor(String deviceId, String period) {
        return this.deviceId.equals(deviceId)
                && (this.period == null ? period == null : this.period.equals(period));
    }
}
