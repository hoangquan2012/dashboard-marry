package com.wedding.management.dto;

import com.wedding.management.domain.enums.AttachmentEntityType;
import com.wedding.management.domain.enums.Side;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class MiscDtos {
    private MiscDtos() {
    }

    // ---- attachments --------------------------------------------------------

    public record AttachmentResponse(Long id, AttachmentEntityType entityType, Long entityId, String originalName,
                                     String contentType, long sizeBytes, String description, String uploadedBy,
                                     String downloadUrl, boolean image, Instant createdAt) {
    }

    // ---- global search ------------------------------------------------------

    public record SearchHit(String type, Long id, String title, String subtitle, String route, Side side, BigDecimal amount) {
    }

    public record SearchResponse(String query, List<SearchHit> guests, List<SearchHit> gifts, List<SearchHit> expenses,
                                 List<SearchHit> vendors, List<SearchHit> tasks, int total) {
    }

    // ---- reminders ----------------------------------------------------------

    public record Reminder(String type, String severity, String title, String message, String route, Long referenceId,
                           LocalDate date, BigDecimal amount) {
    }

    public record ReminderSummary(int total, int critical, int warnings, List<Reminder> items) {
    }

    // ---- backup -------------------------------------------------------------

    public record BackupInfo(String version, Instant generatedAt, List<String> tables, long totalRows, String fileName) {
    }

    public record RestoreResult(boolean success, String message, long restoredRows, String preRestoreBackupFile,
                                List<String> tables) {
    }
}
