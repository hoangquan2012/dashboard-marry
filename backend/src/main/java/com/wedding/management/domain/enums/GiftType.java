package com.wedding.management.domain.enums;

public enum GiftType {
    CASH("Tiền mặt", true),
    BANK_TRANSFER("Chuyển khoản", true),
    GOLD("Vàng", false),
    IN_KIND("Hiện vật", false),
    OTHER("Khác", false);

    private final String label;
    private final boolean monetary;

    GiftType(String label, boolean monetary) {
        this.label = label;
        this.monetary = monetary;
    }

    public String label() {
        return label;
    }

    /** True for gift types where the amount is real money (cash / transfer). */
    public boolean isMonetary() {
        return monetary;
    }
}
