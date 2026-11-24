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
import java.util.List;

/** 홈 2열 타일. 값은 HomeTile이 정하고 여기서는 그리기만 한다. */
public class HomeTileAdapter extends RecyclerView.Adapter<HomeTileAdapter.TileViewHolder> {

    public interface Listener {
        void onToggle(Device device, boolean turnOn);
        void onRefresh(Device device);
        void onOpen(Device device);
    }

    private final Listener listener;
    private List<Device> devices = new ArrayList<>();

    public HomeTileAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setDevices(List<Device> devices) {
        this.devices = new ArrayList<>(devices);
        notifyDataSetChanged();
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
        HomeTile tile = HomeTile.of(device);

        holder.name.setText(tile.name);
        StateChip.apply(holder.state, tile.chipText, tile.tone);

        // 다시 그릴 때 이전 리스너가 setChecked에 반응하지 않도록 먼저 뗀다.
        holder.toggle.setOnCheckedChangeListener(null);
        holder.toggle.setVisibility(tile.switchVisible ? View.VISIBLE : View.INVISIBLE);
        holder.toggle.setChecked(tile.switchChecked);
        holder.toggle.setEnabled(tile.switchEnabled);
        holder.toggle.setContentDescription(tile.name + " 켜기");
        holder.toggle.setOnCheckedChangeListener((button, isChecked) -> {
            if (button.isPressed()) listener.onToggle(device, isChecked);
        });

        holder.refresh.setVisibility(tile.showsRefresh ? View.VISIBLE : View.GONE);
        holder.refresh.setContentDescription(tile.name + " 상태 새로고침");
        holder.refresh.setOnClickListener(v -> listener.onRefresh(device));

        holder.power.setVisibility(tile.showsPower ? View.VISIBLE : View.GONE);
        holder.power.setText(tile.powerText);
        holder.meta.setVisibility(tile.metaText.isEmpty() ? View.GONE : View.VISIBLE);
        holder.meta.setText(tile.metaText);

        holder.main.setClickable(tile.opensDetail);
        holder.main.setContentDescription(tile.opensDetail ? tile.name + ", " + tile.chipText + ", 상세 열기" : null);
        holder.main.setOnClickListener(tile.opensDetail ? v -> listener.onOpen(device) : null);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    static class TileViewHolder extends RecyclerView.ViewHolder {
        final TextView name, state, power, meta;
        final MaterialSwitch toggle;
        final ImageButton refresh;
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
        }
    }
}
