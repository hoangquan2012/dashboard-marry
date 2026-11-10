package com.wedding.management.domain.entity;

import com.wedding.management.domain.enums.EventStatus;
import com.wedding.management.domain.enums.Side;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "wedding_events")
public class WeddingEvent extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column
    private String venue;

    @Column
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Side side = Side.BOTH;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 18, scale = 0)
    private BigDecimal budget = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.PLANNED;

    @Column(name = "is_main", nullable = false)
    private boolean main = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
