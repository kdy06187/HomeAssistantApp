package com.example.homeassistantapp.network;

import java.io.IOException;

/** 설정의 "연결 테스트"(입력한 주소·토큰으로 E1 한 번) 결과 분류와 안내 문구. */
public final class ConnectionTestResult {
    public enum Kind {
        SUCCESS,
        /** 401: 서버에는 닿았지만 토큰이 틀림 */
        TOKEN_ERROR,
        /** 서버에 닿지 않음(꺼짐·주소 틀림·다른 Wi-Fi) */
        UNREACHABLE,
        /** 서버에 닿았지만 다른 오류(404·500 등): 주소가 이 서버가 아닐 수 있음 */
        SERVER_ERROR
    }

    public final Kind kind;
    public final String message;

    private ConnectionTestResult(Kind kind, String message) {
        this.kind = kind;
        this.message = message;
    }

    public boolean isSuccess() {
        return kind == Kind.SUCCESS;
    }

    public static ConnectionTestResult fromHttp(int httpCode) {
        if (httpCode >= 200 && httpCode < 300) {
            return new ConnectionTestResult(Kind.SUCCESS, "연결됐어요. 서버와 토큰이 맞아요.");
        }
        if (httpCode == 401) {
            return new ConnectionTestResult(Kind.TOKEN_ERROR, "서버에는 닿았지만 토큰이 맞지 않아요. 토큰을 다시 확인해 주세요.");
        }
        return new ConnectionTestResult(Kind.SERVER_ERROR,
                "서버가 오류(HTTP " + httpCode + ")로 답했어요. 주소와 포트가 이 홈 서버인지 확인해 주세요.");
    }

    /** IOException은 통신 실패. 그 밖의 예외(주소 형식 등)도 서버에 닿지 못한 것으로 본다. */
    public static ConnectionTestResult fromFailure(Throwable t) {
        String message = t instanceof IOException
                ? "서버에 연결할 수 없어요. 서버가 켜져 있는지, 같은 Wi-Fi인지, IP·포트가 맞는지 확인해 주세요."
                : "서버 주소를 쓸 수 없어요. IP와 포트를 확인해 주세요.";
        return new ConnectionTestResult(Kind.UNREACHABLE, message);
    }

    /** 첫 실행에는 지금 입력 그대로 테스트가 성공했을 때만 저장할 수 있다. */
    public static boolean canSave(boolean firstRun, boolean passedForCurrentInput) {
        return !firstRun || passedForCurrentInput;
    }
}
