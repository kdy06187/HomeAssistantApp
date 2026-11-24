# APP-07 변경 목록 — UI 1차

| 항목 | 값 |
|---|---|
| 상태 | D1~D9 추천안 A로 **승인**(2026-11-24). 07a·07b 구현 끝(구현 메모는 `docs/04_진행현황.md`). **07g 추가, D10~D16 추천안 A 승인(2026-11-24)**. 07c 진행 중. 진행 순서 07c → 07g → 07d → 07e → 07f |
| 브랜치 | `ui-1` (`v2.0`에서 분기). UI가 v2.0 응답(E8 상태, E7 `period`)을 쓰기 때문이다 |
| 기준 | 총괄 `home-assistant-collab/ui-review-2026-11-17.md`(41건) 3절 우선순위, `mockups/01~04`·`README.md`, 협업현황 결정 8~11, 열린 항목 F-01, 사용자 결정(2026-11-24) |
| 계약 | `docs/api-contract-v2.0-초안.md` (06e는 서버 13b 전 초안 기준) |
| 확인 범위 | 이 PC는 javac + JUnit만 된다. 화면·다크 모드 대비·카메라는 단계마다 **Studio 확인**으로 따로 적는다 |

**사용자 결정(2026-11-24)**: 상세는 방식 1(전체 화면)이고 내용은 컴포넌트로 나눠 나중에 방식 3으로 바꿀 수 있게 한다. 홈 타일에 추이선은 넣지 않는다(결정 53). 이것은 **기본값**이며, 07g에서 사용자가 켠 기기에만 추이선을 그린다. 등록 진행 화면은 E8 상태(PENDING → RUNNING → 성공/실패)와 경과 시간만 보여 주고 세부 단계를 지어내지 않는다. UNKNOWN은 바로 "알 수 없음"(주황)으로 보이고 새로고침 버튼을 둔다.

## 0. 한눈에

| 단계 | 내용 | UI 리뷰·결정 |
|---|---|---|
| 07a | 색 토큰·다크 모드, 상태 색쌍, 글자 크기 | UI-03, UI-11, UI-36, 결정 9 |
| 07b | 3탭 정보 구조·홈(타일 격자, 상태 표현, 빈 상태, 실패 Snackbar)·기기 탭 | UI-02·06·09·10·20·22·35, 결정 8, UNKNOWN 결정 |
| 07c | 상세 전체 화면(컴포넌트, 15초 E6, 막대 차트, 일/주/월, 삭제) | UI-14·23·24·25·26·34, 결정 11, 방식 1 결정, D1 |
| 07g | 타일 꾸미기: 기기별로 타일에 보일 항목(현재 W·누적·추이선·크기 1/2칸)을 켜고 끄고 저장. 07c 다음에 한다 | 사용자 요청(2026-11-24), 결정 53(추이선 기본 끔), D10~D16 |
| 07d | 기기 추가(전체 화면 입력, QR 스캔, E8 진행·결과 화면) | UI-17·18·19, 결정 10, 진행 화면 결정 |
| 07e | 설정(첫 실행 안내, 칸별 오류, 가림·보기, 연결 테스트, Wi-Fi 비밀번호 보관) | UI-27·28·29 |
| 07f | 서버 연결 실패 배너, 401 계속 보이는 안내(F-01), 통신 실패를 OFFLINE으로 안 바꿈 | UI-01·31·32, F-01 |

- 각 단계는 작은 하위 단계(07a-1 …)로 나누고, **하위 단계마다 커밋 1개 + 학습 페이지 `learning/APP-07xN-해설.html`**.
- 공통: 모든 아이콘 버튼·스위치는 터치 48dp 이상, `contentDescription`·라벨(UI-35). 이미 끝난 1차 항목은 다시 하지 않는다: UI-04 토스트 재생(APP-05 `OneTimeEvent`), UI-28 포트 힌트(APP-05), UI-19 입력 검사(APP-06e `CommissionInputValidator` — 화면 표시는 07d에서).
- 새 화면 문구도 지금 규칙대로 코드·레이아웃에 직접 쓴다(앱 `CLAUDE.md`). 문자열 리소스화(UI-38)는 이번에 하지 않는다.

---

## 07a. 색 토큰·다크 모드

| 하위 | 내용 | 파일 |
|---|---|---|
| 07a-1 | 색 토큰 정의: `values/colors.xml`(라이트)·`values-night/colors.xml`(다크, 새 파일)에 같은 이름. 배경·표면·글자(주/보조)·구분선·강조, 상태 색쌍 4종(켜짐·꺼짐·오프라인·알 수 없음: 칩 배경 + 글자), 경고·오류. `themes.xml` 두 벌에 Material 3 색 역할(`colorSurface`, `colorOnSurface` …) 연결 | `res/values/colors.xml`, `res/values-night/colors.xml`, `res/values*/themes.xml` |
| 07a-2 | 하드코딩 색 교체: 레이아웃 약 39곳(`fragment_settings` 14, `activity_device_detail` 6 …)과 Java 7곳(`DashboardAdapter` 6, `EnergyLineChartView` 1)을 토큰·테마 속성으로. 12sp 글자는 14sp 이상, 보조 글자 `#888888` 제거 | `res/layout/*.xml`, `view/DashboardAdapter.java`, `view/EnergyLineChartView.java` |
| 07a-3 | 상태 표현 한 곳으로: `DeviceStateLabel`을 "상태 → 문구 + 색 역할 + 스위치 가능 여부"로 넓힌다. 켜짐/꺼짐/오프라인/**알 수 없음(주황)**, LOADING은 "확인 중…" | `view/DeviceStateLabel.java` |

- **테스트**: `DeviceStateLabelTest` 확장(ON·OFF·OFFLINE·UNKNOWN·모르는 값·null·LOADING → 문구·색 역할·스위치 가능), 토큰 이름 누락 검사(라이트·다크 `colors.xml`의 이름 집합이 같은지 읽어서 비교하는 단위 테스트).
- **위험**: Material 3 기본 색과 섞이면 일부 위젯(스위치·FAB)이 의도와 다른 색이 된다 → 테마 속성에도 연결. 대비 4.5:1은 목업 값이지만 실제 화면에서 측정해야 한다.
- **Studio 확인**: 라이트·다크 전환 때 모든 화면 글자가 보이는지, 상태 칩 대비(접근성 검사기 또는 Accessibility Scanner), 차트 축 글자색.

## 07b. 3탭 정보 구조·홈·기기 탭 (결정 8)

| 하위 | 내용 | 파일 |
|---|---|---|
| 07b-1 | 탭 "홈 · 기기 · 설정", **시작 탭 홈**. 홈에 들어올 때도 목록을 요청한다(지금은 기기 탭만 요청, UI-02). 토큰이 없을 때만 설정으로 시작 | `MainActivity.java`, `res/menu/*`, `fragment/HomeFragment.java` |
| 07b-2 | 홈 = **2열 타일 격자**(목업 01 C안): 아이콘, 이름, 상태 칩, 스위치(48dp, 라벨 "○○ 켜기"), Matter는 전력 큰 숫자 + 누적 kWh 한 줄(값 없으면 "–"). 추이선 없음. 타일 이름 부분을 누르면 상세(07c). 홈에 "기기 추가" 버튼 | 새 `view/HomeTileAdapter.java`, 새 `res/layout/item_home_tile.xml`, `fragment_home.xml` |
| 07b-3 | 상태·피드백: 켜는 동안은 요청한 값으로 스위치를 두고 잠금 + "켜는 중…"(UI-10). 오프라인은 스위치 비활성 + 이유. **UNKNOWN은 바로 "알 수 없음"(주황) + 새로고침 버튼**(E5 `isManualRefresh=true`). 제어 실패는 토스트 대신 **Snackbar + "다시 시도"**(UI-01). 빈 상태(기기 0개: 안내 + 추가 버튼) | `HomeFragment.java`, `DeviceViewModel.java`(실패 이벤트에 "다시 시도"할 명령 담기) |
| 07b-4 | 기기 탭: 목록 = 상태 칩·종류·**삭제 버튼**(길게 누르기만 있던 것, UI-20)·순서 핸들(D3)·추가 버튼(FAB `contentDescription`, UI-22) | `fragment/DeviceFragment.java`, `view/DeviceAdapter.java`, `res/layout/item_device.xml` |

- **테스트**: 타일 표시 상태 순수 함수(기기 상태 + 진행 중 명령 → 스위치 위치·잠금·문구), 실패 이벤트(명령 다시 보내기 값) 단위 테스트, 빈 상태 판정.
- **위험**: 홈을 타일로 바꾸면 지금의 섹션·위젯 구조(`DashboardAdapter`·`WidgetAdapter`·위젯 ViewHolder 3개·위젯 레이아웃 3개, 위젯 편집·드래그·`<id>_widget_order`)가 쓰이지 않게 된다(D2). 지울 파일은 구현 때 목록으로 먼저 승인 받는다. 순서 저장 `device_order`는 그대로 쓴다.
- **Studio 확인**: 375dp 폭 2열 타일 줄바꿈, 스위치 48dp 터치, TalkBack 라벨, Snackbar "다시 시도", 빈 상태, 다크 모드.

## 07c. 상세 전체 화면 (방식 1, 결정 11)

| 하위 | 내용 | 파일 |
|---|---|---|
| 07c-1 | 골격: 앱바(뒤로 48dp) + 스크롤. 내용은 **컴포넌트로 나눔**: 상태·제어 카드, 전력 카드, 차트 카드, 정보 카드(종류·ID·IP·마지막 갱신), 삭제. 각 카드는 따로 된 View 클래스 + `<include>` 레이아웃이라 나중에 방식 3(시트)에서 그대로 쓴다. 가짜 "정상 (Online)" 제거(UI-23) | 새 `fragment/DeviceDetailFragment.java`(D4), 새 `view/detail/*Card.java`, 새 `res/layout/detail_*.xml` |
| 07c-2 | **E6 15초 갱신**: 화면이 보일 때만(onStart~onStop), 숫자만 바꾸고 카드를 흐리게 하지 않음(UI-12). "마지막 갱신 HH:mm:ss · 15초마다". 실패하면 카드 위 배너 + "다시 시도"(UI-24) | `DeviceDetailFragment.java`, `DeviceViewModel.java`(상세용 전력 LiveData) |
| 07c-3 | 막대 차트(최근 기록): E7 원본 모드, 구간 사용량(이웃 차이, 음수·빈 구간 처리 그대로), 단위 자동(mWh/Wh/kWh), 제목·단위 표시, 막대 터치 툴팁, 빈 상태("기록이 쌓이는 중"), `truncated`면 "최근 50건" 안내(UI-14) | 새 `view/EnergyBarChartView.java`(MPAndroidChart `BarChart`), 새 `view/EnergyUnits.java` |
| 07c-4 | **일/주/월 탭(D1)**: E7 `period=day/week/month`, 기간은 앱이 정함(일 = 오늘까지 7일, 주 = 이번 주까지 8주, 월 = 이번 달까지 6개월, 모두 +09:00 날짜). `buckets[].energyUsed_mWh`를 막대로, `sampleCount: 0`은 빈칸 | 새 `dto/EnergyBucket.java`, `EnergyLogResponse.java`(`period`·`buckets`), `HomeAssistantApi.java`(쿼리 `from`·`to`·`period`), 새 `view/PeriodRange.java` |
| 07c-5 | 삭제: 하단 빨간 글자 버튼 + 확인 대화상자(파괴적 동작 강조, UI-34). 성공하면 상세를 닫는다. `unpaired:false` 경고는 06c 그대로 | `DeviceDetailFragment.java` |

- **테스트**: `EnergyUnits`(999 mWh → "999 mWh", 1500 → "1.5 Wh", 2,500,000 → "2.5 kWh"), `PeriodRange`(오늘 2026-11-24 화요일 → 일: 11-18~11-24, 주: 10-06(월)~11-24, 월: 06-01~11-24, 연말 넘김, 범위 366일 이하), 버킷 → 막대 값(빈 구간), 15초 주기 결정 함수(보임·숨김 → 예약·취소), 상태 카드 표시.
- **위험**: `DeviceDetailActivity`를 Fragment로 바꾸면 지금 Activity·레이아웃(`activity_device_detail.xml`)·`EnergyLineChartView`·`detailEnergyHistory`가 안 쓰이게 된다(D4) → 지울 목록은 구현 때 승인. 15초 갱신이 화면 밖에서 돌면 요청이 쌓인다 → 수명 주기 테스트는 Studio. 서버 E7 집계는 12a 구현됨, **실서버 확인 필요**.
- **Studio 확인**: 스크롤·회전·뒤로가기, 15초 갱신이 화면을 나가면 멈추는지(서버 로그나 Logcat 요청 간격), 막대 터치 툴팁·TalkBack, 일/주/월 전환, 다크 모드 차트.

## 07g. 타일 꾸미기 (07c 다음, 07d 전)

기기마다 홈 타일에 무엇을 보일지 사용자가 정하고, 앱을 다시 켜도 유지한다. 이름·상태 칩·스위치는 타일의 기본 기능이라 항상 보인다(끌 수 없음). 진입은 **상세 화면의 "타일 꾸미기"**와 **기기 탭**에서만 한다. 홈 타일 길게 누르기는 쓰지 않는다(스위치·상세 진입과 겹치지 않게, D3과 같은 이유).

| 하위 | 내용 | 파일 |
|---|---|---|
| 07g-1 | **설정 모델·저장**: `TileConfig`(현재 W, 누적, 추이선, 크기 1/2칸) + 기기 종류별 가능한 항목(TCP_DIY는 전력 값이 없어 크기만). 저장은 기기 ID별 prefs(D12). 없거나 깨진 값은 기본값(D11), 모르는 필드는 무시하고 빠진 필드는 그 항목만 기본값 | 새 `view/TileConfig.java`, 새 `view/TileConfigStore.java` |
| 07g-2 | **홈 타일 반영**: `HomeTile`이 설정을 받아 W·누적 줄을 숨기거나 보임. 2칸은 `GridLayoutManager.SpanSizeLookup`으로 한 줄 전체. 설정이 바뀌면 홈이 다시 그린다 | `HomeTile.java`, `HomeTileAdapter.java`, `HomeFragment.java` |
| 07g-3 | **추이선**: 켠 Matter 기기만 E7을 요청해 작은 선(축·글자 없음, 터치 없음)을 그린다. 기간은 D14. 값은 07c-4의 일 단위 집계를 재사용하고, 홈에 들어올 때 한 번 요청 + 메모리 캐시(같은 기기 10분 안 재요청 안 함). 기록이 없으면 선 대신 "–" | 새 `view/SparklineView.java`(Canvas로 직접 그림, 차트 라이브러리 안 씀), `DeviceViewModel.java`(기기별 추이 LiveData·캐시), `HomeTileAdapter.java` |
| 07g-4 | **꾸미기 화면**: 스위치 4개(종류별로 못 쓰는 항목은 숨김) + 미리보기 타일 + "기본값으로". 바꾸는 즉시 저장(D15). 진입: 상세(07c) 앱바 메뉴 "타일 꾸미기", 기기 탭 줄의 꾸미기 버튼(48dp, 라벨 "○○ 타일 꾸미기") | 새 `fragment/TileCustomizeSheet.java`(D13), 새 `res/layout/sheet_tile_customize.xml`, `DeviceDetailFragment.java`, `DeviceAdapter.java`, `item_device.xml`, 아이콘 1개(`ic_tune`) |
| 07g-5 | **정리**: E4 삭제가 성공하면 그 기기 설정을 지운다(D16) | `DeviceViewModel.java`(삭제 성공 이벤트) 또는 `DeviceFragment`·`DeviceDetailFragment`, `TileConfigStore.java` |

- **저장 형식 제안(D12)**: prefs 파일 `TilePrefs`(순서용 `DashboardPrefs`와 분리해 정리·백업을 따로 다룸), 키 `tile.<deviceId>`, 값은 Gson JSON `{"v":1,"power":true,"total":true,"trend":false,"span":1}`. Gson은 이미 Retrofit 변환기로 들어와 있어 새 의존성이 없다. `v`는 형식 버전(나중에 항목이 늘면 올림).
- **테스트**: `TileConfig` 읽기·쓰기 왕복, 키 없음·깨진 JSON·빠진 필드 → 기본값, 모르는 필드 무시, TCP_DIY 가능한 항목(크기만), `HomeTile`이 설정대로 줄을 숨기는지, 2칸 span 계산, 추이선 요청 대상(추이선 켠 Matter만, 캐시 10분), 추이 값 → 선 좌표(빈 구간·값 1개·모두 0), 삭제 뒤 정리 함수.
- **위험**: 추이선을 켠 기기가 많으면 홈 진입마다 E7이 기기 수만큼 늘어난다 → 캐시 + 켠 기기만. 2칸 타일과 1칸 타일이 섞이면 격자에 빈칸이 생길 수 있다(순서대로 채움, 자동 메우기 안 함). 서버 E7 집계(12a)는 실서버 확인 전. 설정 형식이 바뀌면 옛 값 읽기 → `v`로 구분.
- **Studio 확인**: 꾸미기 화면 스위치가 홈에 바로 반영, 앱 재시작 뒤 유지, 2칸 타일 375dp 배치, 추이선 라이트·다크 색, 추이선 켠 기기만 E7 요청(Logcat), 상세·기기 탭 두 진입, 기기 삭제 뒤 설정이 지워짐(Device File Explorer로 `TilePrefs.xml`), TalkBack 스위치 라벨.

## 07d. 기기 추가 (결정 10, 진행 화면 결정)

| 하위 | 내용 | 파일 |
|---|---|---|
| 07d-1 | 입력 화면(전체 화면, D5): 종류(Matter / 직접 연결), 이름, Matter = **QR 스캔(주) + 코드 직접 입력(보조)**, TCP_DIY = 서버 IP(빈 값이면 지금 서버 주소 안내). **칸별 오류**는 `CommissionInputValidator` 결과를 칸 아래 표시. Wi-Fi 미설정이면 입력 전에 안내 + 설정 이동. 홈·기기 탭 추가 버튼에서 연다 | 새 `fragment/AddDeviceFragment.java`, 새 `res/layout/fragment_add_device.xml`, `CommissionInputValidator.java`(오류가 어느 칸인지 함께 반환) |
| 07d-2 | **QR 스캔**: CameraX 미리보기 + ZXing `QRCodeReader`(분석기). 권한 안내(처음 1회) → 시스템 권한 창 → 거부 시 다시 요청, **영구 거부 시 "설정에서 허용" 버튼**(앱 설정 화면 열기). `MT:`로 시작하지 않으면 "Matter QR이 아니에요". 인식 성공 → 입력 칸 채우고 등록 진행. 플래시 토글(기기에 있으면). **QR 내용은 로그·저장 금지**(화면 표시도 앞 몇 자만). "직접 입력"을 항상 보인다 | 새 `fragment/QrScanFragment.java`, 새 `view/QrAnalyzer.java`, 새 `res/layout/fragment_qr_scan.xml`, `AndroidManifest.xml` |
| 07d-3 | **진행·결과 화면**: E8 `state`만 보여 준다 — "대기 중"(PENDING) → "등록 중"(RUNNING) + **경과 시간**(mm:ss), 단계를 지어내지 않는다. 화면을 떠나도 계속(06e 그대로), 돌아오면 진행 화면 복원. 결과는 사라지지 않는 화면 3종: 성공(+ `restored`면 "이전 기록을 이어받았어요"), 실패(무슨 일 + 어떻게 하나 두 문장, `ALREADY_REGISTERED` 따로), 확인 불가(LOST·TIMED_OUT: "목록에서 확인, 같은 기기를 다시 등록하기 전에 목록을 먼저 보세요") | 새 `fragment/CommissionProgressFragment.java`, `DeviceViewModel.java`(토스트 대신 진행 상태 LiveData: 상태·시작 시각·결과) |

- **카메라 권한 추가**: `AndroidManifest.xml`에 `<uses-permission android:name="android.permission.CAMERA" />`, `<uses-feature android:name="android.hardware.camera.any" android:required="false" />`(카메라 없는 기기도 설치·직접 입력 가능). 런타임 요청은 `ActivityResultContracts.RequestPermission`(이미 있는 `androidx.activity`).
- **새 의존성**(앱 `CLAUDE.md`: Retrofit·MPAndroidChart·SwipeRefreshLayout 외에는 `gradle/libs.versions.toml`):

| 라이브러리 | 좌표 | 버전(D7) | 용도 |
|---|---|---|---|
| CameraX core | `androidx.camera:camera-core` | 1.4.2 | 카메라 |
| CameraX Camera2 | `androidx.camera:camera-camera2` | 1.4.2 | 구현체 |
| CameraX lifecycle | `androidx.camera:camera-lifecycle` | 1.4.2 | 수명 주기 연결 |
| CameraX view | `androidx.camera:camera-view` | 1.4.2 | `PreviewView` |
| ZXing core | `com.google.zxing:core` | 3.5.3 | QR 해석(Apache 2.0) |

  `libs.versions.toml`에 `camerax = "1.4.2"`, `zxing = "3.5.3"`과 라이브러리 5줄, `app/build.gradle.kts`에 `implementation(libs.…)` 5줄. 버전은 내가 아는 안정판이며 **구현 전에 Studio에서 최신 안정판·compileSdk 36 호환을 확인**한다(D7).
- **테스트**: 칸별 오류 반환(검사기 확장), QR 문자열 필터(`MT:` 아님·빈 값·허용 문자), 진행 표시 순수 함수(E8 상태 + 경과 ms → "등록 중 · 01:05"), 결과 분류 → 화면 종류·문구, 경과 시간 형식(59초·1분·3분 30초).
- **위험**: 카메라 수명 주기(화면 전환·회전 중 카메라 해제)와 분석 스레드 → `ImageAnalysis` 실행기를 화면 수명 주기에 묶음. ZXing은 순수 Java라 단위 테스트 가능하지만 실제 인식률은 기기마다 다르다. E3·E8 모양은 서버 13b 뒤 다시 맞춘다(06e 재확인 표 그대로). 06e의 토스트 안내는 진행 화면으로 옮긴다(중복 안내 제거).
- **Studio 확인(실기기 필요)**: 권한 허용·거부·영구 거부 → 설정 이동, 실제 Matter QR 인식, 잘못된 QR, 플래시, 회전, 카메라 없는 에뮬레이터에서 직접 입력만 되는지, 진행 화면에서 탭 이동 후 복원, 다크 모드(카메라 화면은 고정 색).

## 07e. 설정 (UI-27·28·29)

| 하위 | 내용 | 파일 |
|---|---|---|
| 07e-1 | 첫 실행 안내 카드(3단계: 주소 → 토큰 → 연결 테스트), 칸별 오류(IP 형식, 포트 1~65535, 토큰 형식 — `TokenFormat` 재사용), 401이면 토큰 칸에 오류 + 포커스, 토큰·Wi-Fi 비밀번호 **가림 + 보기 토글**(눈 버튼 라벨), 2.4GHz 안내 | `fragment/SettingsFragment.java`, `fragment_settings.xml`, 새 `network/SettingsInputValidator.java` |
| 07e-2 | **연결 테스트**: 저장하지 않고 입력한 주소·토큰으로 임시 클라이언트를 만들어 E1 호출 → 성공 / 토큰 오류(401) / 연결 불가 3결과. 첫 실행은 테스트 성공 뒤에만 "저장하고 시작하기"(D8) | `SettingsFragment.java`, `RetrofitClient.java`(임시 클라이언트 생성 함수), 새 `network/ConnectionTestResult.java` |
| 07e-3 | **Wi-Fi 비밀번호 보관(UI-29, D6)**: 평문 `HomeAssistantPrefs.WIFI_PASSWORD` → Keystore 암호화(`TokenStore` 방식을 일반화), 처음 실행 때 평문 값을 옮기고 지움, 백업 규칙에서 해당 prefs 제외. 입력칸에 다시 채우지 않고 "저장됨" 힌트 | `security/TokenStore.java`(→ 키별 저장으로 일반화) 또는 새 `security/SecretStore.java`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` |

- **테스트**: 설정 입력 검사(IP·포트 경계값 0·1·65535·65536·문자), 연결 테스트 결과 분류(200·401·IOException·그 외), 평문 → 암호화 옮기기 판단 함수.
- **위험**: Wi-Fi 비밀번호를 옮기다 실패하면 등록이 안 된다 → 실패 시 평문을 지우지 않고 다음 실행에 다시 시도. 백업 제외 설정은 Android 12 전후 파일이 둘(이미 둘 다 있음).
- **Studio 확인**: 첫 실행 흐름, 연결 테스트 3결과(서버 끔·틀린 토큰), 가림·보기, 앱 재설치·백업 복원 뒤 비밀번호를 다시 물어보는지, 다크 모드.

## 07f. 서버 연결 실패 배너·F-01 (UI-01·31·32)

| 하위 | 내용 | 파일 |
|---|---|---|
| 07f-1 | 연결 상태 하나로: 정상 / 서버에 닿지 않음 / 토큰 오류(401). 어느 요청이든 통신 실패면 "닿지 않음", 성공하면 "정상", 401이면 "토큰 오류" | 새 `network/ConnectionState.java`(순수 상태 전이), `RetrofitClient.java`(인터셉터에서 갱신) |
| 07f-2 | `MainActivity` 위쪽 **배너**: 닿지 않음 → "서버에 연결할 수 없어요 · Wi-Fi와 서버 주소를 확인해 주세요 [다시 시도]", **토큰 오류 → "API 토큰이 맞지 않아요 [설정으로]"를 설정을 저장하거나 요청이 성공할 때까지 계속 표시(F-01)**. 지금의 1회 토스트·자동 이동은 첫 1번만 유지 | `MainActivity.java`, `activity_main.xml` |
| 07f-3 | 통신 실패를 기기 OFFLINE으로 바꾸지 않음(`syncDeviceState.onFailure`, UI-32): 마지막 확인값 유지 + 배너. 영문 예외 문구(`t.getMessage()`)를 화면에 쓰지 않고 "서버에 연결할 수 없어요"로(UI-31). 남은 토스트를 성공은 없애고 실패는 Snackbar·배너로 정리 | `DeviceViewModel.java` |

- **테스트**: `ConnectionState` 전이(실패 → 닿지 않음, 성공 → 정상, 401 → 토큰 오류, 토큰 오류 중 다른 요청 실패는 토큰 오류 유지, 설정 저장 → 초기화), 통신 실패 때 기기 상태가 바뀌지 않는지(`PendingStates`).
- **위험**: 요청이 많아 배너가 깜빡일 수 있다 → 상태가 바뀔 때만 갱신. 401 자동 이동과 배너가 겹치지 않게(첫 1번 이동 + 배너 유지).
- **Studio 확인**: 서버 끔 → 배너·다시 시도, 서버 켬 → 배너 사라짐, 틀린 토큰 → 탭을 옮겨도 배너 유지, 설정 저장 뒤 사라짐, 다크 모드.

---

## 이번에 빼는 것 (UI 리뷰 2차·나중)

| 항목 | 원래 분류 | 빼는 이유 / 언제 |
|---|---|---|
| UI-05 탭 전환 때 Fragment 상태 유지 | 나중 | 3탭 구조가 자리 잡은 뒤. 이번엔 홈 진입마다 목록 요청으로 충분 |
| UI-07 edge-to-edge 인셋 | 나중 | `targetSdk 35`라 Android 15에서 강제됨 → 07a~07f 화면 확인 때 겹침이 보이면 그때 별도 항목 |
| UI-08 아이콘 전체 교체 | 나중 | 새 화면에 필요한 아이콘만 벡터로 추가(D9), 나머지 교체는 나중 |
| UI-12·UI-40 `DiffUtil`·갱신 정리 | 2차·나중 | 실서버에서 체감 확인 뒤 |
| UI-13 전력 위젯 폭 | 2차 | 타일로 바뀌며 새로 판정(07b Studio 확인) |
| UI-15 위젯 편집 저장·완료 버튼 | 나중 | D2에서 위젯 편집을 없애면 해당 없음 |
| UI-16 카드 헤더 정리 | 나중 | 타일로 바뀌며 해당 없음 |
| UI-21 기기 관리 카드 모양 | 2차 | 07b-4에서 목록을 새로 그리며 함께 판정 |
| UI-30 현재 Wi-Fi 불러오기 | 나중 | 위치 권한이 필요해 별도 결정 |
| UI-33 조회 토스트 | 2차 | 대부분 APP-05에서 정리, 남은 것은 07f-3 |
| UI-37 컴포넌트 스타일 통일 | 나중 | 07a는 색·글자만. 카드 반경·여백 통일은 나중 |
| UI-38 문자열 리소스화 | 나중 | 앱 규칙(문구는 코드에 직접) 유지. 다국어 계획이 생기면 |
| UI-39 회전 시 대화상자 | 나중 | 기기 추가가 전체 화면이 되면(D5) 대부분 해결 |
| UI-41 미사용 `item_dashboard_tile.xml` | 나중 | 07b 지울 목록에 함께 올린다 |
| 상세 방식 3(시트) | 사용자 결정 | 컴포넌트만 준비, 전환은 나중 |
| 홈 타일 추이선(기본) | 사용자 결정 53 | 기본은 끔. 사용자가 켠 기기만 07g-3에서 그린다 |
| HTTPS·외부 접속 | 계약 보류 | 서버 로드맵과 함께 |

## 사용자가 정할 항목

| # | 질문 | 선택지 | 추천 |
|---|---|---|---|
| D1 | E7 일/주/월 집계(`period`) 화면을 이번 UI 1차에 넣을까 (APP-06 D2에서 UI 1차로 미룸) | A 이번에 07c-4로 / B 다음으로 미룸(원본 모드만) | **A** — 서버 12a에서 집계가 구현됐고 목업 04에 탭 자리가 있다. 날짜 선택기 없이 기간을 앱이 고정(일 7일·주 8주·월 6개월)하면 작게 끝난다. 07c의 마지막 하위 단계라 빼기도 쉽다 |
| D2 | 홈을 타일 격자로 바꾸면서 지금의 섹션·위젯 구조(위젯 편집·드래그·위젯 삭제)를 없앨까 | A 없앰(차트는 상세로) / B 타일 안에 위젯 구조 유지 | **A** — 목업 C안이 확정안이고, 위젯 삭제는 저장되지 않는 반쪽 기능이다(앱 `CLAUDE.md`). 지울 파일은 구현 때 목록으로 승인 |
| D3 | 기기 순서 바꾸기 위치 | A 기기 탭 순서 핸들(홈은 같은 순서로 표시) / B 홈 타일 길게 누르기 유지 | **A** — 목업 01 기기 탭과 같고, 홈 타일 길게 누르기와 스위치·상세 진입이 겹치지 않는다. 저장 키 `device_order`는 그대로 |
| D4 | 상세 화면 구현 형태 | A `MainActivity` 안 Fragment(뒤로 스택) / B 지금처럼 별도 Activity | **A** — `DeviceViewModel`을 공유해 목록·제어·배너·401 처리가 그대로 이어진다(지금 상세는 따로 된 ViewModel이라 목록이 비어 있음). 나중 방식 3의 시트도 Fragment라 같은 컴포넌트를 쓴다 |
| D5 | 기기 추가 형태 | A 전체 화면 Fragment(입력 → QR → 진행 → 결과) / B 대화상자 유지 + 진행만 화면 | **A** — QR 카메라·진행·결과가 한 흐름이고 회전·탭 이동에도 안전하다 |
| D6 | Wi-Fi 비밀번호 보관(UI-29) | A 토큰처럼 Keystore 암호화 + 백업 제외, 평문 값은 옮기고 지움 / B 평문 유지 + 백업만 제외 / C 지금 그대로 | **A** — 비밀값을 평문으로 두지 않는 앱 규칙과 맞고, 토큰 저장 코드를 다시 쓴다 |
| D7 | CameraX·ZXing 버전 | A CameraX 1.4.2, ZXing core 3.5.3(내가 아는 안정판) + 구현 전 Studio에서 최신 안정판 확인 / B 사용자가 버전 지정 | **A** — 확인 결과 더 새 안정판이 있으면 그 버전으로 바꾸고 보고한다 |
| D8 | 첫 실행에서 연결 테스트 성공 전 저장을 막을까 | A 첫 실행만 막음(재방문은 테스트 없이 저장 가능) / B 항상 막음 / C 막지 않음 | **A** — 첫 실행의 잘못된 설정을 막되, 서버가 꺼져 있을 때도 주소를 바꿀 수 있어야 한다 |
| D10 | 07g 타일에서 켜고 끌 항목 | A 현재 W·누적 사용량·추이선·크기(1/2칸) 4개, 이름·상태 칩·스위치는 항상 보임 / B A + 상태 칩·스위치도 끌 수 있음 / C 현재 W·누적만 | **A** — 요청한 후보 그대로. 스위치·칩을 끄면 타일이 제어·상태를 못 보여 줘 홈의 목적과 어긋난다 |
| D11 | 기본값 | A 현재 W 켬·누적 켬·추이선 끔·1칸(07b 지금 모양과 같음) / B 모두 끔(최소 타일) | **A** — 업데이트해도 홈 모양이 바뀌지 않고, 결정 53(추이선 기본 없음)과 맞다 |
| D12 | 저장 형식 | A prefs `TilePrefs`, 키 `tile.<deviceId>`, 값 Gson JSON(`v` 버전 포함) / B 키를 항목마다 나눔(`<id>_tile_power` 등 4개) / C `DashboardPrefs`에 쉼표 문자열 | **A** — 기기 하나가 키 하나라 지우기·옮기기가 쉽고, 항목이 늘어도 키가 안 늘며, 버전으로 형식 변경에 대비한다 |
| D13 | 꾸미기 화면 형태 | A 아래에서 올라오는 시트(BottomSheetDialogFragment) — 상세·기기 탭 어디서나 같은 시트 / B 전체 화면 Fragment | **A** — 스위치 4개라 짧고, 두 진입점에서 그대로 띄울 수 있다. 상세 방식 3(시트)과도 모양이 맞는다 |
| D14 | 추이선 기간 | A 최근 7일 일별 사용량(E7 `period=day`, 07c-4 집계 재사용) / B 최근 기록 50건의 구간 사용량(E7 원본) / C 오늘 24시간 | **A** — 서버 집계라 요청 1번에 막대 7개 크기로 작고, 기록 간격과 상관없이 모양이 일정하다. B는 기록 간격에 따라 몇 시간~며칠로 들쭉날쭉하다 |
| D15 | 저장 시점 | A 스위치를 바꾸는 즉시 저장 + "기본값으로" 버튼 / B "저장" 버튼을 눌러야 저장 | **A** — 항목이 적고 미리보기가 바로 바뀌므로 따로 저장 단계가 필요 없다 |
| D16 | 삭제한 기기의 설정 정리 | A E4 삭제 성공 때 그 기기 키를 지움 / B A + 목록(E1)을 받을 때 목록에 없는 키도 지움 / C 지우지 않음 | **A** — B는 서버가 잠시 목록에서 빼거나(숨김) 다른 서버로 바꿨을 때 설정이 사라질 수 있다. 다른 폰·서버에서 지운 기기의 키는 남지만 수십 바이트라 문제없다. 90일 안에 다시 등록하면 설정은 기본값부터 시작 |
| D9 | 아이콘(UI-08 일부) | A 새 화면에 필요한 약 12개만 Material Symbols 벡터(Apache 2.0)로 `res/drawable`에 추가 / B 시스템 아이콘 유지 | **A** — 새 의존성 없이 벡터 파일만 추가. 라이선스 표기는 학습 페이지·README에 |

## 남은 위험·확인

- 단계가 많아 화면 확인이 몰린다 → 07a·07b가 끝날 때 한 번, 07c·07d 뒤 한 번, 07e·07f 뒤 한 번 Studio 확인을 묶어서 한다.
- `ui-1`은 `v2.0` 위라 v1.0 운영 서버와 맞지 않는다. 실서버 확인은 서버 v2.0 뒤. 화면 모양은 서버 없이도 Studio에서 확인할 수 있다(빈 상태·연결 실패 배너 등).
- 06e(E3·E8)를 서버 13b 뒤 다시 맞추면 07d-3도 함께 본다.
- 지울 파일(07b 위젯 구조, 07c 상세 Activity·선 차트)은 해당 하위 단계 구현 때 목록을 먼저 보고하고 승인 뒤 지운다.
