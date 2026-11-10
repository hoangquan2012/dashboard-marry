package com.wedding.management.service;

import com.wedding.management.common.api.PageResponse;
import com.wedding.management.domain.entity.AuditLog;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.dto.AuditDtos.AuditLogResponse;
import com.wedding.management.repository.AuditLogRepository;
import com.wedding.management.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Records who changed what. Services call {@link #log} after mutating important records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    public void log(String entityType, Long entityId, String entityLabel, AuditAction action, String summary,
                    Map<String, Object> changes) {
        try {
            AuditLog entry = new AuditLog();
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setEntityLabel(entityLabel == null ? null : truncate(entityLabel, 255));
            entry.setAction(action);
            entry.setSummary(summary == null ? null : truncate(summary, 500));
            entry.setChanges(changes == null || changes.isEmpty() ? null : objectMapper.writeValueAsString(changes));
            entry.setPerformedBy(CurrentUser.username());
            entry.setPerformedAt(Instant.now());
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Audit must never break the business transaction
            log.warn("Could not write audit log for {} {}: {}", entityType, entityId, e.getMessage());
        }
    }

    public void log(String entityType, Long entityId, String entityLabel, AuditAction action, String summary) {
        log(entityType, entityId, entityLabel, action, summary, null);
    }

    /**
     * Builds a {field: {from, to}} map with only the fields that actually changed.
     */
    public static Map<String, Object> diff(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> changes = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : after.entrySet()) {
            Object old = before.get(e.getKey());
            if (!Objects.equals(normalize(old), normalize(e.getValue()))) {
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("from", normalize(old));
                c.put("to", normalize(e.getValue()));
                changes.put(e.getKey(), c);
            }
        }
        return changes;
    }

    private static Object normalize(Object v) {
        if (v == null) return null;
        if (v instanceof Enum<?> en) return en.name();
        if (v instanceof java.math.BigDecimal bd) return bd.stripTrailingZeros().toPlainString();
        if (v instanceof Number || v instanceof Boolean || v instanceof String) return v;
        return v.toString();
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String entityType, Long entityId, AuditAction action, String performedBy,
                                                 LocalDate from, LocalDate to, int page, int size) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entityType != null && !entityType.isBlank()) predicates.add(cb.equal(root.get("entityType"), entityType));
            if (entityId != null) predicates.add(cb.equal(root.get("entityId"), entityId));
            if (action != null) predicates.add(cb.equal(root.get("action"), action));
            if (performedBy != null && !performedBy.isBlank()) predicates.add(cb.equal(root.get("performedBy"), performedBy));
            ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("performedAt"), from.atStartOfDay(zone).toInstant()));
            if (to != null) predicates.add(cb.lessThan(root.get("performedAt"), to.plusDays(1).atStartOfDay(zone).toInstant()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<AuditLog> result = auditLogRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "performedAt", "id")));
        return PageResponse.of(result, a -> new AuditLogResponse(a.getId(), a.getEntityType(), a.getEntityId(),
                a.getEntityLabel(), a.getAction(), a.getSummary(), a.getChanges(), a.getPerformedBy(), a.getPerformedAt()));
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
