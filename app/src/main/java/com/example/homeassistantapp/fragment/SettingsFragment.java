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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.DeviceListResponse;
import com.example.homeassistantapp.network.ConnectionTestResult;
import com.example.homeassistantapp.network.RetrofitClient;
import com.example.homeassistantapp.network.ServerDefaults;
import com.example.homeassistantapp.network.SettingsInputValidator;
import com.example.homeassistantapp.security.TokenFormat;
import com.example.homeassistantapp.security.TokenStore;
import com.example.homeassistantapp.security.StoredSecret;
import com.example.homeassistantapp.security.WifiPasswordStore;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.EnumMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {
    private TextInputEditText etServerIp, etServerPort, etWifiSsid, etWifiPassword, etApiToken;
    private TextInputLayout tilApiToken;
    private final Map<SettingsInputValidator.Field, TextInputLayout> fieldLayouts =
            new EnumMap<>(SettingsInputValidator.Field.class);
    private Button btnSaveSettings;
    private View cardFirstRun;
    private Button btnTestConnection;
    private TextView textTestResult;
    private Call<DeviceListResponse> testCall;
    // 주소·토큰 칸을 고칠 때마다 1씩 오른다. 테스트가 성공한 순간의 번호와 같아야 첫 실행 저장이 된다
    // (토큰 문자열을 따로 들고 있지 않으려고 번호만 비교한다).
    private int inputVersion;
    private int passedVersion = -1;
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
        btnTestConnection = view.findViewById(R.id.btnTestConnection);
        textTestResult = view.findViewById(R.id.textTestResult);
        inputVersion = 0;
        passedVersion = -1;

        prefs = requireActivity().getSharedPreferences(ServerDefaults.PREFS_NAME, Context.MODE_PRIVATE);

        // 저장값이 없을 때만 기본값을 보여 준다.
        etServerIp.setText(prefs.getString(ServerDefaults.KEY_SERVER_IP, ServerDefaults.IP));
        etServerPort.setText(prefs.getString(ServerDefaults.KEY_SERVER_PORT, ServerDefaults.PORT));
        etWifiSsid.setText(prefs.getString("WIFI_SSID", ""));

        btnSaveSettings.setOnClickListener(v -> saveSettings());
        btnTestConnection.setOnClickListener(v -> testConnection());
        // 주소·토큰을 바꾸면 이전 테스트 결과는 더 맞지 않는다.
        TextWatcher invalidateTest = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                inputVersion++;
                textTestResult.setVisibility(View.GONE);
                renderSaveButton();
            }
        };
        etServerIp.addTextChangedListener(invalidateTest);
        etServerPort.addTextChangedListener(invalidateTest);
        etApiToken.addTextChangedListener(invalidateTest);
        renderTokenState();
        renderWifiPasswordState();

        // 401로 이 탭에 왔으면 무엇을 고쳐야 하는지 토큰 칸에 바로 보인다.
        if (RetrofitClient.isTokenRejected()) {
            tilApiToken.setError("서버가 이 토큰을 거절했어요. 새 토큰을 입력해 주세요.");
            etApiToken.requestFocus();
        }
        return view;
    }

    // 첫 실행 = 토큰을 저장한 적 없음. 저장됐지만 못 읽으면(UNREADABLE) 첫 실행은 아니지만 다시 입력해야 한다.
    private boolean isFirstRun() {
        return TokenStore.read(requireContext()).state == StoredSecret.State.NONE;
    }

    private boolean hasUsableToken() {
        return TokenStore.read(requireContext()).state == StoredSecret.State.OK;
    }

    private void renderTokenState() {
        // 저장된 토큰은 화면에 다시 채우지 않는다. 칸이 비어 있으면 기존 토큰을 유지한다.
        StoredSecret.State state = TokenStore.read(requireContext()).state;
        tilApiToken.setHint(state == StoredSecret.State.OK ? "API 토큰 (저장됨 · 바꿀 때만 입력)" : "API 토큰 (필수)");
        tilApiToken.setHelperText(state == StoredSecret.State.UNREADABLE
                ? "저장된 토큰을 읽을 수 없어요. 다시 입력해 주세요." : null);
        cardFirstRun.setVisibility(state == StoredSecret.State.NONE ? View.VISIBLE : View.GONE);
        renderSaveButton();
    }

    // 저장된 비밀번호는 토큰처럼 화면에 다시 채우지 않는다.
    private void renderWifiPasswordState() {
        TextInputLayout layout = fieldLayouts.get(SettingsInputValidator.Field.WIFI_PASSWORD);
        StoredSecret saved = WifiPasswordStore.read(requireContext());
        if (saved.state == StoredSecret.State.OK) {
            layout.setHint("비밀번호 (저장됨 · 바꿀 때만 입력)");
            layout.setHelperText(null);
        } else if (saved.state == StoredSecret.State.UNREADABLE) {
            layout.setHint("비밀번호");
            layout.setHelperText("저장된 비밀번호를 읽을 수 없어요. 다시 입력해 주세요.");
        } else {
            layout.setHint("비밀번호");
            layout.setHelperText(null);
        }
    }

    private void renderSaveButton() {
        boolean firstRun = isFirstRun();
        btnSaveSettings.setText(firstRun ? "저장하고 시작하기" : "변경사항 저장");
        btnSaveSettings.setEnabled(ConnectionTestResult.canSave(firstRun, passedVersion == inputVersion));
    }

    /** 저장하지 않고 입력한 주소·토큰으로 E1을 한 번 부른다. 토큰 칸이 비었으면 저장된 토큰을 쓴다. */
    private void testConnection() {
        String ip = text(etServerIp).trim();
        String port = text(etServerPort).trim();
        String typed = TokenFormat.normalize(text(etApiToken));
        Map<SettingsInputValidator.Field, String> errors = SettingsInputValidator.validate(
                ip, port, typed, hasUsableToken(), "", "");
        if (showErrors(errors)) return;
        String token = typed.isEmpty() ? RetrofitClient.currentApiToken() : typed;
        int startedVersion = inputVersion;

        btnTestConnection.setEnabled(false);
        btnTestConnection.setText("연결 테스트 중…");
        textTestResult.setVisibility(View.GONE);
        try {
            testCall = RetrofitClient.createTestApi(ip, port, token).getDeviceList();
        } catch (IllegalArgumentException e) {
            showTestResult(ConnectionTestResult.fromFailure(e), startedVersion);
            return;
        }
        testCall.enqueue(new Callback<DeviceListResponse>() {
            @Override
            public void onResponse(Call<DeviceListResponse> call, Response<DeviceListResponse> response) {
                showTestResult(ConnectionTestResult.fromHttp(response.code()), startedVersion);
            }

            @Override
            public void onFailure(Call<DeviceListResponse> call, Throwable t) {
                if (call.isCanceled()) return;
                showTestResult(ConnectionTestResult.fromFailure(t), startedVersion);
            }
        });
    }

    private void showTestResult(ConnectionTestResult result, int startedVersion) {
        if (getView() == null) return;
        btnTestConnection.setEnabled(true);
        btnTestConnection.setText("연결 테스트");
        // 테스트 중에 입력을 바꿨으면(되돌렸더라도) 이 결과로는 저장을 풀지 않는다.
        passedVersion = result.isSuccess() && startedVersion == inputVersion ? startedVersion : -1;
        textTestResult.setVisibility(View.VISIBLE);
        textTestResult.setText(result.message);
        textTestResult.setTextColor(MaterialColors.getColor(textTestResult, result.isSuccess()
                ? androidx.appcompat.R.attr.colorPrimary : androidx.appcompat.R.attr.colorError));
        if (result.kind == ConnectionTestResult.Kind.TOKEN_ERROR) {
            tilApiToken.setError("서버가 이 토큰을 거절했어요.");
            etApiToken.requestFocus();
        }
        renderSaveButton();
    }

    @Override
    public void onDestroyView() {
        if (testCall != null) testCall.cancel();
        testCall = null;
        super.onDestroyView();
    }

    private void saveSettings() {
        String ip = text(etServerIp).trim();
        String port = text(etServerPort).trim();
        // Wi-Fi 값은 앞뒤 공백도 실제 값일 수 있어 trim하지 않는다.
        String ssid = text(etWifiSsid);
        String password = text(etWifiPassword);
        String token = TokenFormat.normalize(text(etApiToken));

        boolean firstRun = isFirstRun();
        Map<SettingsInputValidator.Field, String> errors = SettingsInputValidator.validate(
                ip, port, token, hasUsableToken(), ssid, password);
        if (showErrors(errors)) return;
        if (!ConnectionTestResult.canSave(firstRun, passedVersion == inputVersion)) return;

        // 토큰을 먼저 저장한다. 실패하면 아무것도 바꾸지 않는다(주소만 바뀐 반쪽 저장 방지).
        if (!token.isEmpty()) {
            if (!TokenStore.save(requireContext(), token)) {
                tilApiToken.setError("토큰을 안전하게 저장하지 못했어요. 다시 시도해 주세요.");
                return;
            }
            RetrofitClient.setApiToken(token);
            etApiToken.setText("");
        }

        // 비밀번호 칸이 비어 있으면 저장된 비밀번호를 그대로 둔다.
        if (!password.isEmpty() && !WifiPasswordStore.save(requireContext(), password)) {
            fieldLayouts.get(SettingsInputValidator.Field.WIFI_PASSWORD)
                    .setError("비밀번호를 안전하게 저장하지 못했어요. 다시 시도해 주세요.");
            return;
        }
        if (!password.isEmpty()) etWifiPassword.setText("");
        renderWifiPasswordState();

        prefs.edit()
                .putString(ServerDefaults.KEY_SERVER_IP, ip)
                .putString(ServerDefaults.KEY_SERVER_PORT, port)
                .putString("WIFI_SSID", ssid)
                .apply();

        RetrofitClient.rebuildClient(ip, port);
        renderTokenState();

        // 저장한 값이 또 틀리면 다시 알려 줘야 하므로, 저장할 때마다 401 안내를 되살린다.
        RetrofitClient.onSettingsSaved();
        Toast.makeText(getContext(), "설정을 저장했어요.", Toast.LENGTH_SHORT).show();
        // 첫 실행이면 연결을 확인했으니 바로 홈으로 간다.
        if (firstRun) {
            BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNavigation);
            nav.setSelectedItemId(R.id.nav_home);
        }
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
