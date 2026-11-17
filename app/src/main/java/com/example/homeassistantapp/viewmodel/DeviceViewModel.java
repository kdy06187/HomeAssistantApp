package com.example.homeassistantapp.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.homeassistantapp.dto.AddDeviceRequest;
import com.example.homeassistantapp.dto.AddDeviceResponse;
import com.example.homeassistantapp.dto.ControlRequest;
import com.example.homeassistantapp.dto.ControlResponse;
import com.example.homeassistantapp.dto.DeleteDeviceRequest;
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.dto.DeviceListResponse;
import com.example.homeassistantapp.dto.DeviceStateResponse;
import com.example.homeassistantapp.dto.EnergyLog;
import com.example.homeassistantapp.dto.EnergyLogResponse;
import com.example.homeassistantapp.dto.EnergyResponse;
import com.example.homeassistantapp.network.ErrorMessages;
import com.example.homeassistantapp.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceViewModel extends ViewModel {
    private final MutableLiveData<List<Device>> deviceList = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private MutableLiveData<List<EnergyLog>> detailEnergyHistory = new MutableLiveData<>();


    public LiveData<List<Device>> getDeviceList() { return deviceList; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<List<EnergyLog>> getDetailEnergyHistory() {return detailEnergyHistory;}

    // ── OFF 뒤 상태 재확인 (불일치 #15) ──
    // 메인 스레드에서 50초 뒤 실행할 작업을 기기별로 들고 있다. 같은 기기에 새 명령이 오면 예전 예약은 취소한다.
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Runnable> pendingRechecks = new HashMap<>();

    private void cancelRecheck(String deviceId) {
        Runnable pending = pendingRechecks.remove(deviceId);
        if (pending != null) mainHandler.removeCallbacks(pending);
    }

    private void scheduleRecheck(String deviceId) {
        cancelRecheck(deviceId);
        Runnable recheck = () -> {
            pendingRechecks.remove(deviceId);
            // true: 서버에 저장된 값이 아니라 기기에 바로 물어보게 한다(계약서 E5 isManualRefresh).
            syncDeviceState(deviceId, true);
        };
        pendingRechecks.put(deviceId, recheck);
        mainHandler.postDelayed(recheck, PowerOffRecheck.DELAY_MS);
    }

    @Override
    protected void onCleared() {
        // 화면이 완전히 사라지면 예약도 버린다(사라진 ViewModel이 요청을 보내지 않게).
        mainHandler.removeCallbacksAndMessages(null);
        pendingRechecks.clear();
        super.onCleared();
    }

    public void fetchDevices() {
        RetrofitClient.getApi().getDeviceList().enqueue(new Callback<DeviceListResponse>() {
            @Override
            public void onResponse(Call<DeviceListResponse> call, Response<DeviceListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 데이터가 도착하면 LiveData에 값을 집어넣음 -> View가 자동으로 알아챔!
                    deviceList.setValue(response.body().devices);
                } else {
                    toastMessage.setValue("목록 로드 실패: " + ErrorMessages.describe(response)); // 불일치 #13
                }
            }

            @Override
            public void onFailure(Call<DeviceListResponse> call, Throwable t) {
                Log.d("DeviceViewModel", "fetchDevices onFailure: " + t.getMessage());
                toastMessage.setValue("서버 연결 실패: " + t.getMessage());
            }
        });
    }

    // 제어 명령 전송 로직
    public void controlDevice(String targetId, String command) {
        // 실패하면 LOADING을 풀고 원래 상태로 돌리기 위해 미리 기억해 둔다(불일치 #11).
        final String previousState = findState(targetId);
        cancelRecheck(targetId); // 새 명령이 왔으니 예전 OFF 재확인 예약은 의미가 없다(불일치 #15)
        updateDevice(targetId, device -> device.setState("LOADING")); // 제어 시작 시 상태를 LOADING으로 표시

        ControlRequest request = new ControlRequest(targetId, command);
        RetrofitClient.getApi().controlDevice(request).enqueue(new Callback<ControlResponse>() {
            @Override
            public void onResponse(Call<ControlResponse> call, Response<ControlResponse> response) {
                ControlResponse body = response.body();
                if (response.isSuccessful() && body != null && body.isCommandSucceeded()) {
                    // 성공: 2xx + "명령 성공!" (계약서 §4.1, 불일치 #5)
                    toastMessage.setValue(body.message);
                    updateDevice(targetId, device -> device.setState(stateOr(body.status, previousState)));
                    // PC 드라이버 기기는 OFF 응답 뒤 45초가 지나야 실제로 꺼진다. 50초 뒤 E5로 다시 확인(불일치 #15).
                    if (PowerOffRecheck.shouldRecheck(command, true)) {
                        scheduleRecheck(targetId);
                    }
                } else if (response.isSuccessful() && body != null) {
                    // 명령 실패인데 200으로 온 경우(§6 I-01). status는 서버가 아는 현재 상태(또는 UNKNOWN)다.
                    toastMessage.setValue("제어 실패: " + (body.message != null ? body.message : "알 수 없는 응답"));
                    updateDevice(targetId, device -> device.setState(stateOr(body.status, previousState)));
                } else {
                    // 400은 명령/JSON 형식 오류일 뿐 기기 상태와 상관없다. OFFLINE으로 바꾸지 않고 되돌린다(불일치 #6).
                    // 401이면 UnauthorizedInterceptor가 설정 화면으로 보낸다.
                    updateDevice(targetId, device -> device.setState(previousState));
                    toastMessage.setValue("제어 실패: " + ErrorMessages.describe(response)); // 서버 문구(불일치 #13)
                }
            }

            @Override
            public void onFailure(Call<ControlResponse> call, Throwable t) {
                updateDevice(targetId, device -> device.setState(previousState)); // LOADING 해제(불일치 #11)
                toastMessage.setValue("제어 에러: " + t.getMessage());
            }
        });
    }

    /** 서버가 준 상태가 비어 있으면(null) 대신 쓸 값을 고른다. null로 덮어쓰면 화면 판정이 꼬인다. */
    private static String stateOr(String state, String fallback) {
        return state != null ? state : fallback;
    }
    // 커미셔닝 요청
    public void addDevice(String type, String name, String payload, String ssid, String password, List<Integer> driver) {
        AddDeviceRequest request = new AddDeviceRequest(type, name, payload, ssid, password, driver);
        // 로딩 토스트 메시지 띄우기
        toastMessage.setValue("기기 등록을 서버에 요청했습니다...");
        // 페어링 중인 기기 추가
        List<Device> currentList = deviceList.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        } else {
            currentList = new ArrayList<>(currentList);
        }

        Device tempDevice = new Device();
        tempDevice.setId("TEMP_PAIRING"); // 식별용 임시 ID
        tempDevice.setName(name);
        tempDevice.setPairing(true);
        tempDevice.setType(type);
        currentList.add(0, tempDevice);

        deviceList.setValue(currentList);

        RetrofitClient.getApi().addDevice(request).enqueue(new Callback<AddDeviceResponse>() {
            @Override
            public void onResponse(Call<AddDeviceResponse> call, Response<AddDeviceResponse> response){
                if(response.isSuccessful() && response.body() != null){
                    toastMessage.setValue("등록 성공: " + response.body().message);
                    fetchDevices();
                } else{
                    toastMessage.setValue("등록 실패: " + ErrorMessages.describe(response)); // 불일치 #13
                    removeTempDevice();
                }
            }
            @Override
            public void onFailure(Call<AddDeviceResponse> call, Throwable t){
                toastMessage.setValue("통신 실패: " + t.getMessage());
                removeTempDevice();
            }
        });
    }
    private void removeTempDevice() {
        List<Device> currentList = deviceList.getValue();
        if (currentList != null) {
            List<Device> newList = new ArrayList<>(currentList);
            // 안드로이드 API 수준에 따라 Iterator 또는 removeIf 사용
            for (int i = 0; i < newList.size(); i++) {
                if ("TEMP_PAIRING".equals(newList.get(i).getId())) {
                    newList.remove(i);
                    break;
                }
            }
            deviceList.setValue(newList);
        }
    }
    public void deleteDevice(String deviceId) {
        final String previousState = findState(deviceId); // 실패 시 LOADING 해제용(불일치 #11)
        updateDevice(deviceId, device -> device.setState("LOADING"));
        toastMessage.setValue("기기 삭제 중...");

        RetrofitClient.getApi().deleteDevice(new DeleteDeviceRequest(deviceId)).enqueue(new Callback<ControlResponse>() {
            @Override
            public void onResponse(Call<ControlResponse> call, Response<ControlResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    toastMessage.setValue("삭제 성공!");
                    fetchDevices(); // 🌟 삭제가 완료되면 서버에서 최신 리스트를 다시 불러와 갱신!
                } else {
                    updateDevice(deviceId, device -> device.setState(previousState));
                    toastMessage.setValue("삭제 실패: " + ErrorMessages.describe(response)); // 불일치 #13
                }
            }

            @Override
            public void onFailure(Call<ControlResponse> call, Throwable t) {
                updateDevice(deviceId, device -> device.setState(previousState));
                toastMessage.setValue("통신 실패: " + t.getMessage());
            }
        });
    }
    public void syncDeviceState(String deviceId, boolean isManualRefresh) {
        final String previousState = findState(deviceId); // 실패 시 LOADING 해제용(불일치 #11)
        updateDevice(deviceId, device -> device.setState("LOADING")); // 상태 동기화 시작 시 LOADING 표시
        RetrofitClient.getApi().getDeviceState(deviceId,isManualRefresh).enqueue(new Callback<DeviceStateResponse>() {
            @Override
            public void onResponse(Call<DeviceStateResponse> call, Response<DeviceStateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String realState = response.body().state; // "ON", "OFF", "UNKNOWN" 등

                    // 🌟 현재 LiveData에 있는 기기 리스트를 꺼내서, 상태가 바뀐 기기만 업데이트!
                    updateDevice(deviceId, device -> device.setState(stateOr(realState, previousState)));
                } else {
                    // 예전에는 이 경우 아무것도 안 해서 LOADING이 그대로 남았다(불일치 #11).
                    updateDevice(deviceId, device -> device.setState(previousState));
                }
            }

            @Override
            public void onFailure(Call<DeviceStateResponse> call, Throwable t) {
                // 조용히 실패 처리 (상태 갱신 실패는 앱을 터뜨릴 정돈 아니므로 로그만 남기거나 무시)
                updateDevice(deviceId, device -> device.setState("OFFLINE"));
            }
        });
    }
    public void fetchDeviceEnergy(String deviceId) {
        toastMessage.setValue("전력량 조회 중...");
        final String previousState = findState(deviceId); // 실패 시 LOADING 해제용(불일치 #11)
        updateDevice(deviceId, device -> device.setState("LOADING"));
        RetrofitClient.getApi().getDeviceEnergy(deviceId).enqueue(new Callback<EnergyResponse>() {
            @Override
            public void onResponse(Call<EnergyResponse> call, Response<EnergyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    // 1. 밀리(m) 단위를 일반 사용자가 보기 편한 W, kWh로 변환
                    double currentPowerW = response.body().activePowerMw / 1000.0;
                    double totalEnergyKwh = response.body().totalEnergyMwh / 1000000.0;

                    // 2. 화면에 띄울 예쁜 문자열 포맷팅 (예: "실시간: 15.0 W / 누적: 0.5400 kWh")
                    String powerText = String.format("실시간: %.1f W / 누적: %.4f kWh", currentPowerW, totalEnergyKwh);

                    // 3. 현재 LiveData의 리스트를 꺼내서 해당 기기의 전력 텍스트만 업데이트!
                    updateDevice(deviceId, device -> {
                        device.setPowerInfo(powerText);
                        device.setState(stateOr(response.body().device_status, previousState)); // 서버가 알려 준 기기 상태
                    });
                    toastMessage.setValue("전력량 업데이트 완료!");

                } else {
                    // E6의 400은 "등록되지 않은 기기"나 "서버 내부 오류"다. 기기가 꺼졌다는 뜻이 아니므로
                    // OFFLINE으로 바꾸지 않고 요청 전 상태로 되돌린 뒤, 서버의 error 문구를 보여 준다(불일치 #12).
                    updateDevice(deviceId, device -> device.setState(previousState));
                    toastMessage.setValue("전력 조회 실패: " + ErrorMessages.describe(response));
                }
            }

            @Override
            public void onFailure(Call<EnergyResponse> call, Throwable t) {
                updateDevice(deviceId, device -> device.setState(previousState)); // LOADING 해제(불일치 #11)
                toastMessage.setValue("통신 실패: " + t.getMessage());
            }
        });
    }

    public void fetchDeviceEnergyHistory(String deviceId) {
        // 오류 응답이 오면 LOADING 대신 원래 상태로 돌려놓기 위해 미리 기억해 둔다(불일치 #7).
        final String previousState = findState(deviceId);
        updateDevice(deviceId, device -> device.setState("LOADING"));

        RetrofitClient.getApi().getDeviceEnergyHistory(deviceId).enqueue(new Callback<EnergyLogResponse>() {
            @Override
            public void onResponse(Call<EnergyLogResponse> call, Response<EnergyLogResponse> response) {
                EnergyLogResponse body = response.body();
                if (response.isSuccessful() && body != null && body.hasError()) {
                    // 계약서 E7: 실패도 HTTP 200 + {"error"} 로 온다. 성공으로 처리하면 상태가 null이 되어 앱이 죽었다.
                    updateDevice(deviceId, device -> device.setState(previousState));
                    toastMessage.setValue("전력량 로그를 불러오지 못했습니다: " + body.error);
                } else if (response.isSuccessful() && body != null) {
                    // 🌟 서버에서 받아온 차트 데이터를 기기 리스트가 아닌, 전용 파이프라인에 쏩니다!
                    detailEnergyHistory.setValue(body.getLogs());
                    updateDevice(deviceId, device -> {
                        device.setEnergyLogs(body.getLogs());
                        // device_status 가 비어 있어도 null 로 덮어쓰지 않는다.
                        device.setState(body.stateOr(previousState));
                    });
                }
                else{
                    // E7 오류는 보통 200 + error로 오므로(위 갈래), 여기는 401 같은 예외 경우다.
                    // 기기 상태와 상관없으므로 요청 전 상태로 되돌리고 서버 문구를 보여 준다(불일치 #12·#13과 같은 방식).
                    updateDevice(deviceId, device -> device.setState(previousState));
                    toastMessage.setValue("전력량 로그를 불러오지 못했습니다: " + ErrorMessages.describe(response));
                }
            }

            @Override
            public void onFailure(Call<EnergyLogResponse> call, Throwable t) {
                updateDevice(deviceId, device -> device.setState(previousState)); // LOADING 해제(불일치 #11)
                toastMessage.setValue("전력량 로그를 불러오지 못했습니다.");
            }
        });
    }
    /**
     * 목록에서 기기의 현재 상태를 찾는다. 없으면(예: DeviceDetailActivity 의 빈 목록) null.
     * 이미 LOADING이면(다른 요청이 진행 중) null을 돌려준다. LOADING을 "원래 상태"로 기억했다가
     * 되돌리면 실패 뒤에도 LOADING이 남기 때문이다(불일치 #11). null은 화면에서 "알 수 없음"으로 보인다.
     */
    private String findState(String deviceId) {
        List<Device> currentList = deviceList.getValue();
        if (currentList == null) return null;
        for (Device device : currentList) {
            if (deviceId.equals(device.getId())) {
                String state = device.getState();
                return "LOADING".equals(state) ? null : state;
            }
        }
        return null;
    }

    private void updateDevice(String deviceId, Consumer<Device> updateAction) {
        List<Device> currentList = deviceList.getValue();
        if (currentList != null) {
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).getId().equals(deviceId)) {
                    // 1. 전달받은 구체적인 데이터 변경 작업 실행
                    updateAction.accept(currentList.get(i));
                    // 2. 리스트 갱신 알림 (공통)
                    deviceList.setValue(currentList);
                    break;
                }
            }
        }
    }
    public void updateDeviceOrder(List<Device> reorderedList) {
        // 1. LiveData가 바라보는 데이터를 순서가 바뀐 새 리스트로 교체
        deviceList.setValue(new ArrayList<>(reorderedList));

        // TODO: (선택 사항) 나중에는 앱을 껐다 켜도 순서가 유지되도록
        // 여기서 Room DB나 서버 API(SharedPreferences 등)에 바뀐 순서를 영구 저장하는 로직이 들어가야 합니다.
    }
}

