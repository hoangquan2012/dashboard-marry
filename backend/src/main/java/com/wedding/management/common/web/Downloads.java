package com.wedding.management.common.web;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Helpers to return downloadable files (Excel, JSON backups, images).
 */
public final class Downloads {

    public static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private Downloads() {
    }

    public static ResponseEntity<byte[]> excel(String baseName, byte[] content) {
        String fileName = baseName + "-" + LocalDate.now() + ".xlsx";
        return file(fileName, XLSX, content);
    }

    public static ResponseEntity<byte[]> file(String fileName, MediaType type, byte[] content) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(type)
                .contentLength(content.length)
                .body(content);
    }
}
