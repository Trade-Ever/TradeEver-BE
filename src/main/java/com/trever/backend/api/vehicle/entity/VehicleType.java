package com.trever.backend.api.vehicle.entity;

public enum VehicleType {
    LARGE("대형"),
    MID_SIZE("중형"),
    SEMI_MID_SIZE("준중형"),
    SMALL("소형"),
    SPORTS("스포츠"),
    SUV("SUV"),
    VAN("승합차"),
    COMPACT("경차");

    private final String displayName;

    VehicleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}