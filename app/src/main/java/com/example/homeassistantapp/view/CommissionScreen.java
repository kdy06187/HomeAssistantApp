package com.example.homeassistantapp.view;

import com.example.homeassistantapp.viewmodel.CommissionStatus;

import java.util.Locale;

/**
 * 등록 진행·결과 화면에 무엇을 보일지 정한다. 서버가 알려 준 상태와 경과 시간만 쓰고,
 * 서버가 주지 않는 세부 단계(스캔 중, 연결 중 …)는 지어내지 않는다.
 */
public final class CommissionScreen {
    public enum Tone { PROGRESS, SUCCESS, ERROR, WARNING }

    public enum Action {
        NONE,
        /** 결과를 확인하고 탭으로 돌아간다 */
        DONE,
        /** 입력 화면으로 돌아가 고쳐서 다시 보낸다 */
        BACK_TO_INPUT,
        /** 기기 탭에서 목록을 확인한다 */
        OPEN_DEVICES
    }

    public final Tone tone;
    public final String title;
    /** 무슨 일인지 + 어떻게 하면 되는지 */
    public final String body;
    /** "등록 중 · 01:05". 경과 시간이 없으면 null */
    public final String elapsed;
    public final Action primary;
    public final String primaryLabel;
    public final Action secondary;
    public final String secondaryLabel;

    private CommissionScreen(Tone tone, String title, String body, String elapsed,
                             Action primary, String primaryLabel, Action secondary, String secondaryLabel) {
        this.tone = tone;
        this.title = title;
        this.body = body;
        this.elapsed = elapsed;
        this.primary = primary;
        this.primaryLabel = primaryLabel;
        this.secondary = secondary;
        this.secondaryLabel = secondaryLabel;
    }

    private static CommissionScreen progress(String title, String body, String elapsed) {
        return new CommissionScreen(Tone.PROGRESS, title, body, elapsed, Action.NONE, null, Action.NONE, null);
    }

    private static CommissionScreen result(Tone tone, String title, String body,
                                           Action primary, String primaryLabel, Action secondary, String secondaryLabel) {
        return new CommissionScreen(tone, title, body, null, primary, primaryLabel, secondary, secondaryLabel);
    }

    private static final String KEEPS_GOING = "이 화면을 나가도 등록은 계속되고, 기기 추가 버튼으로 다시 볼 수 있어요.";

    public static CommissionScreen of(CommissionStatus status, long nowMs) {
        if (status == null) {
            return result(Tone.WARNING, "진행 중인 등록이 없어요", "기기 목록에서 등록 결과를 확인해 주세요.",
                    Action.OPEN_DEVICES, "기기 목록 보기", Action.NONE, null);
        }
        String elapsed = status.startedAtMs > 0 ? mmss(nowMs - status.startedAtMs) : null;
        switch (status.phase) {
            case SENDING:
                return progress("등록 요청 중", "서버에 등록을 요청하고 있어요.", null);
            case PENDING:
                return progress("대기 중", "서버가 등록을 시작하기를 기다리고 있어요. " + KEEPS_GOING,
                        elapsed == null ? null : "대기 중 · " + elapsed);
            case RUNNING:
                return progress("등록 중", "서버가 기기를 등록하고 있어요. " + KEEPS_GOING,
                        elapsed == null ? null : "등록 중 · " + elapsed);
            case SUCCEEDED:
                return result(Tone.SUCCESS, "등록했어요",
                        quoted(status.deviceName) + " 기기를 목록에 추가했어요."
                                + (status.restored ? " 이전 기록을 이어받았어요." : ""),
                        Action.DONE, "확인", Action.NONE, null);
            case FAILED:
                return failed(status);
            case REJECTED:
                return rejected(status);
            case UNCONFIRMED:
            default:
                return result(Tone.WARNING, "결과를 확인하지 못했어요",
                        "등록 결과를 서버에서 받지 못했어요. 같은 기기를 다시 등록하기 전에 기기 목록에서 먼저 확인해 주세요.",
                        Action.OPEN_DEVICES, "기기 목록 보기", Action.DONE, "닫기");
        }
    }

    private static CommissionScreen failed(CommissionStatus status) {
        String code = status.errorCode == null ? "" : status.errorCode;
        switch (code) {
            case "ALREADY_REGISTERED":
                return result(Tone.WARNING, "이미 등록된 기기예요",
                        "이 기기는 이미 목록에 있어요. 다시 등록하려면 목록에서 먼저 삭제해 주세요.",
                        Action.OPEN_DEVICES, "기기 목록 보기", Action.DONE, "닫기");
            case "COMMISSION_TIMEOUT":
                return result(Tone.ERROR, "등록 시간이 지났어요",
                        "기기가 제한 시간 안에 응답하지 않았어요. 기기를 등록 대기 상태로 두고 가까이에서 다시 시도해 주세요.",
                        Action.BACK_TO_INPUT, "다시 시도", Action.DONE, "닫기");
            case "CANCELLED":
                return result(Tone.ERROR, "등록이 취소됐어요",
                        "서버에서 등록 작업이 취소됐어요. 잠시 뒤 다시 시도해 주세요.",
                        Action.BACK_TO_INPUT, "다시 시도", Action.DONE, "닫기");
            default:
                return result(Tone.ERROR, "등록하지 못했어요",
                        orDefault(status.serverMessage, "기기 등록에 실패했습니다.")
                                + " 기기 전원과 Wi-Fi(2.4GHz)를 확인하고 다시 시도해 주세요.",
                        Action.BACK_TO_INPUT, "다시 시도", Action.DONE, "닫기");
        }
    }

    private static CommissionScreen rejected(CommissionStatus status) {
        String title;
        String body;
        if (status.httpCode == 0) {
            title = "서버에 연결할 수 없어요";
            body = "등록 요청을 보내지 못했어요. Wi-Fi와 서버 주소를 확인하고 다시 시도해 주세요.";
        } else if (status.httpCode == 401) {
            title = "API 토큰이 맞지 않아요";
            body = "서버가 요청을 거절했어요. 설정에서 토큰을 다시 입력한 뒤 시도해 주세요.";
        } else if (status.httpCode == 409 || "COMMISSION_BUSY".equals(status.errorCode)) {
            title = "다른 기기를 등록하고 있어요";
            body = "서버는 한 번에 한 기기만 등록해요. 진행 중인 등록이 끝난 뒤 다시 시도해 주세요.";
        } else if (status.httpCode == 502 || "DEVICE_UNREACHABLE".equals(status.errorCode)) {
            // 등록 요청의 502는 서버가 종료되는 중일 때 온다.
            title = "서버가 등록을 받을 수 없어요";
            body = "서버가 종료되거나 다시 시작하는 중이에요. 잠시 뒤 다시 시도해 주세요.";
        } else if (status.httpCode == 400) {
            title = "서버가 입력을 받지 않았어요";
            body = orDefault(status.serverMessage, "요청 형식이 올바르지 않습니다.") + " 입력한 값을 확인하고 다시 시도해 주세요.";
        } else {
            title = "등록을 시작하지 못했어요";
            body = orDefault(status.serverMessage, "서버 오류(HTTP " + status.httpCode + ")가 났어요.") + " 잠시 뒤 다시 시도해 주세요.";
        }
        return result(Tone.ERROR, title, body, Action.BACK_TO_INPUT, "입력으로 돌아가기", Action.DONE, "닫기");
    }

    /** 경과 시간 mm:ss. 음수는 0으로, 60분을 넘으면 분이 계속 늘어난다. */
    public static String mmss(long ms) {
        long seconds = Math.max(0, ms) / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private static String quoted(String name) {
        return "‘" + (name == null ? "" : name) + "’";
    }

    private static String orDefault(String message, String fallback) {
        if (message == null || message.trim().isEmpty()) return fallback;
        String m = message.trim();
        return m.endsWith(".") || m.endsWith("요") ? m : m + ".";
    }
}
