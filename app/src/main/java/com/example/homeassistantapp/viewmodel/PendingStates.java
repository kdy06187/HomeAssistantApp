package com.example.homeassistantapp.viewmodel;

import com.example.homeassistantapp.dto.Device;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 기기별로 진행 중인 요청 수와 마지막 실제 상태를 기억한다.
 * 같은 기기에 요청이 겹쳐도, 마지막 요청이 끝나면 LOADING이 반드시 풀린다.
 */
final class PendingStates {
    static final String LOADING = "LOADING";

    private final Map<String, Integer> counts = new HashMap<>();
    private final Map<String, String> lastKnown = new HashMap<>();

    /** 요청 시작. 지금 상태가 실제 값이면 실패 때 되돌릴 값으로 기억한다. */
    void begin(String deviceId, String currentState) {
        remember(deviceId, currentState);
        Integer count = counts.get(deviceId);
        counts.put(deviceId, count == null ? 1 : count + 1);
    }

    /**
     * 요청 하나가 끝났을 때 보여 줄 상태.
     * newState는 서버가 알려 준 상태(실패면 null). 남은 요청이 있으면 LOADING을 유지한다.
     */
    String finish(String deviceId, String newState) {
        remember(deviceId, newState);
        Integer count = counts.get(deviceId);
        int left = count == null ? 0 : count - 1;
        if (left > 0) {
            counts.put(deviceId, left);
            return LOADING;
        }
        counts.remove(deviceId);
        return lastKnown.get(deviceId);
    }

    /** 이 기기에 끝나지 않은 요청이 있으면 true. */
    boolean isPending(String deviceId) {
        return counts.containsKey(deviceId);
    }

    /**
     * 새로 받은 목록에 진행 중인 요청 표시(LOADING·"켜는 중")를 이어 붙인다. 안 그러면 제어 중에 목록이 오면
     * 잠금이 풀려 같은 기기에 명령이 겹친다. 실제 상태는 이미 remember로 기억해 두었다가 요청이 끝나면 보인다.
     */
    void carryOver(List<Device> fresh, List<Device> previous) {
        if (fresh == null) return;
        for (Device device : fresh) {
            if (device == null || !isPending(device.getId())) continue;
            device.setState(LOADING);
            Device old = find(previous, device.getId());
            if (old != null) device.setPendingCommand(old.getPendingCommand());
        }
    }

    private static Device find(List<Device> devices, String id) {
        if (devices == null || id == null) return null;
        for (Device device : devices) {
            if (device != null && id.equals(device.getId())) return device;
        }
        return null;
    }

    /** 목록을 새로 받았을 때처럼 진행 중인 요청과 상관없이 실제 상태를 알게 된 경우. */
    void remember(String deviceId, String state) {
        if (state != null && !LOADING.equals(state)) {
            lastKnown.put(deviceId, state);
        }
    }
}
