package com.example.homeassistantapp.view.widgets;

import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.DashboardAdapter;
import com.example.homeassistantapp.view.EnergyLineChartView;
import com.example.homeassistantapp.view.WidgetAdapter;

public class ChartViewHolder extends RecyclerView.ViewHolder{
    private EnergyLineChartView chartView;
    private ImageView btnRefresh;
    private ImageButton btnDeleteWidget;
    public ChartViewHolder(@NonNull View itemView) {
        super(itemView);
        chartView = itemView.findViewById(R.id.widgetEnergyChart);
        btnRefresh = itemView.findViewById(R.id.btnRefreshChart);
        btnDeleteWidget = itemView.findViewById(R.id.btnDeleteWidget);
    }
    public void bind(Device device, DashboardAdapter.OnDeviceControlListener listener
            , boolean isEditMode, ItemTouchHelper touchHelper, WidgetAdapter adapter){
        // 1. 편집 모드에 따른 삭제 버튼 표시 처리
        btnDeleteWidget.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        // 4. 기존 정상 동작 로직
        if (device.getEnergyLogs() != null && !device.getEnergyLogs().isEmpty()) {
            chartView.setEnergyData(device.getEnergyLogs());
        }
        // 2. 삭제 버튼 클릭 이벤트
        btnDeleteWidget.setOnClickListener(v -> {
            int pos = getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                adapter.removeWidget(pos);
            }
        });

        // 3. 편집 모드일 때의 터치 및 UI 동작 제어
        if (isEditMode) {
            btnRefresh.setEnabled(false); // 드래그 중 오작동 방지
            itemView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    touchHelper.startDrag(this); // 즉시 드래그 시작
                    return true; // 터치 이벤트 소비
                }
                return false;
            });
        } else {
            btnRefresh.setEnabled(true);
            itemView.setOnTouchListener(null);


            btnRefresh.setOnClickListener(v -> {
                if (listener != null) listener.onRefreshChart(device.getId());
            });
        }
    }
}
