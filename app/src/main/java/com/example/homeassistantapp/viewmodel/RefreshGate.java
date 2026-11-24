package com.example.homeassistantapp.viewmodel;

/**
 * 대시보드는 기기마다 요청을 보내므로 404가 한꺼번에 여러 개 올 수 있다.
 * 예약된 목록 새로고침이 실행될 때까지 추가 요청은 하나로 묶는다. 메인 스레드에서만 쓴다.
 */
final class RefreshGate {
    private boolean scheduled;

    /** 아직 예약이 없으면 true를 돌려주고 예약된 것으로 표시한다. */
    boolean tryClaim() {
        if (scheduled) return false;
        scheduled = true;
        return true;
    }

    /** 예약한 새로고침을 실행할 때 부른다. */
    void release() {
        scheduled = false;
    }
}
