package com.example.homeassistantapp.view;

import com.example.homeassistantapp.network.ConnectionState;

/** 화면 위쪽 연결 배너에 무엇을 보일지. 정상이면 숨긴다. */
public final class ConnectionBanner {
    public enum Action { NONE, RETRY, OPEN_SETTINGS }

    public final boolean visible;
    public final String message;
    public final Action action;
    public final String actionLabel;

    private ConnectionBanner(boolean visible, String message, Action action, String actionLabel) {
        this.visible = visible;
        this.message = message;
        this.action = action;
        this.actionLabel = actionLabel;
    }

    /** onSettingsTab이면 이미 고칠 곳에 있으므로 "설정으로" 버튼을 숨긴다(문구는 계속 보인다). */
    public static ConnectionBanner of(ConnectionState.Kind kind, boolean onSettingsTab) {
        if (kind == ConnectionState.Kind.UNREACHABLE) {
            return new ConnectionBanner(true, "서버에 연결할 수 없어요 · Wi-Fi와 서버 주소를 확인해 주세요",
                    Action.RETRY, "다시 시도");
        }
        if (kind == ConnectionState.Kind.TOKEN_ERROR) {
            return onSettingsTab
                    ? new ConnectionBanner(true, "API 토큰이 맞지 않아요 · 아래에서 새 토큰을 저장해 주세요", Action.NONE, null)
                    : new ConnectionBanner(true, "API 토큰이 맞지 않아요", Action.OPEN_SETTINGS, "설정으로");
        }
        return new ConnectionBanner(false, null, Action.NONE, null);
    }
}
