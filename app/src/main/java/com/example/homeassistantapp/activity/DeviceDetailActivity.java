package com.example.homeassistantapp.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.homeassistantapp.view.EnergyLineChartView;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.Device;
public class DeviceDetailActivity extends AppCompatActivity {
    private DeviceViewModel viewModel;

    public static Intent intentFor(Context context, Device device) {
        Intent intent = new Intent(context, DeviceDetailActivity.class);
        intent.putExtra("EXTRA_DEVICE_ID", device.getId());
        intent.putExtra("EXTRA_DEVICE_NAME", device.getName());
        intent.putExtra("EXTRA_DEVICE_TYPE", device.getType());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);
        TextView tvName = findViewById(R.id.tvDetailDeviceName);
        TextView tvProtocol = findViewById(R.id.tvDetailProtocol);
        TextView tvStatus = findViewById(R.id.tvDetailStatus);
        EnergyLineChartView chartView = findViewById(R.id.detailEnergyChart);
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        String deviceId = getIntent().getStringExtra("EXTRA_DEVICE_ID");
        String deviceName = getIntent().getStringExtra("EXTRA_DEVICE_NAME");
        String deviceType = getIntent().getStringExtra("EXTRA_DEVICE_TYPE");

        tvName.setText(deviceName);
        tvProtocol.setText("프로토콜: " + deviceType);
        tvStatus.setText("연결 상태: 정상 (Online)"); // TODO: 실제 상태 표시


        viewModel = new ViewModelProvider(this).get(DeviceViewModel.class);

        viewModel.getDetailEnergyHistory().observe(this, logs -> {
            if (logs != null && !logs.isEmpty()) {
                chartView.setEnergyData(logs);
            }
        });

        if (deviceId != null) {
            viewModel.fetchDeviceEnergyHistory(deviceId);
        }
    }
}
