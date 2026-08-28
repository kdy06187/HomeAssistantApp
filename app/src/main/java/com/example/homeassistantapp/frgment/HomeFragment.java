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
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.view.DashboardAdapter;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;

public class HomeFragment extends Fragment {
    private DeviceViewModel viewModel;
    private DashboardAdapter adapter;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewDashboard);
        // 🌟 핵심: 2열 그리드(바둑판) 레이아웃 매니저 설정
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        adapter = new DashboardAdapter(new DashboardAdapter.OnDeviceControlListener() {
            @Override
            public void onToggle(String deviceId, boolean isChecked) {
                // 스위치 토글 시 뷰모델을 통해 REST API 제어 명령 전송
                String command = isChecked ? "ON" : "OFF";
                viewModel.controlDevice(deviceId, command);
            }
        });
        recyclerView.setAdapter(adapter);

        // requireActivity()를 사용하여 DeviceFragment와 동일한 뷰모델 공유
        viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);

        viewModel.getDeviceList().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null) {
                adapter.setDeviceList(devices);
            }
        });

        return view;
    }
}
