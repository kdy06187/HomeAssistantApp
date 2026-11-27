package com.example.homeassistantapp.view;

import com.example.homeassistantapp.dto.Device;

import java.util.List;

/**
 * 홈·기기 탭이 목록 자리에 무엇을 보일지. 목록을 한 번도 받지 못했는데 요청이 실패했으면 "불러오지 못함",
 * 받았는데 0개면 "기기 없음"으로 나눈다. 한 번이라도 받은 뒤의 실패는 마지막 목록을 그대로 두고 배너가 알린다.
 */
public enum ListScreenState {
    /** 첫 응답을 기다리는 중 */
    LOADING,
    CONTENT,
    /** 서버가 0개라고 답함 */
    EMPTY,
    /** 첫 목록을 받지 못함 */
    LOAD_FAILED;

    public static ListScreenState of(List<Device> devices, boolean lastLoadFailed) {
        if (devices == null) return lastLoadFailed ? LOAD_FAILED : LOADING;
        return devices.isEmpty() ? EMPTY : CONTENT;
    }
}
