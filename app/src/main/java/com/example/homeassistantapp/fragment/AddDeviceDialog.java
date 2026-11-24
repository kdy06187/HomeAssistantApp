package com.example.homeassistantapp.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.network.CommissionInputValidator;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;

import java.util.ArrayList;
import java.util.List;

/** 기기 추가 대화상자. 홈과 기기 탭이 함께 쓴다(전체 화면 입력으로 바뀌기 전까지). */
public final class AddDeviceDialog {
    private AddDeviceDialog() {}

    public static void show(Fragment fragment, DeviceViewModel viewModel) {
        Context context = fragment.requireContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("새로운 기기 추가");

        View dialogView = fragment.getLayoutInflater().inflate(R.layout.dialog_add_device, null);
        builder.setView(dialogView);

        Spinner spinnerProtocol = dialogView.findViewById(R.id.spinnerProtocol);
        EditText editDeviceName = dialogView.findViewById(R.id.editDeviceName);
        EditText editPayload = dialogView.findViewById(R.id.editPayload);
        Spinner spinnerDriver = dialogView.findViewById(R.id.spinnerDriver);

        builder.setPositiveButton("등록", (dialog, which) -> {
            String type = spinnerProtocol.getSelectedItem().toString();
            String name = editDeviceName.getText().toString();
            String payload = editPayload.getText().toString();

            String[] driverValues = context.getResources().getStringArray(R.array.driver_value_array);
            int selectedDriverIndex = spinnerDriver.getSelectedItemPosition();
            int driverTypeInt = Integer.parseInt(driverValues[selectedDriverIndex]);
            List<Integer> driverTypes = new ArrayList<>();
            if (driverTypeInt > 0) {
                driverTypes.add(driverTypeInt);
            }
            if (!name.isEmpty()) {
                SharedPreferences prefs = context.getSharedPreferences("HomeAssistantPrefs", Context.MODE_PRIVATE);
                String ssid = prefs.getString("WIFI_SSID", "");
                String password = prefs.getString("WIFI_PASSWORD", "");

                if (ssid.isEmpty()) {
                    Toast.makeText(context, "설정 탭에서 와이파이를 먼저 설정해주세요.", Toast.LENGTH_LONG).show();
                    return;
                }
                CommissionInputValidator.Result checked =
                        CommissionInputValidator.validate(type, name, payload, ssid, password);
                if (!checked.isValid()) {
                    Toast.makeText(context, checked.error, Toast.LENGTH_LONG).show();
                    return;
                }
                viewModel.addDevice(type, name, checked.payload, ssid, password, driverTypes);
            } else {
                Toast.makeText(context, "정보를 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
