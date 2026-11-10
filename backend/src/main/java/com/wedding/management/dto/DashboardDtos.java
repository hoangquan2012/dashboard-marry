package com.wedding.management.dto;

import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.ExpenseDtos.ExpenseResponse;
import com.wedding.management.dto.GuestDtos.GuestSummary;
import com.wedding.management.dto.TaskDtos.TaskResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {
    }

    public record KeyFigures(
            long totalGuests,
            long groomGuests,
            long brideGuests,
            long bothGuests,
            long totalInvitations,
            int invitedPeople,
            int expectedPeople,
            int confirmedPeople,
            int actualPeople,
            long confirmedGuests,
            long declinedGuests,
            long undecidedGuests,
            long attendedGuests,
            long checkedInGuests,
            BigDecimal giftTotal,
            BigDecimal giftGroom,
            BigDecimal giftBride,
            long giftCount,
            long giverCount,
            BigDecimal otherIncomeTotal,
            BigDecimal totalIncome,
            BigDecimal expenseEstimated,
            BigDecimal expenseActual,
            BigDecimal expensePaid,
            BigDecimal expenseRemaining,
            BigDecimal totalBudget,
            BigDecimal budgetRemaining,
            BigDecimal balance,
            BigDecimal averageGiftPerGiver,
            BigDecimal averageGiftPerGroup,
            BigDecimal averageCostPerAttendee,
            long tasksTotal,
            long tasksDone,
            long tasksOverdue,
            double taskCompletionPercent,
            Long daysUntilWedding
    ) {
    }

    public record LabelValue(String key, String label, long count, BigDecimal amount) {
    }

    public record StatusSlice(InvitationStatus status, String label, long guests, int people) {
    }

    public record CategorySlice(Long categoryId, String label, BigDecimal budget, BigDecimal estimated, BigDecimal actual, double percent) {
    }

    public record TimePoint(String period, LocalDate from, BigDecimal income, BigDecimal giftIncome, BigDecimal expensePaid,
                            BigDecimal cumulativeIncome, BigDecimal cumulativeExpense) {
    }

    public record Charts(
            List<LabelValue> guestsBySide,
            List<StatusSlice> guestsByStatus,
            List<LabelValue> giftsBySide,
            List<LabelValue> giftsByType,
            List<CategorySlice> expensesByCategory,
            List<CategorySlice> budgetVsActual,
            List<TimePoint> cashflow,
            List<LabelValue> guestsByCategory
    ) {
    }

    public record PendingGuest(Long guestId, String code, String fullName, String phone, Side side, String groupName,
                               InvitationStatus status, String statusLabel, String eventName, LocalDate eventDate, Long daysSinceSent) {
    }

    public record Lists(
            List<ExpenseResponse> topExpenses,
            List<ExpenseResponse> unpaidExpenses,
            List<PendingGuest> pendingGuests,
            long pendingGuestCount,
            List<PendingGuest> guestsToContact,
            long guestsToContactCount,
            List<TaskResponse> upcomingTasks,
            List<GuestSummary> unassignedGuests,
            long unassignedGuestCount
    ) {
    }

    public record EventBrief(Long id, String name, LocalDate eventDate, boolean main, Long daysUntil) {
    }

    public record DashboardSummary(
            String groomName,
            String brideName,
            LocalDate weddingDate,
            Side sideFilter,
            Long eventFilter,
            LocalDate from,
            LocalDate to,
            KeyFigures figures,
            Charts charts,
            Lists lists,
            List<EventBrief> events,
            List<BudgetDtos.BudgetAlert> alerts
    ) {
    }
}
