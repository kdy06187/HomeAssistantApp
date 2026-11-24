package com.example.homeassistantapp.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.Device;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 홈 2열 타일. 값은 HomeTile이 정하고 여기서는 그리기만 한다. */
public class HomeTileAdapter extends RecyclerView.Adapter<HomeTileAdapter.TileViewHolder> {

    public interface Listener {
        void onToggle(Device device, boolean turnOn);
        void onRefresh(Device device);
        void onOpen(Device device);
    }

    private final Listener listener;
    private List<Device> devices = new ArrayList<>();
    private Map<String, TileConfig> configs = new HashMap<>();
    private Map<String, TrendValues> trends = new HashMap<>();

    public HomeTileAdapter(Listener listener) {
        this.listener = listener;
    }

    /** configs에 없는 기기는 기본 설정으로 그린다. */
    public void setDevices(List<Device> devices, Map<String, TileConfig> configs) {
        this.devices = new ArrayList<>(devices);
        this.configs = new HashMap<>(configs);
        notifyDataSetChanged();
    }

    public void setTrends(Map<String, TrendValues> trends) {
        this.trends = new HashMap<>(trends);
        notifyDataSetChanged();
    }

    /** GridLayoutManager.SpanSizeLookup용. 2칸 타일은 한 줄을 다 쓴다. */
    public int spanAt(int position) {
        if (position < 0 || position >= devices.size()) return 1;
        return tileAt(position).span;
    }

    private HomeTile tileAt(int position) {
        Device device = devices.get(position);
        return HomeTile.of(device, configs.get(device.getId()));
    }

    @NonNull
    @Override
    public TileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_tile, parent, false);
        return new TileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TileViewHolder holder, int position) {
        Device device = devices.get(position);
        HomeTile tile = tileAt(position);
        bindLook(holder, tile, tile.showsTrend ? trends.get(device.getId()) : null);

        holder.toggle.setOnCheckedChangeListener((button, isChecked) -> {
            if (button.isPressed()) listener.onToggle(device, isChecked);
        });
        holder.refresh.setOnClickListener(v -> listener.onRefresh(device));
        holder.main.setClickable(tile.opensDetail);
        holder.main.setContentDescription(tile.opensDetail ? tile.name + ", " + tile.chipText + ", 상세 열기" : null);
        holder.main.setOnClickListener(tile.opensDetail ? v -> listener.onOpen(device) : null);
    }

    /** 꾸미기 화면의 미리보기. 홈과 같은 모양으로 그리되 누를 수 없게 한다. */
    public static void bindPreview(View tileRoot, HomeTile tile, TrendValues trend) {
        TileViewHolder holder = new TileViewHolder(tileRoot);
        bindLook(holder, tile, trend);
        holder.toggle.setEnabled(false);
        holder.refresh.setEnabled(false);
        holder.main.setClickable(false);
        // 미리보기 안의 스위치·버튼을 TalkBack이 진짜 버튼으로 읽지 않게 한 덩어리로 묶는다.
        tileRoot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        tileRoot.setContentDescription("미리보기: " + tile.name + ", " + tile.chipText
                + (tile.showsPower ? ", " + tile.powerText : "")
                + (tile.metaText.isEmpty() ? "" : ", " + tile.metaText)
                + (tile.showsTrend ? ", 추이선" : "") + ", " + tile.span + "칸");
        holder.itemView.findViewById(R.id.tileMain).setImportantForAccessibility(
                View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    /** 눌렀을 때 동작을 뺀 보이는 부분. trend는 아직 못 받았으면 null. */
    private static void bindLook(TileViewHolder holder, HomeTile tile, TrendValues trend) {
        holder.name.setText(tile.name);
        StateChip.apply(holder.state, tile.chipText, tile.tone);

        // 다시 그릴 때 이전 리스너가 setChecked에 반응하지 않도록 먼저 뗀다.
        holder.toggle.setOnCheckedChangeListener(null);
        holder.toggle.setVisibility(tile.switchVisible ? View.VISIBLE : View.INVISIBLE);
        holder.toggle.setChecked(tile.switchChecked);
        holder.toggle.setEnabled(tile.switchEnabled);
        holder.toggle.setContentDescription(tile.name + " 켜기");

        holder.refresh.setVisibility(tile.showsRefresh ? View.VISIBLE : View.GONE);
        holder.refresh.setContentDescription(tile.name + " 상태 새로고침");

        holder.power.setVisibility(tile.showsPower ? View.VISIBLE : View.GONE);
        holder.power.setText(tile.powerText);
        holder.meta.setVisibility(tile.metaText.isEmpty() ? View.GONE : View.VISIBLE);
        holder.meta.setText(tile.metaText);

        holder.trend.setVisibility(tile.showsTrend ? View.VISIBLE : View.GONE);
        // 아직 못 받았으면 빈 자리, 받았는데 기록이 없으면 "–".
        boolean empty = trend != null && trend.isEmpty();
        holder.spark.setTrend(empty ? null : trend);
        holder.trendEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class TileViewHolder extends RecyclerView.ViewHolder {
        final TextView name, state, power, meta;
        final MaterialSwitch toggle;
        final ImageButton refresh;
        final View trend, trendEmpty;
        final SparklineView spark;
        final View main;

        TileViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvTileName);
            state = itemView.findViewById(R.id.tvTileState);
            power = itemView.findViewById(R.id.tvTilePower);
            meta = itemView.findViewById(R.id.tvTileMeta);
            toggle = itemView.findViewById(R.id.switchTile);
            refresh = itemView.findViewById(R.id.btnTileRefresh);
            main = itemView.findViewById(R.id.tileMain);
            trend = itemView.findViewById(R.id.tileTrend);
            trendEmpty = itemView.findViewById(R.id.tvTileTrendEmpty);
            spark = itemView.findViewById(R.id.sparkTile);
        }
    }
}
