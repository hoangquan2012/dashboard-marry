package com.wedding.management.domain.entity;

import com.wedding.management.domain.enums.InvitationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invitations")
public class Invitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private WeddingEvent event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.NOT_SENT;

    @Column(name = "invited_count", nullable = false)
    private int invitedCount = 1;

    @Column(name = "expected_count", nullable = false)
    private int expectedCount = 1;

    @Column(name = "confirmed_count", nullable = false)
    private int confirmedCount = 0;

    @Column(name = "actual_count", nullable = false)
    private int actualCount = 0;

    @Column(name = "qr_token", nullable = false, unique = true, length = 64)
    private String qrToken = UUID.randomUUID().toString();

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "checked_in_by", length = 100)
    private String checkedInBy;

    @Column(columnDefinition = "text")
    private String note;

    public boolean isCheckedIn() {
        return checkedInAt != null;
    }
}
