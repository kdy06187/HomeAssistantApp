package com.example.homeassistantapp.view.widgets;

import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.DashboardAdapter;
import com.example.homeassistantapp.R;
import com.example.homeassistantapp.view.WidgetAdapter;

public class ToggleViewHolder extends RecyclerView.ViewHolder{
    private SwitchCompat switchControl;
    private ImageButton btnDeleteWidget;

    public ToggleViewHolder(@NonNull View itemView) {
        super(itemView);
        switchControl = itemView.findViewById(R.id.switchWidgetControl);
        btnDeleteWidget = itemView.findViewById(R.id.btnDeleteWidget);

    }

    public void bind(Device device, DashboardAdapter.OnDeviceControlListener listener
            , boolean isEditMode, ItemTouchHelper touchHelper, WidgetAdapter adapter) {
        btnDeleteWidget.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        btnDeleteWidget.setOnClickListener(v -> {
            int pos = getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                adapter.removeWidget(pos);
            }
        });
        if (isEditMode) {
            itemView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    touchHelper.startDrag(this); // 터치하자마자 드래그 모드 진입
                    return true; // 터치 이벤트를 소모하여 스위치 조작 등을 막음
                }

                return false; // 터치 이벤트를 소모하여 스위치 조작 등을 막음
            });
        } else {
            // 편집 모드가 아니면 기존 스위치 조작 허용
            itemView.setOnTouchListener(null);

            switchControl.setChecked("ON".equalsIgnoreCase(device.getState()));
            switchControl.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed() && listener != null) {
                    listener.onToggle(device.getId(), isChecked);
                }
            });
        }
    }
}
