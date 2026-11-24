package com.example.homeassistantapp.view;

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
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.tvDeviceType.setVisibility(View.GONE);
            holder.itemView.setAlpha(0.5f);

            // 등록 중인 임시 항목은 누를 수 없게 한다.
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.tvDeviceType.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(1.0f);

            holder.itemView.setOnClickListener(v -> {
                v.getContext().startActivity(DeviceDetailActivity.intentFor(v.getContext(), device));
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onDeviceLongClick(device);
                }
                return true;
            });
        }


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
