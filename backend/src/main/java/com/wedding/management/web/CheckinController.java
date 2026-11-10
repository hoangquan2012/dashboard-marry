package com.wedding.management.web;

import com.wedding.management.dto.CheckinDtos.CheckinResult;
import com.wedding.management.dto.CheckinDtos.CheckinStats;
import com.wedding.management.dto.CheckinDtos.ManualCheckinRequest;
import com.wedding.management.dto.CheckinDtos.QrCheckinRequest;
import com.wedding.management.dto.GuestDtos.InvitationResponse;
import com.wedding.management.service.CheckinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
@Tag(name = "Check-in", description = "Check-in khách trong ngày cưới (QR hoặc thủ công)")
public class CheckinController {

    private final CheckinService checkinService;

    @GetMapping("/stats")
    @Operation(summary = "Tình hình check-in của sự kiện")
    public CheckinStats stats(@RequestParam Long eventId) {
        return checkinService.stats(eventId);
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm khách để check-in thủ công")
    public List<InvitationResponse> search(@RequestParam Long eventId,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(required = false) Boolean checkedIn) {
        return checkinService.search(eventId, q, checkedIn);
    }

    @PostMapping("/qr")
    @Operation(summary = "Check-in bằng mã QR")
    public CheckinResult qr(@Valid @RequestBody QrCheckinRequest request) {
        return checkinService.checkinByQr(request);
    }

    @PostMapping("/{invitationId}")
    @Operation(summary = "Check-in thủ công theo lời mời")
    public CheckinResult manual(@PathVariable Long invitationId, @RequestBody(required = false) @Valid ManualCheckinRequest request) {
        return checkinService.checkin(invitationId, request);
    }

    @PostMapping("/{invitationId}/undo")
    @Operation(summary = "Hủy check-in")
    public InvitationResponse undo(@PathVariable Long invitationId) {
        return checkinService.undo(invitationId);
    }
}
