package com.wedding.management.domain.enums;

public enum PaymentMethod {
    CASH("Tiền mặt"),
    BANK_TRANSFER("Chuyển khoản"),
    OTHER("Khác");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
