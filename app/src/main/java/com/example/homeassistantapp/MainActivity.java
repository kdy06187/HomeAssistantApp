package com.example.homeassistantapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.homeassistantapp.fragment.DeviceFragment;
import com.example.homeassistantapp.fragment.HomeFragment;
import com.example.homeassistantapp.fragment.SettingsFragment;
import com.example.homeassistantapp.network.RetrofitClient;
import com.example.homeassistantapp.network.ServerDefaults;
import com.example.homeassistantapp.security.TokenStore;
import com.example.homeassistantapp.viewmodel.ControlFailure;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences(ServerDefaults.PREFS_NAME, Context.MODE_PRIVATE);
        String ip = prefs.getString(ServerDefaults.KEY_SERVER_IP, ServerDefaults.IP);
        String port = prefs.getString(ServerDefaults.KEY_SERVER_PORT, ServerDefaults.PORT);

        RetrofitClient.rebuildClient(ip, port);
        RetrofitClient.setApiToken(TokenStore.load(this));
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_devices) {
                selectedFragment = new DeviceFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selectedFragment)
                        .commit();
            }
            return true;
        });
        if (savedInstanceState == null) {
            if (RetrofitClient.hasApiToken()) {
                bottomNav.setSelectedItemId(R.id.nav_home);
            } else {
                // 토큰 없이는 모든 요청이 401이므로 처음부터 설정 화면을 연다.
                Toast.makeText(this, "서버 API 토큰을 먼저 입력해 주세요.", Toast.LENGTH_LONG).show();
                bottomNav.setSelectedItemId(R.id.nav_settings);
            }
        }

        // 어느 요청이든 401이면 설정 화면으로 보낸다. 다른 Activity에 있다 돌아와도 LiveData가 전달한다.
        // 탭마다 따로 관찰하면 대시보드에서 토스트가 안 뜨거나 탭을 바꿀 때 다시 뜨므로, 여기서 한 번만 띄운다.
        DeviceViewModel viewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        viewModel.getToastMessage().observe(this, event -> {
            String message = event != null ? event.takeIfNotHandled() : null;
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // 제어 실패는 누른 자리 가까이(하단 탭 위)에 Snackbar로 보이고 같은 명령을 다시 보낼 수 있게 한다.
        viewModel.getControlFailure().observe(this, event -> {
            ControlFailure failure = event != null ? event.takeIfNotHandled() : null;
            if (failure == null) return;
            Snackbar.make(findViewById(R.id.fragmentContainer), failure.message, Snackbar.LENGTH_LONG)
                    .setAnchorView(bottomNav)
                    .setAction("다시 시도", v -> viewModel.controlDevice(failure.deviceId, failure.command))
                    .show();
        });

        viewModel.getDeleteWarning().observe(this, event -> {
            String message = event != null ? event.takeIfNotHandled() : null;
            if (message != null) {
                new AlertDialog.Builder(this)
                        .setTitle("기기 초기화 필요")
                        .setMessage(message)
                        .setPositiveButton("확인", null)
                        .show();
            }
        });

        RetrofitClient.getUnauthorizedSignal().observe(this, signal -> {
            if (!RetrofitClient.consumeUnauthorized()) return;
            Toast.makeText(this, "API 토큰이 맞지 않습니다. 설정에서 다시 입력해 주세요.", Toast.LENGTH_LONG).show();
            if (bottomNav.getSelectedItemId() != R.id.nav_settings) {
                bottomNav.setSelectedItemId(R.id.nav_settings);
            }
        });

    }

}