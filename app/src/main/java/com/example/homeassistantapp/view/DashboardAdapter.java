package com.example.homeassistantapp.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.R;
import java.util.ArrayList;
import java.util.List;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardViewHolder>{
    private List<Device> deviceList = new ArrayList<>();
    private OnDeviceControlListener listener;

    public interface OnDeviceControlListener {
        void onToggle(String deviceId, boolean isChecked);
    }

    public DashboardAdapter(OnDeviceControlListener listener) {
        this.listener = listener;
    }

    public void setDeviceList(List<Device> devices) {
        this.deviceList = devices;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DashboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_tile, parent, false);
        return new DashboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DashboardViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.tvDeviceName.setText(device.getName());

        // 기기 상태에 따른 스위치 UI 업데이트 (무한 루프 방지를 위해 리스너 일시 해제)
        holder.switchControl.setOnCheckedChangeListener(null);
        boolean isTurnedOn = "ON".equalsIgnoreCase(device.getState());
        holder.switchControl.setChecked(isTurnedOn);
        holder.tvStatus.setText(isTurnedOn ? "켜짐" : "꺼짐");

        // 사용자 클릭 이벤트 처리
        holder.switchControl.setOnClickListener(v -> {
            boolean isChecked = holder.switchControl.isChecked();
            if (listener != null) {
                listener.onToggle(device.getId(), isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DashboardViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName, tvStatus;
        SwitchCompat switchControl;
        ImageView ivIcon;

        public DashboardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDashboardDeviceName);
            tvStatus = itemView.findViewById(R.id.tvDashboardStatus);
            switchControl = itemView.findViewById(R.id.switchDashboardControl);
            ivIcon = itemView.findViewById(R.id.ivDashboardIcon);
        }
    }
}
