package com.example.homeassistantapp.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.network.ServerDefaults;
import com.example.homeassistantapp.view.CameraPermissionStep;
import com.example.homeassistantapp.view.QrAnalyzer;
import com.example.homeassistantapp.view.QrPayload;
import com.example.homeassistantapp.viewmodel.QrScanResult;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Matter QR 스캔(전체 화면). 읽으면 내용을 QrScanResult로 넘기고 입력 화면으로 돌아간다. */
public class QrScanFragment extends Fragment {
    private static final String KEY_CAMERA_ASKED = "CAMERA_PERMISSION_ASKED";

    private PreviewView preview;
    private View panelPermission;
    private TextView textPermission;
    private MaterialButton btnPermission;
    private TextView textHint;
    private MaterialButton btnTorch;
    private ExecutorService analysisExecutor;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private boolean cameraStarted;
    private QrPayload.Check lastRejected;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                prefs().edit().putBoolean(KEY_CAMERA_ASKED, true).apply();
                render();
            });

    public static void open(FragmentActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        if (fm.isStateSaved() || fm.findFragmentById(R.id.fragmentContainer) instanceof QrScanFragment) return;
        fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, new QrScanFragment())
                .addToBackStack(null)
                .commit();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preview = view.findViewById(R.id.previewQr);
        panelPermission = view.findViewById(R.id.panelPermission);
        textPermission = view.findViewById(R.id.textPermission);
        btnPermission = view.findViewById(R.id.btnPermission);
        textHint = view.findViewById(R.id.textQrHint);
        btnTorch = view.findViewById(R.id.btnTorch);
        cameraStarted = false;
        lastRejected = null;
        analysisExecutor = Executors.newSingleThreadExecutor();

        MaterialToolbar toolbar = view.findViewById(R.id.qrToolbar);
        toolbar.setNavigationOnClickListener(v -> close());
        view.findViewById(R.id.btnManualEntry).setOnClickListener(v -> close());
        btnTorch.setOnClickListener(v -> toggleTorch());
    }

    @Override
    public void onResume() {
        super.onResume();
        // 앱 설정 화면에서 권한을 켜고 돌아왔을 수 있어 보일 때마다 다시 본다.
        render();
    }

    @Override
    public void onDestroyView() {
        // 카메라 사용은 화면 수명 주기에 묶여 자동으로 풀린다. 분석 스레드만 직접 멈춘다.
        if (analysisExecutor != null) analysisExecutor.shutdown();
        analysisExecutor = null;
        camera = null;
        cameraProvider = null;
        super.onDestroyView();
    }

    private void render() {
        if (!hasCameraHardware()) {
            showPanel("이 기기에서는 카메라를 쓸 수 없어요. 페어링 코드를 직접 입력해 주세요.", "직접 입력", this::close);
            return;
        }
        boolean granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        CameraPermissionStep step = CameraPermissionStep.next(granted,
                prefs().getBoolean(KEY_CAMERA_ASKED, false),
                shouldShowRequestPermissionRationale(Manifest.permission.CAMERA));
        switch (step) {
            case START_CAMERA:
                panelPermission.setVisibility(View.GONE);
                startCamera();
                break;
            case EXPLAIN_FIRST:
                showPanel("QR 코드를 읽으려면 카메라가 필요해요. 찍은 화면은 저장하지 않아요.",
                        "카메라 허용", this::requestPermission);
                break;
            case ASK_AGAIN:
                showPanel("카메라 권한이 없어 QR을 읽을 수 없어요. 허용하거나 코드를 직접 입력해 주세요.",
                        "다시 요청", this::requestPermission);
                break;
            case OPEN_SETTINGS:
                showPanel("카메라 권한이 꺼져 있어요. 앱 설정의 권한에서 카메라를 허용해 주세요.",
                        "설정에서 허용", this::openAppSettings);
                break;
        }
    }

    private void showPanel(String message, String action, Runnable onClick) {
        panelPermission.setVisibility(View.VISIBLE);
        textPermission.setText(message);
        btnPermission.setText(action);
        btnPermission.setOnClickListener(v -> onClick.run());
        btnTorch.setVisibility(View.GONE);
    }

    private void requestPermission() {
        permissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().getPackageName(), null));
        startActivity(intent);
    }

    private void startCamera() {
        if (cameraStarted) return;
        cameraStarted = true;
        Context context = requireContext();
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(context);
        future.addListener(() -> {
            if (getView() == null) return;
            try {
                cameraProvider = future.get();
                bindCamera();
            } catch (Exception e) {
                showCameraUnavailable();
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void bindCamera() {
        Preview previewUseCase = new Preview.Builder().build();
        previewUseCase.setSurfaceProvider(preview.getSurfaceProvider());
        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analysis.setAnalyzer(analysisExecutor, new QrAnalyzer(new QrAnalyzer.Listener() {
            @Override
            public void onMatterCode(String payload) {
                runOnMain(() -> deliver(payload));
            }

            @Override
            public void onRejected(QrPayload.Check reason) {
                runOnMain(() -> showRejected(reason));
            }
        }));
        try {
            CameraSelector selector = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                    ? CameraSelector.DEFAULT_BACK_CAMERA : CameraSelector.DEFAULT_FRONT_CAMERA;
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(getViewLifecycleOwner(), selector, previewUseCase, analysis);
        } catch (Exception e) {
            showCameraUnavailable();
            return;
        }
        boolean hasFlash = camera.getCameraInfo().hasFlashUnit();
        btnTorch.setVisibility(hasFlash ? View.VISIBLE : View.GONE);
        camera.getCameraInfo().getTorchState().observe(getViewLifecycleOwner(), state ->
                btnTorch.setText(state != null && state == TorchState.ON ? "플래시 끄기" : "플래시 켜기"));
    }

    private void toggleTorch() {
        if (camera == null) return;
        Integer state = camera.getCameraInfo().getTorchState().getValue();
        camera.getCameraControl().enableTorch(state == null || state != TorchState.ON);
    }

    private void showCameraUnavailable() {
        showPanel("카메라를 켤 수 없어요. 다른 앱이 카메라를 쓰고 있지 않은지 확인하거나 코드를 직접 입력해 주세요.",
                "직접 입력", this::close);
    }

    private void showRejected(QrPayload.Check reason) {
        if (reason == lastRejected) return;
        lastRejected = reason;
        textHint.setText(QrPayload.rejectMessage(reason));
    }

    private void deliver(String payload) {
        new ViewModelProvider(requireActivity()).get(QrScanResult.class).deliver(payload);
        close();
    }

    private void runOnMain(Runnable action) {
        View view = getView();
        if (view != null) view.post(() -> {
            if (getView() != null && isAdded()) action.run();
        });
    }

    private void close() {
        FragmentManager fm = getParentFragmentManager();
        if (!fm.isStateSaved()) fm.popBackStack();
    }

    private boolean hasCameraHardware() {
        return requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    private SharedPreferences prefs() {
        return requireContext().getSharedPreferences(ServerDefaults.PREFS_NAME, Context.MODE_PRIVATE);
    }
}
