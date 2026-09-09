package com.example.homeassistantapp.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.dto.Device;
import com.example.homeassistantapp.R;
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
            holder.btnEditWidgets.setColorFilter(Color.parseColor("#FF9800"));
        } else {
            holder.btnEditWidgets.clearColorFilter();
        }
        holder.btnEditWidgets.setOnClickListener(v -> {
            if (editingDeviceIds.contains(device.getId())) {
                editingDeviceIds.remove(device.getId()); // 편집 모드 끄기
            } else {
                editingDeviceIds.add(device.getId()); // 편집 모드 켜기
            }
            notifyItemChanged(position); // 변경된 상태로 이 기기만 다시 그리기
        });

        holder.tvDeviceName.setText(device.getName());
        String status = device.getState();

        // UI 기본 상태 초기화
        holder.pbDeviceLoading.setVisibility(View.GONE);
        holder.btnRefreshStatus.setVisibility(View.VISIBLE);
        holder.btnRefreshStatus.setEnabled(true);
        holder.widgetContainer.setVisibility(View.VISIBLE);

        if ("TEMP_PAIRING".equals(device.getId()) || device.isPairing()) {
            // [페어링 중]
            holder.tvDeviceStatus.setText("페어링중");
            holder.tvDeviceStatus.setTextColor(Color.parseColor("#888888")); // 회색
            holder.widgetContainer.setVisibility(View.GONE); // 조작 위젯 숨김
            holder.itemView.setAlpha(0.5f);

        } else if ("LOADING".equals(status)) {
            holder.tvDeviceStatus.setTextColor(Color.parseColor("#888888"));
            holder.pbDeviceLoading.setVisibility(View.VISIBLE);
            holder.btnRefreshStatus.setEnabled(false); // 로딩 중 중복 클릭 방지
            holder.itemView.setAlpha(0.7f);
        } else if (device.getState().equals("OFFLINE")) {
            holder.tvDeviceStatus.setText("오프라인");
            holder.tvDeviceStatus.setTextColor(Color.parseColor("#F44336")); // 빨간색
            holder.widgetContainer.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(0.5f); // 비활성화된 느낌 부여

        } else {
            // [연결됨]
            holder.tvDeviceStatus.setText("연결됨");
            holder.tvDeviceStatus.setTextColor(Color.parseColor("#4CAF50")); // 녹색
            holder.widgetContainer.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(1.0f);
        }
        holder.btnRefreshStatus.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRefreshDevice(device.getId());
            }
        });

        List<String> activeWidgets = new ArrayList<>();

        // (나중에는 이 activeWidgets 리스트를 SharedPreferences에서 불러와서 세팅하게 됩니다)
        if (device.getType().equals("MATTER") || device.getType().equals("TCP_DIY")) {
            activeWidgets.add("TOGGLE");
        }
        if (device.getType().equals("MATTER")) {
            activeWidgets.add("ENERGY");
            activeWidgets.add("CHART");
        }

        SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences("DashboardPrefs", Context.MODE_PRIVATE);
        String savedOrder = prefs.getString(device.getId() + "_widget_order", "");

        if (!savedOrder.isEmpty()) {
            List<String> sortedWidgets = new ArrayList<>();
            String[] savedArray = savedOrder.split(",");

            // 저장된 순서대로 새 리스트에 넣기
            for (String type : savedArray) {
                if (activeWidgets.contains(type)) {
                    sortedWidgets.add(type);
                    activeWidgets.remove(type); // 이미 넣은 건 원본에서 제거
                }
            }
            // 혹시 앱 업데이트 등으로 새로 생긴 위젯이 있다면 맨 뒤에 마저 붙이기
            sortedWidgets.addAll(activeWidgets);

            activeWidgets = sortedWidgets; // 정렬된 리스트로 교체
        }

        // 위젯 어댑터에 리스트 전달 -> 자동으로 화면에 렌더링됨
        holder.widgetAdapter.setWidgets(activeWidgets, device, listener);


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
        TextView tvDeviceName,tvDeviceStatus;
        ProgressBar pbDeviceLoading;
        ImageButton btnRefreshStatus;
        ImageButton btnEditWidgets;
        RecyclerView widgetContainer;
        WidgetAdapter widgetAdapter;
        String currentDeviceId;

        public DashboardViewHolder(@NonNull View itemView) {
            super(itemView);
            // item_device_section.xml에 있는 뷰들의 ID와 연결
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
                    // 손가락으로 드래그 시 위젯 어댑터의 위치 변경 로직 호출
                    widgetAdapter.moveItem(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                    return true;
                }
                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    // 스와이프 삭제는 추후 편집 모드에서 구현
                }
                @Override
                public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                    super.onSelectedChanged(viewHolder, actionState);
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        widgetContainer.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }

                // 🌟 추가: 드래그 종료 시 바깥쪽 목록 스크롤 다시 켜기
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
