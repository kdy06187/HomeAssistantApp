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
    public void jobNotFound_isLost_unauthorized_pauses() {
        assertEquals(CommissionPoller.Next.LOST, CommissionPoller.afterResponse(4_000, 404, null));
        assertEquals(CommissionPoller.Next.PAUSED, CommissionPoller.afterResponse(4_000, 401, null));
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
    public void restoredFlag() {
        assertTrue(job("{\"restored\":true}").isRestored());
        assertFalse(job("{\"restored\":null}").isRestored());
        assertFalse(job("{}").isRestored());
    }

    @Test
    public void alreadyRegistered_refreshesList() {
        CommissionJobResponse.JobError e = job(
                "{\"error\":{\"code\":\"ALREADY_REGISTERED\",\"message\":\"이미 등록된 기기입니다.\"}}").error;
        assertTrue(CommissionPoller.shouldRefreshListAfterFailure(e));
    }

    @Test
    public void otherFailures_noListRefresh() {
        CommissionJobResponse.JobError e = job(
                "{\"error\":{\"code\":\"COMMISSION_TIMEOUT\",\"message\":\"기기 등록 시간이 초과되었습니다.\"}}").error;
        assertFalse(CommissionPoller.shouldRefreshListAfterFailure(e));
        assertFalse(CommissionPoller.shouldRefreshListAfterFailure(null));
    }

    @Test
    public void networkFailure_timesOutAtSameBoundary() {
        assertEquals(CommissionPoller.Next.POLL_AGAIN, CommissionPoller.afterNetworkFailure(208_000));
        assertEquals(CommissionPoller.Next.TIMED_OUT, CommissionPoller.afterNetworkFailure(208_001));
    }

    @Test
    public void successWithoutBody_keepsPolling() {
        assertEquals(CommissionPoller.Next.POLL_AGAIN, CommissionPoller.afterResponse(4_000, 200, null));
        // 토큰 오류는 상한을 넘어도 멈춤(결과를 내지 않는다).
        assertEquals(CommissionPoller.Next.PAUSED, CommissionPoller.afterResponse(300_000, 401, null));
    }
}
