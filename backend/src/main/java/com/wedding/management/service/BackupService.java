package com.wedding.management.service;

import com.wedding.management.common.exception.BusinessException;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.dto.MiscDtos.BackupInfo;
import com.wedding.management.dto.MiscDtos.RestoreResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Full data backup (JSON) and restore. Tables are dumped generically through JDBC so the
 * backup always matches the current schema. Restore replaces all business data inside one
 * transaction; a safety snapshot of the current data is written to disk first.
 * User accounts are exported but never restored (so you can't lock yourself out), and
 * uploaded files must be backed up separately (the uploads folder).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    public static final String FORMAT_VERSION = "1";

    /** Dependency order: parents first. */
    static final List<String> TABLE_ORDER = List.of(
            "wedding_settings", "guest_groups", "guests", "wedding_events", "invitations", "table_areas", "wedding_tables",
            "table_assignments", "vendors", "expense_categories", "expenses", "expense_payments", "gifts", "gift_items",
            "other_incomes", "tasks", "attachments", "audit_logs");
    private static final Set<String> EXPORT_ONLY = Set.of("users");

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final AttachmentService attachmentService;
    private final AuditService auditService;

    // ---- export -------------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] export() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("app", "wedding-management");
        doc.put("version", FORMAT_VERSION);
        doc.put("generatedAt", Instant.now().toString());
        Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
        List<String> all = new ArrayList<>(EXPORT_ONLY);
        all.addAll(TABLE_ORDER);
        for (String table : all) {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id");
            List<Map<String, Object>> converted = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                Map<String, Object> out = new LinkedHashMap<>();
                row.forEach((k, v) -> out.put(k, toJson(v)));
                converted.add(out);
            }
            tables.put(table, converted);
        }
        doc.put("tables", tables);
        auditService.log("BACKUP", null, null, AuditAction.BACKUP, "Xuất bản sao lưu dữ liệu");
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(doc);
    }

    @Transactional(readOnly = true)
    public BackupInfo info() {
        long total = 0;
        List<String> tables = new ArrayList<>();
        for (String t : TABLE_ORDER) {
            Long c = jdbc.queryForObject("SELECT count(*) FROM " + t, Long.class);
            total += c == null ? 0 : c;
            tables.add(t);
        }
        return new BackupInfo(FORMAT_VERSION, Instant.now(), tables, total, fileName());
    }

    public String fileName() {
        return "wedding-backup-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now(SettingsService.ZONE)) + ".json";
    }

    private static Object toJson(Object v) {
        if (v == null) return null;
        if (v instanceof String || v instanceof Boolean || v instanceof Integer || v instanceof Long || v instanceof Short) return v;
        if (v instanceof BigDecimal bd) return bd.toPlainString();
        if (v instanceof Number n) return n.toString();
        if (v instanceof Timestamp ts) return ts.toInstant().toString();
        if (v instanceof java.sql.Date d) return d.toLocalDate().toString();
        if (v instanceof java.sql.Time t) return t.toLocalTime().toString();
        if (v instanceof OffsetDateTime odt) return odt.toInstant().toString();
        if (v instanceof LocalDate || v instanceof LocalTime || v instanceof Instant) return v.toString();
        if (v instanceof java.util.Date d) return d.toInstant().toString();
        // PGobject (jsonb) and anything else: use its string form
        try {
            var m = v.getClass().getMethod("getValue");
            Object val = m.invoke(v);
            return val == null ? null : val.toString();
        } catch (ReflectiveOperationException e) {
            return v.toString();
        }
    }

    // ---- restore ------------------------------------------------------------

    @Transactional
    public RestoreResult restore(MultipartFile file, boolean confirmed) {
        if (!confirmed) {
            throw new BusinessException("CONFIRM_REQUIRED", "Khôi phục sẽ thay thế toàn bộ dữ liệu hiện tại. Vui lòng xác nhận trước khi thực hiện.");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn tệp sao lưu (.json)");
        }
        Map<String, Object> doc;
        try (InputStream in = file.getInputStream()) {
            doc = objectMapper.readValue(in, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException | RuntimeException e) {
            throw new BusinessException("Tệp sao lưu không hợp lệ: " + e.getMessage());
        }
        if (!"wedding-management".equals(doc.get("app")) || !(doc.get("tables") instanceof Map<?, ?> tablesRaw)) {
            throw new BusinessException("Tệp này không phải bản sao lưu của Wedding Management");
        }
        if (!FORMAT_VERSION.equals(String.valueOf(doc.get("version")))) {
            throw new BusinessException("Phiên bản bản sao lưu không được hỗ trợ: " + doc.get("version"));
        }

        // 1. safety snapshot
        String snapshotFile;
        try {
            Path dir = attachmentService.rootDir().resolve("backups");
            Files.createDirectories(dir);
            Path target = dir.resolve("pre-restore-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now(SettingsService.ZONE)) + ".json");
            Files.write(target, exportWithoutAudit());
            snapshotFile = target.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được bản sao lưu an toàn trước khi khôi phục", e);
        }

        // 2. wipe in reverse dependency order
        for (int i = TABLE_ORDER.size() - 1; i >= 0; i--) {
            jdbc.update("DELETE FROM " + TABLE_ORDER.get(i));
        }

        // 3. insert
        long restored = 0;
        List<String> restoredTables = new ArrayList<>();
        for (String table : TABLE_ORDER) {
            Object rowsRaw = tablesRaw.get(table);
            if (!(rowsRaw instanceof List<?> rows) || rows.isEmpty()) continue;
            Map<String, String> columnTypes = columnTypes(table);
            for (Object rowObj : rows) {
                if (!(rowObj instanceof Map<?, ?> row)) continue;
                List<String> cols = new ArrayList<>();
                List<Object> values = new ArrayList<>();
                List<String> placeholders = new ArrayList<>();
                for (Map.Entry<?, ?> e : row.entrySet()) {
                    String col = String.valueOf(e.getKey());
                    String type = columnTypes.get(col);
                    if (type == null) continue; // column no longer exists
                    cols.add(col);
                    values.add(convert(e.getValue(), type));
                    placeholders.add("jsonb".equals(type) ? "?::jsonb" : "?");
                }
                if (cols.isEmpty()) continue;
                jdbc.update("INSERT INTO " + table + " (" + String.join(", ", cols) + ") VALUES (" + String.join(", ", placeholders) + ")",
                        values.toArray());
                restored++;
            }
            restoredTables.add(table);
        }

        // 4. sequences
        for (String table : TABLE_ORDER) {
            if (!columnTypes(table).containsKey("id") || "wedding_settings".equals(table)) continue;
            jdbc.execute("SELECT setval(pg_get_serial_sequence('" + table + "', 'id'), COALESCE((SELECT MAX(id) FROM " + table + "), 0) + 1, false)");
        }
        Integer maxCode = jdbc.queryForObject(
                "SELECT COALESCE(MAX(NULLIF(regexp_replace(code, '\\D', '', 'g'), '')::int), 0) FROM guests", Integer.class);
        jdbc.execute("SELECT setval('guest_code_seq', " + ((maxCode == null ? 0 : maxCode) + 1) + ", false)");
        // settings row must always exist
        Integer settingsCount = jdbc.queryForObject("SELECT count(*) FROM wedding_settings", Integer.class);
        if (settingsCount == null || settingsCount == 0) {
            jdbc.update("INSERT INTO wedding_settings (id) VALUES (1)");
        }

        auditService.log("BACKUP", null, null, AuditAction.RESTORE_BACKUP,
                "Khôi phục dữ liệu từ bản sao lưu " + file.getOriginalFilename() + " (" + restored + " bản ghi)");
        log.warn("Database restored from backup {} ({} rows). Pre-restore snapshot: {}", file.getOriginalFilename(), restored, snapshotFile);
        return new RestoreResult(true, "Khôi phục thành công " + restored + " bản ghi", restored, snapshotFile, restoredTables);
    }

    private byte[] exportWithoutAudit() {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("app", "wedding-management");
        doc.put("version", FORMAT_VERSION);
        doc.put("generatedAt", Instant.now().toString());
        Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
        List<String> all = new ArrayList<>(EXPORT_ONLY);
        all.addAll(TABLE_ORDER);
        for (String table : all) {
            List<Map<String, Object>> converted = new ArrayList<>();
            for (Map<String, Object> row : jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id")) {
                Map<String, Object> out = new LinkedHashMap<>();
                row.forEach((k, v) -> out.put(k, toJson(v)));
                converted.add(out);
            }
            tables.put(table, converted);
        }
        doc.put("tables", tables);
        return objectMapper.writeValueAsBytes(doc);
    }

    private Map<String, String> columnTypes(String table) {
        Map<String, String> types = new LinkedHashMap<>();
        jdbc.query("SELECT column_name, data_type, udt_name FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = ?",
                rs -> {
                    String dataType = rs.getString("data_type");
                    types.put(rs.getString("column_name"), "USER-DEFINED".equals(dataType) ? rs.getString("udt_name") : dataType);
                }, table);
        return types;
    }

    private static Object convert(Object value, String type) {
        if (value == null) return null;
        String s = value.toString();
        return switch (type) {
            case "bigint" -> Long.parseLong(s);
            case "integer", "smallint" -> Integer.parseInt(s);
            case "numeric" -> new BigDecimal(s);
            case "boolean" -> value instanceof Boolean b ? b : Boolean.parseBoolean(s);
            case "date" -> java.sql.Date.valueOf(LocalDate.parse(s));
            case "time without time zone" -> java.sql.Time.valueOf(LocalTime.parse(s));
            case "timestamp with time zone", "timestamp without time zone" -> Timestamp.from(parseInstant(s));
            default -> s;
        };
    }

    private static Instant parseInstant(String s) {
        try {
            return Instant.parse(s);
        } catch (Exception e) {
            return OffsetDateTime.parse(s).toInstant();
        }
    }
}
