package com.example.homeassistantapp.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.homeassistantapp.dto.CommissionJobResponse;
import com.google.gson.Gson;

import org.junit.Test;

public class CommissionPollerTest {
    private final Gson gson = new Gson();

    private CommissionJobResponse job(String json) {
        return gson.fromJson(json, CommissionJobResponse.class);
    }

    @Test
    public void pendingOrRunning_pollsAgain() {
        assertEquals(CommissionPoller.Next.POLL_AGAIN,
                CommissionPoller.afterResponse(2_000, 200, job("{\"jobId\":\"c-1\",\"state\":\"PENDING\"}")));
        assertEquals(CommissionPoller.Next.POLL_AGAIN,
                CommissionPoller.afterResponse(30_000, 200, job("{\"jobId\":\"c-1\",\"state\":\"RUNNING\"}")));
    }

    @Test
    public void terminalStates() {
        assertEquals(CommissionPoller.Next.SUCCEEDED, CommissionPoller.afterResponse(10_000, 200,
                job("{\"jobId\":\"c-1\",\"state\":\"SUCCEEDED\",\"deviceId\":\"SN1\",\"restored\":false,\"error\":null}")));
        assertEquals(CommissionPoller.Next.FAILED, CommissionPoller.afterResponse(10_000, 200,
                job("{\"jobId\":\"c-1\",\"state\":\"FAILED\",\"error\":{\"code\":\"COMMISSION_FAILED\",\"message\":\"기기 등록에 실패했습니다.\"}}")));
    }

    @Test
    public void terminalStateWins_evenAfterLimit() {
        assertEquals(CommissionPoller.Next.SUCCEEDED, CommissionPoller.afterResponse(300_000, 200,
                job("{\"jobId\":\"c-1\",\"state\":\"SUCCEEDED\"}")));
    }

    @Test
    public void jobNotFoundOrUnauthorized_isLost() {
        assertEquals(CommissionPoller.Next.LOST, CommissionPoller.afterResponse(4_000, 404, null));
        assertEquals(CommissionPoller.Next.LOST, CommissionPoller.afterResponse(4_000, 401, null));
    }

    @Test
    public void serverErrorOrNetworkFailure_retriesUntilLimit() {
        assertEquals(CommissionPoller.Next.POLL_AGAIN, CommissionPoller.afterResponse(4_000, 500, null));
        assertEquals(CommissionPoller.Next.POLL_AGAIN, CommissionPoller.afterNetworkFailure(4_000));
        assertEquals(CommissionPoller.Next.TIMED_OUT, CommissionPoller.afterNetworkFailure(209_000));
    }

    @Test
    public void limitIs210Seconds() {
        assertEquals(210_000L, CommissionPoller.LIMIT_MS);
        assertEquals(2_000L, CommissionPoller.INTERVAL_MS);
        CommissionJobResponse running = job("{\"state\":\"RUNNING\"}");
        assertEquals(CommissionPoller.Next.POLL_AGAIN, CommissionPoller.afterResponse(208_000, 200, running));
        assertEquals(CommissionPoller.Next.TIMED_OUT, CommissionPoller.afterResponse(208_001, 200, running));
    }

    @Test
    public void unknownState_keepsPolling() {
        assertEquals(CommissionPoller.Next.POLL_AGAIN,
                CommissionPoller.afterResponse(2_000, 200, job("{\"state\":\"QUEUED\"}")));
    }

    @Test
    public void restoredMessage() {
        assertTrue(job("{\"restored\":true}").isRestored());
        assertFalse(job("{\"restored\":null}").isRestored());
        assertFalse(job("{}").isRestored());
        assertEquals("기기 등록 성공! 이전 기록을 이어받았습니다.", CommissionPoller.successMessage(true));
        assertEquals("기기 등록 성공!", CommissionPoller.successMessage(false));
    }

    @Test
    public void alreadyRegistered_ownMessageAndRefresh() {
        CommissionJobResponse.JobError e = job(
                "{\"error\":{\"code\":\"ALREADY_REGISTERED\",\"message\":\"이미 등록된 기기입니다.\"}}").error;
        assertEquals("이미 등록된 기기입니다.", CommissionPoller.failureMessage(e));
        assertTrue(CommissionPoller.shouldRefreshListAfterFailure(e));
    }

    @Test
    public void otherFailures_useServerMessage() {
        CommissionJobResponse.JobError e = job(
                "{\"error\":{\"code\":\"COMMISSION_TIMEOUT\",\"message\":\"기기 등록 시간이 초과되었습니다.\"}}").error;
        assertEquals("등록 실패: 기기 등록 시간이 초과되었습니다.", CommissionPoller.failureMessage(e));
        assertFalse(CommissionPoller.shouldRefreshListAfterFailure(e));
        assertEquals("등록 실패: 기기 등록에 실패했습니다.", CommissionPoller.failureMessage(null));
    }
}
