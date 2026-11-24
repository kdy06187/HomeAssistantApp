package com.example.homeassistantapp.api;

import android.service.controls.Control;

import com.example.homeassistantapp.dto.AddDeviceRequest;
import com.example.homeassistantapp.dto.CommissionJobResponse;
import com.example.homeassistantapp.dto.ControlRequest;
import com.example.homeassistantapp.dto.ControlResponse;
import com.example.homeassistantapp.dto.DeleteDeviceRequest;
import com.example.homeassistantapp.dto.DeleteDeviceResponse;
import com.example.homeassistantapp.dto.DeviceListResponse;
import com.example.homeassistantapp.dto.DeviceStateResponse;
import com.example.homeassistantapp.dto.EnergyLog;
import com.example.homeassistantapp.dto.EnergyLogResponse;
import com.example.homeassistantapp.dto.EnergyResponse;


import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface HomeAssistantApi {
    @GET("/api/devices")
    Call<DeviceListResponse> getDeviceList();
    @POST("/api/control")
    Call<ControlResponse> controlDevice(@Body ControlRequest request);
    @POST("/api/commission")
    Call<CommissionJobResponse> addDevice(@Body AddDeviceRequest request);

    @GET("/api/commission/{jobId}")
    Call<CommissionJobResponse> getCommissionJob(@Path("jobId") String jobId);
    @POST("/api/devices/delete")
    Call<DeleteDeviceResponse> deleteDevice(@Body DeleteDeviceRequest request);
    @GET("/api/devices/{deviceId}/state")
    Call<DeviceStateResponse> getDeviceState(@Path("deviceId") String deviceId
            , @Query("isManualRefresh") boolean isManualRefresh);
    @GET("/api/devices/{deviceId}/energy")
    Call<EnergyResponse> getDeviceEnergy(@Path("deviceId") String deviceId);
    @GET("/api/devices/{id}/energy/history")
    Call<EnergyLogResponse> getDeviceEnergyHistory(@Path("id") String deviceId);
}
