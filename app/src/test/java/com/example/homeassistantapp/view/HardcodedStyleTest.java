package com.example.homeassistantapp.view;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 레이아웃·Java에 색 값이 직접 들어가거나 14sp보다 작은 글자가 다시 생기지 않게 막는다. */
public class HardcodedStyleTest {

    private static final Pattern XML_COLOR = Pattern.compile("=\"#[0-9A-Fa-f]{3,8}\"");
    private static final Pattern SMALL_TEXT = Pattern.compile("textSize=\"(\\d+)sp\"");
    private static final Pattern JAVA_COLOR = Pattern.compile("Color\\.(parseColor|rgb|argb|[A-Z]+\\b)");

    @Test
    public void layouts_useTokensInsteadOfColorValues() throws IOException {
        List<String> found = scan(dir("src/main/res/layout"), ".xml", XML_COLOR);
        found.addAll(scanVectorTints(dir("src/main/res/drawable")));
        assertTrue("직접 쓴 색: " + found, found.isEmpty());
    }

    @Test
    public void layouts_haveNoTextSmallerThan14sp() throws IOException {
        List<String> found = new ArrayList<>();
        for (File f : files(dir("src/main/res/layout"), ".xml")) {
            Matcher m = SMALL_TEXT.matcher(read(f));
            while (m.find()) {
                if (Integer.parseInt(m.group(1)) < 14) found.add(f.getName() + " " + m.group());
            }
        }
        assertTrue("14sp 미만: " + found, found.isEmpty());
    }

    @Test
    public void javaCode_hasNoColorValues() throws IOException {
        List<String> found = scan(dir("src/main/java"), ".java", JAVA_COLOR);
        assertTrue("Java 색 값: " + found, found.isEmpty());
    }

    // 런처 아이콘은 브랜드 색이라 검사에서 뺀다.
    private static List<String> scanVectorTints(File drawables) throws IOException {
        List<String> found = new ArrayList<>();
        Pattern tint = Pattern.compile("android:tint=\"#[0-9A-Fa-f]{3,8}\"");
        for (File f : files(drawables, ".xml")) {
            if (f.getName().startsWith("ic_launcher")) continue;
            Matcher m = tint.matcher(read(f));
            while (m.find()) found.add(f.getName() + " " + m.group());
        }
        return found;
    }

    private static List<String> scan(File root, String ext, Pattern pattern) throws IOException {
        List<String> found = new ArrayList<>();
        for (File f : files(root, ext)) {
            Matcher m = pattern.matcher(read(f));
            while (m.find()) found.add(f.getName() + " " + m.group());
        }
        return found;
    }

    private static List<File> files(File root, String ext) {
        List<File> result = new ArrayList<>();
        File[] children = root.listFiles();
        assertTrue("폴더 없음: " + root, children != null);
        for (File f : children) {
            if (f.isDirectory()) result.addAll(files(f, ext));
            else if (f.getName().endsWith(ext)) result.add(f);
        }
        return result;
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    // Gradle은 모듈 폴더(app)에서, IDE는 저장소 루트에서 테스트를 돌리기도 한다.
    private static File dir(String path) {
        File inModule = new File(path);
        return inModule.exists() ? inModule : new File("app/" + path);
    }
}
