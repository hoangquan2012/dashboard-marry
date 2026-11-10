package com.wedding.management.web;

import com.wedding.management.domain.entity.Attachment;
import com.wedding.management.domain.enums.AttachmentEntityType;
import com.wedding.management.dto.MiscDtos.AttachmentResponse;
import com.wedding.management.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Hóa đơn, hợp đồng, chứng từ, ảnh đính kèm")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping
    @Operation(summary = "Danh sách tệp đính kèm của một bản ghi")
    public List<AttachmentResponse> list(@RequestParam AttachmentEntityType entityType, @RequestParam Long entityId) {
        return attachmentService.list(entityType, entityId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tải lên tệp (ảnh JPG/PNG/WEBP/GIF hoặc PDF, tối đa 10MB)")
    public AttachmentResponse upload(@RequestParam AttachmentEntityType entityType,
                                     @RequestParam Long entityId,
                                     @RequestParam(required = false) String description,
                                     @RequestPart("file") MultipartFile file) {
        return attachmentService.upload(entityType, entityId, description, file);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Tải / xem tệp (hỗ trợ ?access_token= cho thẻ img / link)")
    public ResponseEntity<Resource> download(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean inline) {
        Attachment a = attachmentService.get(id);
        Resource resource = attachmentService.resource(a);
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(a.getOriginalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(a.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(a.getContentType()))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        attachmentService.delete(id);
    }
}
