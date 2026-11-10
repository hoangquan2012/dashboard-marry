package com.wedding.management.domain.entity;

import com.wedding.management.domain.enums.Side;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "wedding_tables")
public class WeddingTable extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private WeddingEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private TableArea area;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int capacity = 10;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Side side = Side.BOTH;

    @Column(name = "position_x", nullable = false)
    private int positionX = 0;

    @Column(name = "position_y", nullable = false)
    private int positionY = 0;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(columnDefinition = "text")
    private String note;
}
