package com.wedding.management.domain.entity;

import com.wedding.management.domain.enums.GuestCategory;
import com.wedding.management.domain.enums.Side;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "guest_groups")
public class GuestGroup extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Side side = Side.BOTH;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private GuestCategory category;

    @Column(name = "representative_name", length = 150)
    private String representativeName;

    @Column(name = "representative_phone", length = 30)
    private String representativePhone;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
