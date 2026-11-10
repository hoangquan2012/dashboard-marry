package com.wedding.management.domain.entity;

import com.wedding.management.domain.enums.ContractStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vendors")
public class Vendor extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "service_type", length = 100)
    private String serviceType;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column
    private String address;

    @Column(name = "service_description", columnDefinition = "text")
    private String serviceDescription;

    @Column(name = "agreed_price", precision = 18, scale = 0)
    private BigDecimal agreedPrice;

    @Column(name = "deposit_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_status", nullable = false, length = 20)
    private ContractStatus contractStatus = ContractStatus.NEGOTIATING;

    @Column(name = "contract_date")
    private LocalDate contractDate;

    @Column
    private Integer rating;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
