package com.wedding.management.domain.entity;

import com.wedding.management.domain.enums.GiftType;
import com.wedding.management.domain.enums.Side;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "gifts")
public class Gift extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id")
    private Guest guest;

    /** Name of the giver when they are not part of the guest list. */
    @Column(name = "giver_name", length = 150)
    private String giverName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Side side;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private WeddingEvent event;

    @Enumerated(EnumType.STRING)
    @Column(name = "gift_type", nullable = false, length = 20)
    private GiftType giftType = GiftType.CASH;

    /** Cash amount, or the optional converted value for gold / in-kind gifts (null when unknown). */
    @Column(precision = 18, scale = 0)
    private BigDecimal amount;

    @Column(name = "received_at", nullable = false)
    private LocalDate receivedAt;

    @Column(name = "recorded_by", length = 100)
    private String recordedBy;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "gift", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<GiftItem> items = new ArrayList<>();

    public String displayGiverName() {
        if (guest != null) return guest.getFullName();
        return giverName;
    }
}
