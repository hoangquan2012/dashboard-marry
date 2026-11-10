package com.wedding.management.dto;

import com.wedding.management.domain.enums.TaskPriority;
import com.wedding.management.domain.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class TaskDtos {
    private TaskDtos() {
    }

    public record TaskRequest(
            @NotBlank(message = "Vui lòng nhập tên công việc") @Size(max = 200) String title,
            @Size(max = 4000) String description,
            @Size(max = 150) String assignee,
            LocalDate startDate,
            LocalDate dueDate,
            TaskPriority priority,
            TaskStatus status,
            Long eventId,
            Integer sortOrder,
            @Size(max = 2000) String note
    ) {
    }

    public record StatusRequest(@NotNull(message = "Thiếu trạng thái") TaskStatus status, Integer sortOrder) {
    }

    public record TaskResponse(
            Long id,
            String title,
            String description,
            String assignee,
            LocalDate startDate,
            LocalDate dueDate,
            TaskPriority priority,
            String priorityLabel,
            TaskStatus status,
            String statusLabel,
            /** TODO / IN_PROGRESS / DONE / OVERDUE (derived) */
            String effectiveStatus,
            String effectiveStatusLabel,
            boolean overdue,
            Long daysUntilDue,
            Instant completedAt,
            int sortOrder,
            Long eventId,
            String eventName,
            String note,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record TaskStats(long total, long todo, long inProgress, long done, long overdue, long dueSoon, double completionPercent) {
    }

    public record ReorderRequest(@NotNull List<Long> orderedIds, TaskStatus status) {
    }
}
