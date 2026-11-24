package com.example.homeassistantapp.network;

import java.nio.charset.StandardCharsets;

/**
 * 기기 등록 요청을 보내기 전에 서버와 같은 입력 규칙으로 검사한다. 걸리면 보내지 않고 바로 안내한다.
 * 서버 13b·계약 14번이 확정되면 규칙(길이·허용 문자)을 다시 맞춘다.
 */
public final class CommissionInputValidator {
    static final int NAME_MAX_CHARS = 64;
    static final int SSID_MAX_BYTES = 32;
    static final int PASSWORD_MAX_BYTES = 63;
    private static final String QR_PREFIX = "MT:";

    private CommissionInputValidator() {}

    /** 검사 결과. error가 null이면 통과이고, payload는 서버에 보낼 정리된 값이다. */
    public static final class Result {
        public final String error;
        public final String payload;

        private Result(String error, String payload) {
            this.error = error;
            this.payload = payload;
        }

        public boolean isValid() {
            return error == null;
        }

        static Result fail(String error) {
            return new Result(error, null);
        }
    }

    public static Result validate(String protocolType, String deviceName, String payload,
                                  String ssid, String password) {
        String nameError = checkName(deviceName);
        if (nameError != null) return Result.fail(nameError);

        String wifiError = checkWifi(ssid, password);
        if (wifiError != null) return Result.fail(wifiError);

        String raw = payload == null ? "" : payload;
        if (hasControlChar(raw)) return Result.fail("페어링 코드·서버 IP에 줄바꿈 같은 제어 문자를 쓸 수 없습니다.");

        if ("MATTER".equals(protocolType)) return matterPayload(raw);
        if ("TCP_DIY".equals(protocolType)) return tcpPayload(raw);
        return Result.fail("알 수 없는 프로토콜입니다.");
    }

    /** 설정 화면에서도 저장 전에 쓴다. 공백·따옴표는 허용하고 제어 문자와 길이만 막는다. */
    public static String checkWifi(String ssid, String password) {
        String s = ssid == null ? "" : ssid;
        String p = password == null ? "" : password;
        if (hasControlChar(s) || hasControlChar(p)) return "Wi-Fi 이름·비밀번호에 줄바꿈 같은 제어 문자를 쓸 수 없습니다.";
        if (utf8Length(s) > SSID_MAX_BYTES) return "Wi-Fi 이름이 너무 깁니다(최대 32바이트, 한글은 글자당 3바이트).";
        if (utf8Length(p) > PASSWORD_MAX_BYTES) return "Wi-Fi 비밀번호가 너무 깁니다(최대 63바이트).";
        return null;
    }

    static String checkName(String name) {
        if (name == null || name.isEmpty()) return "기기 이름을 입력해주세요.";
        if (hasControlChar(name)) return "기기 이름에 줄바꿈 같은 제어 문자를 쓸 수 없습니다.";
        if (name.codePointCount(0, name.length()) > NAME_MAX_CHARS) return "기기 이름은 64자까지 쓸 수 있습니다.";
        return null;
    }

    // 수동 코드는 "3497-011-2332"처럼 적어도 되도록 -·공백을 지운다. QR 내용은 그대로 보낸다.
    private static Result matterPayload(String raw) {
        if (raw.startsWith(QR_PREFIX)) {
            String body = raw.substring(QR_PREFIX.length());
            if (body.isEmpty()) return Result.fail("QR 내용이 비어 있습니다.");
            for (int i = 0; i < body.length(); i++) {
                char c = body.charAt(i);
                boolean ok = (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || c == '.' || c == '-';
                if (!ok) return Result.fail("QR 내용에 쓸 수 없는 문자가 있습니다.");
            }
            return new Result(null, raw);
        }
        String digits = raw.replace("-", "").replace(" ", "");
        if (digits.isEmpty()) return Result.fail("페어링 코드를 입력해주세요.");
        for (int i = 0; i < digits.length(); i++) {
            char c = digits.charAt(i);
            if (c < '0' || c > '9') return Result.fail("페어링 코드는 숫자 11자리 또는 21자리입니다(QR이면 MT:로 시작).");
        }
        if (digits.length() != 11 && digits.length() != 21) {
            return Result.fail("페어링 코드는 숫자 11자리 또는 21자리입니다(지금 " + digits.length() + "자리).");
        }
        return new Result(null, digits);
    }

    // 빈 값이면 서버가 앱이 접속한 서버 주소를 쓴다.
    private static Result tcpPayload(String raw) {
        String ip = raw.trim();
        if (ip.isEmpty()) return new Result(null, "");
        if (!isIpv4(ip)) return Result.fail("서버 IP는 192.168.0.10 같은 형식이어야 합니다(비우면 지금 접속한 서버 주소).");
        return new Result(null, ip);
    }

    static boolean isIpv4(String ip) {
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) return false;
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) return false;
            for (int i = 0; i < part.length(); i++) {
                if (part.charAt(i) < '0' || part.charAt(i) > '9') return false;
            }
            if (Integer.parseInt(part) > 255) return false;
        }
        return true;
    }

    static boolean hasControlChar(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isISOControl(s.charAt(i))) return true;
        }
        return false;
    }

    private static int utf8Length(String s) {
        return s.getBytes(StandardCharsets.UTF_8).length;
    }
}
