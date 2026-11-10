package com.wedding.management.domain.enums;

/**
 * Lifecycle of an invitation (per guest, per event).
 * The {@code rank} is used to derive a single guest-level status when a guest is invited to several events.
 */
public enum InvitationStatus {
    NOT_SENT("Chưa gửi lời mời", 0),
    SENT("Đã gửi lời mời", 1),
    PENDING("Chưa xác nhận", 2),
    DECLINED("Từ chối tham dự", 3),
    ABSENT("Vắng mặt", 4),
    CONFIRMED("Đã xác nhận tham dự", 5),
    ATTENDED("Đã tham dự", 6);

    private final String label;
    private final int rank;

    InvitationStatus(String label, int rank) {
        this.label = label;
        this.rank = rank;
    }

    public String label() {
        return label;
    }

    public int rank() {
        return rank;
    }

    /** Statuses that mean the guest has not decided yet. */
    public boolean isUndecided() {
        return this == NOT_SENT || this == SENT || this == PENDING;
    }

    public boolean isPositive() {
        return this == CONFIRMED || this == ATTENDED;
    }
}
