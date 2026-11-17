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
    private final MutableLiveData<OneTimeEvent<String>> toastMessage = new MutableLiveData<>();
    private MutableLiveData<List<EnergyLog>> detailEnergyHistory = new MutableLiveData<>();


    public LiveData<List<Device>> getDeviceList() { return deviceList; }
    public LiveData<OneTimeEvent<String>> getToastMessage() { return toastMessage; }
    public LiveData<List<EnergyLog>> getDetailEnergyHistory() {return detailEnergyHistory;}

    private final PendingStates pendingStates = new PendingStates();

    // OFF 뒤 상태 재확인 예약. 기기별로 하나만 둔다.
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
            syncDeviceState(deviceId, true); // true: 서버 저장값이 아니라 기기에 직접 조회
        };
        pendingRechecks.put(deviceId, recheck);
        mainHandler.postDelayed(recheck, PowerOffRecheck.DELAY_MS);
    }

    @Override
    protected void onCleared() {
        mainHandler.removeCallbacksAndMessages(null);
        pendingRechecks.clear();
        super.onCleared();
    }

    private void showToast(String message) {
        toastMessage.setValue(new OneTimeEvent<>(message));
    }

    private void showFailure(String prefix, Response<?> response) {
        if (ErrorMessages.isUnauthorized(response.code())) return;
        showToast(prefix + ErrorMessages.describe(response));
    }

    private void beginLoading(String deviceId) {
        pendingStates.begin(deviceId, currentState(deviceId));
        updateDevice(deviceId, device -> device.setState(PendingStates.LOADING));
    }

    /** newState가 null이면(실패) 요청 전 상태로 되돌린다. */
    private void finishLoading(String deviceId, String newState) {
        String shown = pendingStates.finish(deviceId, newState);
        updateDevice(deviceId, device -> device.setState(shown));
    }

    public void fetchDevices() {
        RetrofitClient.getApi().getDeviceList().enqueue(new Callback<DeviceListResponse>() {
            @Override
            public void onResponse(Call<DeviceListResponse> call, Response<DeviceListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Device> devices = response.body().devices;
                    if (devices != null) {
                        for (Device device : devices) {
                            pendingStates.remember(device.getId(), device.getState());
                        }
                    }
                    deviceList.setValue(devices);
                } else {
                    showFailure("목록 로드 실패: ", response);
                }
            }

            @Override
            public void onFailure(Call<DeviceListResponse> call, Throwable t) {
                Log.d("DeviceViewModel", "fetchDevices onFailure: " + t.getMessage());
                showToast("서버 연결 실패: " + t.getMessage());
            }
        });
    }

    public void controlDevice(String targetId, String command) {
        cancelRecheck(targetId);
        beginLoading(targetId);

        ControlRequest request = new ControlRequest(targetId, command);
        RetrofitClient.getApi().controlDevice(request).enqueue(new Callback<ControlResponse>() {
            @Override
            public void onResponse(Call<ControlResponse> call, Response<ControlResponse> response) {
                ControlResponse body = response.body();
                if (response.isSuccessful() && body != null && body.isCommandSucceeded()) {
                    showToast(body.message);
                    finishLoading(targetId, body.status);
                    if (PowerOffRecheck.shouldRecheck(command, true)) {
                        scheduleRecheck(targetId);
                    }
                } else if (response.isSuccessful() && body != null) {
                    // 서버는 명령이 실패해도 200을 준다. 이때 status는 서버가 아는 현재 상태다.
                    showToast("제어 실패: " + (body.message != null ? body.message : "알 수 없는 응답"));
                    finishLoading(targetId, body.status);
                } else {
                    // 400은 요청 형식 오류라 기기 상태와 무관하다. 401 이동은 UnauthorizedInterceptor가 맡는다.
                    finishLoading(targetId, null);
                    showFailure("제어 실패: ", response);
                }
            }

            @Override
            public void onFailure(Call<ControlResponse> call, Throwable t) {
                finishLoading(targetId, null);
                showToast("제어 에러: " + t.getMessage());
            }
        });
    }

    public void addDevice(String type, String name, String payload, String ssid, String password, List<Integer> driver) {
        AddDeviceRequest request = new AddDeviceRequest(type, name, payload, ssid, password, driver);
        showToast("기기 등록을 서버에 요청했습니다...");
        List<Device> currentList = deviceList.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        } else {
            currentList = new ArrayList<>(currentList);
        }

        // 등록이 끝날 때까지 목록 맨 위에 보여 줄 임시 항목
        Device tempDevice = new Device();
        tempDevice.setId("TEMP_PAIRING");
        tempDevice.setName(name);
        tempDevice.setPairing(true);
        tempDevice.setType(type);
        currentList.add(0, tempDevice);

        deviceList.setValue(currentList);

        RetrofitClient.getApi().addDevice(request).enqueue(new Callback<AddDeviceResponse>() {
            @Override
            public void onResponse(Call<AddDeviceResponse> call, Response<AddDeviceResponse> response){
                if(response.isSuccessful() && response.body() != null){
                    showToast("등록 성공: " + response.body().message);
                    fetchDevices();
                } else{
                    showFailure("등록 실패: ", response);
                    removeTempDevice();
                }
            }
            @Override
            public void onFailure(Call<AddDeviceResponse> call, Throwable t){
                showToast("통신 실패: " + t.getMessage());
                removeTempDevice();
            }
        });
    }
    private void removeTempDevice() {
        List<Device> currentList = deviceList.getValue();
        if (currentList != null) {
            List<Device> newList = new ArrayList<>(currentList);
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
        beginLoading(deviceId);
        showToast("기기 삭제 중...");

        RetrofitClient.getApi().deleteDevice(new DeleteDeviceRequest(deviceId)).enqueue(new Callback<ControlResponse>() {
            @Override
            public void onResponse(Call<ControlResponse> call, Response<ControlResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    finishLoading(deviceId, null);
                    showToast("삭제 성공!");
                    fetchDevices();
                } else {
                    finishLoading(deviceId, null);
                    showFailure("삭제 실패: ", response);
                }
            }

            @Override
            public void onFailure(Call<ControlResponse> call, Throwable t) {
                finishLoading(deviceId, null);
                showToast("통신 실패: " + t.getMessage());
            }
        });
    }
    public void syncDeviceState(String deviceId, boolean isManualRefresh) {
        beginLoading(deviceId);
        RetrofitClient.getApi().getDeviceState(deviceId,isManualRefresh).enqueue(new Callback<DeviceStateResponse>() {
            @Override
            public void onResponse(Call<DeviceStateResponse> call, Response<DeviceStateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    finishLoading(deviceId, response.body().state);
                } else {
                    finishLoading(deviceId, null);
                }
            }

            @Override
            public void onFailure(Call<DeviceStateResponse> call, Throwable t) {
                finishLoading(deviceId, "OFFLINE");
            }
        });
    }
    public void fetchDeviceEnergy(String deviceId) {
        // 대시보드가 화면에 돌아올 때마다 자동으로 부르므로 진행·완료 토스트는 띄우지 않는다.
        beginLoading(deviceId);
        RetrofitClient.getApi().getDeviceEnergy(deviceId).enqueue(new Callback<EnergyResponse>() {
            @Override
            public void onResponse(Call<EnergyResponse> call, Response<EnergyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    // 서버 단위는 mW·mWh
                    double currentPowerW = response.body().activePowerMw / 1000.0;
                    double totalEnergyKwh = response.body().totalEnergyMwh / 1000000.0;

                    String powerText = String.format("실시간: %.1f W / 누적: %.4f kWh", currentPowerW, totalEnergyKwh);

                    updateDevice(deviceId, device -> device.setPowerInfo(powerText));
                    finishLoading(deviceId, response.body().device_status);

                } else {
                    // 400은 없는 기기·서버 오류라 기기가 꺼졌다는 뜻이 아니다. 상태는 그대로 둔다.
                    finishLoading(deviceId, null);
                    showFailure("전력 조회 실패: ", response);
                }
            }

            @Override
            public void onFailure(Call<EnergyResponse> call, Throwable t) {
                finishLoading(deviceId, null);
                showToast("통신 실패: " + t.getMessage());
            }
        });
    }

    public void fetchDeviceEnergyHistory(String deviceId) {
        beginLoading(deviceId);

        RetrofitClient.getApi().getDeviceEnergyHistory(deviceId).enqueue(new Callback<EnergyLogResponse>() {
            @Override
            public void onResponse(Call<EnergyLogResponse> call, Response<EnergyLogResponse> response) {
                EnergyLogResponse body = response.body();
                if (response.isSuccessful() && body != null && body.hasError()) {
                    // 이 API는 실패도 200 + {"error"}로 온다.
                    finishLoading(deviceId, null);
                    showToast("전력량 로그를 불러오지 못했습니다: " + body.error);
                } else if (response.isSuccessful() && body != null) {
                    detailEnergyHistory.setValue(body.getLogs());
                    updateDevice(deviceId, device -> device.setEnergyLogs(body.getLogs()));
                    finishLoading(deviceId, body.stateOr(null));
                }
                else{
                    finishLoading(deviceId, null);
                    showFailure("전력량 로그를 불러오지 못했습니다: ", response);
                }
            }

            @Override
            public void onFailure(Call<EnergyLogResponse> call, Throwable t) {
                finishLoading(deviceId, null);
                showToast("전력량 로그를 불러오지 못했습니다.");
            }
        });
    }

    private String currentState(String deviceId) {
        List<Device> currentList = deviceList.getValue();
        if (currentList == null) return null;
        for (Device device : currentList) {
            if (deviceId.equals(device.getId())) {
                return device.getState();
            }
        }
        return null;
    }

    private void updateDevice(String deviceId, Consumer<Device> updateAction) {
        List<Device> currentList = deviceList.getValue();
        if (currentList != null) {
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).getId().equals(deviceId)) {
                    updateAction.accept(currentList.get(i));
                    // 같은 리스트를 다시 넣어 옵저버를 깨운다.
                    deviceList.setValue(currentList);
                    break;
                }
            }
        }
    }
    public void updateDeviceOrder(List<Device> reorderedList) {
        deviceList.setValue(new ArrayList<>(reorderedList));

        // TODO: 앱을 다시 켜도 순서가 유지되도록 영구 저장
    }
}

