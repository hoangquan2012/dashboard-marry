package com.wedding.management.domain.enums;

public enum GuestCategory {
    FAMILY("Gia đình"),
    RELATIVE("Họ hàng"),
    FRIEND("Bạn bè"),
    COLLEAGUE("Đồng nghiệp"),
    NEIGHBOR("Hàng xóm"),
    PARENTS_GUEST("Khách của bố mẹ"),
    OTHER("Khác");

    private final String label;

    GuestCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static GuestCategory fromLabel(String text) {
        if (text == null) return null;
        String t = text.trim();
        for (GuestCategory c : values()) {
            if (c.name().equalsIgnoreCase(t) || c.label.equalsIgnoreCase(t)) return c;
        }
        return null;
    }
}
