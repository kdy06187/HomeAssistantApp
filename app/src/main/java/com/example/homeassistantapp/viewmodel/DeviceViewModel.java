package com.example.homeassistantapp.viewmodel;

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
import com.example.homeassistantapp.dto.EnergyResponse;
import com.example.homeassistantapp.network.RetrofitClient;

import java.util.List;
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
    public void fetchDevices() {
        RetrofitClient.getApi().getDeviceList().enqueue(new Callback<DeviceListResponse>() {
            @Override
            public void onResponse(Call<DeviceListResponse> call, Response<DeviceListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 데이터가 도착하면 LiveData에 값을 집어넣음 -> View가 자동으로 알아챔!
                    deviceList.setValue(response.body().devices);
                } else {
                    toastMessage.setValue("목록 로드 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<DeviceListResponse> call, Throwable t) {
                toastMessage.setValue("서버 연결 실패: " + t.getMessage());
            }
        });
    }

    // 제어 명령 전송 로직
    public void controlDevice(String targetId, String command) {
        ControlRequest request = new ControlRequest(targetId, command);
        RetrofitClient.getApi().controlDevice(request).enqueue(new Callback<ControlResponse>() {
            @Override
            public void onResponse(Call<ControlResponse> call, Response<ControlResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    toastMessage.setValue(response.body().message);
                } else {
                    toastMessage.setValue("제어 실패");
                }
            }

            @Override
            public void onFailure(Call<ControlResponse> call, Throwable t) {
                toastMessage.setValue("제어 에러: " + t.getMessage());
            }
        });
    }
    // 커미셔닝 요청
    public void addDevice(String type, String name, String payload){
        AddDeviceRequest request = new AddDeviceRequest(type, name, payload);
        // 로딩 토스트 메시지 띄우기
        toastMessage.setValue("기기 등록을 서버에 요청했습니다...");

        RetrofitClient.getApi().addDevice(request).enqueue(new Callback<AddDeviceResponse>() {
            @Override
            public void onResponse(Call<AddDeviceResponse> call, Response<AddDeviceResponse> response){
                if(response.isSuccessful() && response.body() != null){
                    toastMessage.setValue("등록 성공: " + response.body().message);
                    fetchDevices();
                } else{
                    toastMessage.setValue("등록 실패: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<AddDeviceResponse> call, Throwable t){
                toastMessage.setValue("통신 실패: " + t.getMessage());
            }
        });
    }
    public void deleteDevice(String deviceId) {
        toastMessage.setValue("기기 삭제 중...");

        RetrofitClient.getApi().deleteDevice(new DeleteDeviceRequest(deviceId)).enqueue(new Callback<ControlResponse>() {
            @Override
            public void onResponse(Call<ControlResponse> call, Response<ControlResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    toastMessage.setValue("삭제 성공!");
                    fetchDevices(); // 🌟 삭제가 완료되면 서버에서 최신 리스트를 다시 불러와 갱신!
                } else {
                    toastMessage.setValue("삭제 실패: 서버 오류");
                }
            }

            @Override
            public void onFailure(Call<ControlResponse> call, Throwable t) {
                toastMessage.setValue("통신 실패: " + t.getMessage());
            }
        });
    }
    public void syncDeviceState(String deviceId) {
        RetrofitClient.getApi().getDeviceState(deviceId).enqueue(new Callback<DeviceStateResponse>() {
            @Override
            public void onResponse(Call<DeviceStateResponse> call, Response<DeviceStateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String realState = response.body().state; // "ON" 또는 "OFF"

                    // 🌟 현재 LiveData에 있는 기기 리스트를 꺼내서, 상태가 바뀐 기기만 업데이트!
                    updateDevice(deviceId, device -> device.setState(realState));
                }
            }

            @Override
            public void onFailure(Call<DeviceStateResponse> call, Throwable t) {
                // 조용히 실패 처리 (상태 갱신 실패는 앱을 터뜨릴 정돈 아니므로 로그만 남기거나 무시)
            }
        });
    }
    public void fetchDeviceEnergy(String deviceId) {
        toastMessage.setValue("전력량 조회 중...");

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
                    updateDevice(deviceId, device -> device.setPowerInfo(powerText));
                    toastMessage.setValue("전력량 업데이트 완료!");

                } else {
                    toastMessage.setValue("전력 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<EnergyResponse> call, Throwable t) {
                toastMessage.setValue("통신 실패: " + t.getMessage());
            }
        });
    }

    public void fetchDeviceEnergyHistory(String deviceId) {
        RetrofitClient.getApi().getDeviceEnergyHistory(deviceId).enqueue(new Callback<List<EnergyLog>>() {
            @Override
            public void onResponse(Call<List<EnergyLog>> call, Response<List<EnergyLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 🌟 서버에서 받아온 차트 데이터를 기기 리스트가 아닌, 전용 파이프라인에 쏩니다!
                    detailEnergyHistory.setValue(response.body());
                    updateDevice(deviceId, device -> device.setEnergyLogs(response.body()));
                }
            }

            @Override
            public void onFailure(Call<List<EnergyLog>> call, Throwable t) {
                toastMessage.setValue("전력량 로그를 불러오지 못했습니다.");
            }
        });
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
}

