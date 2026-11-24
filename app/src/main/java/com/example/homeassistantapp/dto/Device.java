package com.example.homeassistantapp.dto;

import com.google.gson.annotations.SerializedName;

public class Device {
    @SerializedName("deviceId")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("protocolType")
    public String type;
    // TCP_DIY는 마지막 접속 IP, Matter는 null
    @SerializedName("ip")
    public String ip;
    @SerializedName("state")
    public String state;

    public boolean isPairing;

    // 화면용 값. 서버 목록에는 없고 E6 응답으로 채운다(transient: JSON에서 빠짐).
    private transient Double activePowerW;
    private transient Double totalEnergyKwh;

    public Double getActivePowerW() {
        return activePowerW;
    }

    public Double getTotalEnergyKwh() {
        return totalEnergyKwh;
    }

    // 보낸 제어 명령("ON"/"OFF"). 응답이 올 때까지 스위치를 이 값에 두고 잠근다.
    private transient String pendingCommand;

    public String getPendingCommand() {
        return pendingCommand;
    }

    public void setPendingCommand(String pendingCommand) {
        this.pendingCommand = pendingCommand;
    }

    public void setEnergy(Double activePowerW, Double totalEnergyKwh) {
        this.activePowerW = activePowerW;
        this.totalEnergyKwh = totalEnergyKwh;
    }

    public boolean isPairing() {
        return isPairing;
    }

    public void setPairing(boolean pairing) {
        isPairing = pairing;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }
}
