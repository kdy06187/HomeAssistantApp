package com.example.homeassistantapp.network;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

    /** 오류를 어느 입력 칸 아래에 보일지. */
    public enum Field { NAME, PAYLOAD, WIFI, TYPE }

    /**
     * 검사 결과. error가 null이면 통과이고, payload는 서버에 보낼 정리된 값이다.
     * errors는 걸린 칸 전부(검사 순서대로), error·field는 그중 첫 번째.
     */
    public static final class Result {
        public final String error;
        public final Field field;
        public final String payload;
        public final Map<Field, String> errors;

        private Result(Map<Field, String> errors, String payload) {
            this.errors = Collections.unmodifiableMap(errors);
            Map.Entry<Field, String> first = errors.isEmpty() ? null : errors.entrySet().iterator().next();
            this.field = first != null ? first.getKey() : null;
            this.error = first != null ? first.getValue() : null;
            this.payload = first == null ? payload : null;
        }

        public boolean isValid() {
            return error == null;
        }

        static Result ok(String payload) {
            return new Result(new LinkedHashMap<>(), payload);
        }

        static Result fail(Field field, String error) {
            Map<Field, String> errors = new LinkedHashMap<>();
            errors.put(field, error);
            return new Result(errors, null);
        }
    }

    public static Result validate(String protocolType, String deviceName, String payload,
                                  String ssid, String password) {
        return validate(protocolType, deviceName, payload, ssid, password, null);
    }

    /**
     * serverHost는 앱이 접속한 서버 주소. TCP_DIY에서 IP를 비우면 서버가 이 주소를 기기에 알려 주는데,
     * LAN IPv4가 아니면 서버가 400으로 거절하므로 보내기 전에 막는다. null이면 이 검사를 건너뛴다.
     */
    public static Result validate(String protocolType, String deviceName, String payload,
                                  String ssid, String password, String serverHost) {
        // 한 번에 모든 칸의 오류를 보이도록 끝까지 검사한다.
        Map<Field, String> errors = new LinkedHashMap<>();
        String nameError = checkName(deviceName);
        if (nameError != null) errors.put(Field.NAME, nameError);

        String wifiError = checkWifi(ssid, password);
        if (wifiError != null) errors.put(Field.WIFI, wifiError);

        Result payloadResult = checkPayload(protocolType, payload == null ? "" : payload, serverHost);
        errors.putAll(payloadResult.errors);
        return errors.isEmpty() ? payloadResult : new Result(errors, null);
    }

    private static Result checkPayload(String protocolType, String raw, String serverHost) {
        if (hasControlChar(raw)) return Result.fail(Field.PAYLOAD, "페어링 코드·서버 IP에 줄바꿈 같은 제어 문자를 쓸 수 없습니다.");
        if ("MATTER".equals(protocolType)) return matterPayload(raw);
        if ("TCP_DIY".equals(protocolType)) return tcpPayload(raw, serverHost);
        return Result.fail(Field.TYPE, "알 수 없는 프로토콜입니다.");
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
        if (name == null || isBlank(name)) return "기기 이름을 입력해주세요.";
        if (hasControlChar(name)) return "기기 이름에 줄바꿈 같은 제어 문자를 쓸 수 없습니다.";
        if (name.codePointCount(0, name.length()) > NAME_MAX_CHARS) return "기기 이름은 64자까지 쓸 수 있습니다.";
        return null;
    }

    // 수동 코드는 "3497-011-2332"처럼 적어도 되도록 -·공백을 지운다. QR 내용은 그대로 보낸다.
    private static Result matterPayload(String raw) {
        if (raw.startsWith(QR_PREFIX)) {
            String body = raw.substring(QR_PREFIX.length());
            if (body.isEmpty()) return Result.fail(Field.PAYLOAD, "QR 내용이 비어 있습니다.");
            for (int i = 0; i < body.length(); i++) {
                char c = body.charAt(i);
                boolean ok = (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || c == '.' || c == '-';
                if (!ok) return Result.fail(Field.PAYLOAD, "QR 내용에 쓸 수 없는 문자가 있습니다.");
            }
            return Result.ok(raw);
        }
        String digits = raw.replace("-", "").replace(" ", "");
        if (digits.isEmpty()) return Result.fail(Field.PAYLOAD, "페어링 코드를 입력해주세요.");
        for (int i = 0; i < digits.length(); i++) {
            char c = digits.charAt(i);
            if (c < '0' || c > '9') return Result.fail(Field.PAYLOAD, "페어링 코드는 숫자 11자리 또는 21자리입니다(QR이면 MT:로 시작).");
        }
        if (digits.length() != 11 && digits.length() != 21) {
            return Result.fail(Field.PAYLOAD, "페어링 코드는 숫자 11자리 또는 21자리입니다(지금 " + digits.length() + "자리).");
        }
        return Result.ok(digits);
    }

    // 빈 값이면 서버가 앱이 접속한 서버 주소를 쓴다.
    private static Result tcpPayload(String raw, String serverHost) {
        String ip = raw.trim();
        if (ip.isEmpty()) {
            if (serverHost != null && !isLanIpv4(serverHost.trim())) {
                return Result.fail(Field.PAYLOAD, "지금 서버 주소가 집 안 IP가 아니라 비워 둘 수 없습니다. 기기가 접속할 서버 IP를 적어 주세요.");
            }
            return Result.ok("");
        }
        if (!isIpv4(ip)) return Result.fail(Field.PAYLOAD, "서버 IP는 192.168.0.10 같은 형식이어야 합니다(비우면 지금 접속한 서버 주소).");
        return Result.ok(ip);
    }

    /** 사설(10/8, 172.16/12, 192.168/16)·링크 로컬(169.254/16) IPv4인지. 서버가 받아 주는 범위와 같다. */
    public static boolean isLanIpv4(String host) {
        if (host == null || !isIpv4(host)) return false;
        String[] p = host.split("\\.");
        int a = Integer.parseInt(p[0]);
        int b = Integer.parseInt(p[1]);
        return a == 10 || (a == 172 && b >= 16 && b <= 31) || (a == 192 && b == 168) || (a == 169 && b == 254);
    }

    /** 각 자리 0~255, 앞에 0을 붙인 자리(01, 010)는 받지 않는다(8진수로 읽는 프로그램이 있어 서버와 어긋날 수 있다). */
    static boolean isIpv4(String ip) {
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) return false;
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) return false;
            if (part.length() > 1 && part.charAt(0) == '0') return false;
            for (int i = 0; i < part.length(); i++) {
                if (part.charAt(i) < '0' || part.charAt(i) > '9') return false;
            }
            if (Integer.parseInt(part) > 255) return false;
        }
        return true;
    }

    // 공백·전각 공백·줄 바꿈 없는 공백만 있는 이름은 목록에서 빈 이름처럼 보인다.
    private static boolean isBlank(String s) {
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            if (!Character.isWhitespace(cp) && !Character.isSpaceChar(cp)) return false;
            i += Character.charCount(cp);
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
