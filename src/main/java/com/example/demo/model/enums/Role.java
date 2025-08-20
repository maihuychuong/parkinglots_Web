package com.example.demo.model.enums;

public enum Role {
    ADMIN("Quản trị viên"),
    STAFF("Nhân viên");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

