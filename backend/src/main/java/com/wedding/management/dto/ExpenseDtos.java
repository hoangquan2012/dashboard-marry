package com.wedding.management.dto;

import com.wedding.management.domain.enums.PaymentMethod;
import com.wedding.management.domain.enums.PaymentStatus;
import com.wedding.management.domain.enums.PaymentType;
import com.wedding.management.domain.enums.Side;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ExpenseDtos {
    private ExpenseDtos() {
    }

    public record CategoryRequest(
            @NotBlank(message = "Vui lòng nhập tên danh mục") @Size(max = 150) String name,
            @Size(max = 50) String icon,
            @DecimalMin(value = "0", message = "Ngân sách không được âm") BigDecimal budgetAmount,
            Integer sortOrder
    ) {
    }

    public record CategoryResponse(Long id, String name, String icon, BigDecimal budgetAmount, int sortOrder, boolean system,
                                   long expenseCount, BigDecimal estimatedTotal, BigDecimal actualTotal, BigDecimal paidTotal,
                                   boolean overBudget) {
    }

    public record ExpenseRequest(
            @NotBlank(message = "Vui lòng nhập tên khoản chi") @Size(max = 200) String name,
            @NotNull(message = "Vui lòng chọn danh mục") Long categoryId,
            Long vendorId,
            Long eventId,
            Side side,
            @DecimalMin(value = "0", message = "Số tiền dự kiến không được âm") BigDecimal estimatedAmount,
            @DecimalMin(value = "0", message = "Số tiền thực tế không được âm") BigDecimal actualAmount,
            LocalDate expenseDate,
            LocalDate dueDate,
            @Size(max = 150) String paidBy,
            @Size(max = 2000) String note,
            @DecimalMin(value = "0", message = "Số tiền đã trả không được âm") BigDecimal paidAmount
    ) {
    }

    public record PaymentRequest(
            @NotNull(message = "Vui lòng nhập số tiền") @DecimalMin(value = "1", message = "Số tiền phải lớn hơn 0") BigDecimal amount,
            @NotNull(message = "Vui lòng chọn ngày thanh toán") LocalDate paymentDate,
            PaymentType paymentType,
            PaymentMethod method,
            @Size(max = 150) String paidBy,
            @Size(max = 1000) String note
    ) {
    }

    public record PaymentResponse(Long id, Long expenseId, BigDecimal amount, LocalDate paymentDate, PaymentType paymentType,
                                  String paymentTypeLabel, PaymentMethod method, String methodLabel, String paidBy, String note,
                                  Instant createdAt) {
    }

    public record ExpenseResponse(
            Long id,
            String name,
            Long categoryId,
            String categoryName,
            String categoryIcon,
            Long vendorId,
            String vendorName,
            Long eventId,
            String eventName,
            Side side,
            BigDecimal estimatedAmount,
            BigDecimal actualAmount,
            BigDecimal paidAmount,
            BigDecimal depositAmount,
            BigDecimal remainingAmount,
            PaymentStatus paymentStatus,
            String paymentStatusLabel,
            boolean overBudget,
            BigDecimal overBudgetAmount,
            LocalDate expenseDate,
            LocalDate dueDate,
            LocalDate lastPaymentDate,
            boolean overdue,
            String paidBy,
            String note,
            long attachmentCount,
            List<PaymentResponse> payments,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CategoryStat(Long categoryId, String categoryName, String icon, long count, BigDecimal budget,
                               BigDecimal estimated, BigDecimal actual, BigDecimal paid, BigDecimal remaining,
                               double percentOfTotal, boolean overBudget) {
    }

    public record SideStat(Side side, String label, long count, BigDecimal estimated, BigDecimal actual, BigDecimal paid) {
    }

    public record VendorStat(Long vendorId, String vendorName, long count, BigDecimal actual, BigDecimal paid, BigDecimal remaining) {
    }

    public record StatusStat(PaymentStatus status, String label, long count, BigDecimal actual) {
    }

    public record ExpenseStats(
            BigDecimal totalBudget,
            BigDecimal categoryBudgetTotal,
            BigDecimal estimatedTotal,
            BigDecimal actualTotal,
            BigDecimal paidTotal,
            BigDecimal remainingTotal,
            BigDecimal depositTotal,
            long expenseCount,
            long unpaidCount,
            long overBudgetCount,
            BigDecimal overBudgetAmount,
            CategoryStat largestCategory,
            List<CategoryStat> byCategory,
            List<SideStat> bySide,
            List<VendorStat> byVendor,
            List<StatusStat> byStatus,
            List<ExpenseResponse> overBudgetExpenses
    ) {
    }

    public record DebtItem(Long expenseId, String expenseName, String categoryName, Long vendorId, String vendorName,
                           BigDecimal actualAmount, BigDecimal paidAmount, BigDecimal remainingAmount, LocalDate dueDate,
                           PaymentStatus status, boolean overdue, Long daysUntilDue) {
    }

    public record DebtSummary(BigDecimal totalRemaining, long count, long overdueCount, BigDecimal overdueAmount,
                              long dueSoonCount, List<DebtItem> items) {
    }
}
