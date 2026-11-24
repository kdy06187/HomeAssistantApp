package com.example.homeassistantapp.fragment;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.google.android.material.color.MaterialColors;

/** 기기 삭제 확인. 기기 탭과 상세에서 같은 문구로 묻는다. */
public final class DeleteDeviceDialog {
    private DeleteDeviceDialog() {}

    public static void show(Fragment fragment, DeviceViewModel viewModel, Device device) {
        AlertDialog dialog = new AlertDialog.Builder(fragment.requireContext())
                .setTitle("기기 삭제")
                .setMessage("‘" + device.getName() + "’ 기기를 삭제할까요? 홈과 기기 목록에서 사라져요.")
                .setPositiveButton("삭제", (d, which) -> viewModel.deleteDevice(device.getId()))
                .setNegativeButton("취소", null)
                .show();
        // 되돌리기 어려운 동작이라 삭제 버튼을 오류 색으로 구분한다.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                MaterialColors.getColor(dialog.getButton(AlertDialog.BUTTON_POSITIVE),
                        androidx.appcompat.R.attr.colorError));
    }
}
