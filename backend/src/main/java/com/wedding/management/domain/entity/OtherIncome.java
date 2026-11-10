package com.wedding.management.domain.entity;

import com.wedding.management.domain.enums.PaymentMethod;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "other_incomes")
public class OtherIncome extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Side side = Side.BOTH;

    @Column(nullable = false, precision = 18, scale = 0)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "received_at", nullable = false)
    private LocalDate receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method = PaymentMethod.CASH;

    @Column(columnDefinition = "text")
    private String note;
}
