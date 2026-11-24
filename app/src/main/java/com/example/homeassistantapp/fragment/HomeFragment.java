package com.example.homeassistantapp.fragment;

import android.content.Context;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.activity.DeviceDetailActivity;
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.DeviceOrder;
import com.example.homeassistantapp.view.HomeTileAdapter;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

/** 홈: 기기마다 타일 하나(2열). 순서는 기기 탭에서 정한 device_order를 따른다. */
public class HomeFragment extends Fragment {
    private DeviceViewModel viewModel;
    private HomeTileAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // DeviceFragment와 같은 ViewModel을 쓰도록 Activity 범위로 가져온다.
        viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewDashboard);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new HomeTileAdapter(new HomeTileAdapter.Listener() {
            @Override
            public void onToggle(Device device, boolean turnOn) {
                viewModel.controlDevice(device.getId(), turnOn ? "ON" : "OFF");
            }

            @Override
            public void onRefresh(Device device) {
                viewModel.syncDeviceState(device.getId(), true);
            }

            @Override
            public void onOpen(Device device) {
                startActivity(DeviceDetailActivity.intentFor(requireContext(), device));
            }
        });
        recyclerView.setAdapter(adapter);

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefreshHome);
        swipeRefresh.setOnRefreshListener(() -> viewModel.fetchDevices(true));
        // 성공이면 목록이, 실패면 토스트가 온다. 어느 쪽이든 당겨서 새로고침 표시를 끈다.
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), event -> swipeRefresh.setRefreshing(false));

        viewModel.getDeviceList().observe(getViewLifecycleOwner(), devices -> {
            swipeRefresh.setRefreshing(false);
            if (devices == null) return;
            adapter.setDevices(sorted(devices));
        });

        ExtendedFloatingActionButton fab = view.findViewById(R.id.fabHomeAddDevice);
        fab.setOnClickListener(v -> AddDeviceDialog.show(this, viewModel));
        // 등록 작업이 끝날 때까지 새 등록을 막는다(서버도 동시에 1개만 받는다).
        viewModel.getCommissioning().observe(getViewLifecycleOwner(),
                running -> fab.setEnabled(!Boolean.TRUE.equals(running)));
    }

    @Override
    public void onStart() {
        super.onStart();
        // 홈이 시작 탭이므로 들어올 때마다(상세에서 돌아올 때 포함) 목록과 전력을 새로 받는다.
        viewModel.fetchDevices(true);
    }

    private List<Device> sorted(List<Device> devices) {
        String saved = requireContext()
                .getSharedPreferences(DeviceOrder.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(DeviceOrder.KEY_ORDER, "");
        return DeviceOrder.sort(devices, saved);
    }
}
