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
import com.example.homeassistantapp.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceViewModel extends ViewModel {
    private final MutableLiveData<List<Device>> deviceList = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public LiveData<List<Device>> getDeviceList() { return deviceList; }
    public LiveData<String> getToastMessage() { return toastMessage; }

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
}
