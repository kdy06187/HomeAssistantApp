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
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.DeviceOrder;
import com.example.homeassistantapp.view.HomeTile;
import com.example.homeassistantapp.view.HomeTileAdapter;
import com.example.homeassistantapp.view.TileConfig;
import com.example.homeassistantapp.view.TileConfigStore;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 홈: 기기마다 타일 하나(2열). 순서는 기기 탭에서 정한 device_order를 따른다. */
public class HomeFragment extends Fragment {
    private DeviceViewModel viewModel;
    private HomeTileAdapter adapter;
    private TileConfigStore tileConfigs;

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
        GridLayoutManager grid = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(grid);
        tileConfigs = new TileConfigStore(requireContext());
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
                DeviceDetailFragment.open(requireActivity(), device.getId());
            }
        });
        recyclerView.setAdapter(adapter);
        grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.spanAt(position);
            }
        });

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefreshHome);
        swipeRefresh.setOnRefreshListener(() -> viewModel.fetchDevices(true));
        // 성공이면 목록이, 실패면 토스트가 온다. 어느 쪽이든 당겨서 새로고침 표시를 끈다.
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), event -> swipeRefresh.setRefreshing(false));

        ExtendedFloatingActionButton fab = view.findViewById(R.id.fabHomeAddDevice);
        View emptyView = view.findViewById(R.id.emptyHome);
        View emptyAdd = view.findViewById(R.id.btnEmptyAddDevice);
        fab.setOnClickListener(v -> CommissionProgressFragment.openAddOrProgress(requireActivity()));
        emptyAdd.setOnClickListener(v -> CommissionProgressFragment.openAddOrProgress(requireActivity()));

        viewModel.getDeviceList().observe(getViewLifecycleOwner(), devices -> {
            swipeRefresh.setRefreshing(false);
            if (devices == null) return;
            boolean empty = HomeTile.isEmptyHome(devices);
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            fab.setVisibility(empty ? View.GONE : View.VISIBLE);
            List<Device> ordered = sorted(devices);
            Map<String, TileConfig> configs = configsFor(ordered);
            adapter.setDevices(ordered, configs);
            viewModel.fetchTileTrends(trendDeviceIds(ordered, configs));
        });

        viewModel.getTileTrends().observe(getViewLifecycleOwner(), adapter::setTrends);

        // 등록 중에는 새 등록 대신 진행 화면을 연다(서버도 동시에 1개만 받는다).
        viewModel.getCommissioning().observe(getViewLifecycleOwner(),
                running -> fab.setText(Boolean.TRUE.equals(running) ? "등록 진행 보기" : "기기 추가"));
    }

    @Override
    public void onStart() {
        super.onStart();
        // 홈이 시작 탭이므로 들어올 때마다(상세에서 돌아올 때 포함) 목록과 전력을 새로 받는다.
        viewModel.fetchDevices(true);
    }

    /** 설정은 홈에 들어올 때마다 새로 읽으므로, 꾸미기 화면에서 바꾸고 돌아오면 바로 반영된다. */
    private Map<String, TileConfig> configsFor(List<Device> devices) {
        Map<String, TileConfig> result = new HashMap<>();
        for (Device device : devices) {
            if (!DeviceOrder.isPairing(device)) result.put(device.getId(), tileConfigs.load(device.getId()));
        }
        return result;
    }

    /** 추이선을 켠 Matter 기기만 E7을 묻는다. */
    private static List<String> trendDeviceIds(List<Device> devices, Map<String, TileConfig> configs) {
        List<String> ids = new ArrayList<>();
        for (Device device : devices) {
            if (HomeTile.of(device, configs.get(device.getId())).showsTrend) ids.add(device.getId());
        }
        return ids;
    }

    private List<Device> sorted(List<Device> devices) {
        String saved = requireContext()
                .getSharedPreferences(DeviceOrder.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(DeviceOrder.KEY_ORDER, "");
        return DeviceOrder.sort(devices, saved);
    }
}
