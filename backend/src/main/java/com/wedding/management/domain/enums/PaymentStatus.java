package com.wedding.management.domain.enums;

public enum PaymentStatus {
    UNPAID("Chưa thanh toán"),
    DEPOSITED("Đã đặt cọc"),
    PARTIAL("Thanh toán một phần"),
    PAID("Đã thanh toán");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
