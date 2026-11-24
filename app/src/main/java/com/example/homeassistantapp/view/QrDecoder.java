package com.example.homeassistantapp.view;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.EnumMap;
import java.util.Map;

/** 카메라 밝기(Y) 값 한 장에서 QR 글자를 읽는다. Android에 기대지 않아 단위 테스트로 확인할 수 있다. */
public final class QrDecoder {
    private final QRCodeReader reader = new QRCodeReader();
    private final Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);

    public QrDecoder() {
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
    }

    /**
     * rowStride는 한 줄의 실제 바이트 수다. 카메라는 줄 끝에 빈 바이트를 붙이기도 해서 width보다 클 수 있다.
     * 못 읽으면 null.
     */
    public String decode(byte[] luminance, int width, int height, int rowStride) {
        if (luminance == null || width <= 0 || height <= 0 || rowStride < width) return null;
        if (luminance.length < (long) rowStride * (height - 1) + width) return null;
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                luminance, rowStride, height, 0, 0, width, height, false);
        try {
            Result result = reader.decode(new BinaryBitmap(new HybridBinarizer(source)), hints);
            return result.getText();
        } catch (ReaderException e) {
            return null;
        } finally {
            reader.reset();
        }
    }
}
