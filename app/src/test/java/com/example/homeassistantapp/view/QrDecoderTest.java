package com.example.homeassistantapp.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.junit.Test;

public class QrDecoderTest {
    private static final String CODE = "MT:Y3.13OTB00KA0648G00";

    /** QR을 그려 카메라 밝기 값처럼 만든다. 검은 칸 0, 흰 칸 255, 줄 끝에 padding 바이트. */
    private static byte[] luminance(String text, int size, int rowStride) throws Exception {
        BitMatrix m = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
        byte[] y = new byte[rowStride * size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < rowStride; col++) {
                boolean black = col < size && m.get(col, row);
                y[row * rowStride + col] = (byte) (black ? 0 : 255);
            }
        }
        return y;
    }

    @Test
    public void decodesQr() throws Exception {
        assertEquals(CODE, new QrDecoder().decode(luminance(CODE, 300, 300), 300, 300, 300));
    }

    @Test
    public void decodesWithRowPadding() throws Exception {
        assertEquals(CODE, new QrDecoder().decode(luminance(CODE, 300, 320), 300, 300, 320));
    }

    @Test
    public void sameDecoderReadsTwice() throws Exception {
        QrDecoder decoder = new QrDecoder();
        byte[] y = luminance(CODE, 240, 240);
        assertEquals(CODE, decoder.decode(y, 240, 240, 240));
        assertEquals(CODE, decoder.decode(y, 240, 240, 240));
    }

    @Test
    public void blankOrBadInput_null() {
        byte[] white = new byte[100 * 100];
        java.util.Arrays.fill(white, (byte) 255);
        assertNull(new QrDecoder().decode(white, 100, 100, 100));
        assertNull(new QrDecoder().decode(null, 100, 100, 100));
        assertNull(new QrDecoder().decode(new byte[10], 100, 100, 100));
        assertNull(new QrDecoder().decode(white, 100, 100, 50));
    }

    @Test
    public void shortestBuffer_lastRowWithoutPadding() throws Exception {
        // 카메라 버퍼는 마지막 줄 끝 padding을 생략하기도 한다. 그 길이까지는 읽고, 한 바이트라도 짧으면 null.
        byte[] padded = luminance(CODE, 300, 320);
        int minimum = 320 * 299 + 300;
        assertEquals(CODE, new QrDecoder().decode(java.util.Arrays.copyOf(padded, minimum), 300, 300, 320));
        assertNull(new QrDecoder().decode(java.util.Arrays.copyOf(padded, minimum - 1), 300, 300, 320));
    }
}
