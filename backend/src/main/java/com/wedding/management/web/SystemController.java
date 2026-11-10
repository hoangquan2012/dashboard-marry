package com.wedding.management.web;

import com.wedding.management.common.api.PageResponse;
import com.wedding.management.common.web.Downloads;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.dto.AuditDtos.AuditLogResponse;
import com.wedding.management.dto.MiscDtos.BackupInfo;
import com.wedding.management.dto.MiscDtos.ReminderSummary;
import com.wedding.management.dto.MiscDtos.RestoreResult;
import com.wedding.management.dto.MiscDtos.SearchResponse;
import com.wedding.management.service.AuditService;
import com.wedding.management.service.BackupService;
import com.wedding.management.service.ReminderService;
import com.wedding.management.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "System", description = "Tìm kiếm tổng thể, nhắc việc, nhật ký, sao lưu & khôi phục")
public class SystemController {

    private final SearchService searchService;
    private final ReminderService reminderService;
    private final AuditService auditService;
    private final BackupService backupService;

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm nhanh khách mời, tiền mừng, chi phí, nhà cung cấp, công việc")
    public SearchResponse search(@RequestParam String q, @RequestParam(defaultValue = "6") int limit) {
        return searchService.search(q, Math.min(Math.max(limit, 1), 20));
    }

    @GetMapping("/reminders")
    @Operation(summary = "Nhắc việc: công việc / thanh toán sắp đến hạn, khách chưa xác nhận, chưa xếp bàn, vượt ngân sách")
    public ReminderSummary reminders() {
        return reminderService.reminders();
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Nhật ký lịch sử thay đổi")
    public PageResponse<AuditLogResponse> auditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return auditService.search(entityType, entityId, action, performedBy, from, to, page, Math.min(size, 200));
    }

    @GetMapping("/backup/info")
    @PreAuthorize("hasRole('ADMIN')")
    public BackupInfo backupInfo() {
        return backupService.info();
    }

    @GetMapping("/backup/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tải bản sao lưu toàn bộ dữ liệu (JSON)")
    public ResponseEntity<byte[]> export() {
        return Downloads.file(backupService.fileName(), MediaType.APPLICATION_JSON, backupService.export());
    }

    @PostMapping(value = "/backup/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Khôi phục từ bản sao lưu (thay thế toàn bộ dữ liệu, cần confirm=true)")
    public RestoreResult restore(@RequestPart("file") MultipartFile file, @RequestParam(defaultValue = "false") boolean confirm) {
        return backupService.restore(file, confirm);
    }
}
