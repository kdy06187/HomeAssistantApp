package com.example.homeassistantapp.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.R;
import com.google.android.material.color.MaterialColors;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardViewHolder>{
    private List<Device> deviceList = new ArrayList<>();
    private OnDeviceControlListener listener;

    private Set<String> editingDeviceIds = new HashSet<>();
    public interface OnDeviceControlListener {
        void onToggle(String deviceId, boolean isChecked);
        void onRefreshDevice(String deviceId);
        void onRefreshChart(String deviceId);
        void onRefreshEnergy(String deviceId);
    }

    public DashboardAdapter(OnDeviceControlListener listener) {
        this.listener = listener;
    }

    public void setDeviceList(List<Device> devices) {
        this.deviceList = devices;
        notifyDataSetChanged();
    }
    public boolean isAnyDeviceEditing() {
        return !editingDeviceIds.isEmpty();
    }
    @NonNull
    @Override
    public DashboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_section, parent, false);
        return new DashboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DashboardViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.currentDeviceId = device.getId();

        boolean isEditing = editingDeviceIds.contains(device.getId());
        holder.widgetAdapter.setEditMode(isEditing);
        if (isEditing) {
            holder.btnEditWidgets.setColorFilter(MaterialColors.getColor(holder.itemView, androidx.appcompat.R.attr.colorPrimary));
        } else {
            holder.btnEditWidgets.clearColorFilter();
        }
        holder.btnEditWidgets.setOnClickListener(v -> {
            if (editingDeviceIds.contains(device.getId())) {
                editingDeviceIds.remove(device.getId());
            } else {
                editingDeviceIds.add(device.getId());
            }
            notifyItemChanged(position);
        });

        holder.tvDeviceName.setText(device.getName());
        String status = device.getState();

        holder.pbDeviceLoading.setVisibility(View.GONE);
        holder.btnRefreshStatus.setVisibility(View.VISIBLE);
        holder.btnRefreshStatus.setEnabled(true);
        holder.widgetContainer.setVisibility(View.VISIBLE);

        if ("TEMP_PAIRING".equals(device.getId()) || device.isPairing()) {
            holder.tvDeviceStatus.setText("페어링중");
            applyTone(holder.tvDeviceStatus, DeviceStateLabel.Tone.OFF);
            holder.widgetContainer.setVisibility(View.GONE);
            holder.itemView.setAlpha(0.5f);
        } else {
            DeviceStateLabel.Kind kind = DeviceStateLabel.kindOf(status);
            holder.tvDeviceStatus.setText(kind.text);
            applyTone(holder.tvDeviceStatus, kind.tone);
            switch (kind) {
                case LOADING:
                    holder.pbDeviceLoading.setVisibility(View.VISIBLE);
                    holder.btnRefreshStatus.setEnabled(false); // 로딩 중 중복 클릭 방지
                    holder.itemView.setAlpha(0.7f);
                    break;
                case OFFLINE:
                    holder.itemView.setAlpha(0.5f);
                    break;
                case UNKNOWN:
                    holder.itemView.setAlpha(0.8f);
                    break;
                default:
                    holder.itemView.setAlpha(1.0f);
                    break;
            }
        }
        holder.btnRefreshStatus.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRefreshDevice(device.getId());
            }
        });

        List<String> activeWidgets = new ArrayList<>();

        // TODO: 위젯 목록을 기기 타입이 아니라 저장값에서 불러오기
        if ("MATTER".equals(device.getType()) || "TCP_DIY".equals(device.getType())) {
            activeWidgets.add("TOGGLE");
        }
        if ("MATTER".equals(device.getType())) {
            activeWidgets.add("ENERGY");
            activeWidgets.add("CHART");
        }

        SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences("DashboardPrefs", Context.MODE_PRIVATE);
        String savedOrder = prefs.getString(device.getId() + "_widget_order", "");

        if (!savedOrder.isEmpty()) {
            List<String> sortedWidgets = new ArrayList<>();
            String[] savedArray = savedOrder.split(",");

            for (String type : savedArray) {
                if (activeWidgets.contains(type)) {
                    sortedWidgets.add(type);
                    activeWidgets.remove(type);
                }
            }
            // 저장된 순서에 없는 새 위젯은 맨 뒤에 붙인다.
            sortedWidgets.addAll(activeWidgets);

            activeWidgets = sortedWidgets;
        }

        holder.widgetAdapter.setWidgets(activeWidgets, device, listener);


    }
    public void moveItem(int fromPosition, int toPosition) {
        Device movedDevice = deviceList.remove(fromPosition);
        deviceList.add(toPosition, movedDevice);
        notifyItemMoved(fromPosition, toPosition);
    }

    public List<Device> getCurrentList() {
        return deviceList;
    }
    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    private static void applyTone(TextView chip, DeviceStateLabel.Tone tone) {
        chip.setTextColor(ContextCompat.getColor(chip.getContext(), tone.foreground));
        ViewCompat.setBackgroundTintList(chip,
                ColorStateList.valueOf(ContextCompat.getColor(chip.getContext(), tone.background)));
    }

    static class DashboardViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName,tvDeviceStatus;
        ProgressBar pbDeviceLoading;
        ImageButton btnRefreshStatus;
        ImageButton btnEditWidgets;
        RecyclerView widgetContainer;
        WidgetAdapter widgetAdapter;
        String currentDeviceId;

        public DashboardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvSectionDeviceName);
            tvDeviceStatus = itemView.findViewById(R.id.tvDeviceStatus);
            pbDeviceLoading = itemView.findViewById(R.id.pbDeviceLoading);
            btnRefreshStatus = itemView.findViewById(R.id.btnRefreshStatus);
            btnEditWidgets = itemView.findViewById(R.id.btnEditWidgets);
            widgetContainer = itemView.findViewById(R.id.widgetRecyclerView);

            widgetContainer.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            widgetAdapter = new WidgetAdapter();
            widgetContainer.setAdapter(widgetAdapter);

            ItemTouchHelper.SimpleCallback widgetTouch = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                @Override
                public boolean isLongPressDragEnabled() {
                    return false;
                }
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    widgetAdapter.moveItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                    return true;
                }
                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    // 스와이프는 쓰지 않는다.
                }
                @Override
                public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                    super.onSelectedChanged(viewHolder, actionState);
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        widgetContainer.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }

                // 드래그가 끝나면 바깥 목록 스크롤을 다시 허용한다.
                @Override
                public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);

                    if (currentDeviceId != null) {
                        SharedPreferences prefs = itemView.getContext().getSharedPreferences("DashboardPrefs", Context.MODE_PRIVATE);
                        StringBuilder orderStr = new StringBuilder();
                        for (String type : widgetAdapter.getWidgetTypes()) {
                            orderStr.append(type).append(",");
                        }
                        prefs.edit().putString(currentDeviceId + "_widget_order", orderStr.toString()).apply();
                    }
                }

            };
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(widgetTouch);
            itemTouchHelper.attachToRecyclerView(widgetContainer);
            widgetAdapter.setTouchHelper(itemTouchHelper);


        }
    }
}
