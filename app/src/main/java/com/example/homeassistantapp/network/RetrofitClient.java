package com.example.homeassistantapp.network;

import com.example.homeassistantapp.api.HomeAssistantApi;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://192.168.219.109:8000/";
    private static Retrofit retrofit = null;

    public static HomeAssistantApi getApi() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(HomeAssistantApi.class);
    }
}
