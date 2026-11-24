package com.example.homeassistantapp.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.homeassistantapp.dto.AddDeviceRequest;
import com.example.homeassistantapp.dto.CommissionJobResponse;
import com.example.homeassistantapp.dto.ControlRequest;
import com.example.homeassistantapp.dto.ControlResponse;
import com.example.homeassistantapp.dto.DeleteDeviceRequest;
import com.example.homeassistantapp.dto.DeleteDeviceResponse;
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.dto.DeviceListResponse;
import com.example.homeassistantapp.dto.DeviceStateResponse;
import com.example.homeassistantapp.dto.EnergyLogResponse;
import com.example.homeassistantapp.dto.EnergyResponse;
import com.example.homeassistantapp.network.ApiError;
import com.example.homeassistantapp.network.ErrorMessages;
import com.example.homeassistantapp.network.RetrofitClient;
import com.example.homeassistantapp.view.PeriodRange;
import com.example.homeassistantapp.view.TrendValues;

import java.util.ArrayList;
import java.util.Collection;
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

    public LiveData<List<Device>> getDeviceList() { return deviceList; }
    public LiveData<OneTimeEvent<String>> getToastMessage() { return toastMessage; }

    // 삭제는 됐지만 기기 쪽 해제가 실패했을 때의 경고. 토스트보다 오래 보이도록 대화상자로 띄운다.
    private final MutableLiveData<OneTimeEvent<String>> deleteWarning = new MutableLiveData<>();
    public LiveData<OneTimeEvent<String>> getDeleteWarning() { return deleteWarning; }

    // 삭제 성공 시 지운 기기 ID. 기기별 저장값(타일 꾸미기)은 Context가 있는 쪽이 지운다.
    private final MutableLiveData<OneTimeEvent<String>> deviceDeleted = new MutableLiveData<>();
    public LiveData<OneTimeEvent<String>> getDeviceDeleted() { return deviceDeleted; }

    private final PendingStates pendingStates = new PendingStates();

    // 제어 실패. 토스트 대신 MainActivity가 Snackbar + "다시 시도"로 보인다.
    private final MutableLiveData<OneTimeEvent<ControlFailure>> controlFailure = new MutableLiveData<>();
    public LiveData<OneTimeEvent<ControlFailure>> getControlFailure() { return controlFailure; }

    // 등록 작업 폴링 상태. 메인 스레드에서만 바뀐다.
    private final MutableLiveData<Boolean> commissioning = new MutableLiveData<>(false);
    private Device pairingDevice;
    private Runnable pollRunnable;
    private long commissionStartedAt;
    // 진행·결과 화면이 보는 상태. null이면 등록한 적 없거나 결과를 확인함.
    private final MutableLiveData<CommissionStatus> commissionStatus = new MutableLiveData<>();
    // 결과가 나온 순간 1번. 진행 화면 밖에 있으면 MainActivity가 "보기" Snackbar를 띄운다.
    private final MutableLiveData<OneTimeEvent<CommissionStatus>> commissionFinished = new MutableLiveData<>();

    private static final long LIST_REFRESH_DELAY_MS = 500L;
    private final RefreshGate listRefreshGate = new RefreshGate();

    // 지연 수락(202) 뒤 상태 재확인 예약. 기기별로 하나만 둔다.
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Runnable> pendingRechecks = new HashMap<>();

    private void cancelRecheck(String deviceId) {
        Runnable pending = pendingRechecks.remove(deviceId);
        if (pending != null) mainHandler.removeCallbacks(pending);
    }

    private void scheduleRecheck(String deviceId, long delayMs) {
        cancelRecheck(deviceId);
        Runnable recheck = () -> {
            pendingRechecks.remove(deviceId);
            syncDeviceState(deviceId, true); // true: 서버 저장값이 아니라 기기에 직접 조회
        };
        pendingRechecks.put(deviceId, recheck);
        mainHandler.postDelayed(recheck, delayMs);
    }

    /** 기기가 서버에서 사라졌을 때(404). 잠깐 기다려 같은 때 온 404들을 목록 새로고침 1번으로 묶는다. */
    private void scheduleListRefresh() {
        if (!listRefreshGate.tryClaim()) return;
        mainHandler.postDelayed(() -> {
            listRefreshGate.release();
            fetchDevices();
        }, LIST_REFRESH_DELAY_MS);
    }

    private static boolean isDeviceGone(ApiError error) {
        return error.httpCode == 404 && error.is("DEVICE_NOT_FOUND");
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
        showFailure(prefix, ApiError.from(response));
    }

    /** 응답 본문을 이미 읽었으면 이쪽을 쓴다(errorBody는 한 번만 읽힌다). */
    private void showFailure(String prefix, ApiError error) {
        if (ErrorMessages.isUnauthorized(error.httpCode)) return;
        showToast(prefix + ErrorMessages.describe(error));
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
        fetchDevices(false);
    }

    /** withEnergy가 true면 목록을 받은 뒤 Matter 기기의 전력(E6)도 묻는다. 홈 화면용. */
    public void fetchDevices(boolean withEnergy) {
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
                    deviceList.setValue(withPairingItem(devices));
                    if (withEnergy && devices != null) {
                        for (Device device : devices) {
                            if ("MATTER".equals(device.getType())) fetchDeviceEnergy(device.getId());
                        }
                    }
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
        updateDevice(targetId, device -> device.setPendingCommand(command));
        beginLoading(targetId);

        ControlRequest request = new ControlRequest(targetId, command);
        RetrofitClient.getApi().controlDevice(request).enqueue(new Callback<ControlResponse>() {
            @Override
            public void onResponse(Call<ControlResponse> call, Response<ControlResponse> response) {
                ControlResponse body = response.body();
                ApiError error = response.isSuccessful() ? null : ApiError.from(response);
                ControlOutcome outcome = ControlOutcome.of(response.code(), error);
                updateDevice(targetId, device -> device.setPendingCommand(null));
                switch (outcome) {
                    case DONE:
                        showToast("명령 성공!");
                        finishLoading(targetId, body != null ? body.state : null);
                        break;
                    case ACCEPTED:
                        // state는 바뀌기 전 상태다. 실제 결과는 응답으로 오지 않아 나중에 E5로 확인한다.
                        Integer delay = body != null ? body.delaySeconds : null;
                        showToast(ControlOutcome.acceptedMessage(body != null ? body.pendingCommand : null, delay));
                        finishLoading(targetId, body != null ? body.state : null);
                        scheduleRecheck(targetId, PowerOffRecheck.delayMs(delay));
                        break;
                    case DEVICE_GONE:
                        finishLoading(targetId, null);
                        showFailure("제어 실패: ", error);
                        scheduleListRefresh();
                        break;
                    default:
                        finishLoading(targetId, outcome.failedState());
                        // 401 이동은 UnauthorizedInterceptor가 맡는다.
                        if (!ErrorMessages.isUnauthorized(error.httpCode)) {
                            reportControlFailure(targetId, command, ErrorMessages.describe(error));
                        }
                        break;
                }
            }

            @Override
            public void onFailure(Call<ControlResponse> call, Throwable t) {
                updateDevice(targetId, device -> device.setPendingCommand(null));
                finishLoading(targetId, null);
                reportControlFailure(targetId, command, ControlFailure.UNREACHABLE_SERVER);
            }
        });
    }

    private void reportControlFailure(String deviceId, String command, String reason) {
        controlFailure.setValue(new OneTimeEvent<>(
                new ControlFailure(deviceId, currentName(deviceId), command, reason)));
    }

    /** 등록 작업이 진행 중이면 true. 기기 추가 버튼을 잠그는 데 쓴다(서버도 동시에 1개만 받는다). */
    public LiveData<Boolean> getCommissioning() { return commissioning; }

    public LiveData<CommissionStatus> getCommissionStatus() { return commissionStatus; }
    public LiveData<OneTimeEvent<CommissionStatus>> getCommissionFinished() { return commissionFinished; }

    /** 결과 화면에서 확인을 누르면 지운다. 진행 중이면 지우지 않는다. */
    public void acknowledgeCommission() {
        CommissionStatus current = commissionStatus.getValue();
        if (current != null && !current.isActive()) commissionStatus.setValue(null);
    }

    private void finishCommission(CommissionStatus result) {
        commissionStatus.setValue(result);
        commissionFinished.setValue(new OneTimeEvent<>(result));
    }

    /** 등록을 시작하면 true. 이미 진행 중이면 false(서버도 동시에 1개만 받는다). */
    public boolean addDevice(String type, String name, String payload, String ssid, String password, List<Integer> driver) {
        if (pairingDevice != null) {
            showToast("다른 기기 등록이 진행 중입니다.");
            return false;
        }
        AddDeviceRequest request = new AddDeviceRequest(type, name, payload, ssid, password, driver);
        commissionStatus.setValue(CommissionStatus.sending(name));

        // 등록이 끝날 때까지 목록 맨 위에 보여 줄 임시 항목
        Device tempDevice = new Device();
        tempDevice.setId("TEMP_PAIRING");
        tempDevice.setName(name);
        tempDevice.setPairing(true);
        tempDevice.setType(type);
        pairingDevice = tempDevice;
        commissioning.setValue(true);
        deviceList.setValue(withPairingItem(deviceList.getValue()));

        RetrofitClient.getApi().addDevice(request).enqueue(new Callback<CommissionJobResponse>() {
            @Override
            public void onResponse(Call<CommissionJobResponse> call, Response<CommissionJobResponse> response){
                CommissionJobResponse body = response.body();
                CommissionStatus current = currentCommission(name);
                if (response.isSuccessful() && body != null && body.jobId != null) {
                    commissionStartedAt = SystemClock.elapsedRealtime();
                    commissionStatus.setValue(current.accepted(commissionStartedAt).withServerState(body.state));
                    schedulePoll(body.jobId);
                } else if (response.isSuccessful()) {
                    // 작업 ID가 없으면 결과를 물을 수 없어 목록으로 확인한다.
                    endCommission();
                    finishCommission(current.unconfirmed());
                    fetchDevices();
                } else {
                    // 400 INVALID_REQUEST, 409 COMMISSION_BUSY, 502 DEVICE_UNREACHABLE(서버 종료 중) 등
                    ApiError error = ApiError.from(response);
                    endCommission();
                    finishCommission(CommissionStatus.rejected(name, error.httpCode, error.code, error.message));
                }
            }
            @Override
            public void onFailure(Call<CommissionJobResponse> call, Throwable t){
                endCommission();
                finishCommission(CommissionStatus.rejected(name, 0, null, null));
            }
        });
        return true;
    }

    private CommissionStatus currentCommission(String name) {
        CommissionStatus current = commissionStatus.getValue();
        return current != null ? current : CommissionStatus.sending(name);
    }

    private void schedulePoll(String jobId) {
        pollRunnable = () -> pollCommission(jobId);
        mainHandler.postDelayed(pollRunnable, CommissionPoller.INTERVAL_MS);
    }

    private void pollCommission(String jobId) {
        RetrofitClient.getApi().getCommissionJob(jobId).enqueue(new Callback<CommissionJobResponse>() {
            @Override
            public void onResponse(Call<CommissionJobResponse> call, Response<CommissionJobResponse> response) {
                if (pairingDevice == null) return; // 이미 끝냄
                CommissionJobResponse body = response.body();
                handlePoll(jobId, CommissionPoller.afterResponse(elapsedSinceCommission(), response.code(), body), body);
            }

            @Override
            public void onFailure(Call<CommissionJobResponse> call, Throwable t) {
                if (pairingDevice == null) return;
                handlePoll(jobId, CommissionPoller.afterNetworkFailure(elapsedSinceCommission()), null);
            }
        });
    }

    private void handlePoll(String jobId, CommissionPoller.Next next, CommissionJobResponse body) {
        CommissionStatus current = currentCommission(pairingDevice != null ? pairingDevice.getName() : "");
        switch (next) {
            case POLL_AGAIN:
                if (body != null) commissionStatus.setValue(current.withServerState(body.state));
                schedulePoll(jobId);
                break;
            case SUCCEEDED:
                endCommission();
                finishCommission(current.succeeded(body != null && body.isRestored()));
                fetchDevices();
                break;
            case FAILED:
                CommissionJobResponse.JobError error = body != null ? body.error : null;
                endCommission();
                finishCommission(current.failed(error != null ? error.code : null, error != null ? error.message : null));
                if (CommissionPoller.shouldRefreshListAfterFailure(error)) fetchDevices();
                break;
            case LOST:
            case TIMED_OUT:
                endCommission();
                finishCommission(current.unconfirmed());
                fetchDevices();
                break;
        }
    }

    private long elapsedSinceCommission() {
        return SystemClock.elapsedRealtime() - commissionStartedAt;
    }

    /** 폴링을 멈추고 임시 항목을 지운다. jobId는 저장하지 않으므로 앱이 꺼지면 목록으로만 확인한다. */
    private void endCommission() {
        if (pollRunnable != null) {
            mainHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
        pairingDevice = null;
        commissioning.setValue(false);
        removeTempDevice();
    }

    /** 목록을 새로 받아도 등록 중인 임시 항목이 사라지지 않게 맨 위에 다시 붙인다. */
    private List<Device> withPairingItem(List<Device> devices) {
        List<Device> result = devices == null ? new ArrayList<>() : new ArrayList<>(devices);
        if (pairingDevice != null && !result.contains(pairingDevice)) result.add(0, pairingDevice);
        return result;
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

        RetrofitClient.getApi().deleteDevice(new DeleteDeviceRequest(deviceId)).enqueue(new Callback<DeleteDeviceResponse>() {
            @Override
            public void onResponse(Call<DeleteDeviceResponse> call, Response<DeleteDeviceResponse> response) {
                finishLoading(deviceId, null);
                if (response.isSuccessful()) {
                    showToast("기기를 삭제했습니다. 90일 안에 다시 등록하면 기록을 이어받습니다.");
                    forgetTileTrend(deviceId);
                    deviceDeleted.setValue(new OneTimeEvent<>(deviceId));
                    DeleteDeviceResponse body = response.body();
                    if (body != null && body.needsDeviceReset()) {
                        deleteWarning.setValue(new OneTimeEvent<>(
                                "앱에서는 삭제됐지만 기기 쪽 연결 해제에 실패했습니다. 기기를 초기화해 주세요."));
                    }
                    fetchDevices();
                    return;
                }
                ApiError error = ApiError.from(response);
                if (isDeviceGone(error)) {
                    // 이미 없거나 숨긴 기기: 사용자가 원한 결과는 이미 이뤄졌다.
                    showToast("이미 삭제된 기기입니다.");
                    scheduleListRefresh();
                } else {
                    showFailure("삭제 실패: ", error);
                }
            }

            @Override
            public void onFailure(Call<DeleteDeviceResponse> call, Throwable t) {
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
                    // 자동 조회가 많아 실패 토스트는 띄우지 않는다. 기기가 사라졌으면 목록만 맞춘다.
                    if (isDeviceGone(ApiError.from(response))) scheduleListRefresh();
                }
            }

            @Override
            public void onFailure(Call<DeviceStateResponse> call, Throwable t) {
                finishLoading(deviceId, "OFFLINE");
            }
        });
    }
    public void fetchDeviceEnergy(String deviceId) {
        // 홈에 들어올 때마다 자동으로 부르므로 LOADING으로 바꾸지 않는다(타일이 깜빡이지 않게).
        requestEnergy(deviceId, new EnergyListener() {
            @Override
            public void onSuccess() {}

            @Override
            public void onGone() {}

            @Override
            public void onFailure(ApiError error, String reason) {
                if (error != null) {
                    showFailure("전력 조회 실패: ", error);
                } else {
                    showToast("통신 실패: " + reason);
                }
            }
        });
    }

    private interface EnergyListener {
        void onSuccess();
        /** 기기가 사라짐(404). 목록 새로고침은 이미 예약했다. */
        void onGone();
        /** 통신 실패면 error가 null. */
        void onFailure(ApiError error, String reason);
    }

    /** E6 요청. 성공하면 기기의 전력 값과 상태를 바꾼다. */
    private void requestEnergy(String deviceId, EnergyListener listener) {
        RetrofitClient.getApi().getDeviceEnergy(deviceId).enqueue(new Callback<EnergyResponse>() {
            @Override
            public void onResponse(Call<EnergyResponse> call, Response<EnergyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 서버 단위는 mW·mWh
                    double currentPowerW = response.body().activePowerMw / 1000.0;
                    double totalEnergyKwh = response.body().totalEnergyMwh / 1000000.0;

                    String state = response.body().state;
                    pendingStates.remember(deviceId, state);
                    updateDevice(deviceId, device -> {
                        device.setEnergy(currentPowerW, totalEnergyKwh);
                        // 제어 등 다른 요청이 진행 중이면 그 결과가 상태를 정한다.
                        if (state != null && !pendingStates.isPending(deviceId)) device.setState(state);
                    });
                    listener.onSuccess();
                } else {
                    // 실패는 기기가 꺼졌다는 뜻이 아니다(502도 서버 상태는 그대로). 상태는 그대로 둔다.
                    ApiError error = ApiError.from(response);
                    if (isDeviceGone(error)) {
                        scheduleListRefresh();
                        listener.onGone();
                    } else {
                        listener.onFailure(error, ErrorMessages.describe(error));
                    }
                }
            }

            @Override
            public void onFailure(Call<EnergyResponse> call, Throwable t) {
                listener.onFailure(null, t.getMessage());
            }
        });
    }

    // 상세 화면 전력 주기 조회. 화면이 보이는 동안(onStart~onStop)만 돈다.
    private final MutableLiveData<EnergyReading> detailEnergy = new MutableLiveData<>();
    private final EnergyPollSchedule detailPoll = new EnergyPollSchedule();
    private final Runnable detailPollTick = this::pollDetailEnergy;
    private String detailDeviceId;

    public LiveData<EnergyReading> getDetailEnergy() { return detailEnergy; }

    public void startDetailPolling(String deviceId) {
        if (!deviceId.equals(detailDeviceId)) {
            detailDeviceId = deviceId;
            detailPoll.reset();
            detailEnergy.setValue(null);
        }
        mainHandler.removeCallbacks(detailPollTick);
        long delay = detailPoll.start(SystemClock.elapsedRealtime());
        if (delay != EnergyPollSchedule.NONE) mainHandler.postDelayed(detailPollTick, delay);
    }

    public void stopDetailPolling() {
        detailPoll.stop();
        mainHandler.removeCallbacks(detailPollTick);
    }

    /** 실패 배너의 "다시 시도". 진행 중인 요청이 있으면 그 결과를 기다린다. */
    public void retryDetailEnergy() {
        if (!detailPoll.canFetchNow()) return;
        mainHandler.removeCallbacks(detailPollTick);
        pollDetailEnergy();
    }

    private void pollDetailEnergy() {
        String deviceId = detailDeviceId;
        if (deviceId == null) return;
        detailPoll.requestStarted();
        requestEnergy(deviceId, new EnergyListener() {
            @Override
            public void onSuccess() {
                if (!deviceId.equals(detailDeviceId)) return;
                detailEnergy.setValue(EnergyReading.success(deviceId, System.currentTimeMillis()));
                scheduleNextDetailPoll();
            }

            @Override
            public void onGone() {
                // 목록이 새로 오면 상세가 닫힌다. 그 전까지 주기만 이어 간다.
                if (deviceId.equals(detailDeviceId)) scheduleNextDetailPoll();
            }

            @Override
            public void onFailure(ApiError error, String reason) {
                if (!deviceId.equals(detailDeviceId)) return;
                String shown = error == null ? ControlFailure.UNREACHABLE_SERVER : reason;
                detailEnergy.setValue(EnergyReading.failure(detailEnergy.getValue(), deviceId, shown));
                scheduleNextDetailPoll();
            }
        });
    }

    // 상세 차트(E7). 마지막으로 요청한 기기·기간의 결과만 내보낸다(빠르게 바꿀 때 늦게 온 이전 응답 무시).
    private final MutableLiveData<HistoryResult> detailHistory = new MutableLiveData<>();
    private String historyDeviceId;
    private String historyPeriod;

    public LiveData<HistoryResult> getDetailHistory() { return detailHistory; }

    /**
     * period가 null이면 원본 기록(최근 50건), day/week/month면 앱이 정한 기간의 집계.
     * 상태를 LOADING으로 바꾸지 않고 실패도 토스트 대신 차트 카드에 보인다.
     */
    public void loadDetailHistory(String deviceId, String period) {
        historyDeviceId = deviceId;
        historyPeriod = period;
        Call<EnergyLogResponse> call;
        if (period == null) {
            call = RetrofitClient.getApi().getDeviceEnergyHistory(deviceId);
        } else {
            PeriodRange range = PeriodRange.of(period, PeriodRange.today());
            call = RetrofitClient.getApi().getDeviceEnergySummary(deviceId, range.fromText(), range.toText(), period);
        }
        call.enqueue(new Callback<EnergyLogResponse>() {
            @Override
            public void onResponse(Call<EnergyLogResponse> call, Response<EnergyLogResponse> response) {
                if (!isLatestHistory(deviceId, period)) return;
                EnergyLogResponse body = response.body();
                if (response.isSuccessful() && body != null) {
                    detailHistory.setValue(new HistoryResult(deviceId, period, body, null));
                    return;
                }
                ApiError error = ApiError.from(response);
                if (isDeviceGone(error)) scheduleListRefresh();
                detailHistory.setValue(new HistoryResult(deviceId, period, null, ErrorMessages.describe(error)));
            }

            @Override
            public void onFailure(Call<EnergyLogResponse> call, Throwable t) {
                if (!isLatestHistory(deviceId, period)) return;
                detailHistory.setValue(new HistoryResult(deviceId, period, null, ControlFailure.UNREACHABLE_SERVER));
            }
        });
    }

    private boolean isLatestHistory(String deviceId, String period) {
        return deviceId.equals(historyDeviceId)
                && (period == null ? historyPeriod == null : period.equals(historyPeriod));
    }

    // 홈 타일 추이선(최근 7일 일별). 추이선을 켠 기기만, 캐시로 요청 수를 줄인다.
    private final MutableLiveData<Map<String, TrendValues>> tileTrends = new MutableLiveData<>(new HashMap<>());
    private final TrendCache trendCache = new TrendCache();

    public LiveData<Map<String, TrendValues>> getTileTrends() { return tileTrends; }

    /** 실패는 조용히 넘긴다(타일에 "–"). 기기가 사라졌으면 목록만 맞춘다. */
    public void fetchTileTrends(Collection<String> deviceIds) {
        long now = SystemClock.elapsedRealtime();
        for (String deviceId : deviceIds) {
            if (!trendCache.needsFetch(deviceId, now)) continue;
            trendCache.started(deviceId);
            PeriodRange range = PeriodRange.of(PeriodRange.DAY, PeriodRange.today());
            RetrofitClient.getApi()
                    .getDeviceEnergySummary(deviceId, range.fromText(), range.toText(), PeriodRange.DAY)
                    .enqueue(new Callback<EnergyLogResponse>() {
                        @Override
                        public void onResponse(Call<EnergyLogResponse> call, Response<EnergyLogResponse> response) {
                            EnergyLogResponse body = response.body();
                            if (response.isSuccessful() && body != null) {
                                trendCache.succeeded(deviceId, SystemClock.elapsedRealtime());
                                Map<String, TrendValues> next = new HashMap<>(tileTrends.getValue());
                                next.put(deviceId, TrendValues.fromBuckets(body.buckets));
                                tileTrends.setValue(next);
                                return;
                            }
                            trendCache.failed(deviceId, SystemClock.elapsedRealtime());
                            if (isDeviceGone(ApiError.from(response))) scheduleListRefresh();
                        }

                        @Override
                        public void onFailure(Call<EnergyLogResponse> call, Throwable t) {
                            trendCache.failed(deviceId, SystemClock.elapsedRealtime());
                        }
                    });
        }
    }

    private void forgetTileTrend(String deviceId) {
        trendCache.forget(deviceId);
        Map<String, TrendValues> next = new HashMap<>(tileTrends.getValue());
        if (next.remove(deviceId) != null) tileTrends.setValue(next);
    }

    private void scheduleNextDetailPoll() {
        long delay = detailPoll.finished(SystemClock.elapsedRealtime());
        if (delay != EnergyPollSchedule.NONE) mainHandler.postDelayed(detailPollTick, delay);
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

    private String currentName(String deviceId) {
        List<Device> currentList = deviceList.getValue();
        if (currentList == null) return null;
        for (Device device : currentList) {
            if (deviceId.equals(device.getId())) return device.getName();
        }
        return null;
    }

    private void updateDevice(String deviceId, Consumer<Device> updateAction) {
        List<Device> currentList = deviceList.getValue();
        if (currentList != null) {
            for (int i = 0; i < currentList.size(); i++) {
                if (deviceId.equals(currentList.get(i).getId())) {
                    updateAction.accept(currentList.get(i));
                    // 같은 리스트를 다시 넣어 옵저버를 깨운다.
                    deviceList.setValue(currentList);
                    break;
                }
            }
        }
    }
}

