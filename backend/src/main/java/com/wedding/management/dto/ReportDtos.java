package com.wedding.management.dto;

import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.ExpenseDtos.CategoryStat;
import com.wedding.management.dto.ExpenseDtos.ExpenseResponse;
import com.wedding.management.dto.ExpenseDtos.SideStat;
import com.wedding.management.dto.ExpenseDtos.StatusStat;
import com.wedding.management.dto.ExpenseDtos.VendorStat;
import com.wedding.management.dto.GiftDtos.EventStat;
import com.wedding.management.dto.GiftDtos.GiftResponse;
import com.wedding.management.dto.GiftDtos.TypeStat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ReportDtos {
    private ReportDtos() {
    }

    public record ReportFilter(Side side, Long eventId, LocalDate from, LocalDate to) {
    }

    public record ReportHeader(String groomName, String brideName, LocalDate weddingDate, LocalDate generatedAt,
                               String sideLabel, String eventName, LocalDate from, LocalDate to) {
    }

    public record CountRow(String key, String label, long guests, int invitedPeople, int confirmedPeople, int actualPeople, double attendanceRate) {
    }

    public record GuestReport(
            ReportHeader header,
            long totalGuests,
            long groomGuests,
            long brideGuests,
            int invitedPeople,
            int expectedPeople,
            int confirmedPeople,
            int actualPeople,
            long attendedGuests,
            long declinedGuests,
            long undecidedGuests,
            double attendanceRatePeople,
            double attendanceRateGuests,
            List<CountRow> bySide,
            List<CountRow> byCategory,
            List<CountRow> byStatus,
            List<CountRow> byEvent
    ) {
    }

    public record GiftSideRow(Side side, String label, long count, long givers, BigDecimal amount, double percent) {
    }

    public record GiftReport(
            ReportHeader header,
            BigDecimal totalAmount,
            long giftCount,
            long giverCount,
            BigDecimal cashAmount,
            BigDecimal transferAmount,
            BigDecimal inKindValue,
            long inKindCount,
            BigDecimal averagePerGiver,
            List<GiftSideRow> bySide,
            List<TypeStat> byType,
            List<EventStat> byEvent,
            List<GiftResponse> details
    ) {
    }

    public record ExpenseReport(
            ReportHeader header,
            BigDecimal totalBudget,
            BigDecimal estimatedTotal,
            BigDecimal actualTotal,
            BigDecimal paidTotal,
            BigDecimal remainingTotal,
            long expenseCount,
            long overBudgetCount,
            BigDecimal overBudgetAmount,
            List<CategoryStat> byCategory,
            List<SideStat> bySide,
            List<VendorStat> byVendor,
            List<StatusStat> byStatus,
            List<ExpenseResponse> overBudget,
            List<ExpenseResponse> unpaid,
            List<ExpenseResponse> details
    ) {
    }

    public record FinancialReport(
            ReportHeader header,
            BigDecimal totalBudget,
            BigDecimal giftTotal,
            BigDecimal otherIncomeTotal,
            BigDecimal totalIncome,
            BigDecimal expenseEstimated,
            BigDecimal expenseActual,
            BigDecimal expensePaid,
            BigDecimal expenseRemaining,
            BigDecimal balance,
            BigDecimal projectedBalance,
            BigDecimal budgetRemaining,
            long actualAttendees,
            long giverCount,
            BigDecimal costPerAttendee,
            BigDecimal giftPerGiver,
            BigDecimal cashIn,
            BigDecimal transferIn,
            BigDecimal cashOut,
            BigDecimal transferOut,
            List<SideFinanceRow> bySide,
            List<BudgetDtos.OtherIncomeResponse> otherIncomes
    ) {
    }

    public record SideFinanceRow(Side side, String label, long guests, int actualPeople, BigDecimal giftTotal, BigDecimal otherIncome,
                                 BigDecimal expenseActual, BigDecimal balance) {
    }
}
