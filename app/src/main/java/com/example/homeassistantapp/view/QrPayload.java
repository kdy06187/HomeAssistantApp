package com.example.homeassistantapp.view;

import com.example.homeassistantapp.network.CommissionInputValidator;

/** 카메라로 읽은 QR 글자를 등록에 쓸 수 있는지 판단한다. QR 내용은 로그에 남기거나 저장하지 않는다. */
public final class QrPayload {
    private static final String PREFIX = "MT:";
    private static final int SHOWN_CHARS = 6;

    public enum Check { OK, EMPTY, NOT_MATTER, BAD_CHARS }

    private QrPayload() {}

    public static Check check(String text) {
        if (text == null || text.isEmpty()) return Check.EMPTY;
        if (!text.startsWith(PREFIX)) return Check.NOT_MATTER;
        // 허용 문자 규칙은 등록 검사기와 같아야 하므로 그쪽에 맡긴다.
        boolean ok = CommissionInputValidator.validate("MATTER", "x", text, "", "").isValid();
        return ok ? Check.OK : Check.BAD_CHARS;
    }

    /** 화면에는 앞 몇 자만 보인다. 예: "MT:Y3.…" */
    public static String masked(String text) {
        if (text == null) return "";
        if (text.length() <= SHOWN_CHARS) return text;
        return text.substring(0, SHOWN_CHARS) + "…";
    }

    /** 스캔 화면 아래에 보일 안내. OK면 null. */
    public static String rejectMessage(Check check) {
        switch (check) {
            case NOT_MATTER: return "Matter QR이 아니에요. 기기나 설명서의 Matter 표시 옆 QR을 비춰 주세요.";
            case BAD_CHARS: return "읽은 QR 내용이 올바르지 않아요. 다시 비추거나 코드를 직접 입력해 주세요.";
            case EMPTY: return "QR 내용이 비어 있어요.";
            default: return null;
        }
    }
}
