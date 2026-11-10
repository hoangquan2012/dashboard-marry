package com.wedding.management.domain.enums;

public enum Gender {
    MALE("Nam"),
    FEMALE("Nữ"),
    OTHER("Khác");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Gender fromLabel(String text) {
        if (text == null || text.isBlank()) return null;
        String t = text.trim();
        for (Gender g : values()) {
            if (g.name().equalsIgnoreCase(t) || g.label.equalsIgnoreCase(t)) return g;
        }
        return null;
    }
}
