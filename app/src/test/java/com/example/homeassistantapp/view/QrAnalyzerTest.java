package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import androidx.camera.core.ImageProxy;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.junit.Test;

import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class QrAnalyzerTest {
    private static final String MATTER = "MT:Y3.13OTB00KA0648G00";
    private static final int SIZE = 240;

    private final List<String> events = new ArrayList<>();
    private int closed;

    private final QrAnalyzer analyzer = new QrAnalyzer(new QrAnalyzer.Listener() {
        @Override public void onMatterCode(String payload) { events.add("matter " + payload); }
        @Override public void onRejected(QrPayload.Check reason) { events.add("rejected " + reason); }
    });

    private static byte[] qr(String text) throws Exception {
        BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, SIZE, SIZE);
        byte[] y = new byte[SIZE * SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                y[row * SIZE + col] = (byte) (m.get(col, row) ? 0 : 255);
            }
        }
        return y;
    }

    private static byte[] blank() {
        byte[] y = new byte[SIZE * SIZE];
        java.util.Arrays.fill(y, (byte) 255);
        return y;
    }

    /** CameraX 없이 쓰는 가짜 프레임. 쓰는 메서드만 답하고 close 횟수를 센다. */
    private ImageProxy frame(byte[] luminance, boolean brokenPlanes) {
        ImageProxy.PlaneProxy plane = (ImageProxy.PlaneProxy) Proxy.newProxyInstance(
                ImageProxy.PlaneProxy.class.getClassLoader(), new Class<?>[]{ImageProxy.PlaneProxy.class},
                (p, method, args) -> {
                    switch (method.getName()) {
                        case "getBuffer": return ByteBuffer.wrap(luminance);
                        case "getRowStride": return SIZE;
                        case "getPixelStride": return 1;
                        default: throw new UnsupportedOperationException(method.getName());
                    }
                });
        return (ImageProxy) Proxy.newProxyInstance(
                ImageProxy.class.getClassLoader(), new Class<?>[]{ImageProxy.class},
                (p, method, args) -> {
                    switch (method.getName()) {
                        case "getPlanes":
                            if (brokenPlanes) throw new IllegalStateException("image already closed");
                            return new ImageProxy.PlaneProxy[]{plane};
                        case "getWidth":
                        case "getHeight":
                            return SIZE;
                        case "close":
                            closed++;
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    @Test
    public void matterCode_deliveredOnce_everyFrameClosed() throws Exception {
        byte[] code = qr(MATTER);
        analyzer.analyze(frame(code, false));
        analyzer.analyze(frame(code, false));
        analyzer.analyze(frame(code, false));
        assertEquals(List.of("matter " + MATTER), events);
        assertEquals(3, closed);
    }

    @Test
    public void plainQr_rejected_thenMatterAccepted() throws Exception {
        analyzer.analyze(frame(qr("https://example.com"), false));
        analyzer.analyze(frame(qr(MATTER), false));
        assertEquals(List.of("rejected " + QrPayload.Check.NOT_MATTER, "matter " + MATTER), events);
        assertEquals(2, closed);
    }

    @Test
    public void noQr_noCallback_closed() {
        analyzer.analyze(frame(blank(), false));
        assertEquals(List.of(), events);
        assertEquals(1, closed);
    }

    @Test
    public void failureWhileReading_stillClosed() {
        try {
            analyzer.analyze(frame(blank(), true));
            fail();
        } catch (IllegalStateException expected) {
            assertEquals(1, closed);
        }
    }
}
