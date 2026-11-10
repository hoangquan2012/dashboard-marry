package com.wedding.management.service;

import com.wedding.management.common.exception.BusinessException;
import com.wedding.management.common.exception.ResourceNotFoundException;
import com.wedding.management.domain.entity.Invitation;
import com.wedding.management.domain.entity.TableAssignment;
import com.wedding.management.domain.entity.WeddingEvent;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.dto.CheckinDtos.CheckinResult;
import com.wedding.management.dto.CheckinDtos.CheckinStats;
import com.wedding.management.dto.CheckinDtos.ManualCheckinRequest;
import com.wedding.management.dto.CheckinDtos.QrCheckinRequest;
import com.wedding.management.dto.GuestDtos.InvitationResponse;
import com.wedding.management.repository.InvitationRepository;
import com.wedding.management.repository.TableAssignmentRepository;
import com.wedding.management.repository.WeddingEventRepository;
import com.wedding.management.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckinService {

    private static final java.time.format.DateTimeFormatter TIME_FORMAT =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final InvitationRepository invitationRepository;
    private final TableAssignmentRepository tableAssignmentRepository;
    private final WeddingEventRepository eventRepository;
    private final InvitationService invitationService;
    private final QrCodeService qrCodeService;
    private final AuditService auditService;

    @Transactional
    public CheckinResult checkinByQr(QrCheckinRequest request) {
        String token = qrCodeService.tokenFrom(request.code());
        Invitation inv = invitationRepository.findByQrToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Mã QR không hợp lệ hoặc không thuộc đám cưới này"));
        if (request.eventId() != null && !inv.getEvent().getId().equals(request.eventId())) {
            throw new BusinessException("WRONG_EVENT", "Mã QR này thuộc sự kiện \"" + inv.getEvent().getName()
                    + "\", không phải sự kiện đang check-in");
        }
        return doCheckin(inv, request.actualCount(), Boolean.TRUE.equals(request.force()));
    }

    @Transactional
    public CheckinResult checkin(Long invitationId, ManualCheckinRequest request) {
        Invitation inv = invitationRepository.findByIdWithGuestAndEvent(invitationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Lời mời", invitationId));
        return doCheckin(inv, request == null ? null : request.actualCount(), request != null && Boolean.TRUE.equals(request.force()));
    }

    private CheckinResult doCheckin(Invitation inv, Integer actualCount, boolean force) {
        if (inv.getGuest().isArchived()) {
            throw new BusinessException("Khách này đã bị lưu trữ, hãy khôi phục trước khi check-in");
        }
        TableAssignment table = tableAssignmentRepository.findByGuestIdAndEventId(inv.getGuest().getId(), inv.getEvent().getId()).orElse(null);
        if (inv.isCheckedIn() && !force) {
            String time = TIME_FORMAT.format(inv.getCheckedInAt().atZone(SettingsService.ZONE));
            return new CheckinResult(true, "Khách " + inv.getGuest().getFullName() + " đã check-in lúc " + time
                    + ". Xác nhận lại nếu muốn cập nhật số người thực tế.", InvitationService.toResponse(inv, table));
        }
        boolean already = inv.isCheckedIn();
        if (inv.getStatus() == InvitationStatus.DECLINED && !force) {
            throw BusinessException.conflict("DECLINED_GUEST", "Khách này đã từ chối tham dự trước đó. Xác nhận để vẫn check-in.",
                    Map.of("invitationId", inv.getId()));
        }
        int actual = actualCount != null ? actualCount
                : (inv.getActualCount() > 0 ? inv.getActualCount()
                : inv.getConfirmedCount() > 0 ? inv.getConfirmedCount() : Math.max(1, inv.getExpectedCount()));
        invitationService.applyStatus(inv, InvitationStatus.ATTENDED, null, actual);
        if (!already) inv.setCheckedInAt(Instant.now());
        inv.setCheckedInBy(CurrentUser.username());
        invitationRepository.save(inv);
        auditService.log("GUEST", inv.getGuest().getId(), inv.getGuest().getFullName(), AuditAction.CHECKIN,
                (already ? "Cập nhật check-in " : "Check-in ") + inv.getGuest().getFullName() + " tại " + inv.getEvent().getName()
                        + " (" + actual + " người)");
        String msg = already ? "Đã cập nhật số người thực tế" : "Check-in thành công: " + inv.getGuest().getFullName();
        return new CheckinResult(already, msg, InvitationService.toResponse(inv, table));
    }

    @Transactional
    public InvitationResponse undo(Long invitationId) {
        Invitation inv = invitationRepository.findByIdWithGuestAndEvent(invitationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Lời mời", invitationId));
        if (!inv.isCheckedIn()) {
            throw new BusinessException("Khách này chưa check-in");
        }
        InvitationStatus back = inv.getConfirmedCount() > 0 ? InvitationStatus.CONFIRMED : InvitationStatus.SENT;
        invitationService.applyStatus(inv, back, inv.getConfirmedCount(), null);
        inv.setCheckedInAt(null);
        inv.setCheckedInBy(null);
        invitationRepository.save(inv);
        auditService.log("GUEST", inv.getGuest().getId(), inv.getGuest().getFullName(), AuditAction.UNDO_CHECKIN,
                "Hủy check-in " + inv.getGuest().getFullName() + " tại " + inv.getEvent().getName());
        TableAssignment table = tableAssignmentRepository.findByGuestIdAndEventId(inv.getGuest().getId(), inv.getEvent().getId()).orElse(null);
        return InvitationService.toResponse(inv, table);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> search(Long eventId, String q, Boolean checkedIn) {
        List<Invitation> invitations = invitationRepository.findByEventIdWithGuest(eventId);
        List<TableAssignment> assignments = tableAssignmentRepository.findByEventIdWithGuest(eventId);
        String needle = q == null ? "" : q.trim().toLowerCase();
        List<Invitation> filtered = invitations.stream()
                .filter(i -> checkedIn == null || i.isCheckedIn() == checkedIn)
                .filter(i -> needle.isEmpty() || i.getGuest().getFullName().toLowerCase().contains(needle)
                        || (i.getGuest().getPhone() != null && i.getGuest().getPhone().contains(needle))
                        || i.getGuest().getCode().toLowerCase().contains(needle))
                .sorted(Comparator.comparing((Invitation i) -> i.getGuest().getFullName(), String.CASE_INSENSITIVE_ORDER))
                .limit(200)
                .toList();
        return InvitationService.toResponses(filtered, assignments);
    }

    @Transactional(readOnly = true)
    public CheckinStats stats(Long eventId) {
        WeddingEvent event = eventRepository.findById(eventId).orElseThrow(() -> ResourceNotFoundException.of("Sự kiện", eventId));
        List<Invitation> invitations = invitationRepository.findByEventIdWithGuest(eventId);
        List<TableAssignment> assignments = tableAssignmentRepository.findByEventIdWithGuest(eventId);
        long checkedInGuests = 0, declined = 0;
        int checkedInPeople = 0, expected = 0, confirmed = 0;
        for (Invitation i : invitations) {
            if (i.isCheckedIn()) {
                checkedInGuests++;
                checkedInPeople += i.getActualCount();
            }
            if (i.getStatus() == InvitationStatus.DECLINED) declined++;
            expected += i.getExpectedCount();
            confirmed += i.getConfirmedCount();
        }
        List<Invitation> recent = invitations.stream().filter(Invitation::isCheckedIn)
                .sorted(Comparator.comparing(Invitation::getCheckedInAt).reversed()).limit(15).toList();
        return new CheckinStats(eventId, event.getName(), invitations.size(), checkedInGuests, checkedInPeople,
                expected, confirmed, invitations.size() - checkedInGuests - declined, declined,
                InvitationService.toResponses(recent, assignments));
    }
}
