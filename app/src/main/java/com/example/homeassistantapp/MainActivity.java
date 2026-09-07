package com.example.homeassistantapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.homeassistantapp.fragment.DeviceFragment;
import com.example.homeassistantapp.fragment.HomeFragment;
import com.example.homeassistantapp.fragment.SettingsFragment;
import com.example.homeassistantapp.network.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // MainActivity.java의 onCreate() 내부
        SharedPreferences prefs = getSharedPreferences("HomeAssistantPrefs", Context.MODE_PRIVATE);
        String ip = prefs.getString("SERVER_IP", "192.168.219.108");
        String port = prefs.getString("SERVER_PORT", "8000");

        // 앱 구동 시 저장된 설정값으로 Retrofit 초기화
        RetrofitClient.rebuildClient(ip, port);
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
            bottomNav.setSelectedItemId(R.id.nav_devices);
        }

    }

}