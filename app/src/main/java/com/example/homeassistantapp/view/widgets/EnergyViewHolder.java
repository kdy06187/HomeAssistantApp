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
        btnDeleteWidget.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        btnDeleteWidget.setOnClickListener(v -> {
            int pos = getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                adapter.removeWidget(pos);
            }
        });

        if (isEditMode) {
            btnRefresh.setEnabled(false);
            itemView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    touchHelper.startDrag(this);
                    return true;
                }

                return false;
            });
        } else {
            btnRefresh.setEnabled(true);
            itemView.setOnTouchListener(null);

            tvEnergy.setText(device.getPowerInfo());
            btnRefresh.setOnClickListener(v -> {
                if (listener != null) listener.onRefreshEnergy(device.getId());
            });
        }
    }
}
