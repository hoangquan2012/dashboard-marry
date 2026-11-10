package com.wedding.management.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "expense_categories")
public class ExpenseCategory extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(length = 50)
    private String icon;

    @Column(name = "budget_amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal budgetAmount = BigDecimal.ZERO;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "is_system", nullable = false)
    private boolean system = false;
}
