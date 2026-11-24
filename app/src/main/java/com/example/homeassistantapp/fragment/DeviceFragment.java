package com.example.homeassistantapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.DeviceAdapter;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class DeviceFragment extends Fragment {
    private DeviceViewModel viewModel;
    private DeviceAdapter adapter;
    private boolean isManualRefresh = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewDevices);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.fetchDevices();
            isManualRefresh = true;
        });

        adapter = new DeviceAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setOnDeviceLongClickListener(device -> showDeviceDeleteDialog(device));


        viewModel.getDeviceList().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null) {
                adapter.setDevices(devices);
            }
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            if (isManualRefresh) {
                for (Device device : devices) {
                    if (!"TEMP_PAIRING".equals(device.getId()) && !device.isPairing()) {
                        viewModel.syncDeviceState(device.getId(),isManualRefresh);
                    }
                }
                isManualRefresh = false; // 상태 갱신이 목록을 다시 내보내도 반복 요청하지 않게
            }
        });
        // 토스트 표시는 MainActivity가 한다. 여기서는 실패 응답에도 새로고침 표시를 끄기 위해 관찰한다.
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), event -> {
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        viewModel.fetchDevices();
        FloatingActionButton fabAddDevice = view.findViewById(R.id.fabAddDevice);
        fabAddDevice.setOnClickListener(v -> AddDeviceDialog.show(this, viewModel));
        // 등록 작업이 끝날 때까지 새 등록을 막는다(서버도 동시에 1개만 받는다).
        viewModel.getCommissioning().observe(getViewLifecycleOwner(),
                running -> fabAddDevice.setEnabled(!Boolean.TRUE.equals(running)));
    }
    private void showDeviceDeleteDialog(Device device) {
        new AlertDialog.Builder(getContext())
                .setTitle("기기 삭제")
                .setMessage("정말 [" + device.getName() + "] 기기를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    viewModel.deleteDevice(device.getId());
                })
                .setNegativeButton("취소", null)
                .show();
    }
}
