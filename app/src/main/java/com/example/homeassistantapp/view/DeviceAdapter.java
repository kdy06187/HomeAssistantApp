package com.example.homeassistantapp.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.homeassistantapp.R;
import com.example.homeassistantapp.dto.Device;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>{
    private List<Device> deviceList = new ArrayList<>();
    private OnDeviceControlListener listener;

    public interface OnDeviceControlListener{
        void onControlClick(String deviceId, String command);
    }

    public interface OnDeviceLongClickListener{
        void onDeviceLongClick(Device device);
    }
    private OnDeviceLongClickListener longClickListener;

    public void setOnDeviceLongClickListener(OnDeviceLongClickListener listener){
        this.longClickListener = listener;
    }
    public DeviceAdapter(OnDeviceControlListener listener){
        this.listener = listener;
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
        holder.textDeviceName.setText(device.name);
        holder.textDeviceType.setText(device.type);

        holder.btnOn.setOnClickListener(v -> {
            if(listener != null){
                listener.onControlClick(device.id, "ON");
            }
        });
        holder.btnOff.setOnClickListener(v -> {
            if(listener != null){
                listener.onControlClick(device.id, "OFF");
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if(longClickListener != null){
                longClickListener.onDeviceLongClick(device);
            }
            return true;
        });
    }
    @Override
    public int getItemCount(){
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder{
        TextView textDeviceName, textDeviceType;
        Button btnOn, btnOff;

        public DeviceViewHolder(@NonNull View itemView){
            super(itemView);
            textDeviceName = itemView.findViewById(R.id.textDeviceName);
            textDeviceType = itemView.findViewById(R.id.textDeviceType);
            btnOn = itemView.findViewById(R.id.btnOn);
            btnOff = itemView.findViewById(R.id.btnOff);
        }
    }
}
