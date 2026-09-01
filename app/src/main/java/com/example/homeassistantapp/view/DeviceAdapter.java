package com.example.homeassistantapp.view;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.activity.DeviceDetailActivity;
import com.example.homeassistantapp.dto.Device;

import org.jspecify.annotations.NonNull;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>{
    private List<Device> deviceList = new ArrayList<>();
//    private OnDeviceControlListener listener;

//    public interface OnDeviceControlListener{
//        void onControlClick(String deviceId, String command);
//        void onEnergyClick(String deviceId);
//    }

    public interface OnDeviceLongClickListener{
        void onDeviceLongClick(Device device);
    }
    private OnDeviceLongClickListener longClickListener;

    public void setOnDeviceLongClickListener(OnDeviceLongClickListener listener){
        this.longClickListener = listener;
    }

    public void setDevices(List<Device> devices){
        this.deviceList = devices;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder,int position){
        Device device = deviceList.get(position);
        holder.tvDeviceName.setText(device.getName());
        holder.tvDeviceType.setText(device.getType());

        boolean isPairing = "TEMP_PAIRING".equals(device.getId()) || device.isPairing();

        if (isPairing) {
            // [페어링 중 상태 UI]
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.tvDeviceType.setVisibility(View.GONE); // 타입 텍스트 대신 로딩바 노출 (기획에 따라 유지 가능)
            holder.itemView.setAlpha(0.5f); // 전체를 약간 투명하게 처리

            // 임시 기기이므로 클릭 이벤트 차단
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
        } else {
            // [정상 기기 상태 UI]
            holder.progressBar.setVisibility(View.GONE);
            holder.tvDeviceType.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(1.0f);

            // 정상 기기 클릭 이벤트 활성화
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), DeviceDetailActivity.class);
                intent.putExtra("EXTRA_DEVICE_ID", device.getId());
                intent.putExtra("EXTRA_DEVICE_NAME", device.getName());
                intent.putExtra("EXTRA_DEVICE_TYPE", device.getType());
                v.getContext().startActivity(intent);
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onDeviceLongClick(device);
                }
                return true;
            });
        }
//        if (device.getPowerInfo() != null) {
//            holder.textPowerInfo.setText(device.getPowerInfo());
//        } else {
//            holder.textPowerInfo.setText("전력 정보 없음");
//        }
//        if (device.getEnergyLogs() != null && !device.getEnergyLogs().isEmpty()) {
//            holder.chartView.setVisibility(View.VISIBLE); // 차트 보이기
//            holder.chartView.setEnergyData(device.getEnergyLogs()); // 커스텀 뷰에 데이터 쏙!
//        } else {
//            holder.chartView.setVisibility(View.GONE); // 아직 조회 안 했으면 숨기기
//        }
//        holder.btnOn.setOnClickListener(v -> {
//            if(listener != null){
//                listener.onControlClick(device.id, "ON");
//            }
//        });
//        holder.btnOff.setOnClickListener(v -> {
//            if(listener != null){
//                listener.onControlClick(device.id, "OFF");
//            }
//        });

//        holder.btnEnergy.setOnClickListener(v -> {
//            if(listener != null){
//                listener.onEnergyClick(device.id); // 메인 액티비티로 이벤트 전달
//            }
//        });

    }
    @Override
    public int getItemCount(){
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder{
        TextView tvDeviceName;
        TextView tvDeviceType;
        ProgressBar progressBar;
        public DeviceViewHolder(@NonNull View itemView){
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceType = itemView.findViewById(R.id.tvDeviceType);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}
