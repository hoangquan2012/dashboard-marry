package com.wedding.management.domain.enums;

public enum EventStatus {
    PLANNED("Dự kiến"),
    CONFIRMED("Đã chốt"),
    COMPLETED("Đã diễn ra"),
    CANCELLED("Đã hủy");

    private final String label;

    EventStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
