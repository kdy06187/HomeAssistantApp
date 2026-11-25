package com.example.homeassistantapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

public class ConnectionTestResultTest {
    @Test
    public void http_classified() {
        assertEquals(ConnectionTestResult.Kind.SUCCESS, ConnectionTestResult.fromHttp(200).kind);
        assertTrue(ConnectionTestResult.fromHttp(204).isSuccess());
        assertEquals(ConnectionTestResult.Kind.TOKEN_ERROR, ConnectionTestResult.fromHttp(401).kind);
        assertEquals(ConnectionTestResult.Kind.SERVER_ERROR, ConnectionTestResult.fromHttp(404).kind);
        assertEquals(ConnectionTestResult.Kind.SERVER_ERROR, ConnectionTestResult.fromHttp(500).kind);
        assertTrue(ConnectionTestResult.fromHttp(500).message.contains("HTTP 500"));
        assertFalse(ConnectionTestResult.fromHttp(302).isSuccess());
    }

    @Test
    public void failures_unreachable() {
        assertEquals(ConnectionTestResult.Kind.UNREACHABLE, ConnectionTestResult.fromFailure(new IOException()).kind);
        assertEquals(ConnectionTestResult.Kind.UNREACHABLE, ConnectionTestResult.fromFailure(new ConnectException()).kind);
        assertEquals(ConnectionTestResult.Kind.UNREACHABLE, ConnectionTestResult.fromFailure(new SocketTimeoutException()).kind);
        assertEquals(ConnectionTestResult.Kind.UNREACHABLE,
                ConnectionTestResult.fromFailure(new IllegalArgumentException("bad url")).kind);
        // 영문 예외 문구를 화면에 쓰지 않는다.
        assertFalse(ConnectionTestResult.fromFailure(new IOException("Failed to connect")).message.contains("Failed"));
    }

    @Test
    public void firstRun_needsMatchingSuccessfulTest() {
        assertTrue(ConnectionTestResult.canSave(false, false));
        assertFalse(ConnectionTestResult.canSave(true, false));
        assertTrue(ConnectionTestResult.canSave(true, true));
    }
}
