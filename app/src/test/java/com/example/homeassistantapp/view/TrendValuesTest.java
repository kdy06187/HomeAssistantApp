package com.example.homeassistantapp.view;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.homeassistantapp.dto.EnergyBucket;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TrendValuesTest {
    private static final float E = 0.001f;

    private static EnergyBucket bucket(long mWh, long samples) {
        EnergyBucket b = new EnergyBucket();
        b.energyUsedMwh = mWh;
        b.sampleCount = samples;
        return b;
    }

    @Test
    public void fromBuckets_emptySlotIsNull() {
        TrendValues t = TrendValues.fromBuckets(Arrays.asList(bucket(100, 5), bucket(0, 0)));
        assertEquals(Long.valueOf(100), t.values.get(0));
        assertEquals(null, t.values.get(1));
        assertFalse(t.isEmpty());
    }

    @Test
    public void noData_isEmpty() {
        assertTrue(TrendValues.fromBuckets(null).isEmpty());
        assertTrue(TrendValues.fromBuckets(Arrays.asList(bucket(0, 0))).isEmpty());
    }

    @Test
    public void polyline_scalesToMaxAndWidth() {
        TrendValues t = new TrendValues(Arrays.asList(0L, 50L, 100L));
        List<float[]> p = t.polylines(100f, 20f);
        assertEquals(1, p.size());
        assertArrayEquals(new float[]{0f, 20f, 50f, 10f, 100f, 0f}, p.get(0), E);
    }

    @Test
    public void polyline_breaksAtEmptySlot() {
        TrendValues t = new TrendValues(Arrays.asList(10L, null, 10L, 5L));
        List<float[]> p = t.polylines(30f, 10f);
        assertEquals(2, p.size());
        assertArrayEquals(new float[]{0f, 0f}, p.get(0), E);
        assertArrayEquals(new float[]{20f, 0f, 30f, 5f}, p.get(1), E);
    }

    @Test
    public void polyline_singleValueIsCentered() {
        List<float[]> p = new TrendValues(Collections.singletonList(7L)).polylines(40f, 10f);
        assertArrayEquals(new float[]{20f, 0f}, p.get(0), E);
    }

    @Test
    public void polyline_allZeroIsFlatAtBottom() {
        List<float[]> p = new TrendValues(Arrays.asList(0L, 0L)).polylines(10f, 8f);
        assertArrayEquals(new float[]{0f, 8f, 10f, 8f}, p.get(0), E);
    }

    @Test
    public void polyline_noSizeOrNoValues_isEmpty() {
        assertTrue(new TrendValues(Arrays.asList(1L, 2L)).polylines(0f, 10f).isEmpty());
        assertTrue(new TrendValues(Collections.<Long>emptyList()).polylines(10f, 10f).isEmpty());
    }
}
