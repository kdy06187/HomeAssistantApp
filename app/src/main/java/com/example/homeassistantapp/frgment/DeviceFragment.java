package com.example.homeassistantapp.frgment;

import static java.security.AccessController.getContext;

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

import com.example.homeassistantapp.MainActivity;
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
        // XML 레이아웃을 화면에 부풀림(Inflate)
        return inflater.inflate(R.layout.fragment_device, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel 가져오기
        viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
        // RecyclerView 초기화
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewDevices);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        // 🌟 1. 당겨서 새로고침 제스처를 인식했을 때의 동작 설정
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // ViewModel에 만들어둔 기기 목록 조회 API를 호출합니다.
            viewModel.fetchDevices();
            isManualRefresh = true;
        });

        // 어댑터 연결 (버튼 클릭 시 처리할 로직 정의)
        adapter = new DeviceAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setOnDeviceLongClickListener(device -> showDeviceDeleteDialog(device));


        // ViewModel의 데이터(LiveData) 관찰하기
        viewModel.getDeviceList().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null) {
                adapter.setDevices(devices);
            }
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            if (isManualRefresh) {
                isManualRefresh = false; // 즉시 잠금
                for (Device device : devices) {

                    viewModel.syncDeviceState(device.getId());

                }
            }
        });
        // 토스트 메시지가 갱신되면 화면에 띄움
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        viewModel.fetchDevices();
        FloatingActionButton fabAddDevice = view.findViewById(R.id.fabAddDevice);
        fabAddDevice.setOnClickListener(v -> showAddDeviceDialog());
    }
    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("새로운 기기 추가");

        // 만든 레이아웃 인플레이트
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_device, null);
        builder.setView(dialogView);

        Spinner spinnerProtocol = dialogView.findViewById(R.id.spinnerProtocol);
        EditText editDeviceName = dialogView.findViewById(R.id.editDeviceName);
        EditText editPayload = dialogView.findViewById(R.id.editPayload);

        builder.setPositiveButton("등록", (dialog, which) -> {
            String type = spinnerProtocol.getSelectedItem().toString(); // "ARDUINO" or "MATTER"
            String name = editDeviceName.getText().toString();
            String payload = editPayload.getText().toString();

            if(!name.isEmpty() && !payload.isEmpty()){
                // 이전에 만들어둔 ViewModel의 addDevice(커미셔닝 API) 호출!
                viewModel.addDevice(type, name, payload);
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
                    viewModel.deleteDevice(device.getId()); // ViewModel의 삭제 로직 호출!
                })
                .setNegativeButton("취소", null)
                .show();
    }
}
