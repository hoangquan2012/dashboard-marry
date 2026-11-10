package com.wedding.management.domain.entity;

import com.wedding.management.domain.enums.PaymentStatus;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "expenses")
public class Expense extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ExpenseCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private WeddingEvent event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Side side = Side.BOTH;

    @Column(name = "estimated_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal estimatedAmount = BigDecimal.ZERO;

    @Column(name = "actual_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal actualAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "deposit_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "expense_date")
    private LocalDate expenseDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "paid_by", length = 150)
    private String paidBy;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public BigDecimal getRemainingAmount() {
        BigDecimal remaining = actualAmount.subtract(paidAmount);
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    public boolean isOverBudget() {
        return estimatedAmount.signum() > 0 && actualAmount.compareTo(estimatedAmount) > 0;
    }
}
