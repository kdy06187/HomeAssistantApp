package com.example.homeassistantapp.view;

import com.example.homeassistantapp.dto.EnergyBucket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 홈 타일 추이선 값(최근 7일 일별 사용량)과 그 선의 좌표 계산. */
public final class TrendValues {
    /** 구간별 사용량(mWh). 기록이 없는 구간은 null(선이 끊김). */
    public final List<Long> values;

    public TrendValues(List<Long> values) {
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    public static TrendValues fromBuckets(List<EnergyBucket> buckets) {
        List<Long> values = new ArrayList<>();
        if (buckets != null) {
            for (EnergyBucket bucket : buckets) values.add(bucket.sampleCount > 0 ? bucket.energyUsedMwh : null);
        }
        return new TrendValues(values);
    }

    /** 그릴 값이 하나도 없으면 선 대신 "–". */
    public boolean isEmpty() {
        for (Long value : values) if (value != null) return false;
        return true;
    }

    /**
     * 너비·높이 안의 꺾은선 좌표. 기록 없는 구간에서 끊어 여러 조각이 되고,
     * 조각마다 [x0, y0, x1, y1, …]. 점이 하나뿐인 조각은 점으로 그린다.
     */
    public List<float[]> polylines(float width, float height) {
        List<float[]> result = new ArrayList<>();
        int n = values.size();
        if (n == 0 || width <= 0 || height <= 0) return result;
        long max = 0;
        for (Long value : values) if (value != null) max = Math.max(max, value);

        List<Float> current = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Long value = values.get(i);
            if (value == null) {
                flush(current, result);
                continue;
            }
            float x = n == 1 ? width / 2f : width * i / (n - 1);
            // 모두 0이면 바닥에 납작한 선. 아니면 가장 큰 값이 위끝.
            float y = max == 0 ? height : height - height * value / max;
            current.add(x);
            current.add(y);
        }
        flush(current, result);
        return result;
    }

    private static void flush(List<Float> current, List<float[]> result) {
        if (current.isEmpty()) return;
        float[] points = new float[current.size()];
        for (int i = 0; i < points.length; i++) points[i] = current.get(i);
        result.add(points);
        current.clear();
    }
}
