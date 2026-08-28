package com.example.homeassistantapp.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    @NonNull
    @Override
    public DashboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_section, parent, false);
        return new DashboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DashboardViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.tvDeviceName.setText(device.getName());

        holder.widgetContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
        // 기기가 스위치 기능을 지원한다면 토글 위젯 부착
        if (device.getType().equals("MATTER") || device.getType().equals("TCP_DIY")) {
            View toggleWidget = inflater.inflate(R.layout.widget_toggle, holder.widgetContainer, false);
            SwitchCompat switchControl = toggleWidget.findViewById(R.id.switchWidgetControl);

            switchControl.setChecked("ON".equalsIgnoreCase(device.getState()));
            switchControl.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) listener.onToggle(device.getId(), isChecked);
            });

            holder.widgetContainer.addView(toggleWidget);
        }
        // 기기가 전력 측정 기능을 지원한다면 전력량 위젯 부착
        if (device.getType().equals("MATTER")) {
            View energyWidget = inflater.inflate(R.layout.widget_energy_text, holder.widgetContainer, false);
            TextView tvEnergy = energyWidget.findViewById(R.id.tvWidgetEnergy);
            ImageView btnRefresh = energyWidget.findViewById(R.id.btnRefreshEnergyText);

            tvEnergy.setText(device.getPowerInfo());

            btnRefresh.setOnClickListener(v -> {
                if (listener != null) listener.onRefreshEnergy(device.getId());
            });
            holder.widgetContainer.addView(energyWidget);
        }
        // 차트 위젯 부착 (전력량 로그 데이터가 있는 경우에만 조립)
        if (device.getType().equals("MATTER")) {
            View chartWidget = inflater.inflate(R.layout.widget_chart, holder.widgetContainer, false);
            EnergyLineChartView chartView = chartWidget.findViewById(R.id.widgetEnergyChart);
            ImageView btnRefresh = chartWidget.findViewById(R.id.btnRefreshChart);

            // 데이터가 존재하면 차트에 세팅하고, 없으면 텅 빈 상태 유지
            if (device.getEnergyLogs() != null && !device.getEnergyLogs().isEmpty()) {
                chartView.setEnergyData(device.getEnergyLogs());
            }

            // 새로고침 버튼 이벤트 연결
            btnRefresh.setOnClickListener(v -> {
                if (listener != null) listener.onRefreshChart(device.getId());
            });

            holder.widgetContainer.addView(chartWidget);
        }

    }
    public void moveItem(int fromPosition, int toPosition) {
        // 1. 리스트에서 원래 있던 자리의 기기를 뽑아냅니다.
        Device movedDevice = deviceList.remove(fromPosition);
        // 2. 새로운 자리에 그 기기를 밀어 넣습니다.
        deviceList.add(toPosition, movedDevice);
        // 3. UI에 "이 두 자리 순서가 바뀌었어!"라고 알려주어 애니메이션을 실행시킵니다.
        notifyItemMoved(fromPosition, toPosition);
    }

    // 나중에 순서가 바뀐 최종 리스트를 가져오기 위한 Getter 메서드
    public List<Device> getCurrentList() {
        return deviceList;
    }
    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DashboardViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        LinearLayout widgetContainer;

        public DashboardViewHolder(@NonNull View itemView) {
            super(itemView);
            // item_device_section.xml에 있는 뷰들의 ID와 연결
            tvDeviceName = itemView.findViewById(R.id.tvSectionDeviceName);
            widgetContainer = itemView.findViewById(R.id.widgetContainer);
        }
    }
}
