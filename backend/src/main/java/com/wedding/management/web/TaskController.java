package com.wedding.management.web;

import com.wedding.management.domain.enums.TaskPriority;
import com.wedding.management.domain.enums.TaskStatus;
import com.wedding.management.dto.TaskDtos.ReorderRequest;
import com.wedding.management.dto.TaskDtos.StatusRequest;
import com.wedding.management.dto.TaskDtos.TaskRequest;
import com.wedding.management.dto.TaskDtos.TaskResponse;
import com.wedding.management.dto.TaskDtos.TaskStats;
import com.wedding.management.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Checklist công việc chuẩn bị")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Danh sách công việc (lọc theo trạng thái, ưu tiên, người phụ trách, quá hạn)")
    public List<TaskResponse> list(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) TaskStatus status,
                                   @RequestParam(required = false) TaskPriority priority,
                                   @RequestParam(required = false) String assignee,
                                   @RequestParam(required = false) Boolean overdue,
                                   @RequestParam(required = false) Long eventId) {
        return taskService.list(q, status, priority, assignee, overdue, eventId);
    }

    @GetMapping("/stats")
    @Operation(summary = "Tiến độ chuẩn bị: số việc theo trạng thái, phần trăm hoàn thành")
    public TaskStats stats() {
        return taskService.stats();
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id) {
        return taskService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskRequest request) {
        return taskService.create(request);
    }

    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Đổi trạng thái (kéo thả Kanban)")
    public TaskResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return taskService.changeStatus(id, request);
    }

    @PostMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@Valid @RequestBody ReorderRequest request) {
        taskService.reorder(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}
