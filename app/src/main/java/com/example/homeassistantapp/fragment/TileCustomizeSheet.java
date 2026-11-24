package com.example.homeassistantapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.HomeTile;
import com.example.homeassistantapp.view.HomeTileAdapter;
import com.example.homeassistantapp.view.TileConfig;
import com.example.homeassistantapp.view.TileConfigStore;
import com.example.homeassistantapp.view.TrendValues;
import com.example.homeassistantapp.viewmodel.DeviceViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** 타일 꾸미기 시트. 바꾸는 즉시 저장하고 미리보기를 다시 그린다. */
public class TileCustomizeSheet extends BottomSheetDialogFragment {
    private static final String TAG = "TileCustomizeSheet";
    private static final String ARG_DEVICE_ID = "deviceId";

    private DeviceViewModel viewModel;
    private TileConfigStore store;
    private String deviceId;
    private Device device;
    private TileConfig config;
    private boolean binding;

    private View previewTile, previewSpacer, onlySizeNote;
    private MaterialSwitch power, total, trend;
    private MaterialButtonToggleGroup spanGroup;

    public static void open(FragmentManager fm, String deviceId) {
        if (fm.isStateSaved() || fm.findFragmentByTag(TAG) != null) return;
        TileCustomizeSheet sheet = new TileCustomizeSheet();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_ID, deviceId);
        sheet.setArguments(args);
        sheet.show(fm, TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_tile_customize, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
        store = new TileConfigStore(requireContext());
        deviceId = requireArguments().getString(ARG_DEVICE_ID);
        config = store.load(deviceId);

        previewTile = view.findViewById(R.id.previewTile);
        previewSpacer = view.findViewById(R.id.previewSpacer);
        onlySizeNote = view.findViewById(R.id.tvCustomizeOnlySize);
        power = view.findViewById(R.id.switchShowPower);
        total = view.findViewById(R.id.switchShowTotal);
        trend = view.findViewById(R.id.switchShowTrend);
        spanGroup = view.findViewById(R.id.groupTileSpan);
        TextView title = view.findViewById(R.id.tvCustomizeTitle);

        power.setOnCheckedChangeListener((b, on) -> change(b, config.withPower(on)));
        total.setOnCheckedChangeListener((b, on) -> change(b, config.withTotal(on)));
        trend.setOnCheckedChangeListener((b, on) -> change(b, config.withTrend(on)));
        spanGroup.addOnButtonCheckedListener((group, id, checked) -> {
            if (checked && !binding) save(config.withSpan(id == R.id.btnSpanTwo ? 2 : 1));
        });
        view.findViewById(R.id.btnTileDefaults).setOnClickListener(v -> save(TileConfig.DEFAULT));

        viewModel.getDeviceList().observe(getViewLifecycleOwner(), devices -> {
            if (devices == null) return;
            device = find(devices, deviceId);
            if (device == null) {
                // 시트를 연 사이 기기가 삭제됨.
                dismissAllowingStateLoss();
                return;
            }
            title.setText("‘" + device.getName() + "’ 타일 꾸미기");
            render();
        });
        viewModel.getTileTrends().observe(getViewLifecycleOwner(), trends -> render());
    }

    private void change(CompoundButton button, TileConfig next) {
        // render()가 스위치 값을 맞출 때는 저장하지 않는다.
        if (!binding) save(next);
    }

    private void save(TileConfig next) {
        config = next;
        store.save(deviceId, config);
        render();
    }

    private void render() {
        if (device == null) return;
        boolean energy = TileConfig.supportsEnergy(device.getType());
        binding = true;
        power.setChecked(config.power);
        total.setChecked(config.total);
        trend.setChecked(config.trend);
        spanGroup.check(config.span == 2 ? R.id.btnSpanTwo : R.id.btnSpanOne);
        binding = false;
        power.setVisibility(energy ? View.VISIBLE : View.GONE);
        total.setVisibility(energy ? View.VISIBLE : View.GONE);
        trend.setVisibility(energy ? View.VISIBLE : View.GONE);
        onlySizeNote.setVisibility(energy ? View.GONE : View.VISIBLE);

        HomeTile tile = HomeTile.of(device, config);
        TrendValues trendValues = null;
        if (tile.showsTrend) {
            Map<String, TrendValues> trends = viewModel.getTileTrends().getValue();
            trendValues = trends != null ? trends.get(deviceId) : null;
            // 홈에서 아직 받지 않았으면 미리보기를 위해 지금 묻는다(캐시가 중복을 막는다).
            if (trendValues == null) viewModel.fetchTileTrends(Collections.singletonList(deviceId));
        }
        HomeTileAdapter.bindPreview(previewTile, tile, trendValues);
        LinearLayout.LayoutParams spacer = (LinearLayout.LayoutParams) previewSpacer.getLayoutParams();
        spacer.weight = tile.span == 2 ? 0f : 1f;
        previewSpacer.setLayoutParams(spacer);
    }

    private static Device find(List<Device> devices, String id) {
        for (Device d : devices) {
            if (id != null && id.equals(d.getId())) return d;
        }
        return null;
    }
}
