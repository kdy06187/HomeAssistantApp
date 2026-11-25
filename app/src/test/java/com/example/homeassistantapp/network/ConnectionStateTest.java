package com.example.homeassistantapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.homeassistantapp.network.ConnectionState.Change;
import com.example.homeassistantapp.network.ConnectionState.Kind;

import org.junit.Test;

public class ConnectionStateTest {
    /** 첫 클라이언트를 만든 상태. */
    private static ConnectionState started() {
        ConnectionState s = new ConnectionState();
        s.nextGeneration();
        return s;
    }

    private static Change respond(ConnectionState s, int code) {
        return s.onResponse(s.currentGeneration(), code);
    }

    private static Change fail(ConnectionState s) {
        return s.onNetworkFailure(s.currentGeneration());
    }

    @Test
    public void startsOk() {
        assertEquals(Kind.OK, new ConnectionState().get());
    }

    @Test
    public void networkFailure_unreachable_thenAnyResponseRecovers() {
        ConnectionState s = started();
        assertEquals(Change.CHANGED, fail(s));
        assertEquals(Kind.UNREACHABLE, s.get());
        assertEquals(Change.NONE, fail(s));
        assertEquals(Change.CHANGED, respond(s, 500));
        assertEquals(Kind.OK, s.get());
        fail(s);
        assertEquals(Change.CHANGED, respond(s, 200));
        assertEquals(Kind.OK, s.get());
    }

    @Test
    public void unauthorized_staysUntilSuccessOrReset() {
        ConnectionState s = started();
        assertEquals(Change.ENTERED_TOKEN_ERROR, respond(s, 401));
        assertEquals(Kind.TOKEN_ERROR, s.get());
        assertEquals(Change.NONE, respond(s, 401));
        // 토큰 오류 중 다른 실패는 토큰 오류를 유지한다.
        assertEquals(Change.NONE, fail(s));
        assertEquals(Change.NONE, respond(s, 404));
        assertEquals(Change.NONE, respond(s, 502));
        assertEquals(Kind.TOKEN_ERROR, s.get());
        assertEquals(Change.CHANGED, respond(s, 204));
        assertEquals(Kind.OK, s.get());
    }

    @Test
    public void reset_clearsEverything() {
        ConnectionState s = started();
        respond(s, 401);
        assertEquals(Change.CHANGED, s.reset());
        assertEquals(Kind.OK, s.get());
        assertFalse(s.consumeTokenNotice());
        assertEquals(Change.NONE, s.reset());
        fail(s);
        assertEquals(Change.CHANGED, s.reset());
    }

    @Test
    public void unreachableThen401_becomesTokenError() {
        ConnectionState s = started();
        fail(s);
        assertEquals(Change.ENTERED_TOKEN_ERROR, respond(s, 401));
        assertEquals(Kind.TOKEN_ERROR, s.get());
    }

    @Test
    public void okResponses_noChange() {
        ConnectionState s = started();
        assertEquals(Change.NONE, respond(s, 200));
        assertEquals(Change.NONE, respond(s, 404));
    }

    @Test
    public void tokenNotice_oncePerEntry() {
        ConnectionState s = started();
        respond(s, 401);
        assertTrue(s.consumeTokenNotice());
        assertFalse(s.consumeTokenNotice());
        respond(s, 401);
        assertFalse(s.consumeTokenNotice());
        // 2xx로 풀렸다가 다시 401이면 다시 1번 안내한다.
        respond(s, 200);
        respond(s, 401);
        assertTrue(s.consumeTokenNotice());
    }

    @Test
    public void tokenNotice_droppedWhenErrorClearsBeforeShown() {
        ConnectionState s = started();
        respond(s, 401);
        respond(s, 200);
        assertFalse(s.consumeTokenNotice());
    }

    @Test
    public void oldGenerationResults_ignored() {
        ConnectionState s = new ConnectionState();
        int old = s.nextGeneration();
        int current = s.nextGeneration();
        assertEquals(Change.NONE, s.onResponse(old, 401));
        assertEquals(Change.NONE, s.onNetworkFailure(old));
        assertEquals(Kind.OK, s.get());
        assertFalse(s.consumeTokenNotice());
        assertEquals(Change.ENTERED_TOKEN_ERROR, s.onResponse(current, 401));
        // 새 클라이언트로 바꾼 뒤 옛 클라이언트의 2xx도 토큰 오류를 풀지 못한다.
        s.nextGeneration();
        assertEquals(Change.NONE, s.onResponse(current, 200));
        assertEquals(Kind.TOKEN_ERROR, s.get());
    }
}
