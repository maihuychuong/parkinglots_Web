package com.example.demo.model.enums;

public enum TransactionMethod {
    CASH("Tiền mặt"),
    BANK_TRANSFER("Chuyển khoản");

    private final String label;

    TransactionMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

