# APP-06 변경 목록 — 앱을 계약 v2.0(초안)에 맞추기

| 항목 | 값 |
|---|---|
| 상태 | **승인 전**. 2026-11-24 작성. 코드는 아직 안 고쳤다 |
| 브랜치 | `v2.0` (master에서 분기). master는 서버 전환일까지 v1.0 그대로 둔다 |
| 기준 계약 | `docs/api-contract-v2.0-초안.md` (원본 v2.0 초안, 서버 `ccf6643`) + 맨 끝 13번 K 문구 메모 |
| 대상 | 계약 §6 MAJOR 16건 + E8 |
| 확인 범위 | 이 PC는 서버에 접속할 수 없다. javac + JUnit 단위 테스트까지 하고, 나머지는 **실서버 확인 필요**로 남긴다. 브랜치 앱은 v1.0 운영 서버와 맞지 않으므로 실서버 확인은 서버 v2.0(테스트 서버 또는 전환일) 이후에만 가능 |

## 0. 한눈에

| 단계 | 내용 | MAJOR # | 서버 준비 |
|---|---|---|---|
| 06a | 오류 본문 파서, 상태코드 판정, DTO 키 이름, 결과 키 제거, 413 | 2·3·4·5·10·12(키)·15 | ✅ 10·11·12a·12b |
| 06b | E2 200/202/409/502/404, 202 `delaySeconds` 뒤 E5 (`PowerOffRecheck` 50초 대체) | 6 | ✅ |
| 06c | E4 숨김 + `unpaired:false` 경고, E5·E6·E7 404 / E6 502 | 9·11·12(오류) | ✅ |
| 06d | E7 ISO 8601 시각, `deviceId`·`state`·`truncated` | 13·14 | ✅ 12a |
| 06e | E3 202 + E8 2초 폴링, 입력 사전 검사, `restored` 알림, `ALREADY_REGISTERED` | 7·8·16 | ⬜ **13b 전 — 초안 기준** |
| — | #1 접속 주소 | 1 | 변경 없음(보류) |

순서 이유: 06a의 파서·판정 도구를 06b~06e가 모두 쓴다. 06e는 서버 13b가 끝나지 않아 문구가 바뀔 수 있으므로 마지막에 둔다.
각 단계는 커밋 1개 + 학습 페이지 `learning/APP-06x-해설.html` 1개(구현 지시 때).

---

## 06a. 오류 본문·상태코드 판정·DTO 키

**바꿀 것**
| MAJOR | 앱 변경 |
|---|---|
| 2 오류 본문 | `{"error":{"code","message"}}` 한 가지만 읽는다. `ApiError`(code, message) 값과 파서를 만든다. 본문이 없거나 JSON이 아니면(413 포함) `code=null` |
| 3 상태코드 | 성공 = 2xx만. 본문의 `result`·`message`·`error` 문자열로 성공을 판단하던 코드를 모두 없앤다. 401 처리(`UnauthorizedInterceptor`·`UnauthorizedNotice`)는 그대로 |
| 4 결과 키 제거 | `DeviceListResponse.status`, `AddDeviceResponse.result/message`, `ControlResponse.message/SUCCESS_MESSAGE`, `EnergyLogResponse.error/hasError()` 제거 |
| 5 키 이름 | `Device`: `id`→`deviceId`, `type`→`protocolType`. `DeviceListResponse`: `total_count`→`totalCount`. `EnergyResponse`: `device_status`→`state`, `deviceId` 추가 |
| 10 기기 ID | 앱은 ID를 해석하지 않는다(지금도 해석 안 함, 확인 완료). `Device.ip`(nullable) 추가, 표시는 하지 않음(필요하면 UI 1차) |
| 12 E6 키 | 위 5번과 같이 처리 |
| 15 본문 16KiB | 정상 사용 시 넘지 않음. 413이면 본문 없이 "요청이 너무 큽니다" 일반 문구 |

**파일**: `dto/Device.java`, `dto/DeviceListResponse.java`, `dto/AddDeviceResponse.java`, `dto/ControlResponse.java`, `dto/EnergyResponse.java`, `dto/EnergyLogResponse.java`, `network/ErrorMessages.java`(→ `ApiError` 파싱으로 교체), 새 `network/ApiError.java`, `viewmodel/DeviceViewModel.java`(판정 부분)
**테스트**: `ErrorMessagesTest` 교체(새 모양·`error`가 문자열인 옛 모양·빈 본문·JSON 아님·`code`만 있음·모르는 `code`), `ControlResponseTest`·`EnergyLogResponseTest` 갱신, `Device` Gson 역직렬화 테스트(`deviceId`·`protocolType`·`ip: null`).
**위험**
- 키 이름을 하나라도 놓치면 Gson이 `null`을 넣는다. 특히 `Device.type`이 null이면 `HomeFragment`·`DashboardAdapter`의 `device.getType().equals(...)`에서 **크래시**. → 비교를 `"MATTER".equals(device.getType())` 꼴로 바꾸고, DTO 테스트로 키를 고정한다.
- 전환일에 TCP_DIY 기기는 재등록되어 ID가 `ARD_…`→`TCP_…`로 바뀐다. `DashboardPrefs`의 `device_order`·`<id>_widget_order`에 옛 ID가 남지만 해가 없다(새 기기는 기본 순서). 정리 여부는 D9.

## 06b. E2 제어 결과 (불일치 #5·#6·#15 다시 맞춤)

**바꿀 것** — `controlDevice` 응답 분기
| 응답 | 앱 동작 |
|---|---|
| 200 `{deviceId, state}` | `state`로 갱신, "명령 성공" 토스트 |
| 202 `{state, pendingCommand, delaySeconds}` | `state`(바뀌기 전)로 갱신, "N초 뒤 꺼집니다"류 안내. **`delaySeconds` + 여유(D5) 뒤 E5(`isManualRefresh=true`) 1회** |
| 409 `COMMAND_BLOCKED` | 요청 전 상태로, 실패 문구 |
| 502 `DEVICE_UNREACHABLE` | 서버가 `OFFLINE`을 기록하므로 `OFFLINE` 표시(D6), 실패 문구 |
| 404 `DEVICE_NOT_FOUND` | 요청 전 상태로, 실패 문구, `fetchDevices()`로 목록 갱신 |
| 400 / 그 외 / 통신 실패 | 지금처럼 요청 전 상태로(`PendingStates`) |

- `PowerOffRecheck`(성공한 OFF 전부에 50초 재확인)를 **202일 때만 `delaySeconds` 기준 재확인**으로 바꾼다. 200 OFF는 재확인하지 않는다.
- 대기 중에 같은 기기에 새 명령을 보내면 지금처럼 예약을 취소한다(`cancelRecheck`). 서버도 대기 OFF 중 ON이면 대기를 취소하므로 동작이 맞다.

**기존 불일치 처리 변경**
- **#5·#6**: "2xx + `message == "명령 성공!"`" → "**상태코드만**". `ControlResponse.isCommandSucceeded()`·`SUCCESS_MESSAGE` 삭제. 실서버에서 "실패인데 명령 성공!"이 뜨던 문제는 서버 v2.0(409/502)으로 해결된다.
- **#15**: 고정 50초 → 서버가 준 `delaySeconds`. 드라이버를 모르는 앱이 모든 OFF를 50초 뒤 재확인하던 우회가 사라진다.

**파일**: `dto/ControlResponse.java`(`deviceId`, `state`, `pendingCommand`, `delaySeconds`), `viewmodel/PowerOffRecheck.java`(→ 지연 계산 함수로 축소 또는 이름 변경), `viewmodel/DeviceViewModel.java`
**테스트**: 응답 분류 순수 함수(코드·본문 → 표시 상태·재확인 여부·지연 ms) 단위 테스트, `PowerOffRecheckTest` 교체(`delaySeconds` 0·45·누락·음수).
**위험**: 202 뒤 앱을 끄면 재확인이 사라진다(지금도 같음). 다음 목록 새로고침이나 E5에서 맞춰진다. **실서버 확인 필요**: PC 드라이버 기기로 202·409.

## 06c. E4 숨김 삭제, E5·E6·E7 404 (불일치 #12 다시 맞춤)

**바꿀 것**
| 경로 | 앱 동작 |
|---|---|
| E4 200 | `DeleteDeviceResponse`(`deletedAt`, `restorableUntil`, `unpaired`) 새로 만듦(지금은 `ControlResponse`를 재사용). "삭제했습니다(90일 안에 다시 등록하면 기록 복구)" 안내. **`unpaired == false`면 "기기 쪽 해제 실패, 기기 초기화 필요" 경고**(대화상자, 토스트보다 오래 보이게) |
| E4 404 | 이미 없는 기기 → 목록 새로고침 + 문구(D7) |
| E4 500 | 삭제 안 됨 → 요청 전 상태, 실패 문구 |
| E5 404 | 기기가 없어졌다 → 요청 전 상태 대신 목록 새로고침. 200 `OFFLINE`은 그대로 표시 |
| E6 404 / 502 / 400 | 404: 목록 새로고침. 502: 서버 통계 불변 → 요청 전 상태 + 문구. 400 `UNSUPPORTED_OPERATION`: TCP_DIY에는 E6를 부르지 않으므로(지금도 MATTER만) 일반 실패 문구 |
| E7 404 / 400 | 200 + `error` 분기를 없애고 상태코드로. 404는 목록 새로고침 |

**기존 불일치 처리 변경**
- **#12**: "E6 실패 시 요청 전 상태 + 서버 문구"는 502·400에 그대로 유지. 404만 "목록 새로고침"으로 바뀐다.
- E5 통신 실패 시 지금 `OFFLINE`으로 표시하는 동작(`syncDeviceState.onFailure`)은 v1.0 그대로 둔다(계약 무관).

**파일**: 새 `dto/DeleteDeviceResponse.java`, `api/HomeAssistantApi.java`(E4 반환형), `viewmodel/DeviceViewModel.java`(삭제 경고용 이벤트 LiveData 추가), `fragment/DeviceFragment.java` 또는 `MainActivity.java`(경고 표시)
**테스트**: `DeleteDeviceResponse` 역직렬화, 404 → "목록 새로고침" 판정 단위 테스트.
**위험**: 404마다 `fetchDevices()`를 부르면 대시보드 자동 조회(기기마다 E6·E7)와 겹쳐 요청이 늘 수 있다 → 한 응답 묶음에서 1번만 부르도록 짧게 묶는다. **실서버 확인 필요**: `unpaired:false`는 기기 전원을 끈 채 삭제해서 확인.

## 06d. E7 시각·응답 키

**바꿀 것**
- `EnergyLogTimeline`: 서버 시각 형식 `"yyyy-MM-dd HH:mm:ss"`(UTC) → ISO 8601 `"yyyy-MM-dd'T'HH:mm:ss'Z'"`. 표시는 지금처럼 폰 시간대. ISO도 문자열 순서 = 시간 순서라 정렬 코드는 그대로 둔다.
- `EnergyLogResponse`: `device_status`→`state`, `deviceId`·`truncated` 추가. 서버가 이미 오래된 순으로 주지만 앱 정렬(#10)은 안전장치로 유지.
- 요청은 파라미터 없이(기본 `limit=50`, 원본 모드) 그대로. 앱 구간 사용량 계산(#14)도 그대로.
- `period` 집계 화면은 D2.

**파일**: `view/EnergyLogTimeline.java`, `dto/EnergyLogResponse.java`, `viewmodel/DeviceViewModel.java`(`stateOr`)
**테스트**: `EnergyLogTimelineTest` 갱신(ISO 입력, 옛 형식 입력은 빈 문자열, 서울 시간대 변환, 자정 넘김), `EnergyLogResponseTest`(`truncated`·`state`).
**위험**: 서버가 밀리초(`.123Z`)나 `+00:00`을 붙이면 파싱 실패 → 빈 라벨. 계약은 초까지 `Z`. 필요하면 두 형식을 받게 한다. **실서버 확인 필요**: 차트 라벨.

## 06e. E3 비동기 + E8 (서버 13b 전, 초안 + K 메모 기준)

**바꿀 것**
1. **E3 202** `{jobId, state}` → 임시 항목(`TEMP_PAIRING`) 유지, 폴링 시작. 409 `COMMISSION_BUSY`·400 → 임시 항목 제거 + 문구.
2. **E8 폴링**: 2초 간격 `GET /api/commission/{jobId}`. `SUCCEEDED` → 임시 항목 제거 + `fetchDevices()` + 성공 안내. `restored == true`면 "이전 기록을 이어받았습니다" 추가 안내. `FAILED` → 임시 항목 제거 + `error.code`별 문구, **`ALREADY_REGISTERED`는 "이미 등록된 기기입니다" + 목록 새로고침**. 404 `JOB_NOT_FOUND`(서버 재시작 등) → 폴링 중단, `fetchDevices()`로 확인. 통신 실패는 다음 주기에 다시 시도.
3. **상한 210초**(K3; 초안은 180초): 넘으면 폴링을 멈추고 E1로 등록 여부 확인.
4. 폴링은 `DeviceViewModel`(액티비티 범위)에서 하므로 탭을 옮겨도 계속된다. 앱 프로세스가 죽으면 끊긴다(D8).
5. 등록 진행 중에는 기기 추가 버튼을 막는다(서버도 동시 1개).
6. **입력 사전 검사**(새 `CommissionInputValidator`, 순수 함수): `deviceName` 1~64자, MATTER 수동 코드는 `-`·공백 제거 뒤 숫자 11 또는 21자리, QR은 `MT:` + `0-9 A-Z . -`, TCP_DIY는 IPv4 점 표기 또는 `""`(빈 값이면 서버가 앱이 접속한 주소를 씀, K7), SSID 0~32바이트·비밀번호 0~63바이트(UTF-8), 모든 필드 제어문자 금지. 걸리면 대화상자에서 바로 안내하고 보내지 않는다.
7. Wi-Fi 값 trim 제거(D4). K1로 공백·따옴표가 허용되므로.
8. `CommissionTimeoutInterceptor`(150초 읽기 타임아웃)는 필요 없어진다 → 삭제 대상(구현 때 목록으로 승인 받음).

**파일**: `api/HomeAssistantApi.java`(E3 반환형, E8 추가), 새 `dto/CommissionJobResponse.java`(E3·E8 공용: `jobId`, `state`, `deviceId`, `restored`, `error`), `dto/AddDeviceResponse.java`(삭제 또는 대체), 새 `viewmodel/CommissionPoller.java`(다음 동작을 정하는 순수 로직), 새 `network/CommissionInputValidator.java`, `viewmodel/DeviceViewModel.java`, `fragment/DeviceFragment.java`(검사·버튼 잠금), `fragment/SettingsFragment.java`(trim), `network/RetrofitClient.java`·`network/CommissionTimeoutInterceptor.java`(제거), `ResponseInterceptorsTest`(타임아웃 테스트 제거)
**테스트**: 검사기(경계값 11/21자리, `-` 제거, `MT:` 허용 문자, 64자·65자, 32/33바이트 한글 SSID, `\n`·Tab 금지, IPv4 `256.x`·빈 값), 폴링 로직(상태 전이, 210초 상한, 404, `restored`, `ALREADY_REGISTERED`).
**위험**
- 서버 13b가 아직이라 **응답 모양·제한 시간·문구가 바뀔 수 있다**. 13b 완료·계약 14번 확정 뒤 다시 맞춘다(수정 1회 예상).
- 앱이 받아 주는 입력이 서버보다 넓거나 좁으면 400 또는 불필요한 거절 → 서버 13b 시험 케이스와 맞춰 본다.
- 요청 본문(비밀번호 포함)은 로그에 남기지 않는다(지금도 안 남김, 유지).
- **실서버 확인 필요**: TCP_DIY·Matter 실제 등록, 재등록 `restored`, 이미 등록된 기기.

---

## 기존 불일치 처리 변경 요약

| # | v1.0 처리(지금) | v2.0 처리 | 단계 |
|---|---|---|---|
| 5·6 | 2xx + `message == "명령 성공!"`. 400·401이면 요청 전 상태 | 상태코드만. 200/202/409/502/404 분기(06b 표). `isCommandSucceeded` 삭제 | 06b |
| 12 | E6 실패 시 요청 전 상태 + 서버 문구 | 502·400은 그대로. 404는 목록 새로고침 | 06c |
| 13 | 문구 `message` → `error` → "HTTP 코드" | `error.message`(D3) → 없으면 상태코드별 일반 문구. 분기는 `error.code` | 06a |
| 15 | 성공한 OFF마다 50초 뒤 E5 | 202일 때만 `delaySeconds` + 여유 뒤 E5 | 06b |
| 9·10·14 | E7 UTC→현지, 오래된 순 정렬, 구간 사용량 | 형식만 ISO로. 정렬·계산은 유지 | 06d |
| 4 | E3 읽기 타임아웃 150초 | 필요 없음(즉시 202) → 인터셉터 제거 | 06e |

## 사용자가 정할 항목

| # | 질문 | 선택지 | 추천 |
|---|---|---|---|
| D1 | **F-03**(토큰 칸 줄바꿈 → 요청마다 OkHttp 크래시, 앱 데이터 삭제로만 복구)을 어디서 고칠까 | A master에 바로(별도 작은 작업, 그 뒤 master를 `v2.0`에 병합) / B `v2.0` 브랜치에만 / C UI 1차까지 미룸 | **A** — 운영 중인 v1.0 앱의 크래시이고 계약과 무관하다. 지금 `trim()`은 앞뒤만 지워 중간 줄바꿈은 남는다. 수정: 저장 전 형식 검사(인쇄 가능한 ASCII만) + `AuthInterceptor`에서 헤더 예외를 잡아 크래시 대신 토큰 안내 |
| D2 | E7 일/주/월 집계(`period`) 화면을 이번에 할까 | A 이번에(06f로 추가) / B UI 1차로 미룸, 이번엔 원본 모드만 | **B** — 서버 계약에서 MINOR(선택)라 안 해도 깨지지 않는다. 날짜 선택기(2000~2100)·주 경계 등 화면 설계가 필요해 UI 1차와 묶는 게 낫다 |
| D3 | 실패 문구를 서버 고정 `message`로 쓸까, 앱이 `code`별 자체 문구를 쓸까 | A 서버 `message` + 앱 접두사("제어 실패: …"), 없을 때만 상태코드별 앱 기본 문구 / B 앱이 `code`별 표를 가짐, 모르는 코드만 서버 문구 | **A** — 서버가 코드별 고정 한글 문구를 약속했고(내부 정보 없음) 앱 표를 따로 두면 두 곳을 맞춰야 한다. 동작 분기(목록 새로고침·경고 등)는 `code`로 한다 |
| D4 | 설정의 Wi-Fi SSID·비밀번호 앞뒤 공백 `trim()` | A 없앤다(제어문자만 막음) / B 유지 | **A** — K1로 공백이 허용되며, 끝 공백이 있는 실제 비밀번호를 지금은 망가뜨린다. 서버 IP·포트·토큰 trim은 유지 |
| D5 | 202 뒤 재확인 시각 | A `delaySeconds` + 5초 / B `delaySeconds` 그대로 / C `delaySeconds` + 5초, 그때도 바뀌기 전 상태면 10초 뒤 1번 더 | **A** — 서버 전원 차단 직후 기기 응답 지연을 감안. 지금 50초(45+5)와 같은 여유 |
| D6 | E2 502(기기 응답 없음) 때 표시 | A `OFFLINE`(서버 기록과 같게) / B 요청 전 상태 | **A** — 서버가 `OFFLINE`을 기록하므로 다음 목록 조회와 화면이 일치한다 |
| D7 | E4 404(이미 없음·이미 숨김) | A 목록 새로고침 + "이미 삭제된 기기입니다" / B 실패로만 표시 | **A** — 사용자가 원한 결과(목록에서 사라짐)는 이미 이뤄졌다 |
| D8 | 등록 중 앱이 종료되면 | A `jobId`를 저장하지 않음(다시 켜면 목록으로 확인) / B `jobId`를 prefs에 저장해 재개 | **A** — 끝난 작업은 서버에 1시간만 남고, 목록만 봐도 결과를 알 수 있다. 단순함 우선 |
| D9 | 전환일 재등록 뒤 남는 옛 ID의 대시보드 순서 키 | A 그대로 둔다 / B 목록에 없는 ID 키를 앱이 지운다 | **A** — 작은 문자열이라 해가 없고, 자동 삭제는 일시적 목록 오류 때 순서를 잃을 위험이 있다 |
| D10 | E3 입력 사전 검사를 master(v1.0)에도 넣을까 (계약 §7 2단계는 "v1.0에서도 문제없는 준비"로 분류) | A `v2.0` 브랜치에만(06e) / B master에도 | **A** — master는 F-03 외에는 동결. 검사 규칙이 13b에서 바뀔 수 있어 한 곳에서만 맞춘다 |

## 남은 위험·확인

- 브랜치 앱은 운영 서버(v1.0)와 호환되지 않는다(키 이름·성공 판정). **전환일 전에는 설치하지 않는다**. 실서버 확인은 서버 v2.0이 뜬 뒤에만 가능.
- 이 PC는 Gradle이 실행되지 않는다(진행현황 "환경"). 단계마다 javac 컴파일 + JUnit으로 확인하고, Android 빌드·화면은 Android Studio에서 확인한다.
- 06e는 서버 13b·계약 14번 확정 뒤 한 번 더 맞춘다. 계약이 확정되면 사본 이름을 `docs/api-contract-v2.0.md`로 바꾸고 첫 줄을 고친다.
- master 병합은 전환일(계약 §7 5단계)에 한다.
