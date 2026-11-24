package com.example.homeassistantapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ConnectionStateTest {
    @Test
    public void startsOk() {
        assertEquals(ConnectionState.Kind.OK, new ConnectionState().get());
    }

    @Test
    public void networkFailure_unreachable_thenAnyResponseRecovers() {
        ConnectionState s = new ConnectionState();
        assertTrue(s.onNetworkFailure());
        assertEquals(ConnectionState.Kind.UNREACHABLE, s.get());
        assertFalse(s.onNetworkFailure());
        assertTrue(s.onResponse(500));
        assertEquals(ConnectionState.Kind.OK, s.get());
        s.onNetworkFailure();
        assertTrue(s.onResponse(200));
        assertEquals(ConnectionState.Kind.OK, s.get());
    }

    @Test
    public void unauthorized_staysUntilSuccessOrReset() {
        ConnectionState s = new ConnectionState();
        assertTrue(s.onResponse(401));
        assertEquals(ConnectionState.Kind.TOKEN_ERROR, s.get());
        assertFalse(s.onResponse(401));
        // 토큰 오류 중 다른 실패는 토큰 오류를 유지한다.
        assertFalse(s.onNetworkFailure());
        assertFalse(s.onResponse(404));
        assertFalse(s.onResponse(502));
        assertEquals(ConnectionState.Kind.TOKEN_ERROR, s.get());
        assertTrue(s.onResponse(204));
        assertEquals(ConnectionState.Kind.OK, s.get());
    }

    @Test
    public void reset_clearsEverything() {
        ConnectionState s = new ConnectionState();
        s.onResponse(401);
        assertTrue(s.reset());
        assertEquals(ConnectionState.Kind.OK, s.get());
        assertFalse(s.reset());
        s.onNetworkFailure();
        assertTrue(s.reset());
    }

    @Test
    public void unreachableThen401_becomesTokenError() {
        ConnectionState s = new ConnectionState();
        s.onNetworkFailure();
        assertTrue(s.onResponse(401));
        assertEquals(ConnectionState.Kind.TOKEN_ERROR, s.get());
    }

    @Test
    public void okResponses_noChange() {
        ConnectionState s = new ConnectionState();
        assertFalse(s.onResponse(200));
        assertFalse(s.onResponse(404));
    }
}
