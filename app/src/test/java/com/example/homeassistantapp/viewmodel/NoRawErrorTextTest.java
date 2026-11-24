package com.example.homeassistantapp.viewmodel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** 통신 예외의 영문 문구("unexpected end of stream" 등)가 화면으로 가지 않고, 통신 실패로 기기를 OFFLINE으로 바꾸지 않는지. */
public class NoRawErrorTextTest {
    private static String viewModelSource() throws IOException {
        String path = "src/main/java/com/example/homeassistantapp/viewmodel/DeviceViewModel.java";
        File f = new File(path).exists() ? new File(path) : new File("app/" + path);
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void exceptionMessagesNotShown() throws IOException {
        assertFalse(viewModelSource().contains("t.getMessage()"));
    }

    @Test
    public void networkFailureKeepsLastState() throws IOException {
        String src = viewModelSource();
        int i = src.indexOf("public void onFailure(Call<DeviceStateResponse> call, Throwable t)");
        assertTrue(i > 0);
        String body = src.substring(i, src.indexOf('}', i));
        assertFalse(body.contains("OFFLINE"));
    }
}
