package com.example.homeassistantapp.view;

/** 화면(Fragment 자리) 아래에 비워 둘 여백. 하단 탭·제스처 바·키보드가 내용을 가리지 않게 한다. */
public final class BottomInset {
    private BottomInset() {}

    /**
     * 하단 탭이 보이면 탭이 제스처 바 여백을 스스로 가지므로 키보드가 탭보다 높이 올라온 만큼만 비운다.
     * 탭이 숨은 전체 화면이면 제스처 바와 키보드 중 큰 쪽만큼 비운다.
     */
    public static int of(boolean bottomNavShown, int bottomNavHeight, int systemBarsBottom, int imeBottom) {
        if (bottomNavShown) return Math.max(0, imeBottom - bottomNavHeight);
        return Math.max(systemBarsBottom, imeBottom);
    }
}
