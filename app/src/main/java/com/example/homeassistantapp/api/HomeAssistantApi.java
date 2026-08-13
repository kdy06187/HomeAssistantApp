package com.example.homeassistantapp.api;

import android.service.controls.Control;

import com.example.homeassistantapp.dto.AddDeviceRequest;
import com.example.homeassistantapp.dto.AddDeviceResponse;
import com.example.homeassistantapp.dto.ControlRequest;
import com.example.homeassistantapp.dto.ControlResponse;
import com.example.homeassistantapp.dto.DeleteDeviceRequest;
import com.example.homeassistantapp.dto.DeviceListResponse;
import com.example.homeassistantapp.dto.DeviceStateResponse;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface HomeAssistantApi {
    @GET("/api/devices")
    Call<DeviceListResponse> getDeviceList(); // <DeviceListResponse>
    @POST("/api/control")
    Call<ControlResponse> controlDevice(@Body ControlRequest request); // <ControlResponse>
    @POST("/api/commission")
    Call<AddDeviceResponse> addDevice(@Body AddDeviceRequest request);
    @POST("/api/devices/delete")
    Call<ControlResponse> deleteDevice(@Body DeleteDeviceRequest request);
    @GET("/api/devices/{deviceId}/state")
    Call<DeviceStateResponse> getDeviceState(@Path("deviceId") String deviceId);
}
