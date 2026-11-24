package com.example.homeassistantapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.HomeTile;
import com.example.homeassistantapp.view.detail.DeviceInfo;
import com.example.homeassistantapp.view.detail.InfoCard;
import com.example.homeassistantapp.view.detail.StatusCard;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

/** 기기 상세(전체 화면). MainActivity 안에서 뒤로 스택에 쌓이고, 목록·제어는 탭들과 같은 ViewModel을 쓴다. */
public class DeviceDetailFragment extends Fragment {
    private static final String ARG_DEVICE_ID = "deviceId";

    private DeviceViewModel viewModel;
    private String deviceId;
    private boolean closing;

    public static void open(FragmentActivity activity, String deviceId) {
        FragmentManager fm = activity.getSupportFragmentManager();
        // 타일을 빠르게 두 번 눌러 상세가 두 겹 쌓이지 않게 한다.
        if (fm.isStateSaved() || fm.findFragmentById(R.id.fragmentContainer) instanceof DeviceDetailFragment) return;
        DeviceDetailFragment fragment = new DeviceDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_ID, deviceId);
        fragment.setArguments(args);
        fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
        deviceId = requireArguments().getString(ARG_DEVICE_ID);

        MaterialToolbar toolbar = view.findViewById(R.id.detailToolbar);
        toolbar.setNavigationOnClickListener(v -> close());
        toolbar.inflateMenu(R.menu.menu_device_detail);

        StatusCard statusCard = new StatusCard(view.findViewById(R.id.cardStatus), new StatusCard.Listener() {
            @Override
            public void onToggle(boolean turnOn) {
                viewModel.controlDevice(deviceId, turnOn ? "ON" : "OFF");
            }

            @Override
            public void onRefresh() {
                viewModel.syncDeviceState(deviceId, true);
            }
        });
        InfoCard infoCard = new InfoCard(view.findViewById(R.id.cardInfo));

        viewModel.getDeviceList().observe(getViewLifecycleOwner(), devices -> {
            if (devices == null) return;
            Device device = find(devices, deviceId);
            if (device == null) {
                // 삭제됐거나 다른 곳에서 사라진 기기. 빈 상세를 남기지 않고 닫는다.
                close();
                return;
            }
            toolbar.setTitle(device.getName());
            statusCard.bind(HomeTile.of(device));
            infoCard.bind(DeviceInfo.of(device));
        });
        // 앱이 종료됐다 상세로 복원되면 목록이 비어 있다.
        if (viewModel.getDeviceList().getValue() == null) viewModel.fetchDevices();
    }

    private void close() {
        if (closing || !isAdded()) return;
        closing = true;
        getParentFragmentManager().popBackStack();
    }

    private static Device find(List<Device> devices, String id) {
        for (Device device : devices) {
            if (id != null && id.equals(device.getId())) return device;
        }
        return null;
    }
}
