package com.wedding.management.service;

import com.wedding.management.common.exception.BusinessException;
import com.wedding.management.common.exception.ResourceNotFoundException;
import com.wedding.management.config.AppProperties;
import com.wedding.management.domain.entity.Attachment;
import com.wedding.management.domain.enums.AttachmentEntityType;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.dto.MiscDtos.AttachmentResponse;
import com.wedding.management.repository.AttachmentRepository;
import com.wedding.management.security.CurrentUser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded files on disk under {uploadDir}/{entityType}/{yyyy}/{MM}/{uuid}.{ext}.
 * Only images and PDF are accepted; the content is sniffed, not just the declared type.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp", "image/gif", "gif", "application/pdf", "pdf");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "pdf");

    private final AttachmentRepository attachmentRepository;
    private final AppProperties properties;
    private final AuditService auditService;

    private Path root;

    @PostConstruct
    void init() throws IOException {
        root = Paths.get(properties.getStorage().getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        log.info("Upload directory: {}", root);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(AttachmentEntityType type, Long entityId) {
        return attachmentRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(type, entityId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AttachmentResponse upload(AttachmentEntityType type, Long entityId, String description, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn tệp để tải lên");
        }
        long max = properties.getStorage().getMaxFileSizeBytes();
        if (file.getSize() > max) {
            throw new BusinessException("FILE_TOO_LARGE", "Tệp vượt quá kích thước cho phép (" + (max / 1024 / 1024) + " MB)");
        }
        String originalName = file.getOriginalFilename() == null ? "file" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String ext = extension(originalName);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!properties.getStorage().getAllowedContentTypes().contains(contentType) || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("INVALID_FILE_TYPE", "Chỉ chấp nhận ảnh (JPG, PNG, WEBP, GIF) hoặc PDF");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);
            if (!matchesSignature(head, contentType)) {
                throw new BusinessException("INVALID_FILE_TYPE", "Nội dung tệp không khớp với định dạng khai báo");
            }
        } catch (IOException e) {
            throw new BusinessException("Không đọc được tệp tải lên");
        }

        LocalDate today = LocalDate.now(SettingsService.ZONE);
        Path dir = root.resolve(type.name().toLowerCase(Locale.ROOT))
                .resolve(String.valueOf(today.getYear()))
                .resolve(String.format("%02d", today.getMonthValue()));
        String storedName = UUID.randomUUID() + "." + EXTENSIONS.getOrDefault(contentType, ext);
        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không lưu được tệp", e);
        }

        Attachment a = new Attachment();
        a.setEntityType(type);
        a.setEntityId(entityId);
        a.setOriginalName(originalName);
        a.setStoredName(storedName);
        a.setStoragePath(root.relativize(dir.resolve(storedName)).toString().replace('\\', '/'));
        a.setContentType(contentType);
        a.setSizeBytes(file.getSize());
        a.setDescription(description);
        a.setUploadedBy(CurrentUser.username());
        attachmentRepository.save(a);
        auditService.log(type.name(), entityId, originalName, AuditAction.CREATE, "Đính kèm tệp " + originalName);
        return toResponse(a);
    }

    @Transactional(readOnly = true)
    public Attachment get(Long id) {
        return attachmentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Tệp đính kèm", id));
    }

    public Resource resource(Attachment a) {
        try {
            Path path = root.resolve(a.getStoragePath()).normalize();
            if (!path.startsWith(root) || !Files.exists(path)) {
                throw new ResourceNotFoundException("Tệp không còn tồn tại trên máy chủ");
            }
            return new UrlResource(path.toUri());
        } catch (IOException e) {
            throw new ResourceNotFoundException("Không đọc được tệp");
        }
    }

    @Transactional
    public void delete(Long id) {
        Attachment a = get(id);
        attachmentRepository.delete(a);
        try {
            Files.deleteIfExists(root.resolve(a.getStoragePath()).normalize());
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", a.getStoragePath(), e.getMessage());
        }
        auditService.log(a.getEntityType().name(), a.getEntityId(), a.getOriginalName(), AuditAction.DELETE, "Xóa tệp đính kèm " + a.getOriginalName());
    }

    public Path rootDir() {
        return root;
    }

    private AttachmentResponse toResponse(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getEntityType(), a.getEntityId(), a.getOriginalName(), a.getContentType(),
                a.getSizeBytes(), a.getDescription(), a.getUploadedBy(), "/api/attachments/" + a.getId() + "/download",
                a.getContentType() != null && a.getContentType().startsWith("image/"), a.getCreatedAt());
    }

    private static String extension(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean matchesSignature(byte[] head, String contentType) {
        if (head.length < 4) return false;
        return switch (contentType) {
            case "image/jpeg" -> (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8;
            case "image/png" -> (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
            case "image/gif" -> head[0] == 'G' && head[1] == 'I' && head[2] == 'F';
            case "image/webp" -> head.length >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
            case "application/pdf" -> Arrays.equals(Arrays.copyOf(head, 4), new byte[]{'%', 'P', 'D', 'F'});
            default -> false;
        };
    }
}
