package com.example.homeassistantapp.frgment;

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
import androidx.recyclerview.widget.GridLayoutManager;
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
            public void onRefreshDevice(String deviceId) {
                // 뷰모델을 통해 해당 기기의 최신 상태를 서버에 요청
                viewModel.syncDeviceState(deviceId,true);
            }
            @Override
            public void onRefreshChart(String deviceId) {
                // 뷰모델을 통해 해당 기기의 최신 전력량 로그를 서버에 요청
                viewModel.fetchDeviceEnergyHistory(deviceId);
            }
            @Override
            public void onRefreshEnergy(String deviceId) {
                // 🌟 현재 상태(전력량)를 서버에 요청하는 메서드 호출
                // (현재 사용 중인 '기기 상태 갱신 API' 메서드명으로 변경해 주세요)
                viewModel.fetchDeviceEnergy(deviceId);
            }
        });
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback touchHelperCallback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) { // 위아래 드래그만 허용 (스와이프 삭제는 0으로 비활성화)

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                // 손가락이 움직일 때마다 어댑터의 순서 변경 로직 실행 (실시간 애니메이션)
                adapter.moveItem(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // 스와이프 기능은 사용하지 않음
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                List<Device> currentList = adapter.getCurrentList();
                viewModel.updateDeviceOrder(currentList); // 뷰모델 업데이트 (유지)

                // 🌟 추가: SharedPreferences에 변경된 기기 ID 순서를 영구 저장!
                SharedPreferences prefs = requireActivity().getSharedPreferences("DashboardPrefs", Context.MODE_PRIVATE);
                StringBuilder orderString = new StringBuilder();
                for (Device device : currentList) {
                    orderString.append(device.getId()).append(",");
                }
                // "id1,id2,id3," 형태로 저장됨
                prefs.edit().putString("device_order", orderString.toString()).apply();
            }
        };

        // 리사이클러뷰에 터치 도우미 철컥! 부착
        new ItemTouchHelper(touchHelperCallback).attachToRecyclerView(recyclerView);
        // requireActivity()를 사용하여 DeviceFragment와 동일한 뷰모델 공유
        viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);

        viewModel.getDeviceList().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null) {
                SharedPreferences prefs = requireActivity().getSharedPreferences("DashboardPrefs", Context.MODE_PRIVATE);
                String savedOrder = prefs.getString("device_order", "");

                // 2. 원본 리스트를 복사하여 정렬 준비
                List<Device> sortedList = new ArrayList<>(devices);

                // 3. 순서표가 존재한다면 그 순서에 맞게 재정렬!
                if (!savedOrder.isEmpty()) {
                    List<String> orderList = Arrays.asList(savedOrder.split(","));

                    Collections.sort(sortedList, (d1, d2) -> {
                        int index1 = orderList.indexOf(d1.getId());
                        int index2 = orderList.indexOf(d2.getId());

                        // 순서표에 없는 새로운 기기가 추가되었다면 맨 뒤로 배치
                        if (index1 == -1) index1 = Integer.MAX_VALUE;
                        if (index2 == -1) index2 = Integer.MAX_VALUE;

                        return Integer.compare(index1, index2);
                    });
                }

                // 4. 재정렬이 완료된 리스트를 어댑터에 전달
                adapter.setDeviceList(sortedList);

                for (Device device : sortedList) {
                    // 전력 측정 기기인데 아직 차트 데이터(Log)가 null이라면 자동 요청!
                    if(!"TEMP_PAIRING".equals(device.getId()) && !device.isPairing()) {
                        if (device.getType().equals("MATTER") && device.getPowerInfo() != null && device.getEnergyLogs() == null) {
                            if (!requestedChartIds.contains(device.getId())) {
                                requestedChartIds.add(device.getId()); // 명단에 추가!
                                viewModel.fetchDeviceEnergyHistory(device.getId()); // 서버에 요청
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

        // 🌟 다른 화면에 갔다가 대시보드로 돌아올 때마다 모든 차트 최신화
        List<Device> currentDevices = viewModel.getDeviceList().getValue();
        if (currentDevices != null) {
            for (Device device : currentDevices) {
                if(!"TEMP_PAIRING".equals(device.getId()) && !device.isPairing()) {
                    if (device.getType().equals("MATTER") && device.getPowerInfo() != null) {
                        // 🌟 차트 최신화와 동시에 현재 전력량 텍스트도 강제 최신화!
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
