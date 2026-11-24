package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CameraPermissionStepTest {
    @Test
    public void granted_startsCamera() {
        assertEquals(CameraPermissionStep.START_CAMERA, CameraPermissionStep.next(true, false, false));
        assertEquals(CameraPermissionStep.START_CAMERA, CameraPermissionStep.next(true, true, false));
    }

    @Test
    public void firstTime_explainsFirst() {
        assertEquals(CameraPermissionStep.EXPLAIN_FIRST, CameraPermissionStep.next(false, false, false));
        assertEquals(CameraPermissionStep.EXPLAIN_FIRST, CameraPermissionStep.next(false, false, true));
    }

    @Test
    public void deniedOnce_asksAgain() {
        assertEquals(CameraPermissionStep.ASK_AGAIN, CameraPermissionStep.next(false, true, true));
    }

    @Test
    public void deniedForGood_opensSettings() {
        assertEquals(CameraPermissionStep.OPEN_SETTINGS, CameraPermissionStep.next(false, true, false));
    }
}
