package com.example.demo.model.enums;

public enum TransactionStatus {
    PAID("Đã thanh toán"),
    PENDING("Chờ thanh toán"),
    FAILED("Thất bại");

    private final String label;

    TransactionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

