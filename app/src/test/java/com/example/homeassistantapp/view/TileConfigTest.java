package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TileConfigTest {

    @Test
    public void default_matchesCurrentTileLook() {
        TileConfig d = TileConfig.DEFAULT;
        assertTrue(d.power);
        assertTrue(d.total);
        assertFalse(d.trend);
        assertEquals(1, d.span);
    }

    @Test
    public void json_roundTrip() {
        TileConfig c = new TileConfig(false, true, true, 2);
        assertEquals("{\"v\":1,\"power\":false,\"total\":true,\"trend\":true,\"span\":2}", c.toJson());
        assertEquals(c, TileConfig.fromJson(c.toJson()));
    }

    @Test
    public void missingOrBroken_givesDefault() {
        assertEquals(TileConfig.DEFAULT, TileConfig.fromJson(null));
        assertEquals(TileConfig.DEFAULT, TileConfig.fromJson(""));
        assertEquals(TileConfig.DEFAULT, TileConfig.fromJson("{not json"));
        assertEquals(TileConfig.DEFAULT, TileConfig.fromJson("[1,2]"));
        assertEquals(TileConfig.DEFAULT, TileConfig.fromJson("{\"power\":\"yes\"}"));
    }

    @Test
    public void missingField_usesDefaultForThatFieldOnly() {
        TileConfig c = TileConfig.fromJson("{\"v\":1,\"trend\":true}");
        assertEquals(new TileConfig(true, true, true, 1), c);
    }

    @Test
    public void unknownFields_areIgnored() {
        TileConfig c = TileConfig.fromJson("{\"v\":2,\"power\":false,\"color\":\"blue\"}");
        assertEquals(new TileConfig(false, true, false, 1), c);
    }

    @Test
    public void invalidSpan_becomesOne() {
        assertEquals(1, TileConfig.fromJson("{\"span\":3}").span);
        assertEquals(1, new TileConfig(true, true, false, 0).span);
    }

    @Test
    public void tcpDiy_onlySize() {
        assertFalse(TileConfig.supportsEnergy("TCP_DIY"));
        assertTrue(TileConfig.supportsEnergy("MATTER"));
        TileConfig e = new TileConfig(true, true, true, 2).effectiveFor("TCP_DIY");
        assertEquals(new TileConfig(false, false, false, 2), e);
        TileConfig m = new TileConfig(true, false, true, 1);
        assertEquals(m, m.effectiveFor("MATTER"));
    }

    @Test
    public void withers_changeOneField() {
        TileConfig c = TileConfig.DEFAULT.withTrend(true).withSpan(2).withPower(false).withTotal(false);
        assertEquals(new TileConfig(false, false, true, 2), c);
    }

    @Test
    public void storeKey_perDevice() {
        assertEquals("tile.SN12345", TileConfigStore.keyFor("SN12345"));
    }
}
