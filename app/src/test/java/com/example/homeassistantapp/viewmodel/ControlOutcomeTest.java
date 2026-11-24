package com.example.homeassistantapp.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.homeassistantapp.network.ApiError;

import org.junit.Test;

public class ControlOutcomeTest {

    private static ApiError error(int httpCode, String code) {
        return ApiError.parse(httpCode, "{\"error\":{\"code\":\"" + code + "\",\"message\":\"m\"}}");
    }

    @Test
    public void successCodes() {
        assertEquals(ControlOutcome.DONE, ControlOutcome.of(200, null));
        assertEquals(ControlOutcome.ACCEPTED, ControlOutcome.of(202, null));
    }

    @Test
    public void contractFailures() {
        assertEquals(ControlOutcome.BLOCKED, ControlOutcome.of(409, error(409, "COMMAND_BLOCKED")));
        assertEquals(ControlOutcome.UNREACHABLE, ControlOutcome.of(502, error(502, "DEVICE_UNREACHABLE")));
        assertEquals(ControlOutcome.DEVICE_GONE, ControlOutcome.of(404, error(404, "DEVICE_NOT_FOUND")));
        assertEquals(ControlOutcome.FAILED, ControlOutcome.of(400, error(400, "INVALID_REQUEST")));
        assertEquals(ControlOutcome.FAILED, ControlOutcome.of(401, error(401, "UNAUTHORIZED")));
        assertEquals(ControlOutcome.FAILED, ControlOutcome.of(500, error(500, "INTERNAL_ERROR")));
    }

    @Test
    public void notFoundWithoutDeviceCode_isPlainFailure() {
        // 경로 404(NOT_FOUND)나 본문 없는 404는 기기가 사라졌다는 뜻이 아니다.
        assertEquals(ControlOutcome.FAILED, ControlOutcome.of(404, error(404, "NOT_FOUND")));
        assertEquals(ControlOutcome.FAILED, ControlOutcome.of(404, ApiError.parse(404, "")));
    }

    @Test
    public void bodyMissing_502StillUnreachable() {
        assertEquals(ControlOutcome.UNREACHABLE, ControlOutcome.of(502, ApiError.parse(502, null)));
    }

    @Test
    public void onlyUnreachable_showsOffline() {
        assertEquals("OFFLINE", ControlOutcome.UNREACHABLE.failedState());
        assertNull(ControlOutcome.BLOCKED.failedState());
        assertNull(ControlOutcome.DEVICE_GONE.failedState());
        assertNull(ControlOutcome.FAILED.failedState());
    }

    @Test
    public void acceptedMessage() {
        assertEquals("명령을 접수했습니다. 약 45초 뒤 꺼집니다.", ControlOutcome.acceptedMessage("OFF", 45));
        assertEquals("명령을 접수했습니다. 약 3초 뒤 켜집니다.", ControlOutcome.acceptedMessage("ON", 3));
        assertEquals("명령을 접수했습니다. 잠시 뒤 실행됩니다.", ControlOutcome.acceptedMessage(null, null));
    }
}
