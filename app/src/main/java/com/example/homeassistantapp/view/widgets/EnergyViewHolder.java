package com.example.homeassistantapp.view.widgets;

import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.DashboardAdapter;
import com.example.homeassistantapp.view.WidgetAdapter;

public class EnergyViewHolder extends RecyclerView.ViewHolder {
    private TextView tvEnergy;
    private ImageView btnRefresh;
    private ImageButton btnDeleteWidget;
    public EnergyViewHolder(@NonNull View itemView) {
        super(itemView);
        tvEnergy = itemView.findViewById(R.id.tvWidgetEnergy);
        btnRefresh = itemView.findViewById(R.id.btnRefreshEnergyText);
        btnDeleteWidget = itemView.findViewById(R.id.btnDeleteWidget);
    }

    public void bind(Device device, DashboardAdapter.OnDeviceControlListener listener
            , boolean isEditMode, ItemTouchHelper touchHelper, WidgetAdapter adapter) {
        // 1. 편집 모드에 따른 삭제 버튼 표시 처리
        btnDeleteWidget.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

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
                    return true;
                }

                return false;
            });
        } else {
            btnRefresh.setEnabled(true);
            itemView.setOnTouchListener(null); // 편집 모드가 아닐 땐 터치 가로채기 해제

            // 4. 기존 정상 동작 로직
            tvEnergy.setText(device.getPowerInfo());
            btnRefresh.setOnClickListener(v -> {
                if (listener != null) listener.onRefreshEnergy(device.getId());
            });
        }
    }
}
