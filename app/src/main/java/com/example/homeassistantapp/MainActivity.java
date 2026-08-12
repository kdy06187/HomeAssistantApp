package com.example.homeassistantapp;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.dto.DeviceListResponse;
import com.example.homeassistantapp.view.DeviceAdapter;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
        adapter.setOnDeviceLongClickListener(device -> showDeviceDeleteDialog(device));


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
        FloatingActionButton fabAddDevice = findViewById(R.id.fabAddDevice);
        fabAddDevice.setOnClickListener(v -> showAddDeviceDialog());
    }
    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
                Toast.makeText(MainActivity.this, "정보를 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    private void showDeviceDeleteDialog(Device device) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("기기 삭제")
                .setMessage("정말 [" + device.getName() + "] 기기를 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    viewModel.deleteDevice(device.getId()); // ViewModel의 삭제 로직 호출!
                })
                .setNegativeButton("취소", null)
                .show();
    }
}