package com.example.homeassistantapp.activity;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);
        // 뷰 초기화
        TextView tvName = findViewById(R.id.tvDetailDeviceName);
        TextView tvProtocol = findViewById(R.id.tvDetailProtocol);
        TextView tvStatus = findViewById(R.id.tvDetailStatus);
        EnergyLineChartView chartView = findViewById(R.id.detailEnergyChart);
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish()); // 뒤로가기 버튼 액션
        // 인텐트 데이터 수신
        String deviceId = getIntent().getStringExtra("EXTRA_DEVICE_ID");
        String deviceName = getIntent().getStringExtra("EXTRA_DEVICE_NAME");
        String deviceType = getIntent().getStringExtra("EXTRA_DEVICE_TYPE");

        tvName.setText(deviceName);
        tvProtocol.setText("프로토콜: " + deviceType);
        tvStatus.setText("연결 상태: 정상 (Online)"); // 우선 임시 텍스트 고정


        // 3. ViewModel 세팅
        viewModel = new ViewModelProvider(this).get(DeviceViewModel.class);

        // 4. 데이터 갱신 관찰
        viewModel.getDetailEnergyHistory().observe(this, logs -> {
            if (logs != null && !logs.isEmpty()) {
                chartView.setEnergyData(logs);
            }
        });

        // 6. 화면 진입 시 서버에 차트 데이터 요청
        if (deviceId != null) {
            viewModel.fetchDeviceEnergyHistory(deviceId);
        }
    }
}
