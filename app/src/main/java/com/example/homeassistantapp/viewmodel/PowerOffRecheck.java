package com.example.homeassistantapp.viewmodel;

/**
 * 끄기(OFF) 명령 뒤 상태를 다시 확인하는 규칙 (계약서 E2, 불일치 #15).
 *
 * 계약서 E2: PC 드라이버가 붙은 기기는 OFF를 받으면 PC에 종료 메시지를 보내고 45초 뒤에 전원을 끊는다.
 * 응답은 상태가 바뀌기 전에 오므로 앱이 잠시 뒤 E5로 다시 확인해야 한다.
 *
 * 정한 값과 이유 (계약서에 재확인 간격·횟수는 없다):
 * - 지연 50초: 서버의 전원 차단 45초 + 여유 5초. 더 일찍 물으면 아직 ON이라 의미가 없다.
 * - 횟수 1번: 45초는 서버가 정한 고정 시간이라 한 번이면 충분하다. 계속 물으면 서버·기기에 부담만 준다.
 * - 대상: 성공한 OFF 전부. 앱은 기기에 PC 드라이버가 붙었는지 알 수 없다(E1에 드라이버 정보가 없다).
 *   드라이버가 없는 기기는 이미 OFF라 요청 한 번이 더 갈 뿐이다.
 */
public final class PowerOffRecheck {
    public static final long DELAY_MS = 50_000L;

    private PowerOffRecheck() {}

    /** 이 제어 결과 뒤에 상태를 다시 확인해야 하는가. */
    public static boolean shouldRecheck(String command, boolean commandSucceeded) {
        return commandSucceeded && "OFF".equals(command);
    }
}
