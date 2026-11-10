package com.wedding.management.domain.entity;

import com.wedding.management.domain.enums.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_label")
    private String entityLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    @Column(length = 500)
    private String summary;

    /** JSON document describing changed fields: {"field": {"from": ..., "to": ...}} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String changes;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt = Instant.now();
}
