package com.example.demo.model.enums;

public enum ShiftType {
    MORNING("Ca sáng"),
    AFTERNOON("Ca chiều"),
    NIGHT("Ca tối");

    private final String label;

    ShiftType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

