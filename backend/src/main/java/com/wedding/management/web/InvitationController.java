package com.wedding.management.web;

import com.wedding.management.domain.entity.Invitation;
import com.wedding.management.dto.GuestDtos.BulkStatusRequest;
import com.wedding.management.dto.GuestDtos.InvitationResponse;
import com.wedding.management.dto.GuestDtos.InvitationUpdateRequest;
import com.wedding.management.service.InvitationService;
import com.wedding.management.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
@Tag(name = "Invitations", description = "Lời mời theo từng sự kiện, mã QR")
public class InvitationController {

    private final InvitationService invitationService;
    private final QrCodeService qrCodeService;

    @GetMapping("/{id}")
    public InvitationResponse get(@PathVariable Long id) {
        Invitation inv = invitationService.getWithRelations(id);
        return InvitationService.toResponse(inv, null);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật trạng thái / số người của một lời mời")
    public InvitationResponse update(@PathVariable Long id, @Valid @RequestBody InvitationUpdateRequest request) {
        return invitationService.update(id, request);
    }

    @PostMapping("/bulk-status")
    @Operation(summary = "Cập nhật trạng thái hàng loạt (ví dụ: đã gửi thiệp)")
    public Map<String, Integer> bulkStatus(@Valid @RequestBody BulkStatusRequest request) {
        return Map.of("updated", invitationService.bulkStatus(request));
    }

    @GetMapping(value = "/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Ảnh QR của lời mời (dùng để in thiệp / check-in)")
    public ResponseEntity<byte[]> qr(@PathVariable Long id, @RequestParam(defaultValue = "320") int size) {
        Invitation inv = invitationService.getWithRelations(id);
        int s = Math.max(120, Math.min(size, 1024));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCodeService.png(inv.getQrToken(), s));
    }
}
