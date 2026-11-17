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
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        etServerIp = view.findViewById(R.id.etServerIp);
        etServerPort = view.findViewById(R.id.etServerPort);
        etWifiSsid = view.findViewById(R.id.etWifiSsid);
        etWifiPassword = view.findViewById(R.id.etWifiPassword);
        etApiToken = view.findViewById(R.id.etApiToken);
        tilApiToken = view.findViewById(R.id.tilApiToken);
        btnSaveSettings = view.findViewById(R.id.btnSaveSettings);

        prefs = requireActivity().getSharedPreferences(ServerDefaults.PREFS_NAME, Context.MODE_PRIVATE);

        // 저장값이 없을 때만 기본값을 보여 준다.
        etServerIp.setText(prefs.getString(ServerDefaults.KEY_SERVER_IP, ServerDefaults.IP));
        etServerPort.setText(prefs.getString(ServerDefaults.KEY_SERVER_PORT, ServerDefaults.PORT));
        etWifiSsid.setText(prefs.getString("WIFI_SSID", ""));
        etWifiPassword.setText(prefs.getString("WIFI_PASSWORD", ""));

        // 저장된 토큰은 화면에 다시 채우지 않는다. 칸이 비어 있으면 기존 토큰을 유지한다.
        tilApiToken.setHint(TokenStore.hasToken(requireContext())
                ? "API 토큰 (저장됨 · 바꿀 때만 입력)"
                : "API 토큰 (필수)");

        btnSaveSettings.setOnClickListener(v -> saveSettings());

        return view;
    }

    private void saveSettings() {
        String ip = etServerIp.getText() != null ? etServerIp.getText().toString().trim() : "";
        String port = etServerPort.getText() != null ? etServerPort.getText().toString().trim() : "";
        String ssid = etWifiSsid.getText() != null ? etWifiSsid.getText().toString().trim() : "";
        String password = etWifiPassword.getText() != null ? etWifiPassword.getText().toString().trim() : "";
        String token = etApiToken.getText() != null ? etApiToken.getText().toString().trim() : "";

        // 저장된 토큰도 입력도 없으면 모든 요청이 401이 되므로 막는다.
        if (token.isEmpty() && !TokenStore.hasToken(requireContext())) {
            Toast.makeText(getContext(), "서버 API 토큰을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ip.isEmpty() || port.isEmpty()) {
            Toast.makeText(getContext(), "서버 IP와 포트 번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        prefs.edit()
                .putString(ServerDefaults.KEY_SERVER_IP, ip)
                .putString(ServerDefaults.KEY_SERVER_PORT, port)
                .putString("WIFI_SSID", ssid)
                .putString("WIFI_PASSWORD", password)
                .apply();

        RetrofitClient.rebuildClient(ip, port);

        if (!token.isEmpty()) {
            if (!TokenStore.save(requireContext(), token)) {
                Toast.makeText(getContext(), "토큰을 안전하게 저장하지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            RetrofitClient.setApiToken(token);
            etApiToken.setText("");
            tilApiToken.setHint("API 토큰 (저장됨 · 바꿀 때만 입력)");
        }

        // 저장한 값이 또 틀리면 다시 알려 줘야 하므로, 저장할 때마다 401 안내를 되살린다.
        RetrofitClient.resetUnauthorizedNotice();
        Toast.makeText(getContext(), "설정이 성공적으로 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }
}