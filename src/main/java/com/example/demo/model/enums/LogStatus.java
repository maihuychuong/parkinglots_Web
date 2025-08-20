package com.example.demo.model.enums;

public enum LogStatus {
    IN_PROGRESS("Đang xử lý"),
    COMPLETED("Đã hoàn tất");

    private final String label;

    LogStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

