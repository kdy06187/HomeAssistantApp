package com.example.homeassistantapp.viewmodel;

/**
 * PC 드라이버 기기는 OFF 뒤 45초가 지나야 전원이 꺼지므로, 50초 뒤 한 번 상태를 다시 확인한다.
 * 앱은 드라이버 종류를 알 수 없어 성공한 OFF 전부에 적용한다.
 */
public final class PowerOffRecheck {
    public static final long DELAY_MS = 50_000L;

    private PowerOffRecheck() {}

    public static boolean shouldRecheck(String command, boolean commandSucceeded) {
        return commandSucceeded && "OFF".equals(command);
    }
}
