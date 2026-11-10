package com.wedding.management.service;

import com.wedding.management.domain.entity.Expense;
import com.wedding.management.domain.entity.Invitation;
import com.wedding.management.domain.entity.TableAssignment;
import com.wedding.management.domain.entity.Task;
import com.wedding.management.domain.entity.WeddingEvent;
import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.domain.enums.TaskStatus;
import com.wedding.management.dto.BudgetDtos.BudgetAlert;
import com.wedding.management.dto.MiscDtos.Reminder;
import com.wedding.management.dto.MiscDtos.ReminderSummary;
import com.wedding.management.repository.ExpenseRepository;
import com.wedding.management.repository.InvitationRepository;
import com.wedding.management.repository.TableAssignmentRepository;
import com.wedding.management.repository.TaskRepository;
import com.wedding.management.repository.WeddingEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes reminders on the fly (nothing is stored): tasks due soon, payments due soon,
 * unconfirmed guests, guests without a table, categories over budget.
 */
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final TaskRepository taskRepository;
    private final ExpenseRepository expenseRepository;
    private final InvitationRepository invitationRepository;
    private final TableAssignmentRepository tableAssignmentRepository;
    private final WeddingEventRepository eventRepository;
    private final BudgetService budgetService;

    @Transactional(readOnly = true)
    public ReminderSummary reminders() {
        LocalDate today = LocalDate.now(SettingsService.ZONE);
        List<Reminder> items = new ArrayList<>();

        // tasks
        for (Task t : taskRepository.findAllWithEvent()) {
            if (t.getStatus() == TaskStatus.DONE || t.getDueDate() == null) continue;
            long days = ChronoUnit.DAYS.between(today, t.getDueDate());
            if (days < 0) {
                items.add(new Reminder("TASK_OVERDUE", "error", "Công việc quá hạn", t.getTitle() + " (quá hạn " + (-days) + " ngày)",
                        "/tasks?taskId=" + t.getId(), t.getId(), t.getDueDate(), null));
            } else if (days <= 7) {
                items.add(new Reminder("TASK_DUE", "warning", "Công việc sắp đến hạn", t.getTitle() + (days == 0 ? " (hôm nay)" : " (còn " + days + " ngày)"),
                        "/tasks?taskId=" + t.getId(), t.getId(), t.getDueDate(), null));
            }
        }

        // payments
        for (Expense e : expenseRepository.findAllActiveWithRelations()) {
            BigDecimal remaining = e.getRemainingAmount();
            if (remaining.signum() <= 0 || e.getDueDate() == null) continue;
            long days = ChronoUnit.DAYS.between(today, e.getDueDate());
            if (days < 0) {
                items.add(new Reminder("PAYMENT_OVERDUE", "error", "Khoản thanh toán quá hạn",
                        e.getName() + " còn nợ " + money(remaining) + (e.getVendor() != null ? " (" + e.getVendor().getName() + ")" : ""),
                        "/expenses/" + e.getId(), e.getId(), e.getDueDate(), remaining));
            } else if (days <= 7) {
                items.add(new Reminder("PAYMENT_DUE", "warning", "Khoản thanh toán sắp đến hạn",
                        e.getName() + " còn nợ " + money(remaining) + (days == 0 ? " (hạn hôm nay)" : " (còn " + days + " ngày)"),
                        "/expenses/" + e.getId(), e.getId(), e.getDueDate(), remaining));
            }
        }

        // guests not confirmed / not seated, per upcoming event
        List<WeddingEvent> events = eventRepository.findAllByOrderByEventDateAscSortOrderAsc();
        for (WeddingEvent ev : events) {
            if (ev.getEventDate().isBefore(today)) continue;
            List<Invitation> invs = invitationRepository.findByEventIdWithGuest(ev.getId());
            long undecided = invs.stream().filter(i -> i.getStatus().isUndecided()).count();
            long daysToEvent = ChronoUnit.DAYS.between(today, ev.getEventDate());
            if (undecided > 0) {
                items.add(new Reminder("GUEST_UNCONFIRMED", daysToEvent <= 14 ? "warning" : "info", "Khách chưa xác nhận",
                        undecided + " khách chưa xác nhận tham dự " + ev.getName() + " (còn " + daysToEvent + " ngày)",
                        "/guests?eventId=" + ev.getId() + "&status=PENDING", ev.getId(), ev.getEventDate(), null));
            }
            Set<Long> seated = new HashSet<>();
            for (TableAssignment a : tableAssignmentRepository.findByEventIdWithGuest(ev.getId())) seated.add(a.getGuest().getId());
            long unseated = invs.stream().filter(i -> i.getStatus() != InvitationStatus.DECLINED)
                    .filter(i -> !seated.contains(i.getGuest().getId())).count();
            if (unseated > 0 && !seated.isEmpty()) {
                items.add(new Reminder("GUEST_UNSEATED", daysToEvent <= 7 ? "warning" : "info", "Khách chưa xếp bàn",
                        unseated + " khách của " + ev.getName() + " chưa được xếp bàn", "/seating?eventId=" + ev.getId(), ev.getId(), ev.getEventDate(), null));
            }
        }

        // budget
        for (BudgetAlert alert : budgetService.overview().alerts()) {
            items.add(new Reminder("BUDGET_" + alert.type(), alert.severity(), "Ngân sách", alert.message(), "/budget", alert.referenceId(), null, alert.amount()));
        }

        Map<String, Integer> severityOrder = Map.of("error", 0, "warning", 1, "info", 2);
        items.sort(Comparator.comparing((Reminder r) -> severityOrder.getOrDefault(r.severity(), 3))
                .thenComparing(r -> r.date() == null ? LocalDate.MAX : r.date()));
        int critical = (int) items.stream().filter(r -> "error".equals(r.severity())).count();
        int warnings = (int) items.stream().filter(r -> "warning".equals(r.severity())).count();
        return new ReminderSummary(items.size(), critical, warnings, items);
    }

    private static String money(BigDecimal v) {
        return String.format("%,d ₫", v.longValue()).replace(',', '.');
    }
}
