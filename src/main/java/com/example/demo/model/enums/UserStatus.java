package com.example.demo.model.enums;

public enum UserStatus {
    ACTIVE("Hoạt động"),
    INACTIVE("Không hoạt động"),
    BANNED("Bị khóa");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

