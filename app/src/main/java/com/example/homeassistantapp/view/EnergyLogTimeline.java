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
     * 누적 전력량을 구간 사용량으로 바꾼다 (계약서 E7, 불일치 #14).
     * 반드시 oldestFirst()로 정렬한 목록을 넣는다. 결과 i번째 = (i+1)번째 기록 − i번째 기록.
     * 그래서 결과는 입력보다 하나 적고, i번째 값은 (i+1)번째 기록 시각에 끝난 구간이다.
     *
     * 경계 처리:
     * - 기록이 0~1개: 차이를 낼 이웃이 없으므로 빈 목록.
     * - 차이가 음수: 기기 초기화·교체 등으로 누적값이 리셋된 것이다. 사용량이 음수일 수는 없으므로 0으로 둔다.
     *   (리셋 뒤 값 자체를 그 구간 사용량으로 볼 수도 있지만, 리셋 시점을 알 수 없어 과대 표시될 수 있다.)
     */
    public static List<Long> intervalUsage(List<EnergyLog> oldestFirst) {
        List<Long> result = new ArrayList<>();
        if (oldestFirst == null) return result;
        for (int i = 1; i < oldestFirst.size(); i++) {
            long diff = oldestFirst.get(i).getTotalEnergy_mWh() - oldestFirst.get(i - 1).getTotalEnergy_mWh();
            result.add(Math.max(0L, diff));
        }
        return result;
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
