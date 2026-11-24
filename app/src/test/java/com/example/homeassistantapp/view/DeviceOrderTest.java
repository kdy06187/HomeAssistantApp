package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;

import com.example.homeassistantapp.dto.Device;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeviceOrderTest {

    static Device device(String id) {
        Device d = new Device();
        d.setId(id);
        d.setName(id);
        return d;
    }

    static Device pairing() {
        Device d = device("TEMP_PAIRING");
        d.setPairing(true);
        return d;
    }

    private static List<String> ids(List<Device> devices) {
        List<String> ids = new ArrayList<>();
        for (Device d : devices) ids.add(d.getId());
        return ids;
    }

    @Test
    public void sort_followsSavedOrder() {
        List<Device> list = Arrays.asList(device("a"), device("b"), device("c"));
        assertEquals(Arrays.asList("c", "a", "b"), ids(DeviceOrder.sort(list, "c,a,b,")));
    }

    @Test
    public void sort_putsNewDevicesLastInServerOrder() {
        List<Device> list = Arrays.asList(device("new1"), device("a"), device("new2"), device("b"));
        assertEquals(Arrays.asList("b", "a", "new1", "new2"), ids(DeviceOrder.sort(list, "b,a,")));
    }

    @Test
    public void sort_keepsPairingItemFirst() {
        List<Device> list = Arrays.asList(device("a"), pairing(), device("b"));
        assertEquals(Arrays.asList("TEMP_PAIRING", "b", "a"), ids(DeviceOrder.sort(list, "b,a,")));
    }

    @Test
    public void sort_withoutSavedOrder_keepsServerOrder() {
        List<Device> list = Arrays.asList(device("b"), device("a"));
        assertEquals(Arrays.asList("b", "a"), ids(DeviceOrder.sort(list, "")));
        assertEquals(Arrays.asList("b", "a"), ids(DeviceOrder.sort(list, null)));
    }

    @Test
    public void sort_doesNotChangeInputAndAcceptsNull() {
        List<Device> list = new ArrayList<>(Arrays.asList(device("a"), device("b")));
        DeviceOrder.sort(list, "b,a,");
        assertEquals(Arrays.asList("a", "b"), ids(list));
        assertEquals(0, DeviceOrder.sort(null, "a,").size());
    }

    @Test
    public void serialize_skipsPairingItem() {
        List<Device> list = Arrays.asList(pairing(), device("b"), device("a"));
        assertEquals("b,a,", DeviceOrder.serialize(list));
    }
}
