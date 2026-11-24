package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 라이트·다크 colors.xml을 직접 읽어 토큰 이름이 맞는지, 글자/배경 쌍 대비가 4.5:1 이상인지 본다. */
public class ColorTokensTest {

    // 템플릿에서 온 색. 다크 짝이 필요 없다.
    private static final Set<String> LIGHT_ONLY = new HashSet<>(Arrays.asList("black", "white"));

    private static final String[][] TEXT_PAIRS = {
            {"color_on_surface", "color_surface"},
            {"color_on_surface", "color_surface_container"},
            {"color_on_surface_variant", "color_surface"},
            {"color_on_surface_variant", "color_surface_container"},
            {"color_on_primary", "color_primary"},
            {"color_on_primary_container", "color_primary_container"},
            {"color_on_error", "color_error"},
            {"color_on_error_container", "color_error_container"},
            {"color_on_warning_container", "color_warning_container"},
            {"color_inverse_on_surface", "color_inverse_surface"},
            {"color_inverse_primary", "color_inverse_surface"},
            {"state_on_fg", "state_on_bg"},
            {"state_off_fg", "state_off_bg"},
            {"state_offline_fg", "state_offline_bg"},
            {"state_unknown_fg", "state_unknown_bg"},
    };

    @Test
    public void lightAndDark_haveSameTokenNames() throws IOException {
        Set<String> light = new HashSet<>(read("values").keySet());
        light.removeAll(LIGHT_ONLY);
        Set<String> dark = read("values-night").keySet();
        assertEquals(light, dark);
    }

    @Test
    public void stateTokens_existForEveryState() throws IOException {
        Map<String, Integer> light = read("values");
        for (String state : new String[]{"on", "off", "offline", "unknown"}) {
            assertTrue(state, light.containsKey("state_" + state + "_bg"));
            assertTrue(state, light.containsKey("state_" + state + "_fg"));
        }
    }

    @Test
    public void textPairs_meetContrastInLight() throws IOException {
        assertContrast(read("values"), "light");
    }

    @Test
    public void textPairs_meetContrastInDark() throws IOException {
        assertContrast(read("values-night"), "dark");
    }

    @Test
    public void contrastRatio_matchesKnownValues() {
        assertEquals(21.0, contrast(0xFF000000, 0xFFFFFFFF), 0.01);
        assertEquals(1.0, contrast(0xFF777777, 0xFF777777), 0.01);
    }

    private static void assertContrast(Map<String, Integer> colors, String theme) {
        for (String[] pair : TEXT_PAIRS) {
            double ratio = contrast(colors.get(pair[0]), colors.get(pair[1]));
            assertTrue(theme + " " + pair[0] + "/" + pair[1] + " = " + ratio, ratio >= 4.5);
        }
    }

    static double contrast(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(int argb) {
        return 0.2126 * channel(argb >> 16) + 0.7152 * channel(argb >> 8) + 0.0722 * channel(argb);
    }

    private static double channel(int value) {
        double c = (value & 0xFF) / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static Map<String, Integer> read(String folder) throws IOException {
        File file = resFile(folder + "/colors.xml");
        String xml = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("<color name=\"([a-z_]+)\">#([0-9A-Fa-f]{8})</color>").matcher(xml);
        Map<String, Integer> colors = new LinkedHashMap<>();
        while (m.find()) colors.put(m.group(1), (int) Long.parseLong(m.group(2), 16));
        assertTrue(folder + " 색이 비어 있음", !colors.isEmpty());
        return colors;
    }

    // Gradle은 모듈 폴더(app)에서, IDE는 저장소 루트에서 테스트를 돌리기도 한다.
    private static File resFile(String path) {
        File inModule = new File("src/main/res/" + path);
        return inModule.exists() ? inModule : new File("app/src/main/res/" + path);
    }
}
