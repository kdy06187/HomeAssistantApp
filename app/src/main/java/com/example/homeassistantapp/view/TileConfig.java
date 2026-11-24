package com.example.homeassistantapp.view;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.Objects;

/**
 * 기기 하나의 홈 타일 꾸미기 설정. 이름·상태 칩·스위치는 항상 보이므로 여기에 없다.
 * 저장 형식은 JSON 한 줄이고 v로 형식 버전을 구분한다.
 */
public final class TileConfig {
    public static final int FORMAT_VERSION = 1;
    public static final TileConfig DEFAULT = new TileConfig(true, true, false, 1);

    private static final Gson GSON = new Gson();

    public final boolean power;
    public final boolean total;
    public final boolean trend;
    /** 타일 폭: 1칸 또는 2칸(한 줄 전체). */
    public final int span;

    public TileConfig(boolean power, boolean total, boolean trend, int span) {
        this.power = power;
        this.total = total;
        this.trend = trend;
        this.span = span == 2 ? 2 : 1;
    }

    /** 전력 값이 있는 기기만 W·누적·추이선을 고를 수 있다(TCP_DIY는 크기만). */
    public static boolean supportsEnergy(String deviceType) {
        return "MATTER".equals(deviceType);
    }

    /** 기기 종류에서 못 쓰는 항목은 끈 실제 적용값. 저장된 값은 바꾸지 않는다. */
    public TileConfig effectiveFor(String deviceType) {
        if (supportsEnergy(deviceType)) return this;
        return new TileConfig(false, false, false, span);
    }

    public TileConfig withPower(boolean value) {
        return new TileConfig(value, total, trend, span);
    }

    public TileConfig withTotal(boolean value) {
        return new TileConfig(power, value, trend, span);
    }

    public TileConfig withTrend(boolean value) {
        return new TileConfig(power, total, value, span);
    }

    public TileConfig withSpan(int value) {
        return new TileConfig(power, total, trend, value);
    }

    public String toJson() {
        Stored stored = new Stored();
        stored.v = FORMAT_VERSION;
        stored.power = power;
        stored.total = total;
        stored.trend = trend;
        stored.span = span;
        return GSON.toJson(stored);
    }

    /** 없거나 깨진 값은 기본값, 빠졌거나 타입이 틀린 항목은 그 항목만 기본값, 모르는 필드는 무시한다. */
    public static TileConfig fromJson(String json) {
        if (json == null || json.trim().isEmpty()) return DEFAULT;
        Map<?, ?> stored;
        try {
            stored = GSON.fromJson(json, Map.class);
        } catch (JsonParseException | IllegalStateException | ClassCastException e) {
            return DEFAULT;
        }
        if (stored == null) return DEFAULT;
        Object span = stored.get("span");
        return new TileConfig(
                bool(stored.get("power"), DEFAULT.power),
                bool(stored.get("total"), DEFAULT.total),
                bool(stored.get("trend"), DEFAULT.trend),
                span instanceof Number ? ((Number) span).intValue() : DEFAULT.span);
    }

    private static boolean bool(Object value, boolean fallback) {
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    // 저장할 때만 쓰는 모양. 필드 순서가 JSON 순서가 된다.
    private static final class Stored {
        @SerializedName("v") int v;
        @SerializedName("power") boolean power;
        @SerializedName("total") boolean total;
        @SerializedName("trend") boolean trend;
        @SerializedName("span") int span;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TileConfig)) return false;
        TileConfig other = (TileConfig) o;
        return power == other.power && total == other.total && trend == other.trend && span == other.span;
    }

    @Override
    public int hashCode() {
        return Objects.hash(power, total, trend, span);
    }
}
