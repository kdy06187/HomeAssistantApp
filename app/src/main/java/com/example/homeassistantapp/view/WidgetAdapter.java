package com.example.homeassistantapp.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.view.widgets.ChartViewHolder;
import com.example.homeassistantapp.view.widgets.EnergyViewHolder;
import com.example.homeassistantapp.view.widgets.ToggleViewHolder;

import java.util.ArrayList;
import java.util.List;

public class WidgetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<String> widgetTypes = new ArrayList<>();
    private Device device;
    private DashboardAdapter.OnDeviceControlListener listener;

    private boolean isEditMode = false;
    private ItemTouchHelper touchHelper;

    private static final int TYPE_TOGGLE = 0;
    private static final int TYPE_ENERGY = 1;
    private static final int TYPE_CHART = 2;
    public List<String> getWidgetTypes() {
        return widgetTypes;
    }
    public void setEditMode(boolean editMode) {
        if (this.isEditMode != editMode) {
            this.isEditMode = editMode;
            notifyDataSetChanged();
        }
    }

    public void setTouchHelper(ItemTouchHelper touchHelper) {
        this.touchHelper = touchHelper;
    }

    // 🌟 위젯 삭제 메서드
    public void removeWidget(int position) {
        widgetTypes.remove(position);
        notifyItemRemoved(position);
    }

    public void setWidgets(List<String> widgets, Device device, DashboardAdapter.OnDeviceControlListener listener) {
        this.device = device;
        this.listener = listener;
        // 1. 위젯이 한 번도 세팅되지 않은 최초 상태
        if (this.widgetTypes.isEmpty()) {
            this.widgetTypes = new ArrayList<>(widgets);
            notifyDataSetChanged();
            return;
        }

        // 2. 편집 모드일 때는 백그라운드 새로고침으로 인한 화면 깜빡임과 터치 초기화를 완전히 차단!
        if (isEditMode) {
            return;
        }

        // 3. 평상시 상태 갱신
        if (!this.widgetTypes.equals(widgets)) {
            this.widgetTypes = new ArrayList<>(widgets);
            notifyDataSetChanged();
        } else {
            notifyItemRangeChanged(0, this.widgetTypes.size());
        }
    }
    public void moveItem(int from, int to) {
        String moved = widgetTypes.remove(from);
        widgetTypes.add(to, moved);
        notifyItemMoved(from, to);
    }
    @Override
    public int getItemViewType(int position) {
        switch (widgetTypes.get(position)) {
            case "TOGGLE": return TYPE_TOGGLE;
            case "ENERGY": return TYPE_ENERGY;
            case "CHART": return TYPE_CHART;
            default: return -1;
        }
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_TOGGLE) {
            return new ToggleViewHolder(inflater.inflate(R.layout.widget_toggle, parent, false));
        } else if (viewType == TYPE_ENERGY) {
            return new EnergyViewHolder(inflater.inflate(R.layout.widget_energy_text, parent, false));
        } else {
            return new ChartViewHolder(inflater.inflate(R.layout.widget_chart, parent, false));
        }
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ToggleViewHolder) {
            ((ToggleViewHolder) holder).bind(device, listener, isEditMode, touchHelper, this);
        } else if (holder instanceof EnergyViewHolder) {
            ((EnergyViewHolder) holder).bind(device, listener, isEditMode, touchHelper, this);
        } else if (holder instanceof ChartViewHolder) {
            ((ChartViewHolder) holder).bind(device, listener, isEditMode, touchHelper, this);
        }
    }

    @Override
    public int getItemCount() {
        return widgetTypes.size();
    }
}
