package com.wedding.management.domain.enums;

public enum ContractStatus {
    NEGOTIATING("Đang thương lượng"),
    CONTRACTED("Đã ký hợp đồng"),
    DEPOSITED("Đã đặt cọc"),
    COMPLETED("Đã hoàn thành"),
    CANCELLED("Đã hủy");

    private final String label;

    ContractStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
