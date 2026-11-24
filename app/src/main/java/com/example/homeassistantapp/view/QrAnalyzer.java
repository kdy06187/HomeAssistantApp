package com.example.homeassistantapp.view;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

/** CameraX 분석 스레드에서 프레임마다 QR을 찾는다. 읽은 내용은 리스너에만 넘기고 어디에도 남기지 않는다. */
public final class QrAnalyzer implements ImageAnalysis.Analyzer {
    public interface Listener {
        /** Matter QR을 읽었을 때 한 번만. 분석 스레드에서 불린다. */
        void onMatterCode(String payload);

        /** Matter QR이 아닌 것을 읽었을 때. 분석 스레드에서 불린다. */
        void onRejected(QrPayload.Check reason);
    }

    private final QrDecoder decoder = new QrDecoder();
    private final Listener listener;
    private byte[] buffer = new byte[0];
    private volatile boolean done;

    public QrAnalyzer(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        try {
            if (done) return;
            // YUV 형식의 첫 면(Y)이 밝기 값이라 QR 해석에는 이것만 있으면 된다.
            ImageProxy.PlaneProxy plane = image.getPlanes()[0];
            ByteBuffer data = plane.getBuffer();
            data.rewind();
            int size = data.remaining();
            if (buffer.length != size) buffer = new byte[size];
            data.get(buffer);
            String text = decoder.decode(buffer, image.getWidth(), image.getHeight(), plane.getRowStride());
            if (text == null) return;
            QrPayload.Check check = QrPayload.check(text);
            if (check == QrPayload.Check.OK) {
                done = true;
                listener.onMatterCode(text);
            } else {
                listener.onRejected(check);
            }
        } finally {
            image.close();
        }
    }
}
