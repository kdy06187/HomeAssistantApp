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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.DeviceAdapter;
import com.example.homeassistantapp.view.DeviceOrder;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

/** 기기 탭: 목록 관리(상태·종류·삭제·순서). 순서는 홈 타일에도 그대로 쓰인다. */
public class DeviceFragment extends Fragment {
    private DeviceViewModel viewModel;
    private DeviceAdapter adapter;
    private ItemTouchHelper touchHelper;
    private boolean isManualRefresh = true;
    // 드래그 중에 상태 갱신으로 목록을 다시 그리면 끌던 항목이 사라진다. 끝난 뒤 마지막 목록을 그린다.
    private boolean dragging;
    private List<Device> deferredDevices;

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

        adapter = new DeviceAdapter(new DeviceAdapter.Listener() {
            @Override
            public void onOpen(Device device) {
                DeviceDetailFragment.open(requireActivity(), device.getId());
            }

            @Override
            public void onDelete(Device device) {
                DeleteDeviceDialog.show(DeviceFragment.this, viewModel, device);
            }

            @Override
            public void onCustomize(Device device) {
                TileCustomizeSheet.open(getChildFragmentManager(), device.getId());
            }

            @Override
            public void onStartDrag(RecyclerView.ViewHolder holder) {
                touchHelper.startDrag(holder);
            }

            @Override
            public void onMoveBy(RecyclerView.ViewHolder holder, int delta) {
                int from = holder.getBindingAdapterPosition();
                if (adapter.move(from, from + delta)) saveOrder();
            }
        });
        recyclerView.setAdapter(adapter);

        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean isLongPressDragEnabled() {
                return false; // 핸들로만 끈다
            }

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder from,
                                  @NonNull RecyclerView.ViewHolder to) {
                return adapter.move(from.getBindingAdapterPosition(), to.getBindingAdapterPosition());
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
                // 스와이프는 쓰지 않는다.
            }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder holder, int actionState) {
                super.onSelectedChanged(holder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) dragging = true;
            }

            @Override
            public void clearView(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder holder) {
                super.clearView(rv, holder);
                dragging = false;
                saveOrder();
                if (deferredDevices != null) {
                    showDevices(deferredDevices);
                    deferredDevices = null;
                }
            }
        });
        touchHelper.attachToRecyclerView(recyclerView);

        viewModel.getDeviceList().observe(getViewLifecycleOwner(), devices -> {
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            if (devices == null) return;
            if (dragging) {
                deferredDevices = devices;
            } else {
                showDevices(devices);
            }
            if (isManualRefresh) {
                for (Device device : devices) {
                    if (!"TEMP_PAIRING".equals(device.getId()) && !device.isPairing()) {
                        viewModel.syncDeviceState(device.getId(), isManualRefresh);
                    }
                }
                isManualRefresh = false; // 상태 갱신이 목록을 다시 내보내도 반복 요청하지 않게
            }
        });
        // 목록 요청이 실패해도 새로고침 표시를 끈다(성공은 목록 관찰에서 끈다).
        viewModel.getListRequestFinished().observe(getViewLifecycleOwner(), done -> {
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        viewModel.fetchDevices();
        ExtendedFloatingActionButton fabAddDevice = view.findViewById(R.id.fabAddDevice);
        fabAddDevice.setOnClickListener(v -> CommissionProgressFragment.openAddOrProgress(requireActivity()));
        // 등록 중에는 새 등록 대신 진행 화면을 연다(서버도 동시에 1개만 받는다).
        viewModel.getCommissioning().observe(getViewLifecycleOwner(),
                running -> fabAddDevice.setText(Boolean.TRUE.equals(running) ? "등록 진행 보기" : "기기 추가"));
    }

    private void showDevices(List<Device> devices) {
        String saved = requireContext()
                .getSharedPreferences(DeviceOrder.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(DeviceOrder.KEY_ORDER, "");
        adapter.setDevices(DeviceOrder.sort(devices, saved));
    }

    private void saveOrder() {
        requireContext().getSharedPreferences(DeviceOrder.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(DeviceOrder.KEY_ORDER, DeviceOrder.serialize(adapter.getDevices()))
                .apply();
    }

}
