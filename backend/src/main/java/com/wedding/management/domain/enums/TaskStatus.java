package com.wedding.management.domain.enums;

public enum TaskStatus {
    TODO("Chưa thực hiện"),
    IN_PROGRESS("Đang thực hiện"),
    DONE("Hoàn thành");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
