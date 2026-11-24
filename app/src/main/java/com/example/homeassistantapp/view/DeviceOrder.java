package com.example.homeassistantapp.view;

import com.example.homeassistantapp.dto.Device;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 홈·기기 탭이 함께 쓰는 기기 순서. "id1,id2,"처럼 쉼표로 이어 SharedPreferences에 저장한다. */
public final class DeviceOrder {
    public static final String PREFS_NAME = "DashboardPrefs";
    public static final String KEY_ORDER = "device_order";

    private DeviceOrder() {}

    /** 등록 중 임시 항목은 맨 앞, 저장된 순서에 없는 새 기기는 맨 뒤. 원본 목록은 바꾸지 않는다. */
    public static List<Device> sort(List<Device> devices, String saved) {
        List<Device> sorted = devices == null ? new ArrayList<>() : new ArrayList<>(devices);
        List<String> order = saved == null || saved.isEmpty()
                ? Collections.emptyList() : Arrays.asList(saved.split(","));
        Collections.sort(sorted, (a, b) -> Integer.compare(rank(a, order), rank(b, order)));
        return sorted;
    }

    public static String serialize(List<Device> devices) {
        StringBuilder sb = new StringBuilder();
        for (Device device : devices) {
            if (isPairing(device)) continue;
            sb.append(device.getId()).append(",");
        }
        return sb.toString();
    }

    static boolean isPairing(Device device) {
        return device.isPairing() || "TEMP_PAIRING".equals(device.getId());
    }

    private static int rank(Device device, List<String> order) {
        if (isPairing(device)) return -1;
        int index = order.indexOf(device.getId());
        return index == -1 ? Integer.MAX_VALUE : index;
    }
}
