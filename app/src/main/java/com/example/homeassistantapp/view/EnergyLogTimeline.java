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

/**
 * E7 전력량 기록을 차트에 그리기 좋게 다듬는다 (불일치 #9, #10).
 * 차트 뷰(Android)와 떼어 놓은 순수 자바라서 단위 테스트로 확인할 수 있다.
 */
public final class EnergyLogTimeline {
    private static final String SERVER_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String LABEL_PATTERN = "HH:mm";

    private EnergyLogTimeline() {}

    /**
     * 오래된 기록이 앞에 오도록 정렬한 새 목록을 돌려준다(원본은 그대로).
     * 왜: 서버는 최신 기록부터 보낸다(계약서 E7). 그대로 그리면 시간축이 오른쪽→왼쪽으로 거꾸로 간다.
     * 단순히 뒤집지 않고 timestamp로 정렬하는 이유: 서버 순서가 바뀌어도 안전하게.
     * "yyyy-MM-dd HH:mm:ss"는 글자 순서가 곧 시간 순서라 문자열 비교로 충분하다.
     */
    public static List<EnergyLog> oldestFirst(List<EnergyLog> logs) {
        if (logs == null) return new ArrayList<>();
        List<EnergyLog> sorted = new ArrayList<>(logs);
        Collections.sort(sorted, Comparator.comparing(
                EnergyLog::getTimestamp, Comparator.nullsFirst(Comparator.naturalOrder())));
        return sorted;
    }

    /**
     * 서버 UTC 시각을 현지 시간 "HH:mm"으로 바꾼다. 읽을 수 없으면 빈 문자열.
     * 왜: 서버 timestamp는 UTC다(계약서 E7). 시간대를 지정하지 않고 읽으면 폰 시간대로 착각해
     * 한국에서는 9시간 어긋난다.
     */
    public static String toLocalTimeLabel(String utcTimestamp, TimeZone localZone) {
        if (utcTimestamp == null) return "";
        SimpleDateFormat input = new SimpleDateFormat(SERVER_PATTERN, Locale.US);
        input.setTimeZone(TimeZone.getTimeZone("UTC"));
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
