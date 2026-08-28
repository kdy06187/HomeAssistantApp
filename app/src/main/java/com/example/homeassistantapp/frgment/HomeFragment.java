package com.example.homeassistantapp.frgment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.DashboardAdapter;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {
    private DeviceViewModel viewModel;
    private DashboardAdapter adapter;

    private Set<String> requestedChartIds = new HashSet<>();
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewDashboard);
        // 🌟 핵심: 2열 그리드(바둑판) 레이아웃 매니저 설정
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new DashboardAdapter(new DashboardAdapter.OnDeviceControlListener() {
            @Override
            public void onToggle(String deviceId, boolean isChecked) {
                // 스위치 토글 시 뷰모델을 통해 REST API 제어 명령 전송
                String command = isChecked ? "ON" : "OFF";
                viewModel.controlDevice(deviceId, command);
            }
            @Override
            public void onRefreshChart(String deviceId) {
                // 뷰모델을 통해 해당 기기의 최신 전력량 로그를 서버에 요청
                viewModel.fetchDeviceEnergyHistory(deviceId);
            }
        });
        recyclerView.setAdapter(adapter);

        // requireActivity()를 사용하여 DeviceFragment와 동일한 뷰모델 공유
        viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);

        viewModel.getDeviceList().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null) {
                adapter.setDeviceList(devices);
                for (Device device : devices) {
                    // 전력 측정 기기인데 아직 차트 데이터(Log)가 null이라면 자동 요청!
                    // (무한 루프를 방지하기 위해 null일 때만 요청하는 것이 핵심입니다)
                    if (device.getPowerInfo() != null && device.getEnergyLogs() == null) {
                        if (!requestedChartIds.contains(device.getId())) {
                            requestedChartIds.add(device.getId()); // 명단에 추가!
                            viewModel.fetchDeviceEnergyHistory(device.getId()); // 서버에 요청
                        }
                    }
                }
            }
        });

        return view;
    }
    @Override
    public void onResume() {
        super.onResume();

        // 🌟 다른 화면에 갔다가 대시보드로 돌아올 때마다 모든 차트 최신화
        List<Device> currentDevices = viewModel.getDeviceList().getValue();
        if (currentDevices != null) {
            for (Device device : currentDevices) {
                if (device.getPowerInfo() != null && device.getEnergyLogs() != null) {
                    // 화면에 돌아올 때마다 강제로 차트 최신 데이터를 다시 물어옵니다.
                    viewModel.fetchDeviceEnergyHistory(device.getId());
                }
            }
        }
    }
}
