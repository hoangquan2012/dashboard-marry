package com.wedding.management.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Single-row table (id = 1) holding global wedding information.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "wedding_settings")
public class WeddingSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "groom_name", nullable = false, length = 150)
    private String groomName = "Hoàng Quân";

    @Column(name = "bride_name", nullable = false, length = 150)
    private String brideName = "Hoàng Thị Diệu Anh";

    @Column(name = "wedding_date")
    private LocalDate weddingDate = LocalDate.of(2026, 12, 19);

    @Column(name = "total_budget", nullable = false, precision = 18, scale = 0)
    private BigDecimal totalBudget = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "main_venue")
    private String mainVenue;

    @Column(name = "groom_family_info", columnDefinition = "text")
    private String groomFamilyInfo;

    @Column(name = "bride_family_info", columnDefinition = "text")
    private String brideFamilyInfo;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "demo_seeded", nullable = false)
    private boolean demoSeeded = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
