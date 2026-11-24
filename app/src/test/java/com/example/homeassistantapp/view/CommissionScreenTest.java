package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.homeassistantapp.viewmodel.CommissionStatus;

import org.junit.Test;

public class CommissionScreenTest {
    private static final long START = 10_000L;

    private static CommissionStatus running() {
        return CommissionStatus.sending("거실 플러그").accepted(START).withServerState("RUNNING");
    }

    @Test
    public void mmss_formats() {
        assertEquals("00:00", CommissionScreen.mmss(0));
        assertEquals("00:59", CommissionScreen.mmss(59_999));
        assertEquals("01:00", CommissionScreen.mmss(60_000));
        assertEquals("03:30", CommissionScreen.mmss(210_000));
        assertEquals("61:01", CommissionScreen.mmss(3_661_000));
        assertEquals("00:00", CommissionScreen.mmss(-5_000));
    }

    @Test
    public void progress_showsOnlyServerStateAndElapsed() {
        CommissionScreen sending = CommissionScreen.of(CommissionStatus.sending("a"), START);
        assertEquals(CommissionScreen.Tone.PROGRESS, sending.tone);
        assertNull(sending.elapsed);
        assertEquals(CommissionScreen.Action.NONE, sending.primary);

        CommissionScreen pending = CommissionScreen.of(CommissionStatus.sending("a").accepted(START), START + 5_000);
        assertEquals("대기 중", pending.title);
        assertEquals("대기 중 · 00:05", pending.elapsed);

        CommissionScreen r = CommissionScreen.of(running(), START + 65_000);
        assertEquals("등록 중", r.title);
        assertEquals("등록 중 · 01:05", r.elapsed);
        assertEquals(CommissionScreen.Action.NONE, r.primary);
    }

    @Test
    public void success_mentionsRestored() {
        CommissionScreen plain = CommissionScreen.of(running().succeeded(false), START);
        assertEquals(CommissionScreen.Tone.SUCCESS, plain.tone);
        assertTrue(plain.body.contains("거실 플러그"));
        assertFalse(plain.body.contains("이어받"));
        assertEquals(CommissionScreen.Action.DONE, plain.primary);
        assertNull(plain.elapsed);

        assertTrue(CommissionScreen.of(running().succeeded(true), START).body.contains("이전 기록을 이어받았어요"));
    }

    @Test
    public void failure_byCode() {
        CommissionScreen already = CommissionScreen.of(running().failed("ALREADY_REGISTERED", "이미 등록된 기기입니다."), START);
        assertEquals("이미 등록된 기기예요", already.title);
        assertEquals(CommissionScreen.Action.OPEN_DEVICES, already.primary);

        CommissionScreen timeout = CommissionScreen.of(running().failed("COMMISSION_TIMEOUT", null), START);
        assertEquals(CommissionScreen.Tone.ERROR, timeout.tone);
        assertEquals(CommissionScreen.Action.BACK_TO_INPUT, timeout.primary);

        assertEquals("등록이 취소됐어요", CommissionScreen.of(running().failed("CANCELLED", null), START).title);

        CommissionScreen generic = CommissionScreen.of(running().failed("COMMISSION_FAILED", "기기 등록에 실패했습니다."), START);
        assertTrue(generic.body.startsWith("기기 등록에 실패했습니다. "));
        CommissionScreen noMessage = CommissionScreen.of(running().failed(null, null), START);
        assertTrue(noMessage.body.startsWith("기기 등록에 실패했습니다."));
    }

    @Test
    public void rejected_byHttpCode() {
        assertEquals("서버에 연결할 수 없어요",
                CommissionScreen.of(CommissionStatus.rejected("a", 0, null, null), START).title);
        assertEquals("다른 기기를 등록하고 있어요",
                CommissionScreen.of(CommissionStatus.rejected("a", 409, "COMMISSION_BUSY", "다른 기기 등록이 진행 중입니다."), START).title);
        // 서버 종료 중
        assertEquals("서버가 등록을 받을 수 없어요",
                CommissionScreen.of(CommissionStatus.rejected("a", 502, "DEVICE_UNREACHABLE", "기기가 응답하지 않습니다."), START).title);
        CommissionScreen bad = CommissionScreen.of(CommissionStatus.rejected("a", 400, "INVALID_REQUEST", "요청 형식이 올바르지 않습니다."), START);
        assertTrue(bad.body.startsWith("요청 형식이 올바르지 않습니다. "));
        assertEquals(CommissionScreen.Action.BACK_TO_INPUT, bad.primary);
        assertEquals("API 토큰이 맞지 않아요",
                CommissionScreen.of(CommissionStatus.rejected("a", 401, "UNAUTHORIZED", null), START).title);
        assertTrue(CommissionScreen.of(CommissionStatus.rejected("a", 500, null, null), START).body.contains("HTTP 500"));
    }

    @Test
    public void unconfirmedAndMissing_pointToList() {
        CommissionScreen lost = CommissionScreen.of(running().unconfirmed(), START);
        assertEquals(CommissionScreen.Tone.WARNING, lost.tone);
        assertEquals(CommissionScreen.Action.OPEN_DEVICES, lost.primary);
        assertTrue(lost.body.contains("다시 등록하기 전에"));
        assertEquals(CommissionScreen.Action.OPEN_DEVICES, CommissionScreen.of(null, START).primary);
    }
}
