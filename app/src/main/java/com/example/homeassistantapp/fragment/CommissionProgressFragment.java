package com.example.homeassistantapp.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.view.CommissionScreen;
import com.example.homeassistantapp.viewmodel.CommissionStatus;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

/**
 * 등록 진행·결과(전체 화면). 서버 상태(E8)와 경과 시간만 보인다. 등록은 ViewModel이 하므로
 * 이 화면을 나가도 계속되고, 결과는 확인할 때까지 남는다.
 */
public class CommissionProgressFragment extends Fragment {
    private static final long TICK_MS = 1_000L;

    private DeviceViewModel viewModel;
    private final Handler ticker = new Handler(Looper.getMainLooper());
    // 경과 시간은 진행 중일 때만 1초마다 고친다. 결과 화면에서 TalkBack이 매초 읽지 않게 한다.
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            render();
            scheduleTick();
        }
    };
    private boolean started;

    private View progress;
    private TextView textName;
    private TextView textTitle;
    private TextView textElapsed;
    private TextView textBody;
    private MaterialButton btnPrimary;
    private MaterialButton btnSecondary;

    public static void open(FragmentActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        if (fm.isStateSaved() || fm.findFragmentById(R.id.fragmentContainer) instanceof CommissionProgressFragment) return;
        fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, new CommissionProgressFragment())
                .addToBackStack(null)
                .commit();
    }

    /** 홈·기기 탭의 추가 버튼. 등록이 진행 중이면 진행 화면을, 아니면 입력 화면을 연다. */
    public static void openAddOrProgress(FragmentActivity activity) {
        DeviceViewModel vm = new ViewModelProvider(activity).get(DeviceViewModel.class);
        CommissionStatus status = vm.getCommissionStatus().getValue();
        if (status != null && status.isActive()) {
            open(activity);
        } else {
            vm.acknowledgeCommission();
            AddDeviceFragment.open(activity);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_commission_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
        progress = view.findViewById(R.id.progressCommission);
        textName = view.findViewById(R.id.textDeviceName);
        textTitle = view.findViewById(R.id.textProgressTitle);
        textElapsed = view.findViewById(R.id.textElapsed);
        textBody = view.findViewById(R.id.textProgressBody);
        btnPrimary = view.findViewById(R.id.btnProgressPrimary);
        btnSecondary = view.findViewById(R.id.btnProgressSecondary);

        MaterialToolbar toolbar = view.findViewById(R.id.progressToolbar);
        toolbar.setNavigationOnClickListener(v -> leave());
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        leave();
                    }
                });

        viewModel.getCommissionStatus().observe(getViewLifecycleOwner(), status -> {
            render();
            scheduleTick();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        started = true;
        render();
        scheduleTick();
    }

    @Override
    public void onStop() {
        started = false;
        ticker.removeCallbacks(tick);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        ticker.removeCallbacks(tick);
        progress = null;
        textName = null;
        textTitle = null;
        textElapsed = null;
        textBody = null;
        btnPrimary = null;
        btnSecondary = null;
        super.onDestroyView();
    }

    private void scheduleTick() {
        ticker.removeCallbacks(tick);
        CommissionStatus status = viewModel != null ? viewModel.getCommissionStatus().getValue() : null;
        if (started && status != null && status.isActive()) ticker.postDelayed(tick, TICK_MS);
    }

    private void render() {
        if (textTitle == null) return;
        CommissionStatus status = viewModel.getCommissionStatus().getValue();
        CommissionScreen screen = CommissionScreen.of(status, SystemClock.elapsedRealtime());
        progress.setVisibility(screen.tone == CommissionScreen.Tone.PROGRESS ? View.VISIBLE : View.INVISIBLE);
        textName.setText(status != null && status.deviceName != null ? status.deviceName : "");
        textTitle.setText(screen.title);
        textTitle.setTextColor(colorFor(screen.tone));
        textElapsed.setVisibility(screen.elapsed != null ? View.VISIBLE : View.GONE);
        textElapsed.setText(screen.elapsed);
        textBody.setText(screen.body);
        bindButton(btnPrimary, screen.primary, screen.primaryLabel);
        bindButton(btnSecondary, screen.secondary, screen.secondaryLabel);
    }

    private void bindButton(MaterialButton button, CommissionScreen.Action action, String label) {
        button.setVisibility(action == CommissionScreen.Action.NONE ? View.GONE : View.VISIBLE);
        button.setText(label);
        button.setOnClickListener(v -> perform(action));
    }

    private void perform(CommissionScreen.Action action) {
        switch (action) {
            case DONE:
                viewModel.acknowledgeCommission();
                closeAll();
                break;
            case BACK_TO_INPUT:
                // 입력 화면이 뒤에 있으면 값이 그대로 남아 있다. 없으면(탭에서 다시 연 경우) 새로 연다.
                viewModel.acknowledgeCommission();
                FragmentManager fm = getParentFragmentManager();
                if (fm.isStateSaved()) return;
                // 이름으로 찾아 그 위(이 화면)만 걷어 낸다. 입력 화면이 스택에 없으면 이 화면을 닫고 새로 연다.
                FragmentActivity activity = requireActivity();
                if (!fm.popBackStackImmediate(AddDeviceFragment.BACK_STACK_NAME, 0)) {
                    fm.popBackStackImmediate();
                    AddDeviceFragment.open(activity);
                }
                break;
            case OPEN_DEVICES:
                viewModel.acknowledgeCommission();
                BottomNavigationView nav = requireActivity().findViewById(R.id.bottomNavigation);
                nav.setSelectedItemId(R.id.nav_devices);
                break;
            default:
                break;
        }
    }

    /** 뒤로: 진행 중이면 등록은 두고 탭으로, 결과면 확인한 것으로 보고 탭으로 돌아간다. */
    private void leave() {
        CommissionStatus status = viewModel.getCommissionStatus().getValue();
        if (status != null && status.phase == CommissionStatus.Phase.REJECTED) {
            perform(CommissionScreen.Action.BACK_TO_INPUT);
            return;
        }
        if (status == null || !status.isActive()) viewModel.acknowledgeCommission();
        closeAll();
    }

    private void closeAll() {
        FragmentManager fm = getParentFragmentManager();
        if (!fm.isStateSaved()) fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    private int colorFor(CommissionScreen.Tone tone) {
        switch (tone) {
            case SUCCESS: return MaterialColors.getColor(textTitle, androidx.appcompat.R.attr.colorPrimary);
            case ERROR: return MaterialColors.getColor(textTitle, androidx.appcompat.R.attr.colorError);
            default: return MaterialColors.getColor(textTitle, com.google.android.material.R.attr.colorOnSurface);
        }
    }
}
