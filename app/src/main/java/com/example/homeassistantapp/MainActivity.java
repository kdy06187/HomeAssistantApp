package com.example.homeassistantapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.homeassistantapp.fragment.CommissionProgressFragment;
import com.example.homeassistantapp.fragment.DeviceFragment;
import com.example.homeassistantapp.fragment.HomeFragment;
import com.example.homeassistantapp.fragment.SettingsFragment;
import com.example.homeassistantapp.network.ConnectionState;
import com.example.homeassistantapp.network.RetrofitClient;
import com.example.homeassistantapp.network.ServerDefaults;
import com.example.homeassistantapp.security.StoredSecret;
import com.example.homeassistantapp.security.TokenStore;
import com.example.homeassistantapp.security.WifiPasswordStore;
import com.example.homeassistantapp.view.BottomInset;
import com.example.homeassistantapp.view.ConnectionBanner;
import com.example.homeassistantapp.view.TileConfigStore;
import com.example.homeassistantapp.viewmodel.CommissionStatus;
import com.example.homeassistantapp.viewmodel.ControlFailure;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {
    private View banner;
    private TextView bannerText;
    private MaterialButton bannerAction;
    private BottomNavigationView bottomNav;
    private DeviceViewModel viewModel;
    private View fragmentContainer;
    private WindowInsetsCompat lastInsets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Android 15부터는 어차피 화면이 시스템 바 아래까지 그려진다. 낮은 버전도 같게 맞춰 여백을 한 곳에서 처리한다.
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 회전처럼 Activity만 다시 만들어질 때는 정적 클라이언트·토큰이 이미 있으므로
        // Keystore 복호화와 클라이언트 재생성을 되풀이하지 않는다(프로세스가 죽었다 살아나면 다시 한다).
        if (savedInstanceState == null || !RetrofitClient.isBuilt()) {
            // 예전 버전이 둔 토큰 암호문·평문 Wi-Fi 비밀번호를 백업 제외 저장소로 옮긴다. 실패하면 다음 실행에 다시 한다.
            TokenStore.migrateLegacy(this);
            WifiPasswordStore.migrateLegacy(this);

            SharedPreferences prefs = getSharedPreferences(ServerDefaults.PREFS_NAME, Context.MODE_PRIVATE);
            String ip = prefs.getString(ServerDefaults.KEY_SERVER_IP, ServerDefaults.IP);
            String port = prefs.getString(ServerDefaults.KEY_SERVER_PORT, ServerDefaults.PORT);

            RetrofitClient.rebuildClient(ip, port);
            StoredSecret token = TokenStore.read(this);
            RetrofitClient.setApiToken(token.state == StoredSecret.State.OK ? token.value : null);
        }
        bottomNav = findViewById(R.id.bottomNavigation);
        banner = findViewById(R.id.connectionBanner);
        bannerText = findViewById(R.id.textConnectionBanner);
        bannerAction = findViewById(R.id.btnConnectionBanner);
        fragmentContainer = findViewById(R.id.fragmentContainer);
        applySystemInsets(findViewById(R.id.mainRoot));
        FragmentManager fragmentManager = getSupportFragmentManager();

        // 상세는 전체 화면이라 열려 있는 동안 하단 탭을 숨긴다.
        fragmentManager.addOnBackStackChangedListener(() -> {
            syncBottomNav(bottomNav);
            renderBanner(RetrofitClient.getConnectionState().getValue());
        });
        syncBottomNav(bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            // 상세가 열린 채 탭이 바뀌면(401 → 설정 등) 상세를 먼저 닫는다.
            if (fragmentManager.getBackStackEntryCount() > 0) {
                fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_devices) {
                selectedFragment = new DeviceFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selectedFragment)
                        .commit();
            }
            // 선택 표시는 이 콜백이 끝난 뒤 바뀌므로 아직 isSettingsShown을 쓸 수 없다.
            renderBanner(RetrofitClient.getConnectionState().getValue(), itemId == R.id.nav_settings);
            return true;
        });
        if (savedInstanceState == null) {
            if (RetrofitClient.hasApiToken()) {
                bottomNav.setSelectedItemId(R.id.nav_home);
            } else {
                // 토큰 없이는 모든 요청이 401이므로 처음부터 설정 화면을 연다.
                Toast.makeText(this, "서버 API 토큰을 먼저 입력해 주세요.", Toast.LENGTH_LONG).show();
                bottomNav.setSelectedItemId(R.id.nav_settings);
            }
        }

        // 어느 요청이든 401이면 설정 화면으로 보낸다. 다른 Activity에 있다 돌아와도 LiveData가 전달한다.
        // 탭마다 따로 관찰하면 대시보드에서 토스트가 안 뜨거나 탭을 바꿀 때 다시 뜨므로, 여기서 한 번만 띄운다.
        viewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        viewModel.getToastMessage().observe(this, event -> {
            String message = event != null ? event.takeIfNotHandled() : null;
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // 제어 실패는 누른 자리 가까이(하단 탭 위)에 Snackbar로 보이고 같은 명령을 다시 보낼 수 있게 한다.
        viewModel.getControlFailure().observe(this, event -> {
            ControlFailure failure = event != null ? event.takeIfNotHandled() : null;
            if (failure == null) return;
            Snackbar.make(findViewById(R.id.fragmentContainer), failure.message, Snackbar.LENGTH_LONG)
                    .setAnchorView(bottomNav.getVisibility() == View.VISIBLE ? bottomNav : null)
                    .setAction("다시 시도", v -> viewModel.controlDevice(failure.deviceId, failure.command))
                    .show();
        });

        // 등록 결과가 나왔을 때 진행 화면 밖에 있으면 알려 주고 결과 화면으로 갈 수 있게 한다.
        viewModel.getCommissionFinished().observe(this, event -> {
            CommissionStatus result = event != null ? event.takeIfNotHandled() : null;
            if (result == null) return;
            if (fragmentManager.findFragmentById(R.id.fragmentContainer) instanceof CommissionProgressFragment) return;
            Snackbar.make(findViewById(R.id.fragmentContainer),
                            "‘" + result.deviceName + "’ 등록 결과가 나왔어요.", Snackbar.LENGTH_LONG)
                    .setAnchorView(bottomNav.getVisibility() == View.VISIBLE ? bottomNav : null)
                    .setAction("보기", v -> CommissionProgressFragment.open(this))
                    .show();
        });

        viewModel.getDeviceDeleted().observe(this, event -> {
            String deviceId = event != null ? event.takeIfNotHandled() : null;
            if (deviceId != null) new TileConfigStore(this).remove(deviceId);
        });

        viewModel.getDeleteWarning().observe(this, event -> {
            String message = event != null ? event.takeIfNotHandled() : null;
            if (message != null) {
                new AlertDialog.Builder(this)
                        .setTitle("기기 초기화 필요")
                        .setMessage(message)
                        .setPositiveButton("확인", null)
                        .show();
            }
        });

        // 탭을 옮겨도 고칠 때까지 보이는 배너. 자동 이동·토스트는 토큰 오류로 바뀐 순간 1번만
        // (회전으로 관찰자가 다시 붙어도 consumeTokenNotice가 false라 되풀이하지 않는다).
        RetrofitClient.getConnectionState().observe(this, kind -> {
            renderBanner(kind);
            if (kind != ConnectionState.Kind.TOKEN_ERROR || !RetrofitClient.consumeTokenNotice()) return;
            Toast.makeText(this, "API 토큰이 맞지 않습니다. 설정에서 다시 입력해 주세요.", Toast.LENGTH_LONG).show();
            openSettings();
        });
    }

    /** 설정 탭이 선택돼 있고 그 위에 전체 화면이 없을 때만 "설정 화면에 있다"고 본다. */
    private boolean isSettingsShown() {
        return bottomNav.getSelectedItemId() == R.id.nav_settings
                && getSupportFragmentManager().getBackStackEntryCount() == 0;
    }

    /** 이미 설정 탭이면 탭 선택 콜백이 다시 오지 않으므로 위에 열린 전체 화면만 닫는다. */
    private void openSettings() {
        if (bottomNav.getSelectedItemId() == R.id.nav_settings) {
            getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            bottomNav.setSelectedItemId(R.id.nav_settings);
        }
    }

    private void renderBanner(ConnectionState.Kind kind) {
        renderBanner(kind, isSettingsShown());
    }

    private void renderBanner(ConnectionState.Kind kind, boolean onSettingsScreen) {
        ConnectionBanner state = ConnectionBanner.of(kind, onSettingsScreen);
        banner.setVisibility(state.visible ? View.VISIBLE : View.GONE);
        if (!state.visible) return;
        bannerText.setText(state.message);
        bannerAction.setVisibility(state.action == ConnectionBanner.Action.NONE ? View.GONE : View.VISIBLE);
        bannerAction.setText(state.actionLabel);
        bannerAction.setOnClickListener(v -> {
            if (state.action == ConnectionBanner.Action.RETRY) viewModel.fetchDevices(true);
            else if (state.action == ConnectionBanner.Action.OPEN_SETTINGS) openSettings();
        });
    }

    private void syncBottomNav(BottomNavigationView bottomNav) {
        boolean detailOpen = getSupportFragmentManager().getBackStackEntryCount() > 0;
        bottomNav.setVisibility(detailOpen ? View.GONE : View.VISIBLE);
        applyBottomInset();
    }

    /**
     * 시스템 바·키보드 여백을 이 한 곳에서 처리한다. 위·좌우는 루트에 주어 배너와 모든 화면이 상태 표시줄 아래에 오게 하고,
     * 아래는 하단 탭이 보이는지에 따라 Fragment 자리에 준다(하단 탭은 제스처 바 여백을 스스로 가진다).
     */
    private void applySystemInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            lastInsets = insets;
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            applyBottomInset();
            // 위·좌우는 여기서 썼으므로 하단 탭에는 아래 여백만 넘긴다(두 번 비지 않게).
            return insets.inset(bars.left, bars.top, bars.right, 0);
        });
        // 하단 탭 높이가 정해진 뒤 키보드 여백을 다시 계산한다.
        bottomNav.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (b - t != ob - ot) applyBottomInset();
        });
    }

    private void applyBottomInset() {
        if (lastInsets == null || fragmentContainer == null) return;
        int bottom = BottomInset.of(bottomNav.getVisibility() == View.VISIBLE, bottomNav.getHeight(),
                lastInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom,
                lastInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
        if (fragmentContainer.getPaddingBottom() != bottom) fragmentContainer.setPadding(0, 0, 0, bottom);
    }
}