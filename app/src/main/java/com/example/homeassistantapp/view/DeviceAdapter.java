package com.example.homeassistantapp.view;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.Device;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 기기 탭 목록: 순서 핸들 · 이름·종류·상태 칩 · 삭제 버튼. */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    public interface Listener {
        void onOpen(Device device);
        void onDelete(Device device);
        void onStartDrag(RecyclerView.ViewHolder holder);
        /** TalkBack 사용자용 "위로/아래로 이동"(드래그 대신). delta는 -1 또는 +1. */
        void onMoveBy(RecyclerView.ViewHolder holder, int delta);
    }

    private final Listener listener;
    private List<Device> deviceList = new ArrayList<>();

    public DeviceAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setDevices(List<Device> devices) {
        this.deviceList = new ArrayList<>(devices);
        notifyDataSetChanged();
    }

    public List<Device> getDevices() {
        return deviceList;
    }

    /** 드래그 중 한 칸 이동. 등록 중 임시 항목 자리로는 옮기지 않는다. */
    public boolean move(int from, int to) {
        if (from < 0 || to < 0 || from >= deviceList.size() || to >= deviceList.size()) return false;
        if (!DeviceRow.of(deviceList.get(to)).editable) return false;
        Collections.swap(deviceList, from, to);
        notifyItemMoved(from, to);
        return true;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);
        DeviceRow row = DeviceRow.of(device);

        holder.name.setText(row.name);
        holder.type.setText(row.typeLabel);
        StateChip.apply(holder.state, row.chipText, row.tone);
        holder.progress.setVisibility(row.pairing ? View.VISIBLE : View.GONE);

        holder.handle.setVisibility(row.editable ? View.VISIBLE : View.INVISIBLE);
        holder.handle.setContentDescription(row.name + " 순서 바꾸기");
        holder.handle.setOnTouchListener(row.editable ? (v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) listener.onStartDrag(holder);
            return false;
        } : null);

        holder.delete.setVisibility(row.editable ? View.VISIBLE : View.INVISIBLE);
        holder.delete.setContentDescription(row.name + " 삭제");
        holder.delete.setOnClickListener(row.editable ? v -> listener.onDelete(device) : null);

        holder.main.setClickable(row.editable);
        holder.main.setContentDescription(row.name + ", " + row.typeLabel + ", " + row.chipText
                + (row.editable ? ", 상세 열기" : ""));
        holder.main.setOnClickListener(row.editable ? v -> listener.onOpen(device) : null);

        for (int id : holder.moveActionIds) ViewCompat.removeAccessibilityAction(holder.itemView, id);
        holder.moveActionIds.clear();
        if (row.editable) {
            holder.moveActionIds.add(ViewCompat.addAccessibilityAction(holder.itemView, "위로 이동",
                    (v, args) -> { listener.onMoveBy(holder, -1); return true; }));
            holder.moveActionIds.add(ViewCompat.addAccessibilityAction(holder.itemView, "아래로 이동",
                    (v, args) -> { listener.onMoveBy(holder, 1); return true; }));
        }
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        final TextView name, type, state;
        final ProgressBar progress;
        final ImageView handle;
        final ImageButton delete;
        final View main;
        final List<Integer> moveActionIds = new ArrayList<>();

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvDeviceName);
            type = itemView.findViewById(R.id.tvDeviceType);
            state = itemView.findViewById(R.id.tvDeviceState);
            progress = itemView.findViewById(R.id.progressBar);
            handle = itemView.findViewById(R.id.handleReorder);
            delete = itemView.findViewById(R.id.btnDeleteDevice);
            main = itemView.findViewById(R.id.rowMain);
        }
    }
}
