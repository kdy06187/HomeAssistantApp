package com.example.homeassistantapp;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.api.HomeAssistantApi;
import com.example.homeassistantapp.dto.ControlRequest;
import com.example.homeassistantapp.dto.ControlResponse;
import com.example.homeassistantapp.dto.DeviceListResponse;
import com.example.homeassistantapp.view.DeviceAdapter;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private DeviceViewModel viewModel;
    private DeviceAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ViewModel 가져오기
        viewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        // RecyclerView 초기화
        RecyclerView recyclerView = findViewById(R.id.recyclerViewDevices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 어댑터 연결 (버튼 클릭 시 처리할 로직 정의)
        adapter = new DeviceAdapter((deviceId, command) -> {
            viewModel.controlDevice(deviceId, command);
        });
        recyclerView.setAdapter(adapter);
        // ViewModel의 데이터(LiveData) 관찰하기
        viewModel.getDeviceList().observe(this, devices -> {
            if (devices != null) {
                adapter.setDevices(devices);
            }
        });
        // 토스트 메시지가 갱신되면 화면에 띄움
        viewModel.getToastMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.fetchDevices();
    }
}