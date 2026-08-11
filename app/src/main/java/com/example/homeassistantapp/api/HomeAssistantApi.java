package com.example.homeassistantapp.api;

import com.example.homeassistantapp.dto.ControlRequest;
import com.example.homeassistantapp.dto.ControlResponse;
import com.example.homeassistantapp.dto.DeviceListResponse;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface HomeAssistantApi {
    @GET("/api/devices")
    Call<DeviceListResponse> getDeviceList(); // <DeviceListResponse>
    @POST("/api/control")
    Call<ControlResponse> controlDevice(@Body ControlRequest request); // <ControlResponse>
}
