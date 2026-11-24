package com.example.homeassistantapp.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

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
import com.example.homeassistantapp.network.CommissionInputValidator;
import com.example.homeassistantapp.view.DeviceAdapter;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

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
        fabAddDevice.setOnClickListener(v -> showAddDeviceDialog());
        // 등록 작업이 끝날 때까지 새 등록을 막는다(서버도 동시에 1개만 받는다).
        viewModel.getCommissioning().observe(getViewLifecycleOwner(),
                running -> fabAddDevice.setEnabled(!Boolean.TRUE.equals(running)));
    }
    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("새로운 기기 추가");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_device, null);
        builder.setView(dialogView);

        Spinner spinnerProtocol = dialogView.findViewById(R.id.spinnerProtocol);
        EditText editDeviceName = dialogView.findViewById(R.id.editDeviceName);
        EditText editPayload = dialogView.findViewById(R.id.editPayload);

        Spinner spinnerDriver = dialogView.findViewById(R.id.spinnerDriver);

        builder.setPositiveButton("등록", (dialog, which) -> {
            String type = spinnerProtocol.getSelectedItem().toString();
            String name = editDeviceName.getText().toString();
            String payload = editPayload.getText().toString();

            String[] driverValues = getResources().getStringArray(R.array.driver_value_array);
            int selectedDriverIndex = spinnerDriver.getSelectedItemPosition();
            int driverTypeInt = Integer.parseInt(driverValues[selectedDriverIndex]);
            List<Integer> driverTypes = new ArrayList<>();
            if (driverTypeInt > 0) {
                driverTypes.add(driverTypeInt);
            }
            if(!name.isEmpty()){
                SharedPreferences prefs = requireActivity().getSharedPreferences("HomeAssistantPrefs", Context.MODE_PRIVATE);
                String ssid = prefs.getString("WIFI_SSID", "");
                String password = prefs.getString("WIFI_PASSWORD", "");

                if (ssid.isEmpty()) {
                    Toast.makeText(getContext(), "설정 탭에서 와이파이를 먼저 설정해주세요.", Toast.LENGTH_LONG).show();
                    return;
                }
                CommissionInputValidator.Result checked =
                        CommissionInputValidator.validate(type, name, payload, ssid, password);
                if (!checked.isValid()) {
                    Toast.makeText(getContext(), checked.error, Toast.LENGTH_LONG).show();
                    return;
                }
                viewModel.addDevice(type, name, checked.payload, ssid, password, driverTypes);
            } else {
                Toast.makeText(getContext(), "정보를 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
        builder.show();
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
