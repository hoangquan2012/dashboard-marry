package com.wedding.management.domain.enums;

public enum PaymentType {
    DEPOSIT("Đặt cọc"),
    PARTIAL("Thanh toán một phần"),
    FINAL("Thanh toán nốt");

    private final String label;

    PaymentType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
