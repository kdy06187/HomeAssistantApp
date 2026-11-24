package com.example.homeassistantapp.view;

/** 카메라 권한 상태에 따라 스캔 화면이 할 일을 정한다. */
public enum CameraPermissionStep {
    /** 허용됨: 카메라를 켠다 */
    START_CAMERA,
    /** 처음: 왜 필요한지 먼저 보이고 버튼을 누르면 시스템 권한 창 */
    EXPLAIN_FIRST,
    /** 한 번 거부: 다시 요청할 수 있다 */
    ASK_AGAIN,
    /** "다시 묻지 않음" 등으로 시스템 창이 더 뜨지 않음: 앱 설정 화면으로 보낸다 */
    OPEN_SETTINGS;

    /**
     * Android는 영구 거부를 직접 알려 주지 않는다. 한 번이라도 물어봤는데 설명 표시(rationale)도 false면
     * 시스템 창이 다시 뜨지 않는 상태로 본다.
     */
    public static CameraPermissionStep next(boolean granted, boolean askedBefore, boolean showRationale) {
        if (granted) return START_CAMERA;
        if (!askedBefore) return EXPLAIN_FIRST;
        return showRationale ? ASK_AGAIN : OPEN_SETTINGS;
    }
}
