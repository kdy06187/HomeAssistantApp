package com.example.homeassistantapp.network;

/**
 * 앱 전체의 서버 연결 상태(정상 / 서버에 닿지 않음 / 토큰 오류). 모든 요청 결과가 여기로 모이고,
 * 배너·401 안내·설정 화면의 토큰 오류 표시가 이 값 하나를 본다. 여러 OkHttp 스레드에서 불리므로 synchronized.
 */
public final class ConnectionState {
    public enum Kind { OK, UNREACHABLE, TOKEN_ERROR }

    private Kind kind = Kind.OK;

    public synchronized Kind get() {
        return kind;
    }

    /**
     * HTTP 응답을 받았을 때. 401이면 토큰 오류, 2xx면 정상.
     * 그 밖의 코드(404·500 등)는 서버에 닿았다는 뜻이라 "닿지 않음"만 풀고 토큰 오류는 그대로 둔다.
     * 바뀌었으면 true.
     */
    public synchronized boolean onResponse(int httpCode) {
        Kind next;
        if (httpCode == 401) next = Kind.TOKEN_ERROR;
        else if (httpCode >= 200 && httpCode < 300) next = Kind.OK;
        else next = kind == Kind.UNREACHABLE ? Kind.OK : kind;
        return set(next);
    }

    /** 통신 실패. 토큰 오류 중이면 토큰 오류를 계속 보인다(고칠 것이 토큰이므로). 바뀌었으면 true. */
    public synchronized boolean onNetworkFailure() {
        return set(kind == Kind.TOKEN_ERROR ? Kind.TOKEN_ERROR : Kind.UNREACHABLE);
    }

    /** 설정을 저장하면 새 값으로 다시 판단하도록 처음 상태로 돌린다. 바뀌었으면 true. */
    public synchronized boolean reset() {
        return set(Kind.OK);
    }

    private boolean set(Kind next) {
        if (next == kind) return false;
        kind = next;
        return true;
    }
}
