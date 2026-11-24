package com.example.homeassistantapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * 스캔 화면 → 입력 화면으로 QR 내용을 넘긴다. Bundle(Fragment 결과)은 프로세스가 죽을 때 디스크에 남을 수 있어
 * 메모리에만 있는 액티비티 범위 ViewModel을 쓴다.
 */
public class QrScanResult extends ViewModel {
    private final MutableLiveData<OneTimeEvent<String>> scanned = new MutableLiveData<>();

    public LiveData<OneTimeEvent<String>> getScanned() { return scanned; }

    public void deliver(String payload) {
        scanned.setValue(new OneTimeEvent<>(payload));
    }

    /** 입력 화면이 넘겨받은 뒤 값을 비운다(같은 객체가 화면보다 오래 살기 때문). */
    public void clear() {
        scanned.setValue(null);
    }
}
