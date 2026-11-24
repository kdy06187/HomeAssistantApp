package com.example.homeassistantapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.HomeTile;
import com.example.homeassistantapp.view.detail.ChartBars;
import com.example.homeassistantapp.view.detail.ChartCard;
import com.example.homeassistantapp.view.detail.DeviceInfo;
import com.example.homeassistantapp.view.detail.InfoCard;
import com.example.homeassistantapp.view.detail.PowerCard;
import com.example.homeassistantapp.view.detail.PowerPanel;
import com.example.homeassistantapp.view.detail.StatusCard;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.example.homeassistantapp.viewmodel.EnergyReading;
import com.example.homeassistantapp.viewmodel.HistoryResult;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;
import java.util.TimeZone;

/** 기기 상세(전체 화면). MainActivity 안에서 뒤로 스택에 쌓이고, 목록·제어는 탭들과 같은 ViewModel을 쓴다. */
public class DeviceDetailFragment extends Fragment {
    private static final String ARG_DEVICE_ID = "deviceId";
    private static final String STATE_PERIOD = "chartPeriod";

    private DeviceViewModel viewModel;
    private String deviceId;
    private boolean closing;
    private PowerCard powerCard;
    private ChartCard chartCard;
    private boolean historyRequested;
    // 회전처럼 화면만 다시 만들어진 경우에만 받아 둔 차트 결과를 다시 쓴다.
    private boolean reuseHistory;
    /** 차트 탭. null이면 최근 기록. */
    private String period;
    private Device device;
    // 전력 조회(E6)는 Matter 기기만, 화면이 보이는 동안만.
    private boolean started;
    private boolean polling;

    public static void open(FragmentActivity activity, String deviceId) {
        FragmentManager fm = activity.getSupportFragmentManager();
        // 타일을 빠르게 두 번 눌러 상세가 두 겹 쌓이지 않게 한다.
        if (fm.isStateSaved() || fm.findFragmentById(R.id.fragmentContainer) instanceof DeviceDetailFragment) return;
        DeviceDetailFragment fragment = new DeviceDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_ID, deviceId);
        fragment.setArguments(args);
        fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
        deviceId = requireArguments().getString(ARG_DEVICE_ID);
        reuseHistory = savedInstanceState != null;
        period = savedInstanceState != null ? savedInstanceState.getString(STATE_PERIOD) : null;

        MaterialToolbar toolbar = view.findViewById(R.id.detailToolbar);
        toolbar.setNavigationOnClickListener(v -> close());
        toolbar.inflateMenu(R.menu.menu_device_detail);

        StatusCard statusCard = new StatusCard(view.findViewById(R.id.cardStatus), new StatusCard.Listener() {
            @Override
            public void onToggle(boolean turnOn) {
                viewModel.controlDevice(deviceId, turnOn ? "ON" : "OFF");
            }

            @Override
            public void onRefresh() {
                viewModel.syncDeviceState(deviceId, true);
            }
        });
        InfoCard infoCard = new InfoCard(view.findViewById(R.id.cardInfo));
        powerCard = new PowerCard(view.findViewById(R.id.cardPower), () -> viewModel.retryDetailEnergy());
        powerCard.setVisible(false);
        chartCard = new ChartCard(view.findViewById(R.id.cardChart), new ChartCard.Listener() {
            @Override
            public void onPeriodSelected(String selected) {
                boolean changed = selected == null ? period != null : !selected.equals(period);
                period = selected;
                // 복원 때 탭을 다시 고르는 것도 여기로 온다. 기기를 아직 모르면 목록이 온 뒤 불러온다.
                if (changed && historyRequested) loadHistory();
            }

            @Override
            public void onRetry() {
                loadHistory();
            }
        });
        chartCard.select(period);
        chartCard.setVisible(false);

        viewModel.getDeviceList().observe(getViewLifecycleOwner(), devices -> {
            if (devices == null) return;
            device = find(devices, deviceId);
            if (device == null) {
                // 삭제됐거나 다른 곳에서 사라진 기기. 빈 상세를 남기지 않고 닫는다.
                close();
                return;
            }
            toolbar.setTitle(device.getName());
            statusCard.bind(HomeTile.of(device));
            infoCard.bind(DeviceInfo.of(device));
            powerCard.setVisible(isMatter());
            chartCard.setVisible(isMatter());
            bindPower();
            if (isMatter() && !historyRequested) showOrLoadHistory();
            updatePolling();
        });
        // 삭제가 성공하면 목록이 새로 오고, 기기가 빠졌으니 위의 관찰에서 상세가 닫힌다.
        view.findViewById(R.id.btnDetailDelete).setOnClickListener(v -> {
            if (device != null) DeleteDeviceDialog.show(this, viewModel, device);
        });
        viewModel.getDetailEnergy().observe(getViewLifecycleOwner(), reading -> bindPower());
        viewModel.getDetailHistory().observe(getViewLifecycleOwner(), this::bindHistory);
        // 앱이 종료됐다 상세로 복원되면 목록이 비어 있다.
        if (viewModel.getDeviceList().getValue() == null) viewModel.fetchDevices();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_PERIOD, period);
    }

    @Override
    public void onStart() {
        super.onStart();
        started = true;
        updatePolling();
    }

    @Override
    public void onStop() {
        started = false;
        updatePolling();
        super.onStop();
    }

    private boolean isMatter() {
        return device != null && "MATTER".equals(device.getType());
    }

    private void updatePolling() {
        boolean shouldPoll = started && isMatter() && !closing;
        if (shouldPoll == polling) return;
        polling = shouldPoll;
        if (shouldPoll) {
            viewModel.startDetailPolling(deviceId);
        } else {
            viewModel.stopDetailPolling();
        }
    }

    private void bindPower() {
        if (!isMatter()) return;
        EnergyReading reading = viewModel.getDetailEnergy().getValue();
        powerCard.bind(PowerPanel.of(device, reading, TimeZone.getDefault()));
    }

    private void showOrLoadHistory() {
        historyRequested = true;
        HistoryResult last = viewModel.getDetailHistory().getValue();
        if (reuseHistory && last != null && last.isFor(deviceId, period)) {
            bindHistory(last);
        } else {
            loadHistory();
        }
    }

    private void loadHistory() {
        historyRequested = true;
        chartCard.showLoading();
        viewModel.loadDetailHistory(deviceId, period);
    }

    private void bindHistory(HistoryResult result) {
        if (result == null || !historyRequested || !result.isFor(deviceId, period)) return;
        if (result.failure != null) {
            chartCard.bind(ChartBars.failed(result.failure), true);
        } else if (period != null) {
            chartCard.bind(ChartBars.fromBuckets(result.response.buckets, period), false);
        } else {
            chartCard.bind(ChartBars.fromLogs(result.response.getLogs(), result.response.isTruncated(),
                    TimeZone.getDefault()), false);
        }
    }

    private void close() {
        if (closing || !isAdded()) return;
        closing = true;
        updatePolling();
        getParentFragmentManager().popBackStack();
    }

    private static Device find(List<Device> devices, String id) {
        for (Device device : devices) {
            if (id != null && id.equals(device.getId())) return device;
        }
        return null;
    }
}
