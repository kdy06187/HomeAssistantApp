package com.example.homeassistantapp.network;

import com.example.homeassistantapp.security.TokenFormat;

import java.util.EnumMap;
import java.util.Map;

/** 설정 화면 입력을 칸별로 검사한다. 걸린 칸마다 문구를 하나씩 돌려줘 한 번에 모두 보이게 한다. */
public final class SettingsInputValidator {
    public enum Field { SERVER_IP, SERVER_PORT, API_TOKEN, WIFI_SSID, WIFI_PASSWORD }

    private SettingsInputValidator() {}

    /**
     * token은 이번에 새로 입력한 값(정리 전). 비어 있으면 저장된 토큰을 그대로 쓴다는 뜻이고,
     * 저장된 토큰도 없을 때만 오류다. 빈 결과면 통과.
     */
    public static Map<Field, String> validate(String ip, String port, String token, boolean hasSavedToken,
                                              String ssid, String password) {
        Map<Field, String> errors = new EnumMap<>(Field.class);
        String ipError = checkIp(ip);
        if (ipError != null) errors.put(Field.SERVER_IP, ipError);
        String portError = checkPort(port);
        if (portError != null) errors.put(Field.SERVER_PORT, portError);
        String tokenError = checkToken(token, hasSavedToken);
        if (tokenError != null) errors.put(Field.API_TOKEN, tokenError);
        String ssidError = checkSsid(ssid);
        if (ssidError != null) errors.put(Field.WIFI_SSID, ssidError);
        String passwordError = checkPassword(password);
        if (passwordError != null) errors.put(Field.WIFI_PASSWORD, passwordError);
        return errors;
    }

    static String checkIp(String ip) {
        String v = ip == null ? "" : ip.trim();
        if (v.isEmpty()) return "서버 IP를 입력해 주세요.";
        if (!CommissionInputValidator.isIpv4(v)) return "192.168.0.10 같은 IPv4 주소로 입력해 주세요.";
        return null;
    }

    static String checkPort(String port) {
        String v = port == null ? "" : port.trim();
        if (v.isEmpty()) return "포트 번호를 입력해 주세요.";
        if (v.length() > 5) return "포트는 1~65535 사이 숫자예요.";
        for (int i = 0; i < v.length(); i++) {
            if (v.charAt(i) < '0' || v.charAt(i) > '9') return "포트는 숫자만 쓸 수 있어요.";
        }
        if (v.length() > 1 && v.charAt(0) == '0') return "포트 앞에 0을 붙이지 말아 주세요(예: 8000).";
        int n = Integer.parseInt(v);
        if (n < 1 || n > 65535) return "포트는 1~65535 사이 숫자예요.";
        return null;
    }

    static String checkToken(String token, boolean hasSavedToken) {
        String v = TokenFormat.normalize(token);
        if (v.isEmpty()) return hasSavedToken ? null : "서버 API 토큰을 입력해 주세요.";
        if (!TokenFormat.isValid(v)) return "영문·숫자·기호만 쓸 수 있어요(공백·줄바꿈·한글 불가).";
        return null;
    }

    // Wi-Fi 규칙은 등록 요청과 같아야 하므로 등록 검사기의 문구를 그대로 쓴다.
    static String checkSsid(String ssid) {
        return CommissionInputValidator.checkWifi(ssid, "");
    }

    static String checkPassword(String password) {
        return CommissionInputValidator.checkWifi("", password);
    }
}
