package com.example.homeassistantapp.network;

/**
 * 앱 전체의 서버 연결 상태(정상 / 서버에 닿지 않음 / 토큰 오류). 모든 요청 결과가 여기로 모이고,
 * 배너·401 안내·설정 화면의 토큰 오류 표시가 이 값 하나를 본다. 여러 OkHttp 스레드에서 불리므로 synchronized.
 *
 * 클라이언트를 다시 만들 때마다 세대 번호가 올라가고, 옛 세대 클라이언트의 늦은 결과는 무시한다.
 * 토큰 오류로 들어간 순간의 1회 안내 여부도 같은 잠금 안에서 함께 바뀐다.
 */
public final class ConnectionState {
    public enum Kind { OK, UNREACHABLE, TOKEN_ERROR }

    /** 전이 결과. ENTERED_TOKEN_ERROR는 이번 결과로 토큰 오류가 막 시작됐다는 뜻. */
    public enum Change { NONE, CHANGED, ENTERED_TOKEN_ERROR }

    private Kind kind = Kind.OK;
    private int generation = 0;
    private boolean tokenNoticePending = false;

    public synchronized Kind get() {
        return kind;
    }

    /** 새 클라이언트를 만들 때 부른다. 돌려준 번호를 그 클라이언트의 결과와 함께 넘긴다. */
    public synchronized int nextGeneration() {
        return ++generation;
    }

    public synchronized int currentGeneration() {
        return generation;
    }

    /**
     * HTTP 응답을 받았을 때. 401이면 토큰 오류, 2xx면 정상.
     * 그 밖의 코드(404·500 등)는 서버에 닿았다는 뜻이라 "닿지 않음"만 풀고 토큰 오류는 그대로 둔다.
     */
    public synchronized Change onResponse(int fromGeneration, int httpCode) {
        if (fromGeneration != generation) return Change.NONE;
        Kind next;
        if (httpCode == 401) next = Kind.TOKEN_ERROR;
        else if (httpCode >= 200 && httpCode < 300) next = Kind.OK;
        else next = kind == Kind.UNREACHABLE ? Kind.OK : kind;
        return set(next);
    }

    /** 통신 실패. 토큰 오류 중이면 토큰 오류를 계속 보인다(고칠 것이 토큰이므로). */
    public synchronized Change onNetworkFailure(int fromGeneration) {
        if (fromGeneration != generation) return Change.NONE;
        return set(kind == Kind.TOKEN_ERROR ? Kind.TOKEN_ERROR : Kind.UNREACHABLE);
    }

    /** 설정을 저장하면 새 값으로 다시 판단하도록 처음 상태로 돌린다. */
    public synchronized Change reset() {
        return set(Kind.OK);
    }

    /**
     * 토큰 오류 진입 안내를 아직 안 했으면 true를 돌려주고 내린다. 화면 회전으로 관찰자가 다시 붙어도
     * 안내(토스트·설정 탭 이동)가 한 번만 일어나게 한다. 토큰 오류가 풀리면 안내할 것도 사라진다.
     */
    public synchronized boolean consumeTokenNotice() {
        boolean pending = tokenNoticePending;
        tokenNoticePending = false;
        return pending;
    }

    private Change set(Kind next) {
        if (next == kind) return Change.NONE;
        boolean entered = next == Kind.TOKEN_ERROR;
        kind = next;
        tokenNoticePending = entered;
        return entered ? Change.ENTERED_TOKEN_ERROR : Change.CHANGED;
    }
}
