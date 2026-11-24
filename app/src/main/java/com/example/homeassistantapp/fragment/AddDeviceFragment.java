package com.example.homeassistantapp.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.network.CommissionInputValidator;
import com.example.homeassistantapp.network.ServerDefaults;
import com.example.homeassistantapp.view.QrPayload;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.example.homeassistantapp.viewmodel.QrScanResult;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

/** 기기 추가 입력(전체 화면). 홈·기기 탭의 추가 버튼에서 열리고, 등록을 시작하면 진행 화면으로 넘어간다. */
public class AddDeviceFragment extends Fragment {
    private static final String KEY_WIFI_SSID = "WIFI_SSID";
    private static final String KEY_WIFI_PASSWORD = "WIFI_PASSWORD";

    private DeviceViewModel viewModel;
    private MaterialButtonToggleGroup toggleType;
    private TextInputLayout layoutName;
    private TextInputLayout layoutCode;
    private TextInputLayout layoutServerIp;
    private EditText editName;
    private EditText editCode;
    private EditText editServerIp;
    private View groupMatter;
    private View cardWifiMissing;
    private TextView textFormError;
    private Spinner spinnerDriver;
    private Draft draft;
    private View rowScanned;
    private TextView textScanned;
    private TextView textScannedError;

    /** 스캔한 QR 내용. 화면을 돌려도 남지만 저장(Bundle·prefs)하지 않는다. */
    public static class Draft extends ViewModel {
        String scannedPayload;
    }

    public static void open(FragmentActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        if (fm.isStateSaved() || fm.findFragmentById(R.id.fragmentContainer) instanceof AddDeviceFragment) return;
        fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, new AddDeviceFragment())
                .addToBackStack(null)
                .commit();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_device, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);

        MaterialToolbar toolbar = view.findViewById(R.id.addToolbar);
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        toggleType = view.findViewById(R.id.toggleType);
        layoutName = view.findViewById(R.id.layoutName);
        layoutCode = view.findViewById(R.id.layoutCode);
        layoutServerIp = view.findViewById(R.id.layoutServerIp);
        editName = view.findViewById(R.id.editName);
        editCode = view.findViewById(R.id.editCode);
        editServerIp = view.findViewById(R.id.editServerIp);
        groupMatter = view.findViewById(R.id.groupMatter);
        cardWifiMissing = view.findViewById(R.id.cardWifiMissing);
        textFormError = view.findViewById(R.id.textFormError);
        spinnerDriver = view.findViewById(R.id.spinnerDriver);
        rowScanned = view.findViewById(R.id.rowScanned);
        textScanned = view.findViewById(R.id.textScanned);
        textScannedError = view.findViewById(R.id.textScannedError);
        draft = new ViewModelProvider(this).get(Draft.class);

        View btnScanQr = view.findViewById(R.id.btnScanQr);
        // 카메라가 없는 기기는 직접 입력만 보인다.
        boolean hasCamera = requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        btnScanQr.setVisibility(hasCamera ? View.VISIBLE : View.GONE);
        btnScanQr.setOnClickListener(v -> QrScanFragment.open(requireActivity()));
        view.findViewById(R.id.btnClearScanned).setOnClickListener(v -> {
            draft.scannedPayload = null;
            renderScanned();
        });
        renderScanned();

        QrScanResult scanResult = new ViewModelProvider(requireActivity()).get(QrScanResult.class);
        scanResult.getScanned().observe(getViewLifecycleOwner(), event -> {
            String payload = event != null ? event.takeIfNotHandled() : null;
            if (payload == null) return;
            scanResult.clear();
            draft.scannedPayload = payload;
            editCode.setText("");
            renderScanned();
            // 이름까지 적었으면 바로 등록하고, 아니면 이름 칸 오류로 안내한다.
            submit();
        });

        layoutServerIp.setHelperText("비워 두면 지금 접속한 서버 주소(" + serverHost() + ")를 써요.");
        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) showTypeFields();
        });
        showTypeFields();

        clearErrorOnEdit(editName, layoutName);
        clearErrorOnEdit(editCode, layoutCode);
        clearErrorOnEdit(editServerIp, layoutServerIp);

        view.findViewById(R.id.btnGoSettings).setOnClickListener(v -> goToSettings());
        view.findViewById(R.id.btnRegister).setOnClickListener(v -> submit());
    }

    @Override
    public void onResume() {
        super.onResume();
        // 설정 탭에서 Wi-Fi를 저장하고 돌아왔을 수 있어 보일 때마다 다시 본다.
        cardWifiMissing.setVisibility(wifiSsid().isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean isMatter() {
        return toggleType.getCheckedButtonId() != R.id.btnTypeTcp;
    }

    private void showTypeFields() {
        boolean matter = isMatter();
        groupMatter.setVisibility(matter ? View.VISIBLE : View.GONE);
        layoutServerIp.setVisibility(matter ? View.GONE : View.VISIBLE);
        textFormError.setVisibility(View.GONE);
    }

    /** 칸 아래에 오류를 보이고 보내지 않는다. 통과하면 등록을 시작한다. */
    private void submit() {
        clearErrors();
        String ssid = wifiSsid();
        if (ssid.isEmpty()) {
            cardWifiMissing.setVisibility(View.VISIBLE);
            showFormError("Wi-Fi를 먼저 설정해 주세요.");
            return;
        }
        String type = isMatter() ? "MATTER" : "TCP_DIY";
        String payload = !isMatter() ? text(editServerIp)
                : draft.scannedPayload != null ? draft.scannedPayload : text(editCode);
        CommissionInputValidator.Result checked = CommissionInputValidator.validate(
                type, text(editName), payload, ssid, wifiPassword(), serverHost());
        if (!checked.isValid()) {
            showError(checked);
            return;
        }
        // 입력 화면은 뒤로 스택에 남겨 둔다. 서버가 거절하면 진행 화면에서 돌아와 고칠 수 있다.
        if (viewModel.addDevice(type, text(editName), checked.payload, ssid, wifiPassword(), driverTypes())) {
            CommissionProgressFragment.open(requireActivity());
        }
    }

    private void showError(CommissionInputValidator.Result result) {
        switch (result.field) {
            case NAME:
                layoutName.setError(result.error);
                editName.requestFocus();
                break;
            case PAYLOAD:
                if (isMatter() && draft.scannedPayload != null) {
                    textScannedError.setText(result.error);
                    textScannedError.setVisibility(View.VISIBLE);
                    break;
                }
                TextInputLayout target = isMatter() ? layoutCode : layoutServerIp;
                target.setError(result.error);
                if (target.getEditText() != null) target.getEditText().requestFocus();
                break;
            default:
                showFormError(result.error);
                break;
        }
    }

    private void showFormError(String message) {
        textFormError.setText(message);
        textFormError.setVisibility(View.VISIBLE);
    }

    private void clearErrors() {
        layoutName.setError(null);
        layoutCode.setError(null);
        layoutServerIp.setError(null);
        textFormError.setVisibility(View.GONE);
        textScannedError.setVisibility(View.GONE);
    }

    // 스캔한 값이 있으면 앞 몇 자만 보이고, 직접 입력 칸은 숨긴다.
    private void renderScanned() {
        boolean scanned = draft.scannedPayload != null;
        rowScanned.setVisibility(scanned ? View.VISIBLE : View.GONE);
        layoutCode.setVisibility(scanned ? View.GONE : View.VISIBLE);
        textScanned.setText(scanned ? "QR 인식됨 · " + QrPayload.masked(draft.scannedPayload) : "");
        textScannedError.setVisibility(View.GONE);
    }

    private static void clearErrorOnEdit(EditText edit, TextInputLayout layout) {
        edit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (layout.getError() != null) layout.setError(null);
            }
        });
    }

    // 하단 탭을 고르면 MainActivity가 이 화면을 뒤로 스택에서 닫는다.
    private void goToSettings() {
        BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNavigation);
        nav.setSelectedItemId(R.id.nav_settings);
    }

    private List<Integer> driverTypes() {
        String[] values = getResources().getStringArray(R.array.driver_value_array);
        int value = Integer.parseInt(values[spinnerDriver.getSelectedItemPosition()]);
        List<Integer> types = new ArrayList<>();
        if (value > 0) types.add(value);
        return types;
    }

    private static String text(EditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString();
    }

    private SharedPreferences prefs() {
        return requireContext().getSharedPreferences(ServerDefaults.PREFS_NAME, Context.MODE_PRIVATE);
    }

    private String wifiSsid() {
        return prefs().getString(KEY_WIFI_SSID, "");
    }

    private String wifiPassword() {
        return prefs().getString(KEY_WIFI_PASSWORD, "");
    }

    private String serverHost() {
        return prefs().getString(ServerDefaults.KEY_SERVER_IP, ServerDefaults.IP);
    }
}
