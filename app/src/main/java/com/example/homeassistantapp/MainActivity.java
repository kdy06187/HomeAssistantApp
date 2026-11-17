package com.example.homeassistantapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.homeassistantapp.fragment.DeviceFragment;
import com.example.homeassistantapp.fragment.HomeFragment;
import com.example.homeassistantapp.fragment.SettingsFragment;
import com.example.homeassistantapp.network.RetrofitClient;
import com.example.homeassistantapp.network.ServerDefaults;
import com.example.homeassistantapp.security.TokenStore;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // MainActivity.java의 onCreate() 내부
        // 저장된 설정값이 우선이고, 처음 실행이라 값이 없을 때만 ServerDefaults를 쓴다(불일치 #3).
        SharedPreferences prefs = getSharedPreferences(ServerDefaults.PREFS_NAME, Context.MODE_PRIVATE);
        String ip = prefs.getString(ServerDefaults.KEY_SERVER_IP, ServerDefaults.IP);
        String port = prefs.getString(ServerDefaults.KEY_SERVER_PORT, ServerDefaults.PORT);

        // 앱 구동 시 저장된 설정값으로 Retrofit 초기화
        RetrofitClient.rebuildClient(ip, port);
        // 계약서 §1.1: 암호화해 둔 API 토큰을 꺼내 모든 요청의 Bearer 헤더에 쓰게 한다.
        RetrofitClient.setApiToken(TokenStore.load(this));
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);

        // 탭 클릭 리스너 설정
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

            // 프래그먼트 교체 (null 체크 후)
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selectedFragment)
                        .commit();
            }
            return true;
        });
        if (savedInstanceState == null) {
            if (RetrofitClient.hasApiToken()) {
                bottomNav.setSelectedItemId(R.id.nav_devices);
            } else {
                // 왜: 토큰 없이 요청하면 서버가 모두 401로 거절한다. 빈 목록과 오류 토스트를 보여 주는 대신
                // 처음부터 설정 화면을 열어 토큰을 넣도록 안내한다.
                Toast.makeText(this, "서버 API 토큰을 먼저 입력해 주세요.", Toast.LENGTH_LONG).show();
                bottomNav.setSelectedItemId(R.id.nav_settings);
            }
        }

        // 계약서 §4.2: 어느 요청이든 401이 오면 토큰을 다시 넣는 설정 화면으로 보낸다(불일치 #2).
        // 토큰이 틀렸거나 서버에서 바뀐 경우다. 다른 화면(상세 화면)에 있다가 돌아와도
        // LiveData가 그때 값을 전달하므로 이동이 빠지지 않는다.
        RetrofitClient.getUnauthorizedSignal().observe(this, signal -> {
            if (!RetrofitClient.consumeUnauthorized()) return; // 이미 처리한 401
            Toast.makeText(this, "API 토큰이 맞지 않습니다. 설정에서 다시 입력해 주세요.", Toast.LENGTH_LONG).show();
            if (bottomNav.getSelectedItemId() != R.id.nav_settings) {
                bottomNav.setSelectedItemId(R.id.nav_settings);
            }
        });

    }

}