package com.wedding.management.service;

import com.wedding.management.common.exception.ResourceNotFoundException;
import com.wedding.management.domain.entity.Task;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.domain.enums.TaskPriority;
import com.wedding.management.domain.enums.TaskStatus;
import com.wedding.management.dto.TaskDtos.ReorderRequest;
import com.wedding.management.dto.TaskDtos.StatusRequest;
import com.wedding.management.dto.TaskDtos.TaskRequest;
import com.wedding.management.dto.TaskDtos.TaskResponse;
import com.wedding.management.dto.TaskDtos.TaskStats;
import com.wedding.management.repository.TaskRepository;
import com.wedding.management.repository.WeddingEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final WeddingEventRepository eventRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<TaskResponse> list(String q, TaskStatus status, TaskPriority priority, String assignee, Boolean overdue, Long eventId) {
        LocalDate today = LocalDate.now(SettingsService.ZONE);
        String needle = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        return taskRepository.findAllWithEvent().stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> assignee == null || assignee.isBlank() || (t.getAssignee() != null && t.getAssignee().toLowerCase().contains(assignee.toLowerCase())))
                .filter(t -> overdue == null || t.isOverdue(today) == overdue)
                .filter(t -> eventId == null || (t.getEvent() != null && t.getEvent().getId().equals(eventId)))
                .filter(t -> needle == null || t.getTitle().toLowerCase().contains(needle)
                        || (t.getDescription() != null && t.getDescription().toLowerCase().contains(needle))
                        || (t.getAssignee() != null && t.getAssignee().toLowerCase().contains(needle)))
                .map(t -> toResponse(t, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return toResponse(find(id), LocalDate.now(SettingsService.ZONE));
    }

    @Transactional(readOnly = true)
    public TaskStats stats() {
        LocalDate today = LocalDate.now(SettingsService.ZONE);
        List<Task> tasks = taskRepository.findAll();
        long todo = 0, inProgress = 0, done = 0, overdue = 0, dueSoon = 0;
        for (Task t : tasks) {
            switch (t.getStatus()) {
                case TODO -> todo++;
                case IN_PROGRESS -> inProgress++;
                case DONE -> done++;
            }
            if (t.isOverdue(today)) overdue++;
            else if (t.getStatus() != TaskStatus.DONE && t.getDueDate() != null && !t.getDueDate().isAfter(today.plusDays(7))) dueSoon++;
        }
        double percent = tasks.isEmpty() ? 0 : BigDecimal.valueOf(done * 100.0 / tasks.size()).setScale(1, RoundingMode.HALF_UP).doubleValue();
        return new TaskStats(tasks.size(), todo, inProgress, done, overdue, dueSoon, percent);
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task t = new Task();
        apply(t, request);
        if (t.getSortOrder() == 0) t.setSortOrder((int) taskRepository.count() + 1);
        taskRepository.save(t);
        auditService.log("TASK", t.getId(), t.getTitle(), AuditAction.CREATE, "Thêm công việc " + t.getTitle());
        return get(t.getId());
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task t = find(id);
        TaskStatus before = t.getStatus();
        apply(t, request);
        taskRepository.save(t);
        auditService.log("TASK", t.getId(), t.getTitle(), before != t.getStatus() ? AuditAction.STATUS_CHANGE : AuditAction.UPDATE,
                before != t.getStatus() ? "Công việc \"" + t.getTitle() + "\": " + before.label() + " → " + t.getStatus().label()
                        : "Cập nhật công việc " + t.getTitle());
        return get(id);
    }

    @Transactional
    public TaskResponse changeStatus(Long id, StatusRequest request) {
        Task t = find(id);
        TaskStatus before = t.getStatus();
        setStatus(t, request.status());
        if (request.sortOrder() != null) t.setSortOrder(request.sortOrder());
        taskRepository.save(t);
        if (before != t.getStatus()) {
            auditService.log("TASK", t.getId(), t.getTitle(), AuditAction.STATUS_CHANGE,
                    "Công việc \"" + t.getTitle() + "\": " + before.label() + " → " + t.getStatus().label());
        }
        return get(id);
    }

    @Transactional
    public void reorder(ReorderRequest request) {
        int order = 1;
        for (Long id : request.orderedIds()) {
            Task t = find(id);
            t.setSortOrder(order++);
            if (request.status() != null && t.getStatus() != request.status()) setStatus(t, request.status());
            taskRepository.save(t);
        }
    }

    @Transactional
    public void delete(Long id) {
        Task t = find(id);
        taskRepository.delete(t);
        auditService.log("TASK", id, t.getTitle(), AuditAction.DELETE, "Xóa công việc " + t.getTitle());
    }

    private Task find(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Công việc", id));
    }

    private void apply(Task t, TaskRequest r) {
        t.setTitle(r.title().trim());
        t.setDescription(r.description());
        t.setAssignee(r.assignee());
        t.setStartDate(r.startDate());
        t.setDueDate(r.dueDate());
        t.setPriority(r.priority() == null ? TaskPriority.MEDIUM : r.priority());
        setStatus(t, r.status() == null ? (t.getStatus() == null ? TaskStatus.TODO : t.getStatus()) : r.status());
        t.setEvent(r.eventId() == null ? null : eventRepository.findById(r.eventId()).orElseThrow(() -> ResourceNotFoundException.of("Sự kiện", r.eventId())));
        if (r.sortOrder() != null) t.setSortOrder(r.sortOrder());
        t.setNote(r.note());
    }

    private void setStatus(Task t, TaskStatus status) {
        t.setStatus(status);
        if (status == TaskStatus.DONE) {
            if (t.getCompletedAt() == null) t.setCompletedAt(Instant.now());
        } else {
            t.setCompletedAt(null);
        }
    }

    public static TaskResponse toResponse(Task t, LocalDate today) {
        boolean overdue = t.isOverdue(today);
        String effective = overdue ? "OVERDUE" : t.getStatus().name();
        String effectiveLabel = overdue ? "Quá hạn" : t.getStatus().label();
        Long days = t.getDueDate() == null ? null : ChronoUnit.DAYS.between(today, t.getDueDate());
        return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getAssignee(), t.getStartDate(), t.getDueDate(),
                t.getPriority(), t.getPriority().label(), t.getStatus(), t.getStatus().label(), effective, effectiveLabel, overdue, days,
                t.getCompletedAt(), t.getSortOrder(), t.getEvent() != null ? t.getEvent().getId() : null,
                t.getEvent() != null ? t.getEvent().getName() : null, t.getNote(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
