package com.example.homeassistantapp.network;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 틀린 토큰이면 요청마다 401이 시간 차를 두고 오므로, 안내는 설정을 다시 저장할 때까지 한 번만 한다.
 * OkHttp의 여러 스레드에서 동시에 불려도 한 번만 true가 된다.
 */
final class UnauthorizedNotice {
    private final AtomicBoolean shown = new AtomicBoolean(false);

    /** 아직 안내하지 않았으면 true를 돌려주고 안내한 것으로 표시한다. */
    boolean tryClaim() {
        return shown.compareAndSet(false, true);
    }

    boolean isShown() {
        return shown.get();
    }

    void reset() {
        shown.set(false);
    }
}
