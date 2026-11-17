package com.example.homeassistantapp.viewmodel;

/**
 * LiveData는 새 관찰자에게 마지막 값을 다시 주므로(화면 회전, 탭 전환),
 * 토스트처럼 한 번만 보여야 하는 값은 이 상자에 담아 꺼낼 때 표시한다.
 */
public final class OneTimeEvent<T> {
    private final T content;
    private boolean handled = false;

    public OneTimeEvent(T content) {
        this.content = content;
    }

    /** 처음 부를 때만 값을 주고, 그 뒤로는 null. */
    public T takeIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }
}
