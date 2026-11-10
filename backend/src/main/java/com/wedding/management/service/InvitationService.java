package com.wedding.management.service;

import com.wedding.management.common.exception.BusinessException;
import com.wedding.management.common.exception.ResourceNotFoundException;
import com.wedding.management.domain.entity.Guest;
import com.wedding.management.domain.entity.Invitation;
import com.wedding.management.domain.entity.TableAssignment;
import com.wedding.management.domain.entity.WeddingEvent;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.dto.GuestDtos.BulkStatusRequest;
import com.wedding.management.dto.GuestDtos.InvitationInput;
import com.wedding.management.dto.GuestDtos.InvitationResponse;
import com.wedding.management.dto.GuestDtos.InvitationUpdateRequest;
import com.wedding.management.repository.InvitationRepository;
import com.wedding.management.repository.TableAssignmentRepository;
import com.wedding.management.repository.WeddingEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Invitation lifecycle rules shared by guests, events and check-in.
 */
@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final WeddingEventRepository eventRepository;
    private final TableAssignmentRepository tableAssignmentRepository;
    private final AuditService auditService;

    /**
     * Applies a new status and keeps the people counters consistent with it.
     */
    public void applyStatus(Invitation inv, InvitationStatus status, Integer confirmedCount, Integer actualCount) {
        Instant now = Instant.now();
        inv.setStatus(status);
        switch (status) {
            case NOT_SENT -> {
                inv.setSentAt(null);
                inv.setRespondedAt(null);
                inv.setConfirmedCount(0);
                inv.setActualCount(0);
                inv.setCheckedInAt(null);
                inv.setCheckedInBy(null);
            }
            case SENT, PENDING -> {
                if (inv.getSentAt() == null) inv.setSentAt(now);
                inv.setConfirmedCount(0);
                inv.setActualCount(0);
                inv.setCheckedInAt(null);
                inv.setCheckedInBy(null);
            }
            case CONFIRMED -> {
                if (inv.getSentAt() == null) inv.setSentAt(now);
                inv.setRespondedAt(now);
                int confirmed = confirmedCount != null ? confirmedCount
                        : (inv.getConfirmedCount() > 0 ? inv.getConfirmedCount() : Math.max(1, inv.getExpectedCount()));
                inv.setConfirmedCount(Math.max(1, confirmed));
                inv.setActualCount(0);
                inv.setCheckedInAt(null);
                inv.setCheckedInBy(null);
            }
            case DECLINED -> {
                if (inv.getSentAt() == null) inv.setSentAt(now);
                inv.setRespondedAt(now);
                inv.setConfirmedCount(0);
                inv.setActualCount(0);
                inv.setCheckedInAt(null);
                inv.setCheckedInBy(null);
            }
            case ATTENDED -> {
                if (inv.getSentAt() == null) inv.setSentAt(now);
                if (confirmedCount != null) inv.setConfirmedCount(confirmedCount);
                int actual = actualCount != null ? actualCount
                        : (inv.getActualCount() > 0 ? inv.getActualCount()
                        : inv.getConfirmedCount() > 0 ? inv.getConfirmedCount() : Math.max(1, inv.getExpectedCount()));
                inv.setActualCount(Math.max(1, actual));
                if (inv.getCheckedInAt() == null) inv.setCheckedInAt(now);
            }
            case ABSENT -> {
                if (inv.getSentAt() == null) inv.setSentAt(now);
                if (confirmedCount != null) inv.setConfirmedCount(confirmedCount);
                inv.setActualCount(0);
                inv.setCheckedInAt(null);
                inv.setCheckedInBy(null);
            }
        }
    }

    public Invitation createInvitation(Guest guest, WeddingEvent event, InvitationInput input) {
        Invitation inv = new Invitation();
        inv.setGuest(guest);
        inv.setEvent(event);
        inv.setQrToken(UUID.randomUUID().toString());
        int invited = input.invitedCount() == null ? 1 : input.invitedCount();
        inv.setInvitedCount(invited);
        inv.setExpectedCount(input.expectedCount() == null ? invited : input.expectedCount());
        inv.setNote(input.note());
        applyStatus(inv, input.status() == null ? InvitationStatus.NOT_SENT : input.status(), null, null);
        return inv;
    }

    /**
     * Replaces the set of invitations of a guest with the given list.
     * Invitations that were checked in cannot be removed.
     */
    public void syncInvitations(Guest guest, List<InvitationInput> inputs) {
        if (inputs == null) return;
        List<Invitation> existing = invitationRepository.findByGuestIdWithEvent(guest.getId());
        Map<Long, Invitation> byEvent = new HashMap<>();
        existing.forEach(i -> byEvent.put(i.getEvent().getId(), i));

        Map<Long, InvitationInput> wanted = new LinkedHashMap<>();
        for (InvitationInput in : inputs) {
            if (in.eventId() != null) wanted.put(in.eventId(), in);
        }

        for (Invitation inv : existing) {
            if (!wanted.containsKey(inv.getEvent().getId())) {
                if (inv.isCheckedIn()) {
                    throw new BusinessException("Không thể bỏ lời mời sự kiện \"" + inv.getEvent().getName()
                            + "\" vì khách đã check-in");
                }
                tableAssignmentRepository.findByGuestIdAndEventId(guest.getId(), inv.getEvent().getId())
                        .ifPresent(tableAssignmentRepository::delete);
                invitationRepository.delete(inv);
            }
        }

        for (Map.Entry<Long, InvitationInput> e : wanted.entrySet()) {
            Invitation inv = byEvent.get(e.getKey());
            InvitationInput in = e.getValue();
            if (inv == null) {
                WeddingEvent event = eventRepository.findById(e.getKey())
                        .orElseThrow(() -> ResourceNotFoundException.of("Sự kiện", e.getKey()));
                invitationRepository.save(createInvitation(guest, event, in));
            } else {
                if (in.invitedCount() != null) inv.setInvitedCount(in.invitedCount());
                if (in.expectedCount() != null) inv.setExpectedCount(in.expectedCount());
                if (in.note() != null) inv.setNote(in.note());
                if (in.status() != null && in.status() != inv.getStatus()) {
                    applyStatus(inv, in.status(), null, null);
                }
            }
        }
    }

    @Transactional
    public InvitationResponse update(Long id, InvitationUpdateRequest request) {
        Invitation inv = invitationRepository.findByIdWithGuestAndEvent(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Lời mời", id));
        InvitationStatus before = inv.getStatus();
        if (request.invitedCount() != null) inv.setInvitedCount(request.invitedCount());
        if (request.expectedCount() != null) inv.setExpectedCount(request.expectedCount());
        if (request.note() != null) inv.setNote(request.note());
        if (request.status() != null) {
            applyStatus(inv, request.status(), request.confirmedCount(), request.actualCount());
        } else {
            // counts only: keep the current status, apply explicit values when provided
            if (request.confirmedCount() != null) inv.setConfirmedCount(request.confirmedCount());
            if (request.actualCount() != null) {
                inv.setActualCount(request.actualCount());
                if (request.actualCount() > 0 && inv.getStatus() != InvitationStatus.ATTENDED) {
                    applyStatus(inv, InvitationStatus.ATTENDED, request.confirmedCount(), request.actualCount());
                }
            }
        }
        invitationRepository.save(inv);
        if (before != inv.getStatus()) {
            auditService.log("GUEST", inv.getGuest().getId(), inv.getGuest().getFullName(), AuditAction.STATUS_CHANGE,
                    "Đổi trạng thái lời mời " + inv.getEvent().getName() + ": " + before.label() + " → " + inv.getStatus().label());
        }
        return toResponse(inv, null);
    }

    @Transactional
    public int bulkStatus(BulkStatusRequest request) {
        List<Invitation> list = invitationRepository.findAllById(request.invitationIds());
        for (Invitation inv : list) {
            applyStatus(inv, request.status(), null, null);
        }
        invitationRepository.saveAll(list);
        auditService.log("INVITATION", null, null, AuditAction.STATUS_CHANGE,
                "Cập nhật trạng thái " + list.size() + " lời mời thành " + request.status().label());
        return list.size();
    }

    @Transactional(readOnly = true)
    public Invitation getWithRelations(Long id) {
        return invitationRepository.findByIdWithGuestAndEvent(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Lời mời", id));
    }

    /**
     * Derives a single status for a guest invited to several events (highest rank wins).
     */
    public static InvitationStatus overallStatus(List<Invitation> invitations) {
        return invitations.stream().map(Invitation::getStatus)
                .max(Comparator.comparingInt(InvitationStatus::rank))
                .orElse(InvitationStatus.NOT_SENT);
    }

    public static InvitationResponse toResponse(Invitation inv, TableAssignment assignment) {
        Guest g = inv.getGuest();
        WeddingEvent e = inv.getEvent();
        return new InvitationResponse(
                inv.getId(), g.getId(), g.getCode(), g.getFullName(), g.getPhone(), g.getSide(),
                g.getGroup() != null ? g.getGroup().getName() : null,
                e.getId(), e.getName(), e.getEventDate(),
                inv.getStatus(), inv.getStatus().label(),
                inv.getInvitedCount(), inv.getExpectedCount(), inv.getConfirmedCount(), inv.getActualCount(),
                inv.getQrToken(), inv.getSentAt(), inv.getRespondedAt(), inv.getCheckedInAt(), inv.getCheckedInBy(),
                inv.getNote(),
                assignment != null ? assignment.getTable().getId() : null,
                assignment != null ? assignment.getTable().getName() : null);
    }

    public static List<InvitationResponse> toResponses(List<Invitation> invitations, List<TableAssignment> assignments) {
        Map<String, TableAssignment> byGuestEvent = new HashMap<>();
        if (assignments != null) {
            for (TableAssignment a : assignments) {
                byGuestEvent.put(a.getGuest().getId() + ":" + a.getTable().getEvent().getId(), a);
            }
        }
        List<InvitationResponse> out = new ArrayList<>();
        for (Invitation inv : invitations) {
            out.add(toResponse(inv, byGuestEvent.get(inv.getGuest().getId() + ":" + inv.getEvent().getId())));
        }
        return out;
    }
}
