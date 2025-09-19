package com.trever.backend.api.vehicle.entity;

public enum VehicleStatus {
    ACTIVE("판매중"),
    IN_PROGRESS("거래중"),
    ENDED("판매완료");

    private final String displayName;

    VehicleStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}