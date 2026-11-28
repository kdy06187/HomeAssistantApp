package com.example.homeassistantapp.view;

/**
 * QR 스캔 네모의 한 변 길이. 위 도구 막대와 아래 안내·버튼 사이 빈 곳에 여백을 두고 들어가는 가장 큰 정사각형이되
 * 기본 크기(maxPx)보다 커지지 않는다. 가로 화면처럼 세로 공간이 좁으면 네모가 줄어 글자·버튼과 겹치지 않는다.
 */
public final class QrFrameSize {
    private QrFrameSize() {}

    public static int of(int maxPx, int gapWidthPx, int gapHeightPx, int marginPx) {
        int side = Math.min(maxPx, Math.min(gapWidthPx, gapHeightPx) - 2 * marginPx);
        return Math.max(side, 0);
    }
}
