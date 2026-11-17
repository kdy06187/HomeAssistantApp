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
import com.example.homeassistantapp.network.ServerDefaults;

import com.example.homeassistantapp.security.TokenStore;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SettingsFragment extends Fragment {

    private TextInputEditText etServerIp, etServerPort, etWifiSsid, etWifiPassword, etApiToken;
    private TextInputLayout tilApiToken;
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
        etApiToken = view.findViewById(R.id.etApiToken);
        tilApiToken = view.findViewById(R.id.tilApiToken);
        btnSaveSettings = view.findViewById(R.id.btnSaveSettings);

        // 2. SharedPreferences 초기화 (앱 전역에서 공유할 설정 파일 이름 지정)
        prefs = requireActivity().getSharedPreferences(ServerDefaults.PREFS_NAME, Context.MODE_PRIVATE);

        // 3. 기존 저장값 불러오기 (값이 없으면 ServerDefaults의 기본값을 띄웁니다)
        // 불일치 #3: 예전 기본값 :8080은 DIY 기기 전용 포트라 앱이 쓰면 안 된다(계약서 §1).
        etServerIp.setText(prefs.getString(ServerDefaults.KEY_SERVER_IP, ServerDefaults.IP));
        etServerPort.setText(prefs.getString(ServerDefaults.KEY_SERVER_PORT, ServerDefaults.PORT));
        etWifiSsid.setText(prefs.getString("WIFI_SSID", ""));
        etWifiPassword.setText(prefs.getString("WIFI_PASSWORD", ""));

        // 왜 저장된 토큰을 칸에 다시 채우지 않는가: 화면에 비밀 값을 꺼내 둘 이유가 없다.
        // 저장 여부만 힌트로 알려 주고, 칸이 비어 있으면 기존 토큰을 그대로 둔다.
        tilApiToken.setHint(TokenStore.hasToken(requireContext())
                ? "API 토큰 (저장됨 · 바꿀 때만 입력)"
                : "API 토큰 (필수)");

        // 4. 저장 버튼 클릭 이벤트
        btnSaveSettings.setOnClickListener(v -> saveSettings());

        return view;
    }

    private void saveSettings() {
        String ip = etServerIp.getText() != null ? etServerIp.getText().toString().trim() : "";
        String port = etServerPort.getText() != null ? etServerPort.getText().toString().trim() : "";
        String ssid = etWifiSsid.getText() != null ? etWifiSsid.getText().toString().trim() : "";
        String password = etWifiPassword.getText() != null ? etWifiPassword.getText().toString().trim() : "";
        String token = etApiToken.getText() != null ? etApiToken.getText().toString().trim() : "";

        // 토큰이 한 번도 저장된 적 없는데 칸도 비어 있으면, 저장해도 모든 요청이 401이 되므로 막는다.
        if (token.isEmpty() && !TokenStore.hasToken(requireContext())) {
            Toast.makeText(getContext(), "서버 API 토큰을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // IP와 포트는 필수 입력값이므로 검증 로직을 태웁니다.
        if (ip.isEmpty() || port.isEmpty()) {
            Toast.makeText(getContext(), "서버 IP와 포트 번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 입력받은 정보를 기기 내부에 안전하게 저장
        prefs.edit()
                .putString(ServerDefaults.KEY_SERVER_IP, ip)
                .putString(ServerDefaults.KEY_SERVER_PORT, port)
                .putString("WIFI_SSID", ssid)
                .putString("WIFI_PASSWORD", password)
                .apply(); // commit()보다 비동기로 처리되는 apply()가 UI 버벅임을 막아줍니다.

        // 🌟 핵심 로직: 저장된 새 IP와 Port를 바탕으로 Retrofit 통신 객체 즉시 재생성
        RetrofitClient.rebuildClient(ip, port);

        // 새 토큰을 입력했을 때만 암호화해서 저장하고, 메모리의 토큰도 바꾼다.
        if (!token.isEmpty()) {
            if (!TokenStore.save(requireContext(), token)) {
                Toast.makeText(getContext(), "토큰을 안전하게 저장하지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            RetrofitClient.setApiToken(token);
            etApiToken.setText(""); // 저장 후 화면에서 비밀 값을 지운다.
            tilApiToken.setHint("API 토큰 (저장됨 · 바꿀 때만 입력)");
        }

        Toast.makeText(getContext(), "설정이 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }
}