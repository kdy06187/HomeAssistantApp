package com.example.homeassistantapp.viewmodel;

import com.example.homeassistantapp.dto.CommissionJobResponse;

/**
 * 등록 작업(E8) 폴링에서 다음에 할 일을 정한다. 시간과 응답만 받아 결정하므로 Android 없이 시험할 수 있다.
 * 간격·상한은 서버 13b·계약 14번 확정 뒤 다시 맞춘다.
 */
final class CommissionPoller {
    static final long INTERVAL_MS = 2_000L;
    // 서버 상한(Matter 190초) + 여유 20초. 넘으면 폴링을 멈추고 목록으로 확인한다.
    static final long LIMIT_MS = 210_000L;

    enum Next {
        /** 아직 진행 중이거나 일시적 오류: 2초 뒤 다시 묻는다 */
        POLL_AGAIN,
        SUCCEEDED,
        FAILED,
        /** 작업이 없어짐(서버 재시작 등): 멈추고 목록으로 확인 */
        LOST,
        /** 토큰 오류: 결과 없이 멈췄다가 토큰을 다시 저장하면 같은 작업을 이어서 묻는다 */
        PAUSED,
        /** 상한 초과: 멈추고 목록으로 확인 */
        TIMED_OUT
    }

    private CommissionPoller() {}

    /** 응답을 받은 뒤. body는 2xx일 때만 의미가 있다. */
    static Next afterResponse(long elapsedMs, int httpCode, CommissionJobResponse body) {
        if (httpCode >= 200 && httpCode < 300 && body != null) {
            if (CommissionJobResponse.SUCCEEDED.equals(body.state)) return Next.SUCCEEDED;
            if (CommissionJobResponse.FAILED.equals(body.state)) return Next.FAILED;
        }
        if (httpCode == 401) return Next.PAUSED;
        if (httpCode == 404) return Next.LOST;
        return continueOrTimeOut(elapsedMs);
    }

    /** 통신 실패 뒤. 일시적일 수 있어 상한 전까지는 다시 묻는다. */
    static Next afterNetworkFailure(long elapsedMs) {
        return continueOrTimeOut(elapsedMs);
    }

    private static Next continueOrTimeOut(long elapsedMs) {
        return elapsedMs + INTERVAL_MS > LIMIT_MS ? Next.TIMED_OUT : Next.POLL_AGAIN;
    }

    /** ALREADY_REGISTERED는 실패지만 이미 목록에 있으므로 목록을 새로고침한다. */
    static boolean shouldRefreshListAfterFailure(CommissionJobResponse.JobError error) {
        return error != null && "ALREADY_REGISTERED".equals(error.code);
    }
}
