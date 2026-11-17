package com.example.homeassistantapp.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.DashboardAdapter;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new DashboardAdapter(new DashboardAdapter.OnDeviceControlListener() {
            @Override
            public void onToggle(String deviceId, boolean isChecked) {
                String command = isChecked ? "ON" : "OFF";
                viewModel.controlDevice(deviceId, command);
            }
            @Override
            public void onRefreshDevice(String deviceId) {
                viewModel.syncDeviceState(deviceId,true);
            }
            @Override
            public void onRefreshChart(String deviceId) {
                viewModel.fetchDeviceEnergyHistory(deviceId);
            }
            @Override
            public void onRefreshEnergy(String deviceId) {
                viewModel.fetchDeviceEnergy(deviceId);
            }
        });
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback touchHelperCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) { // 위아래 드래그만, 스와이프 없음

            @Override
            public boolean isLongPressDragEnabled() {
                return !adapter.isAnyDeviceEditing();
            }
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                adapter.moveItem(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // 스와이프는 쓰지 않는다.
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                List<Device> currentList = adapter.getCurrentList();
                viewModel.updateDeviceOrder(currentList);

                SharedPreferences prefs = requireActivity().getSharedPreferences("DashboardPrefs", Context.MODE_PRIVATE);
                StringBuilder orderString = new StringBuilder();
                for (Device device : currentList) {
                    orderString.append(device.getId()).append(",");
                }
                // "id1,id2,id3," 형태로 저장
                prefs.edit().putString("device_order", orderString.toString()).apply();
            }
        };

        new ItemTouchHelper(touchHelperCallback).attachToRecyclerView(recyclerView);
        // DeviceFragment와 같은 ViewModel을 쓰도록 Activity 범위로 가져온다.
        viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);

        viewModel.getDeviceList().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null) {
                SharedPreferences prefs = requireActivity().getSharedPreferences("DashboardPrefs", Context.MODE_PRIVATE);
                String savedOrder = prefs.getString("device_order", "");

                List<Device> sortedList = new ArrayList<>(devices);

                if (!savedOrder.isEmpty()) {
                    List<String> orderList = Arrays.asList(savedOrder.split(","));

                    Collections.sort(sortedList, (d1, d2) -> {
                        int index1 = orderList.indexOf(d1.getId());
                        int index2 = orderList.indexOf(d2.getId());

                        // 순서표에 없는 새 기기는 맨 뒤로
                        if (index1 == -1) index1 = Integer.MAX_VALUE;
                        if (index2 == -1) index2 = Integer.MAX_VALUE;

                        return Integer.compare(index1, index2);
                    });
                }

                adapter.setDeviceList(sortedList);

                for (Device device : sortedList) {
                    // MATTER 기기는 차트 기록을 한 번만 자동 요청한다.
                    if(!"TEMP_PAIRING".equals(device.getId()) && !device.isPairing()) {
                        if (device.getType().equals("MATTER") && device.getPowerInfo() != null && device.getEnergyLogs() == null) {
                            if (!requestedChartIds.contains(device.getId())) {
                                requestedChartIds.add(device.getId());
                                viewModel.fetchDeviceEnergyHistory(device.getId());
                            }
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

        // 화면에 돌아올 때마다 전력값과 차트를 새로 고친다.
        List<Device> currentDevices = viewModel.getDeviceList().getValue();
        if (currentDevices != null) {
            for (Device device : currentDevices) {
                if(!"TEMP_PAIRING".equals(device.getId()) && !device.isPairing()) {
                    if (device.getType().equals("MATTER") && device.getPowerInfo() != null) {
                        viewModel.fetchDeviceEnergy(device.getId());

                        if (device.getEnergyLogs() != null) {
                            viewModel.fetchDeviceEnergyHistory(device.getId());
                        }
                    }
                }
            }
        }
    }
}
