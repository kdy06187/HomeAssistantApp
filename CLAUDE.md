# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트

이 저장소 밖에 있는 자체 호스팅 스마트홈 서버용 Android 클라이언트입니다(Java, XML 레이아웃, 단일 `:app` 모듈). 앱은 기기와 직접 통신하지 않고 서버의 REST API만 호출하므로, `dto/`의 요청/응답 형식은 서버 패킷과 일치해야 합니다.

## 명령어

저장소 루트에서 실행합니다. cmd나 PowerShell에서는 `./gradlew` 대신 `gradlew.bat`을 사용합니다.

```bash
./gradlew assembleDebug
```

```bash
./gradlew installDebug
```

```bash
./gradlew lint
```

```bash
./gradlew testDebugUnitTest
```

```bash
./gradlew testDebugUnitTest --tests "com.example.homeassistantapp.ExampleUnitTest.addition_isCorrect"
```

```bash
./gradlew connectedDebugAndroidTest
```

템플릿 예제 테스트만 있으므로 UI와 네트워크 동작은 서버에 연결할 수 있는 기기나 에뮬레이터에서 확인해야 합니다. `local.properties`는 Git으로 관리되지 않으므로, 새로 클론한 저장소나 워크트리에서 SDK를 찾으려면 이 파일을 따로 준비하거나 `ANDROID_HOME`을 설정해야 합니다.

## 아키텍처

리포지토리 계층이 없는 단일 액티비티 MVVM 구조입니다: Fragment → `DeviceViewModel` → `RetrofitClient.getApi()` → `HomeAssistantApi`.

**공유 상태.** `MainActivity`는 하단 내비게이션에 따라 `HomeFragment`, `DeviceFragment`, `SettingsFragment`를 교체하며, 전환할 때마다 새 인스턴스를 만듭니다. Home과 Device는 모두 `requireActivity()`로 `DeviceViewModel`을 가져오므로 같은 기기 목록을 공유합니다. 여기서 비롯되는 특성은 다음과 같습니다.
- `fetchDevices()` 호출과 `toastMessage` 관찰은 `DeviceFragment`에서만 합니다. Home은 Device 탭(기본 탭)이 목록을 불러왔다는 전제로 동작하고, 다른 탭이 표시되는 동안에는 토스트가 나타나지 않습니다.
- `DeviceDetailActivity`는 자체 `DeviceViewModel`을 만들기 때문에 기기 목록이 비어 있습니다. 데이터는 `detailEnergyHistory`로만 읽습니다.

**기기 상태 갱신.** `DeviceViewModel.updateDevice()`는 `Device`를 직접 수정한 뒤 같은 목록을 다시 내보냅니다. 따라서 상태가 바뀔 때마다 `HomeFragment`의 재정렬 및 차트 요청 로직을 포함한 모든 옵저버가 다시 실행됩니다. `Device.state`에는 서버 값(`ON`, `OFF`, `OFFLINE` 등)과 클라이언트 전용 값 `LOADING`이 들어갑니다. `LOADING`은 요청 전에 설정되고 응답이 오면 덮어씌워집니다. 커미셔닝 중인 기기는 id가 `TEMP_PAIRING`이고 `isPairing = true`인 임시 항목으로 표시되므로, 기기를 순회하며 API를 호출하는 코드는 이 항목을 건너뛰어야 합니다.

**대시보드(중첩 RecyclerView).** `DashboardAdapter`는 기기마다 섹션을 하나씩 그리고, 각 섹션에는 `WidgetAdapter`가 들어 있습니다. 위젯(`view/widgets/*ViewHolder`)은 `Device.type`에 따라 정해집니다. `TCP_DIY`는 `TOGGLE`, `MATTER`는 `TOGGLE`, `ENERGY`, `CHART`를 가집니다. 드래그는 두 단계로 나뉩니다.
- 섹션은 `HomeFragment`의 `ItemTouchHelper`를 통해 길게 눌러 순서를 바꿉니다. 어떤 기기든 편집 모드이면 비활성화됩니다.
- 위젯은 해당 기기의 편집 모드에서만 각 위젯 ViewHolder의 `startDrag`로 순서를 바꿉니다. 편집 모드 중에는 백그라운드 새로고침이 드래그를 방해하지 않도록 `WidgetAdapter.setWidgets()`가 갱신을 무시합니다.

위젯 삭제는 메모리의 목록만 변경하고 저장되지 않으므로, `Device.type`으로 목록을 다시 만들면 위젯이 되살아납니다.

**네트워크.** `RetrofitClient`는 고정된 Base URL이 없는 정적 홀더입니다. `MainActivity`가 시작할 때 저장된 설정값으로 생성하고, `SettingsFragment`가 저장할 때 다시 생성합니다. 통신은 평문 HTTP입니다(`usesCleartextTraffic`). 호출은 `enqueue`로 하므로 콜백은 메인 스레드에서 실행되며 `LiveData.setValue`를 사용합니다.

**저장소.** 데이터베이스 없이 모두 SharedPreferences에 저장합니다.
- `HomeAssistantPrefs`: `SERVER_IP`, `SERVER_PORT`, `WIFI_SSID`, `WIFI_PASSWORD`. Wi-Fi 값은 커미셔닝 요청에 함께 전송되며, SSID가 설정되기 전에는 기기를 추가할 수 없습니다.
- `DashboardPrefs`: `device_order`와 `<deviceId>_widget_order`. 둘 다 쉼표로 구분합니다.

서버 기본 주소는 파일마다 다릅니다. `MainActivity`와 `RetrofitClient`는 `192.168.219.108:8000`을 쓰고, `SettingsFragment`는 `192.168.129.108:8080`을 표시합니다. 이 기본값을 수정할 때는 함께 바꿔야 합니다.

## 규칙

- `strings.xml`의 `driver_display_array`와 `driver_value_array`는 인덱스로 대응되므로 순서가 같아야 합니다. 값 `0`은 드라이버 없음을 뜻하며 빈 `driverTypes` 목록으로 전송됩니다.
- 새 의존성은 기존 방식대로 추가합니다. Retrofit, MPAndroidChart, SwipeRefreshLayout은 `app/build.gradle.kts`에 직접 적혀 있고, 나머지는 `gradle/libs.versions.toml`에 있습니다. MPAndroidChart를 위해 `settings.gradle.kts`에 JitPack이 활성화되어 있습니다.
- 코드 주석과 사용자에게 보이는 문자열은 한글로 작성하며, UI 문자열은 대부분 `strings.xml`이 아니라 코드에 직접 들어 있습니다.
- 커밋 메시지는 `feat)` 또는 `fix)` 접두사와 한글 요약을 사용하고, 파일별 메모(`- 파일명 : 변경 내용`)를 덧붙이는 경우가 많습니다.
