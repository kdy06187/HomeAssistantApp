package com.example.homeassistantapp.fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.homeassistantapp.R;
import com.example.homeassistantapp.network.RetrofitClient;

import com.google.android.material.textfield.TextInputEditText;

public class SettingsFragment extends Fragment {

    private TextInputEditText etServerIp, etServerPort, etWifiSsid, etWifiPassword;
    private Button btnSaveSettings;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // fragment_settings.xml 레이아웃을 연결합니다.
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 1. 뷰 바인딩
        etServerIp = view.findViewById(R.id.etServerIp);
        etServerPort = view.findViewById(R.id.etServerPort);
        etWifiSsid = view.findViewById(R.id.etWifiSsid);
        etWifiPassword = view.findViewById(R.id.etWifiPassword);
        btnSaveSettings = view.findViewById(R.id.btnSaveSettings);

        // 2. SharedPreferences 초기화 (앱 전역에서 공유할 설정 파일 이름 지정)
        prefs = requireActivity().getSharedPreferences("HomeAssistantPrefs", Context.MODE_PRIVATE);

        // 3. 기존 저장값 불러오기 (값이 없으면 두 번째 인자인 기본값을 띄웁니다)
        etServerIp.setText(prefs.getString("SERVER_IP", "192.168.129.108"));
        etServerPort.setText(prefs.getString("SERVER_PORT", "8080"));
        etWifiSsid.setText(prefs.getString("WIFI_SSID", ""));
        etWifiPassword.setText(prefs.getString("WIFI_PASSWORD", ""));

        // 4. 저장 버튼 클릭 이벤트
        btnSaveSettings.setOnClickListener(v -> saveSettings());

        return view;
    }

    private void saveSettings() {
        String ip = etServerIp.getText() != null ? etServerIp.getText().toString().trim() : "";
        String port = etServerPort.getText() != null ? etServerPort.getText().toString().trim() : "";
        String ssid = etWifiSsid.getText() != null ? etWifiSsid.getText().toString().trim() : "";
        String password = etWifiPassword.getText() != null ? etWifiPassword.getText().toString().trim() : "";

        // IP와 포트는 필수 입력값이므로 검증 로직을 태웁니다.
        if (ip.isEmpty() || port.isEmpty()) {
            Toast.makeText(getContext(), "서버 IP와 포트 번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 입력받은 정보를 기기 내부에 안전하게 저장
        prefs.edit()
                .putString("SERVER_IP", ip)
                .putString("SERVER_PORT", port)
                .putString("WIFI_SSID", ssid)
                .putString("WIFI_PASSWORD", password)
                .apply(); // commit()보다 비동기로 처리되는 apply()가 UI 버벅임을 막아줍니다.

        // 🌟 핵심 로직: 저장된 새 IP와 Port를 바탕으로 Retrofit 통신 객체 즉시 재생성
        RetrofitClient.rebuildClient(ip, port);

        Toast.makeText(getContext(), "설정이 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }
}