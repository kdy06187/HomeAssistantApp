package com.example.homeassistantapp.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.homeassistantapp.network.SettingsInputValidator;
import com.example.homeassistantapp.security.TokenFormat;
import com.example.homeassistantapp.security.TokenStore;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.EnumMap;
import java.util.Map;

public class SettingsFragment extends Fragment {
    private TextInputEditText etServerIp, etServerPort, etWifiSsid, etWifiPassword, etApiToken;
    private TextInputLayout tilApiToken;
    private final Map<SettingsInputValidator.Field, TextInputLayout> fieldLayouts =
            new EnumMap<>(SettingsInputValidator.Field.class);
    private Button btnSaveSettings;
    private View cardFirstRun;
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
        cardFirstRun = view.findViewById(R.id.cardFirstRun);
        fieldLayouts.put(SettingsInputValidator.Field.SERVER_IP, view.findViewById(R.id.tilServerIp));
        fieldLayouts.put(SettingsInputValidator.Field.SERVER_PORT, view.findViewById(R.id.tilServerPort));
        fieldLayouts.put(SettingsInputValidator.Field.API_TOKEN, tilApiToken);
        fieldLayouts.put(SettingsInputValidator.Field.WIFI_SSID, view.findViewById(R.id.tilWifiSsid));
        fieldLayouts.put(SettingsInputValidator.Field.WIFI_PASSWORD, view.findViewById(R.id.tilWifiPassword));
        for (TextInputLayout layout : fieldLayouts.values()) clearErrorOnEdit(layout);

        prefs = requireActivity().getSharedPreferences(ServerDefaults.PREFS_NAME, Context.MODE_PRIVATE);

        // 저장값이 없을 때만 기본값을 보여 준다.
        etServerIp.setText(prefs.getString(ServerDefaults.KEY_SERVER_IP, ServerDefaults.IP));
        etServerPort.setText(prefs.getString(ServerDefaults.KEY_SERVER_PORT, ServerDefaults.PORT));
        etWifiSsid.setText(prefs.getString("WIFI_SSID", ""));
        etWifiPassword.setText(prefs.getString("WIFI_PASSWORD", ""));

        btnSaveSettings.setOnClickListener(v -> saveSettings());
        renderTokenState();

        // 401로 이 탭에 왔으면 무엇을 고쳐야 하는지 토큰 칸에 바로 보인다.
        if (RetrofitClient.hadUnauthorizedSinceSave()) {
            tilApiToken.setError("서버가 이 토큰을 거절했어요. 새 토큰을 입력해 주세요.");
            etApiToken.requestFocus();
        }
        return view;
    }

    private boolean isFirstRun() {
        return !TokenStore.hasToken(requireContext());
    }

    private void renderTokenState() {
        // 저장된 토큰은 화면에 다시 채우지 않는다. 칸이 비어 있으면 기존 토큰을 유지한다.
        tilApiToken.setHint(isFirstRun() ? "API 토큰 (필수)" : "API 토큰 (저장됨 · 바꿀 때만 입력)");
        cardFirstRun.setVisibility(isFirstRun() ? View.VISIBLE : View.GONE);
    }

    private void saveSettings() {
        String ip = text(etServerIp).trim();
        String port = text(etServerPort).trim();
        // Wi-Fi 값은 앞뒤 공백도 실제 값일 수 있어 trim하지 않는다.
        String ssid = text(etWifiSsid);
        String password = text(etWifiPassword);
        String token = TokenFormat.normalize(text(etApiToken));

        Map<SettingsInputValidator.Field, String> errors = SettingsInputValidator.validate(
                ip, port, token, !isFirstRun(), ssid, password);
        if (showErrors(errors)) return;

        prefs.edit()
                .putString(ServerDefaults.KEY_SERVER_IP, ip)
                .putString(ServerDefaults.KEY_SERVER_PORT, port)
                .putString("WIFI_SSID", ssid)
                .putString("WIFI_PASSWORD", password)
                .apply();

        RetrofitClient.rebuildClient(ip, port);

        if (!token.isEmpty()) {
            if (!TokenStore.save(requireContext(), token)) {
                tilApiToken.setError("토큰을 안전하게 저장하지 못했어요. 다시 시도해 주세요.");
                return;
            }
            RetrofitClient.setApiToken(token);
            etApiToken.setText("");
        }
        renderTokenState();

        // 저장한 값이 또 틀리면 다시 알려 줘야 하므로, 저장할 때마다 401 안내를 되살린다.
        RetrofitClient.resetUnauthorizedNotice();
        Toast.makeText(getContext(), "설정을 저장했어요.", Toast.LENGTH_SHORT).show();
    }

    /** 오류가 있으면 칸마다 보이고 첫 칸으로 커서를 옮긴 뒤 true. */
    private boolean showErrors(Map<SettingsInputValidator.Field, String> errors) {
        for (TextInputLayout layout : fieldLayouts.values()) layout.setError(null);
        if (errors.isEmpty()) return false;
        boolean focused = false;
        for (Map.Entry<SettingsInputValidator.Field, String> e : errors.entrySet()) {
            TextInputLayout layout = fieldLayouts.get(e.getKey());
            layout.setError(e.getValue());
            if (!focused && layout.getEditText() != null) {
                layout.getEditText().requestFocus();
                focused = true;
            }
        }
        return true;
    }

    private static void clearErrorOnEdit(TextInputLayout layout) {
        if (layout.getEditText() == null) return;
        layout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (layout.getError() != null) layout.setError(null);
            }
        });
    }

    private static String text(TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString() : "";
    }
}
