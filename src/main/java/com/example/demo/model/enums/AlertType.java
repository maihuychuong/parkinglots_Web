package com.example.demo.model.enums;

public enum AlertType {
    MISSING_EXIT("Xe chưa ghi nhận ra bãi"),
    OVERDUE("Xe đỗ quá thời gian cho phép"),
    PAYMENT_ERROR("Lỗi thanh toán");

    private final String label;

    AlertType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

