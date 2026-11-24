package com.example.homeassistantapp.view;

import com.example.homeassistantapp.dto.EnergyLog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class EnergyLogTimeline {
    // 서버는 UTC를 "2026-11-16T00:20:00Z" 꼴로 준다(밀리초·오프셋 없음).
    private static final String SERVER_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String LABEL_PATTERN = "HH:mm";

    private EnergyLogTimeline() {}

    // 오래된 순으로 정렬한 복사본. 서버가 이미 오래된 순으로 주지만 안전장치로 둔다.
    // 같은 꼴의 UTC 시각은 문자열 순서가 곧 시간 순서다.
    public static List<EnergyLog> oldestFirst(List<EnergyLog> logs) {
        if (logs == null) return new ArrayList<>();
        List<EnergyLog> sorted = new ArrayList<>(logs);
        Collections.sort(sorted, Comparator.comparing(
                EnergyLog::getTimestamp, Comparator.nullsFirst(Comparator.naturalOrder())));
        return sorted;
    }

    // 오래된 순 목록에서 이웃 기록의 차이를 구한다. 결과는 입력보다 하나 적다.
    // 기록이 1개 이하면 빈 목록, 누적값이 줄면(기기 초기화) 사용량 0으로 처리
    public static List<Long> intervalUsage(List<EnergyLog> oldestFirst) {
        List<Long> result = new ArrayList<>();
        if (oldestFirst == null) return result;
        for (int i = 1; i < oldestFirst.size(); i++) {
            long diff = oldestFirst.get(i).getTotalEnergy_mWh() - oldestFirst.get(i - 1).getTotalEnergy_mWh();
            result.add(Math.max(0L, diff));
        }
        return result;
    }

    // 서버 시각(UTC)을 현지 "HH:mm"으로. 읽을 수 없으면 빈 문자열.
    public static String toLocalTimeLabel(String utcTimestamp, TimeZone localZone) {
        if (utcTimestamp == null) return "";
        SimpleDateFormat input = new SimpleDateFormat(SERVER_PATTERN, Locale.US);
        input.setTimeZone(TimeZone.getTimeZone("UTC"));
        input.setLenient(false);
        SimpleDateFormat output = new SimpleDateFormat(LABEL_PATTERN, Locale.US);
        output.setTimeZone(localZone);
        try {
            Date date = input.parse(utcTimestamp);
            return date == null ? "" : output.format(date);
        } catch (ParseException e) {
            return "";
        }
    }
}
