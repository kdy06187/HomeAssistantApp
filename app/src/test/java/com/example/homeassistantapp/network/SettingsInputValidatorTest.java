package com.example.homeassistantapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;

public class SettingsInputValidatorTest {
    private static Map<SettingsInputValidator.Field, String> valid(String ip, String port) {
        return SettingsInputValidator.validate(ip, port, "", true, "MyWiFi", "pw");
    }

    @Test
    public void allGood_noErrors() {
        assertTrue(valid("192.168.219.108", "8000").isEmpty());
        assertTrue(valid(" 10.0.0.2 ", " 8000 ").isEmpty());
    }

    @Test
    public void portBoundaries() {
        assertNotNull(SettingsInputValidator.checkPort("0"));
        assertNull(SettingsInputValidator.checkPort("1"));
        assertNull(SettingsInputValidator.checkPort("65535"));
        assertNotNull(SettingsInputValidator.checkPort("65536"));
        assertNotNull(SettingsInputValidator.checkPort("99999"));
        assertNotNull(SettingsInputValidator.checkPort("123456"));
        assertNotNull(SettingsInputValidator.checkPort("80a"));
        assertNotNull(SettingsInputValidator.checkPort("-1"));
        assertNotNull(SettingsInputValidator.checkPort(""));
        assertNotNull(SettingsInputValidator.checkPort(null));
    }

    @Test
    public void ipFormat() {
        assertNull(SettingsInputValidator.checkIp("192.168.0.10"));
        assertNotNull(SettingsInputValidator.checkIp(""));
        assertNotNull(SettingsInputValidator.checkIp("256.1.1.1"));
        assertNotNull(SettingsInputValidator.checkIp("192.168.0"));
        assertNotNull(SettingsInputValidator.checkIp("myserver.local"));
        assertNotNull(SettingsInputValidator.checkIp("http://192.168.0.10"));
    }

    @Test
    public void token_requiredOnlyWithoutSavedToken() {
        assertNull(SettingsInputValidator.checkToken("", true));
        assertNotNull(SettingsInputValidator.checkToken("", false));
        assertNotNull(SettingsInputValidator.checkToken("  ", false));
        assertNull(SettingsInputValidator.checkToken(" abc123 \n", false));
        assertNotNull(SettingsInputValidator.checkToken("ab c", true));
        assertNotNull(SettingsInputValidator.checkToken("토큰", true));
    }

    @Test
    public void everyBadFieldReportedAtOnce() {
        Map<SettingsInputValidator.Field, String> errors = SettingsInputValidator.validate(
                "", "70000", "", false, "a\nb", "p\tw");
        assertEquals(5, errors.size());
        assertTrue(errors.containsKey(SettingsInputValidator.Field.SERVER_IP));
        assertTrue(errors.containsKey(SettingsInputValidator.Field.SERVER_PORT));
        assertTrue(errors.containsKey(SettingsInputValidator.Field.API_TOKEN));
        assertTrue(errors.containsKey(SettingsInputValidator.Field.WIFI_SSID));
        assertTrue(errors.containsKey(SettingsInputValidator.Field.WIFI_PASSWORD));
    }

    @Test
    public void wifiLengthsPerField() {
        StringBuilder longSsid = new StringBuilder();
        for (int i = 0; i < 33; i++) longSsid.append('a');
        Map<SettingsInputValidator.Field, String> errors = SettingsInputValidator.validate(
                "192.168.0.10", "8000", "", true, longSsid.toString(), "");
        assertEquals(1, errors.size());
        assertTrue(errors.containsKey(SettingsInputValidator.Field.WIFI_SSID));
        assertTrue(SettingsInputValidator.validate("192.168.0.10", "8000", "", true, "", "").isEmpty());
    }

    @Test
    public void port_leadingZeroRejected() {
        assertNotNull(SettingsInputValidator.checkPort("08000"));
        assertNotNull(SettingsInputValidator.checkPort("00"));
        assertNull(SettingsInputValidator.checkPort("8000"));
        assertNotNull(valid("192.168.0.010", "8000").get(SettingsInputValidator.Field.SERVER_IP));
    }

    @Test
    public void token_innerNewlineRejected_surroundingTrimmed() {
        assertNotNull(SettingsInputValidator.checkToken("abc\ndef", false));
        assertNotNull(SettingsInputValidator.checkToken("abc def", true));
        assertNull(SettingsInputValidator.checkToken("\n abcdef \r\n", false));
        // 칸이 비면 저장된 토큰이 있을 때만 통과한다.
        assertNull(SettingsInputValidator.checkToken(" \n", true));
        assertNotNull(SettingsInputValidator.checkToken(" \n", false));
    }
}
