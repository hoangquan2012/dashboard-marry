package com.wedding.management.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.wedding.management.domain.enums.AuditAction;

import java.time.Instant;

public final class AuditDtos {
    private AuditDtos() {
    }

    public record AuditLogResponse(
            Long id,
            String entityType,
            Long entityId,
            String entityLabel,
            AuditAction action,
            String summary,
            @JsonRawValue String changes,
            String performedBy,
            Instant performedAt
    ) {
    }
}
