package com.wedding.management.service;

import com.wedding.management.domain.entity.Expense;
import com.wedding.management.domain.entity.ExpenseCategory;
import com.wedding.management.domain.entity.ExpensePayment;
import com.wedding.management.domain.entity.Gift;
import com.wedding.management.domain.entity.Guest;
import com.wedding.management.domain.entity.Invitation;
import com.wedding.management.domain.entity.OtherIncome;
import com.wedding.management.domain.entity.TableAssignment;
import com.wedding.management.domain.entity.Task;
import com.wedding.management.domain.entity.WeddingEvent;
import com.wedding.management.domain.entity.WeddingSettings;
import com.wedding.management.domain.enums.GiftType;
import com.wedding.management.domain.enums.GuestCategory;
import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.domain.enums.TaskStatus;
import com.wedding.management.dto.BudgetDtos.BudgetAlert;
import com.wedding.management.dto.DashboardDtos.CategorySlice;
import com.wedding.management.dto.DashboardDtos.Charts;
import com.wedding.management.dto.DashboardDtos.DashboardSummary;
import com.wedding.management.dto.DashboardDtos.EventBrief;
import com.wedding.management.dto.DashboardDtos.KeyFigures;
import com.wedding.management.dto.DashboardDtos.LabelValue;
import com.wedding.management.dto.DashboardDtos.Lists;
import com.wedding.management.dto.DashboardDtos.PendingGuest;
import com.wedding.management.dto.DashboardDtos.StatusSlice;
import com.wedding.management.dto.DashboardDtos.TimePoint;
import com.wedding.management.dto.ExpenseDtos.ExpenseResponse;
import com.wedding.management.dto.GuestDtos.GuestSummary;
import com.wedding.management.repository.ExpenseCategoryRepository;
import com.wedding.management.repository.ExpensePaymentRepository;
import com.wedding.management.repository.ExpenseRepository;
import com.wedding.management.repository.GiftRepository;
import com.wedding.management.repository.GuestRepository;
import com.wedding.management.repository.InvitationRepository;
import com.wedding.management.repository.OtherIncomeRepository;
import com.wedding.management.repository.TableAssignmentRepository;
import com.wedding.management.repository.TaskRepository;
import com.wedding.management.repository.WeddingEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * All dashboard numbers are computed here, on the server, from the real data.
 * Filters: side (GROOM/BRIDE), event, date range.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SettingsService settingsService;
    private final GuestRepository guestRepository;
    private final InvitationRepository invitationRepository;
    private final GiftRepository giftRepository;
    private final OtherIncomeRepository otherIncomeRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpensePaymentRepository paymentRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final TaskRepository taskRepository;
    private final TableAssignmentRepository tableAssignmentRepository;
    private final WeddingEventRepository eventRepository;
    private final BudgetService budgetService;

    @Transactional(readOnly = true)
    public DashboardSummary summary(Side side, Long eventId, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(SettingsService.ZONE);
        WeddingSettings settings = settingsService.getEntity();
        List<WeddingEvent> events = eventRepository.findAllByOrderByEventDateAscSortOrderAsc();

        // ---- base data (active only) ----
        List<Guest> guests = guestRepository.findByDeletedAtIsNull();
        List<Invitation> invitations = invitationRepository.findAllActiveWithGuestAndEvent();
        List<Gift> gifts = giftRepository.findAllActiveWithRelations();
        List<OtherIncome> otherIncomes = otherIncomeRepository.findAll();
        List<Expense> expenses = expenseRepository.findAllActiveWithRelations();
        List<ExpensePayment> payments = paymentRepository.findAllActive();
        List<ExpenseCategory> categories = categoryRepository.findAllByOrderBySortOrderAscNameAsc();
        List<Task> tasks = taskRepository.findAllWithEvent();

        // ---- apply filters ----
        if (eventId != null) {
            Set<Long> invitedGuestIds = new HashSet<>();
            invitations.stream().filter(i -> i.getEvent().getId().equals(eventId)).forEach(i -> invitedGuestIds.add(i.getGuest().getId()));
            guests = guests.stream().filter(g -> invitedGuestIds.contains(g.getId())).toList();
            invitations = invitations.stream().filter(i -> i.getEvent().getId().equals(eventId)).toList();
            gifts = gifts.stream().filter(g -> g.getEvent() != null && g.getEvent().getId().equals(eventId)).toList();
            expenses = expenses.stream().filter(e -> e.getEvent() != null && e.getEvent().getId().equals(eventId)).toList();
            Set<Long> expenseIds = new HashSet<>();
            expenses.forEach(e -> expenseIds.add(e.getId()));
            payments = payments.stream().filter(p -> expenseIds.contains(p.getExpense().getId())).toList();
            tasks = tasks.stream().filter(t -> t.getEvent() == null || t.getEvent().getId().equals(eventId)).toList();
        }
        if (side != null) {
            guests = guests.stream().filter(g -> g.getSide() == side).toList();
            invitations = invitations.stream().filter(i -> i.getGuest().getSide() == side).toList();
            gifts = gifts.stream().filter(g -> g.getSide() == side).toList();
            otherIncomes = otherIncomes.stream().filter(i -> i.getSide() == side).toList();
            expenses = expenses.stream().filter(e -> e.getSide() == side).toList();
            Set<Long> expenseIds = new HashSet<>();
            expenses.forEach(e -> expenseIds.add(e.getId()));
            payments = payments.stream().filter(p -> expenseIds.contains(p.getExpense().getId())).toList();
        }
        if (from != null || to != null) {
            gifts = gifts.stream().filter(g -> inRange(g.getReceivedAt(), from, to)).toList();
            otherIncomes = otherIncomes.stream().filter(i -> inRange(i.getReceivedAt(), from, to)).toList();
            expenses = expenses.stream().filter(e -> inRange(e.getExpenseDate() != null ? e.getExpenseDate() : e.getDueDate(), from, to)
                    || (e.getExpenseDate() == null && e.getDueDate() == null)).toList();
            payments = payments.stream().filter(p -> inRange(p.getPaymentDate(), from, to)).toList();
        }

        // ---- guests & invitations ----
        long groom = guests.stream().filter(g -> g.getSide() == Side.GROOM).count();
        long bride = guests.stream().filter(g -> g.getSide() == Side.BRIDE).count();
        long both = guests.size() - groom - bride;
        Map<Long, List<Invitation>> invByGuest = new HashMap<>();
        invitations.forEach(i -> invByGuest.computeIfAbsent(i.getGuest().getId(), k -> new ArrayList<>()).add(i));
        int invitedPeople = 0, expectedPeople = 0, confirmedPeople = 0, actualPeople = 0;
        long checkedIn = 0;
        for (Invitation i : invitations) {
            invitedPeople += i.getInvitedCount();
            expectedPeople += i.getExpectedCount();
            confirmedPeople += i.getConfirmedCount();
            actualPeople += i.getActualCount();
            if (i.isCheckedIn()) checkedIn++;
        }
        Map<InvitationStatus, long[]> statusAgg = new LinkedHashMap<>();
        for (InvitationStatus s : InvitationStatus.values()) statusAgg.put(s, new long[2]);
        long confirmedGuests = 0, declinedGuests = 0, undecidedGuests = 0, attendedGuests = 0;
        for (Guest g : guests) {
            List<Invitation> list = invByGuest.getOrDefault(g.getId(), List.of());
            InvitationStatus overall = InvitationService.overallStatus(list);
            long[] acc = statusAgg.get(overall);
            acc[0]++;
            acc[1] += list.stream().mapToInt(i -> switch (overall) {
                case CONFIRMED -> i.getConfirmedCount();
                case ATTENDED -> i.getActualCount();
                default -> i.getExpectedCount();
            }).max().orElse(0);
            switch (overall) {
                case CONFIRMED -> confirmedGuests++;
                case DECLINED -> declinedGuests++;
                case ATTENDED -> attendedGuests++;
                case NOT_SENT, SENT, PENDING -> undecidedGuests++;
                default -> {
                }
            }
        }

        // ---- gifts ----
        BigDecimal giftTotal = BigDecimal.ZERO, giftGroom = BigDecimal.ZERO, giftBride = BigDecimal.ZERO;
        Set<String> givers = new HashSet<>();
        Map<Long, BigDecimal> giftByGroup = new HashMap<>();
        Map<GiftType, BigDecimal[]> giftByType = new LinkedHashMap<>();
        Map<Side, BigDecimal[]> giftBySide = new LinkedHashMap<>();
        for (Gift g : gifts) {
            BigDecimal amount = g.getAmount() == null ? BigDecimal.ZERO : g.getAmount();
            giftTotal = giftTotal.add(amount);
            if (g.getSide() == Side.GROOM) giftGroom = giftGroom.add(amount);
            if (g.getSide() == Side.BRIDE) giftBride = giftBride.add(amount);
            givers.add(g.getGuest() != null ? "G" + g.getGuest().getId() : "N" + g.getGiverName());
            if (g.getGuest() != null && g.getGuest().getGroup() != null) giftByGroup.merge(g.getGuest().getGroup().getId(), amount, BigDecimal::add);
            BigDecimal[] t = giftByType.computeIfAbsent(g.getGiftType(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            t[0] = t[0].add(BigDecimal.ONE);
            t[1] = t[1].add(amount);
            BigDecimal[] s = giftBySide.computeIfAbsent(g.getSide(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            s[0] = s[0].add(BigDecimal.ONE);
            s[1] = s[1].add(amount);
        }
        BigDecimal otherIncomeTotal = otherIncomes.stream().map(OtherIncome::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIncome = giftTotal.add(otherIncomeTotal);

        // ---- expenses ----
        BigDecimal estimated = BigDecimal.ZERO, actual = BigDecimal.ZERO, paid = BigDecimal.ZERO;
        Map<Long, BigDecimal[]> byCategory = new LinkedHashMap<>();
        for (Expense e : expenses) {
            estimated = estimated.add(e.getEstimatedAmount());
            actual = actual.add(e.getActualAmount());
            paid = paid.add(e.getPaidAmount());
            BigDecimal[] c = byCategory.computeIfAbsent(e.getCategory().getId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            c[0] = c[0].add(e.getEstimatedAmount());
            c[1] = c[1].add(e.getActualAmount());
        }
        BigDecimal totalBudget = settings.getTotalBudget();

        // ---- averages ----
        BigDecimal avgPerGiver = givers.isEmpty() ? BigDecimal.ZERO : giftTotal.divide(BigDecimal.valueOf(givers.size()), 0, RoundingMode.HALF_UP);
        BigDecimal groupTotal = giftByGroup.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgPerGroup = giftByGroup.isEmpty() ? BigDecimal.ZERO : groupTotal.divide(BigDecimal.valueOf(giftByGroup.size()), 0, RoundingMode.HALF_UP);
        BigDecimal avgCostPerAttendee = actualPeople == 0 ? null : actual.divide(BigDecimal.valueOf(actualPeople), 0, RoundingMode.HALF_UP);

        // ---- tasks ----
        long tasksDone = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        long tasksOverdue = tasks.stream().filter(t -> t.isOverdue(today)).count();
        double completion = tasks.isEmpty() ? 0 : BigDecimal.valueOf(tasksDone * 100.0 / tasks.size()).setScale(1, RoundingMode.HALF_UP).doubleValue();

        Long daysUntil = settings.getWeddingDate() == null ? null : ChronoUnit.DAYS.between(today, settings.getWeddingDate());

        KeyFigures figures = new KeyFigures(guests.size(), groom, bride, both, invitations.size(), invitedPeople, expectedPeople,
                confirmedPeople, actualPeople, confirmedGuests, declinedGuests, undecidedGuests, attendedGuests, checkedIn,
                giftTotal, giftGroom, giftBride, gifts.size(), givers.size(), otherIncomeTotal, totalIncome,
                estimated, actual, paid, actual.subtract(paid).max(BigDecimal.ZERO), totalBudget, totalBudget.subtract(actual),
                totalIncome.subtract(actual), avgPerGiver, avgPerGroup, avgCostPerAttendee,
                tasks.size(), tasksDone, tasksOverdue, completion, daysUntil);

        // ---- charts ----
        List<LabelValue> guestsBySide = List.of(
                new LabelValue("GROOM", Side.GROOM.label(), groom, null),
                new LabelValue("BRIDE", Side.BRIDE.label(), bride, null),
                new LabelValue("BOTH", Side.BOTH.label(), both, null));
        List<StatusSlice> guestsByStatus = new ArrayList<>();
        for (Map.Entry<InvitationStatus, long[]> e : statusAgg.entrySet()) {
            guestsByStatus.add(new StatusSlice(e.getKey(), e.getKey().label(), e.getValue()[0], (int) e.getValue()[1]));
        }
        List<LabelValue> giftsBySide = new ArrayList<>();
        for (Side s : Side.values()) {
            BigDecimal[] v = giftBySide.getOrDefault(s, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            giftsBySide.add(new LabelValue(s.name(), s.label(), v[0].longValue(), v[1]));
        }
        List<LabelValue> giftsByType = new ArrayList<>();
        for (GiftType t : GiftType.values()) {
            BigDecimal[] v = giftByType.getOrDefault(t, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            giftsByType.add(new LabelValue(t.name(), t.label(), v[0].longValue(), v[1]));
        }
        List<CategorySlice> expensesByCategory = new ArrayList<>();
        List<CategorySlice> budgetVsActual = new ArrayList<>();
        for (ExpenseCategory cat : categories) {
            BigDecimal[] c = byCategory.get(cat.getId());
            BigDecimal catEst = c == null ? BigDecimal.ZERO : c[0];
            BigDecimal catAct = c == null ? BigDecimal.ZERO : c[1];
            double percent = actual.signum() == 0 ? 0 : catAct.multiply(BigDecimal.valueOf(100)).divide(actual, 1, RoundingMode.HALF_UP).doubleValue();
            if (catAct.signum() > 0) expensesByCategory.add(new CategorySlice(cat.getId(), cat.getName(), cat.getBudgetAmount(), catEst, catAct, percent));
            if (catAct.signum() > 0 || catEst.signum() > 0 || cat.getBudgetAmount().signum() > 0) {
                budgetVsActual.add(new CategorySlice(cat.getId(), cat.getName(), cat.getBudgetAmount(), catEst, catAct, percent));
            }
        }
        expensesByCategory.sort(Comparator.comparing(CategorySlice::actual).reversed());
        budgetVsActual.sort(Comparator.comparing((CategorySlice c) -> c.actual().max(c.budget())).reversed());
        Map<GuestCategory, Long> catCount = new LinkedHashMap<>();
        for (Guest g : guests) catCount.merge(g.getCategory(), 1L, Long::sum);
        List<LabelValue> guestsByCategory = new ArrayList<>();
        for (GuestCategory c : GuestCategory.values()) {
            long n = catCount.getOrDefault(c, 0L);
            if (n > 0) guestsByCategory.add(new LabelValue(c.name(), c.label(), n, null));
        }
        Charts charts = new Charts(guestsBySide, guestsByStatus, giftsBySide, giftsByType, expensesByCategory, budgetVsActual,
                cashflow(gifts, otherIncomes, payments, from, to), guestsByCategory);

        // ---- lists ----
        List<ExpenseResponse> topExpenses = expenses.stream().filter(e -> e.getActualAmount().signum() > 0)
                .sorted(Comparator.comparing(Expense::getActualAmount).reversed()).limit(5)
                .map(e -> ExpenseService.toResponse(e, null, 0)).toList();
        List<ExpenseResponse> unpaid = expenses.stream().filter(e -> e.getRemainingAmount().signum() > 0)
                .sorted(Comparator.comparing((Expense e) -> e.getDueDate() == null ? LocalDate.MAX : e.getDueDate())).limit(8)
                .map(e -> ExpenseService.toResponse(e, null, 0)).toList();

        List<PendingGuest> pending = new ArrayList<>();
        List<PendingGuest> toContact = new ArrayList<>();
        Instant now = Instant.now();
        for (Guest g : guests) {
            List<Invitation> list = invByGuest.getOrDefault(g.getId(), List.of());
            InvitationStatus overall = InvitationService.overallStatus(list);
            if (!overall.isUndecided()) continue;
            Invitation ref = list.stream().min(Comparator.comparing(i -> i.getEvent().getEventDate())).orElse(null);
            Long daysSince = ref != null && ref.getSentAt() != null ? ChronoUnit.DAYS.between(ref.getSentAt(), now) : null;
            PendingGuest pg = new PendingGuest(g.getId(), g.getCode(), g.getFullName(), g.getPhone(), g.getSide(),
                    g.getGroup() != null ? g.getGroup().getName() : null, overall, overall.label(),
                    ref != null ? ref.getEvent().getName() : null, ref != null ? ref.getEvent().getEventDate() : null, daysSince);
            pending.add(pg);
            boolean needsContact = overall == InvitationStatus.PENDING || (overall == InvitationStatus.SENT && daysSince != null && daysSince >= 7);
            if (needsContact) toContact.add(pg);
        }
        pending.sort(Comparator.comparing(PendingGuest::fullName, String.CASE_INSENSITIVE_ORDER));
        toContact.sort(Comparator.comparing((PendingGuest p) -> p.daysSinceSent() == null ? -1L : p.daysSinceSent()).reversed());

        List<Task> upcoming = tasks.stream().filter(t -> t.getStatus() != TaskStatus.DONE)
                .filter(t -> t.getDueDate() != null && !t.getDueDate().isAfter(today.plusDays(14)))
                .sorted(Comparator.comparing(Task::getDueDate)).limit(8).toList();

        // guests without a table for the (filtered or main/next) event
        WeddingEvent seatingEvent = eventId != null ? events.stream().filter(e -> e.getId().equals(eventId)).findFirst().orElse(null)
                : events.stream().filter(WeddingEvent::isMain).findFirst()
                .orElse(events.stream().filter(e -> !e.getEventDate().isBefore(today)).findFirst().orElse(events.isEmpty() ? null : events.get(events.size() - 1)));
        List<GuestSummary> unassigned = new ArrayList<>();
        if (seatingEvent != null) {
            Set<Long> seated = new HashSet<>();
            for (TableAssignment a : tableAssignmentRepository.findByEventIdWithGuest(seatingEvent.getId())) seated.add(a.getGuest().getId());
            Set<Long> visibleGuests = new HashSet<>();
            guests.forEach(g -> visibleGuests.add(g.getId()));
            for (Invitation i : invitationRepository.findByEventIdWithGuest(seatingEvent.getId())) {
                if (i.getStatus() == InvitationStatus.DECLINED || seated.contains(i.getGuest().getId()) || !visibleGuests.contains(i.getGuest().getId())) continue;
                unassigned.add(GuestService.toSummary(i.getGuest()));
            }
        }
        Lists lists = new Lists(topExpenses, unpaid, pending.stream().limit(10).toList(), pending.size(),
                toContact.stream().limit(10).toList(), toContact.size(),
                upcoming.stream().map(t -> TaskService.toResponse(t, today)).toList(),
                unassigned.stream().limit(10).toList(), unassigned.size());

        List<EventBrief> eventBriefs = events.stream().map(e -> new EventBrief(e.getId(), e.getName(), e.getEventDate(), e.isMain(),
                ChronoUnit.DAYS.between(today, e.getEventDate()))).toList();

        List<BudgetAlert> alerts = (side == null && eventId == null && from == null && to == null)
                ? budgetService.overview().alerts() : List.of();

        return new DashboardSummary(settings.getGroomName(), settings.getBrideName(), settings.getWeddingDate(), side, eventId, from, to,
                figures, charts, lists, eventBriefs, alerts);
    }

    private static boolean inRange(LocalDate d, LocalDate from, LocalDate to) {
        if (d == null) return false;
        if (from != null && d.isBefore(from)) return false;
        return to == null || !d.isAfter(to);
    }

    /**
     * Income (gifts + other incomes) vs. expense payments over time.
     * Daily points when the range is short (≤ 62 days), monthly otherwise.
     */
    private static List<TimePoint> cashflow(List<Gift> gifts, List<OtherIncome> incomes, List<ExpensePayment> payments,
                                            LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        gifts.forEach(g -> dates.add(g.getReceivedAt()));
        incomes.forEach(i -> dates.add(i.getReceivedAt()));
        payments.forEach(p -> dates.add(p.getPaymentDate()));
        if (dates.isEmpty()) return List.of();
        LocalDate min = from != null ? from : dates.stream().min(Comparator.naturalOrder()).orElseThrow();
        LocalDate max = to != null ? to : dates.stream().max(Comparator.naturalOrder()).orElseThrow();
        boolean daily = ChronoUnit.DAYS.between(min, max) <= 62;

        TreeMap<LocalDate, BigDecimal[]> buckets = new TreeMap<>();
        java.util.function.Function<LocalDate, LocalDate> bucketOf = d -> daily ? d : d.withDayOfMonth(1);
        for (LocalDate cursor = bucketOf.apply(min); !cursor.isAfter(max); cursor = daily ? cursor.plusDays(1) : cursor.plusMonths(1)) {
            buckets.put(cursor, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Gift g : gifts) {
            BigDecimal[] b = buckets.get(bucketOf.apply(g.getReceivedAt()));
            if (b != null && g.getAmount() != null) {
                b[0] = b[0].add(g.getAmount());
                b[1] = b[1].add(g.getAmount());
            }
        }
        for (OtherIncome i : incomes) {
            BigDecimal[] b = buckets.get(bucketOf.apply(i.getReceivedAt()));
            if (b != null) b[0] = b[0].add(i.getAmount());
        }
        for (ExpensePayment p : payments) {
            BigDecimal[] b = buckets.get(bucketOf.apply(p.getPaymentDate()));
            if (b != null) b[2] = b[2].add(p.getAmount());
        }
        List<TimePoint> out = new ArrayList<>();
        BigDecimal cumIn = BigDecimal.ZERO, cumOut = BigDecimal.ZERO;
        for (Map.Entry<LocalDate, BigDecimal[]> e : buckets.entrySet()) {
            cumIn = cumIn.add(e.getValue()[0]);
            cumOut = cumOut.add(e.getValue()[2]);
            String label = daily ? String.format("%02d/%02d", e.getKey().getDayOfMonth(), e.getKey().getMonthValue())
                    : String.format("%02d/%d", e.getKey().getMonthValue(), YearMonth.from(e.getKey()).getYear());
            out.add(new TimePoint(label, e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2], cumIn, cumOut));
        }
        return out;
    }
}
