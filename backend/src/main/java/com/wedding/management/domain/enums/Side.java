package com.wedding.management.domain.enums;

public enum Side {
    GROOM("Nhà trai"),
    BRIDE("Nhà gái"),
    BOTH("Chung");

    private final String label;

    Side(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
