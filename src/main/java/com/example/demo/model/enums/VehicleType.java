package com.example.demo.model.enums;

public enum VehicleType {
    CAR_UNDER_9("Xe dưới 9 chỗ"),
    CAR_9_TO_16("Xe từ 9 đến 16 chỗ");

    private final String label;

    VehicleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
