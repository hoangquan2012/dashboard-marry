package com.wedding.management.dto;

import com.wedding.management.domain.enums.PaymentMethod;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.ExpenseDtos.CategoryStat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class BudgetDtos {
    private BudgetDtos() {
    }

    public record OtherIncomeRequest(
            @NotBlank(message = "Vui lòng nhập nguồn thu") @Size(max = 200) String source,
            Side side,
            @NotNull(message = "Vui lòng nhập số tiền") @DecimalMin(value = "0", message = "Số tiền không được âm") BigDecimal amount,
            @NotNull(message = "Vui lòng chọn ngày nhận") LocalDate receivedAt,
            PaymentMethod method,
            @Size(max = 2000) String note
    ) {
    }

    public record OtherIncomeResponse(Long id, String source, Side side, BigDecimal amount, LocalDate receivedAt,
                                      PaymentMethod method, String note, Instant createdAt) {
    }

    public record BudgetAlert(String type, String severity, String message, Long referenceId, BigDecimal amount) {
    }

    public record SideFinance(Side side, String label, BigDecimal giftTotal, BigDecimal otherIncome, BigDecimal expenseActual,
                              BigDecimal expensePaid, BigDecimal balance) {
    }

    public record BudgetOverview(
            BigDecimal totalBudget,
            BigDecimal categoryBudgetTotal,
            BigDecimal unallocatedBudget,
            BigDecimal estimatedTotal,
            BigDecimal actualTotal,
            BigDecimal paidTotal,
            BigDecimal remainingToPay,
            BigDecimal depositTotal,
            BigDecimal budgetRemaining,
            double budgetUsedPercent,
            BigDecimal giftTotal,
            BigDecimal otherIncomeTotal,
            BigDecimal totalIncome,
            BigDecimal projectedBalance,
            BigDecimal actualBalance,
            BigDecimal cashBalance,
            BigDecimal cashIncome,
            BigDecimal transferIncome,
            BigDecimal cashExpense,
            BigDecimal transferExpense,
            List<CategoryStat> categories,
            List<SideFinance> bySide,
            List<BudgetAlert> alerts
    ) {
    }
}
