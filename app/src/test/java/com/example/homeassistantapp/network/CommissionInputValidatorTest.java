package com.example.homeassistantapp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// Wi-Fi 값은 테스트용 가짜 값.
public class CommissionInputValidatorTest {

    private static CommissionInputValidator.Result matter(String payload) {
        return CommissionInputValidator.validate("MATTER", "플러그", payload, "MyWiFi", "pw");
    }

    private static CommissionInputValidator.Result tcp(String payload) {
        return CommissionInputValidator.validate("TCP_DIY", "전등", payload, "MyWiFi", "pw");
    }

    private static String repeat(String s, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(s);
        return b.toString();
    }

    @Test
    public void matterManualCode_11or21Digits() {
        assertEquals("34970112332", matter("34970112332").payload);
        assertEquals("123456789012345678901", matter("123456789012345678901").payload);
        assertFalse(matter("3497011233").isValid());      // 10자리
        assertFalse(matter("349701123321").isValid());    // 12자리
        assertFalse(matter("3497011233a").isValid());
        assertFalse(matter("").isValid());
    }

    @Test
    public void matterManualCode_dashesAndSpacesRemoved() {
        assertEquals("34970112332", matter("3497-011-2332").payload);
        assertEquals("34970112332", matter("3497 011 2332").payload);
    }

    @Test
    public void matterQr_allowedChars() {
        assertEquals("MT:Y.K9042C00KA0648G00", matter("MT:Y.K9042C00KA0648G00").payload);
        assertEquals("MT:ABC-123", matter("MT:ABC-123").payload);
        assertFalse(matter("MT:").isValid());
        assertFalse(matter("MT:abc").isValid());          // 소문자 불가
        assertFalse(matter("MT:AB C").isValid());         // 공백 불가
    }

    @Test
    public void tcpPayload_ipv4OrEmpty() {
        assertEquals("", tcp("").payload);
        assertEquals("", tcp("  ").payload);
        assertEquals("192.168.0.10", tcp("192.168.0.10").payload);
        assertEquals("192.168.0.10", tcp(" 192.168.0.10 ").payload);
        assertTrue(tcp("0.0.0.0").isValid());
        assertFalse(tcp("256.1.1.1").isValid());
        assertFalse(tcp("192.168.0").isValid());
        assertFalse(tcp("192.168.0.1.5").isValid());
        assertFalse(tcp("192.168..1").isValid());
        assertFalse(tcp("server.local").isValid());
        assertFalse(tcp("1234.1.1.1").isValid());
    }

    @Test
    public void deviceName_1to64Chars() {
        assertTrue(CommissionInputValidator.validate("MATTER", repeat("가", 64), "34970112332", "w", "p").isValid());
        assertFalse(CommissionInputValidator.validate("MATTER", repeat("가", 65), "34970112332", "w", "p").isValid());
        assertFalse(CommissionInputValidator.validate("MATTER", "", "34970112332", "w", "p").isValid());
        assertFalse(CommissionInputValidator.validate("MATTER", null, "34970112332", "w", "p").isValid());
    }

    @Test
    public void controlChars_rejectedEverywhere() {
        assertFalse(CommissionInputValidator.validate("MATTER", "플\n러그", "34970112332", "w", "p").isValid());
        assertFalse(CommissionInputValidator.validate("MATTER", "플러그", "34970112332\n", "w", "p").isValid());
        assertFalse(CommissionInputValidator.validate("TCP_DIY", "전등", "192.168.0.10\t", "w", "p").isValid());
        assertNotNull(CommissionInputValidator.checkWifi("My\tWiFi", "pw"));
        assertNotNull(CommissionInputValidator.checkWifi("MyWiFi", "p\u0000w"));
        assertNotNull(CommissionInputValidator.checkWifi("MyWiFi\r", "pw"));
    }

    @Test
    public void wifi_spacesAndQuotesAllowed() {
        assertNull(CommissionInputValidator.checkWifi(" My \"Home\" WiFi ", "pass 'word' "));
        assertNull(CommissionInputValidator.checkWifi("", ""));
        assertNull(CommissionInputValidator.checkWifi(null, null));
    }

    @Test
    public void wifi_byteLimits() {
        assertNull(CommissionInputValidator.checkWifi(repeat("a", 32), "p"));
        assertNotNull(CommissionInputValidator.checkWifi(repeat("a", 33), "p"));
        // 한글은 UTF-8로 3바이트: 10자 = 30바이트, 11자 = 33바이트
        assertNull(CommissionInputValidator.checkWifi(repeat("집", 10), "p"));
        assertNotNull(CommissionInputValidator.checkWifi(repeat("집", 11), "p"));
        assertNull(CommissionInputValidator.checkWifi("w", repeat("p", 63)));
        assertNotNull(CommissionInputValidator.checkWifi("w", repeat("p", 64)));
    }

    @Test
    public void wifiErrorBlocksCommission() {
        assertFalse(CommissionInputValidator.validate("MATTER", "플러그", "34970112332", repeat("a", 33), "p").isValid());
    }

    @Test
    public void unknownProtocol_rejected() {
        assertFalse(CommissionInputValidator.validate("ZIGBEE", "x", "1", "w", "p").isValid());
    }

    @Test
    public void errorsPointToTheirField() {
        assertEquals(CommissionInputValidator.Field.NAME,
                CommissionInputValidator.validate("MATTER", "", "34970112332", "w", "p").field);
        assertEquals(CommissionInputValidator.Field.WIFI,
                CommissionInputValidator.validate("MATTER", "플러그", "34970112332", repeat("a", 33), "p").field);
        assertEquals(CommissionInputValidator.Field.PAYLOAD, matter("123").field);
        assertEquals(CommissionInputValidator.Field.PAYLOAD, tcp("256.1.1.1").field);
        assertEquals(CommissionInputValidator.Field.TYPE,
                CommissionInputValidator.validate("ZIGBEE", "x", "1", "w", "p").field);
        assertNull(matter("34970112332").field);
    }

    @Test
    public void tcpEmptyPayload_needsLanServerAddress() {
        assertTrue(tcpVia("", "192.168.219.108").isValid());
        assertTrue(tcpVia("", "10.0.0.5").isValid());
        assertTrue(tcpVia("", "172.16.0.1").isValid());
        assertTrue(tcpVia("", "172.31.255.255").isValid());
        assertTrue(tcpVia("", "169.254.1.1").isValid());
        assertEquals(CommissionInputValidator.Field.PAYLOAD, tcpVia("", "8.8.8.8").field);
        assertFalse(tcpVia("", "172.32.0.1").isValid());
        assertFalse(tcpVia("", "myserver.local").isValid());
        assertFalse(tcpVia("  ", "127.0.0.1").isValid());
        // IP를 적었으면 서버 주소와 상관없다.
        assertTrue(tcpVia("192.168.0.10", "myserver.local").isValid());
        // 서버 주소를 모르면(null) 이 검사는 건너뛴다.
        assertTrue(tcpVia("", null).isValid());
    }

    @Test
    public void lanIpv4Ranges() {
        assertTrue(CommissionInputValidator.isLanIpv4("192.168.0.1"));
        assertFalse(CommissionInputValidator.isLanIpv4("192.169.0.1"));
        assertFalse(CommissionInputValidator.isLanIpv4("172.15.0.1"));
        assertFalse(CommissionInputValidator.isLanIpv4(""));
        assertFalse(CommissionInputValidator.isLanIpv4(null));
        assertFalse(CommissionInputValidator.isLanIpv4("10.0.0"));
    }

    private static CommissionInputValidator.Result tcpVia(String payload, String serverHost) {
        return CommissionInputValidator.validate("TCP_DIY", "전등", payload, "MyWiFi", "pw", serverHost);
    }

    @Test
    public void reportsEveryField_firstStaysAsField() {
        CommissionInputValidator.Result r = CommissionInputValidator.validate(
                "MATTER", "", "12", repeat("a", 33), "p");
        assertFalse(r.isValid());
        assertEquals(3, r.errors.size());
        assertTrue(r.errors.containsKey(CommissionInputValidator.Field.NAME));
        assertTrue(r.errors.containsKey(CommissionInputValidator.Field.WIFI));
        assertTrue(r.errors.containsKey(CommissionInputValidator.Field.PAYLOAD));
        assertEquals(CommissionInputValidator.Field.NAME, r.field);
        assertEquals(r.errors.get(CommissionInputValidator.Field.NAME), r.error);
        assertNull(r.payload);
    }

    @Test
    public void valid_hasNoErrors() {
        CommissionInputValidator.Result r = matter("34970112332");
        assertTrue(r.errors.isEmpty());
        assertNull(r.field);
    }

    @Test
    public void blankName_rejected() {
        assertFalse(CommissionInputValidator.validate("MATTER", "   ", "34970112332", "w", "p").isValid());
        assertFalse(CommissionInputValidator.validate("MATTER", "　", "34970112332", "w", "p").isValid());
        assertFalse(CommissionInputValidator.validate("MATTER", " ", "34970112332", "w", "p").isValid());
        assertTrue(CommissionInputValidator.validate("MATTER", " 거실 등 ", "34970112332", "w", "p").isValid());
    }

    @Test
    public void ipv4_leadingZeroRejected() {
        assertFalse(CommissionInputValidator.isIpv4("192.168.01.10"));
        assertFalse(CommissionInputValidator.isIpv4("010.0.0.1"));
        assertFalse(CommissionInputValidator.isIpv4("10.0.0.00"));
        assertTrue(CommissionInputValidator.isIpv4("10.0.0.0"));
        assertTrue(CommissionInputValidator.isIpv4("192.168.100.10"));
        assertFalse(tcp("192.168.0.010").isValid());
    }
}
