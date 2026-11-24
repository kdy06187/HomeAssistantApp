package com.example.homeassistantapp.viewmodel;

/** 상세 화면 전력 조회 결과. 값 자체는 Device에 들어가고, 여기에는 언제 받았는지와 실패 문구만 둔다. */
public final class EnergyReading {
    public final String deviceId;
    /** 마지막으로 성공한 시각(epoch ms). 아직 성공한 적 없으면 null. */
    public final Long lastSuccessAt;
    /** 마지막 요청이 실패했으면 그 이유, 성공했으면 null. */
    public final String failure;

    private EnergyReading(String deviceId, Long lastSuccessAt, String failure) {
        this.deviceId = deviceId;
        this.lastSuccessAt = lastSuccessAt;
        this.failure = failure;
    }

    public static EnergyReading success(String deviceId, long at) {
        return new EnergyReading(deviceId, at, null);
    }

    /** 실패해도 이전 성공 시각은 남긴다("마지막 갱신"이 실제 값의 시각을 가리키게). */
    public static EnergyReading failure(EnergyReading previous, String deviceId, String reason) {
        Long last = previous != null && deviceId.equals(previous.deviceId) ? previous.lastSuccessAt : null;
        return new EnergyReading(deviceId, last, reason);
    }
}
