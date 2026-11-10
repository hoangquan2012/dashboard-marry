package com.wedding.management.service;

import com.wedding.management.common.exception.BusinessException;
import com.wedding.management.common.exception.ResourceNotFoundException;
import com.wedding.management.domain.entity.Expense;
import com.wedding.management.domain.entity.Gift;
import com.wedding.management.domain.entity.Guest;
import com.wedding.management.domain.entity.Invitation;
import com.wedding.management.domain.entity.TableAssignment;
import com.wedding.management.domain.entity.WeddingEvent;
import com.wedding.management.domain.entity.WeddingTable;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.domain.enums.EventStatus;
import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.EventDtos.EventRequest;
import com.wedding.management.dto.EventDtos.EventResponse;
import com.wedding.management.dto.EventDtos.EventStats;
import com.wedding.management.dto.EventDtos.InviteGuestsRequest;
import com.wedding.management.dto.GuestDtos.InvitationInput;
import com.wedding.management.dto.GuestDtos.InvitationResponse;
import com.wedding.management.repository.ExpenseRepository;
import com.wedding.management.repository.GiftRepository;
import com.wedding.management.repository.GuestRepository;
import com.wedding.management.repository.InvitationRepository;
import com.wedding.management.repository.TableAssignmentRepository;
import com.wedding.management.repository.WeddingEventRepository;
import com.wedding.management.repository.WeddingTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventService {

    private final WeddingEventRepository eventRepository;
    private final InvitationRepository invitationRepository;
    private final InvitationService invitationService;
    private final GuestRepository guestRepository;
    private final GiftRepository giftRepository;
    private final ExpenseRepository expenseRepository;
    private final WeddingTableRepository tableRepository;
    private final TableAssignmentRepository tableAssignmentRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<EventResponse> list() {
        List<WeddingEvent> events = eventRepository.findAllByOrderByEventDateAscSortOrderAsc();
        List<Invitation> invitations = invitationRepository.findAllActiveWithGuestAndEvent();
        List<Gift> gifts = giftRepository.findAllActiveWithRelations();
        List<Expense> expenses = expenseRepository.findAllActiveWithRelations();
        List<EventResponse> out = new ArrayList<>();
        for (WeddingEvent e : events) {
            out.add(toResponse(e, stats(e, invitations, gifts, expenses)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public EventResponse get(Long id) {
        WeddingEvent e = find(id);
        return toResponse(e, stats(e, invitationRepository.findAllActiveWithGuestAndEvent(),
                giftRepository.findAllActiveWithRelations(), expenseRepository.findAllActiveWithRelations()));
    }

    @Transactional
    public EventResponse create(EventRequest request) {
        WeddingEvent e = new WeddingEvent();
        apply(e, request);
        eventRepository.save(e);
        if (e.isMain()) clearOtherMain(e.getId());
        auditService.log("EVENT", e.getId(), e.getName(), AuditAction.CREATE, "Tạo sự kiện " + e.getName());
        return get(e.getId());
    }

    @Transactional
    public EventResponse update(Long id, EventRequest request) {
        WeddingEvent e = find(id);
        apply(e, request);
        eventRepository.save(e);
        if (e.isMain()) clearOtherMain(e.getId());
        auditService.log("EVENT", e.getId(), e.getName(), AuditAction.UPDATE, "Cập nhật sự kiện " + e.getName());
        return get(id);
    }

    @Transactional
    public EventResponse setMain(Long id) {
        WeddingEvent e = find(id);
        e.setMain(true);
        eventRepository.save(e);
        clearOtherMain(id);
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        WeddingEvent e = find(id);
        boolean hasCheckins = invitationRepository.findByEventIdWithGuest(id).stream().anyMatch(Invitation::isCheckedIn);
        if (hasCheckins) {
            throw new BusinessException("Sự kiện đã có khách check-in nên không thể xóa");
        }
        boolean hasGifts = giftRepository.findAllActiveWithRelations().stream()
                .anyMatch(g -> g.getEvent() != null && g.getEvent().getId().equals(id));
        if (hasGifts) {
            throw new BusinessException("Sự kiện đã có tiền mừng được ghi nhận nên không thể xóa. Hãy chuyển các khoản mừng sang sự kiện khác trước.");
        }
        String name = e.getName();
        eventRepository.delete(e);
        auditService.log("EVENT", id, name, AuditAction.DELETE, "Xóa sự kiện " + name);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> guests(Long eventId, InvitationStatus status, Side side, String q) {
        find(eventId);
        List<Invitation> invitations = invitationRepository.findByEventIdWithGuest(eventId);
        List<TableAssignment> assignments = tableAssignmentRepository.findByEventIdWithGuest(eventId);
        String needle = q == null ? null : q.trim().toLowerCase();
        List<Invitation> filtered = invitations.stream()
                .filter(i -> status == null || i.getStatus() == status)
                .filter(i -> side == null || i.getGuest().getSide() == side)
                .filter(i -> needle == null || needle.isEmpty()
                        || i.getGuest().getFullName().toLowerCase().contains(needle)
                        || (i.getGuest().getPhone() != null && i.getGuest().getPhone().contains(needle))
                        || i.getGuest().getCode().toLowerCase().contains(needle))
                .sorted((a, b) -> a.getGuest().getFullName().compareToIgnoreCase(b.getGuest().getFullName()))
                .toList();
        return InvitationService.toResponses(filtered, assignments);
    }

    @Transactional
    public int inviteGuests(Long eventId, InviteGuestsRequest request) {
        WeddingEvent event = find(eventId);
        int count = 0;
        for (Long guestId : new HashSet<>(request.guestIds())) {
            if (invitationRepository.existsByGuestIdAndEventId(guestId, eventId)) continue;
            Guest guest = guestRepository.findById(guestId).orElseThrow(() -> ResourceNotFoundException.of("Khách mời", guestId));
            int invited = request.invitedCount() == null ? 1 : request.invitedCount();
            invitationRepository.save(invitationService.createInvitation(guest, event,
                    new InvitationInput(eventId, invited, invited, InvitationStatus.NOT_SENT, null)));
            count++;
        }
        auditService.log("EVENT", eventId, event.getName(), AuditAction.UPDATE, "Thêm " + count + " khách vào sự kiện " + event.getName());
        return count;
    }

    @Transactional
    public void removeGuest(Long eventId, Long guestId) {
        Invitation inv = invitationRepository.findByGuestIdAndEventId(guestId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Khách chưa được mời tới sự kiện này"));
        if (inv.isCheckedIn()) {
            throw new BusinessException("Khách đã check-in, không thể bỏ khỏi sự kiện");
        }
        tableAssignmentRepository.findByGuestIdAndEventId(guestId, eventId).ifPresent(tableAssignmentRepository::delete);
        invitationRepository.delete(inv);
    }

    public WeddingEvent find(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Sự kiện", id));
    }

    private void clearOtherMain(Long keepId) {
        for (WeddingEvent other : eventRepository.findAll()) {
            if (!other.getId().equals(keepId) && other.isMain()) {
                other.setMain(false);
                eventRepository.save(other);
            }
        }
    }

    private void apply(WeddingEvent e, EventRequest r) {
        e.setName(r.name().trim());
        e.setEventDate(r.eventDate());
        e.setStartTime(r.startTime());
        e.setEndTime(r.endTime());
        e.setVenue(r.venue());
        e.setAddress(r.address());
        e.setSide(r.side() == null ? Side.BOTH : r.side());
        e.setDescription(r.description());
        e.setBudget(r.budget() == null ? BigDecimal.ZERO : r.budget());
        e.setStatus(r.status() == null ? EventStatus.PLANNED : r.status());
        if (r.main() != null) e.setMain(r.main());
        if (r.sortOrder() != null) e.setSortOrder(r.sortOrder());
    }

    private EventStats stats(WeddingEvent e, List<Invitation> allInvitations, List<Gift> allGifts, List<Expense> allExpenses) {
        List<Invitation> invs = allInvitations.stream().filter(i -> i.getEvent().getId().equals(e.getId())).toList();
        long groom = 0, bride = 0, confirmedGuests = 0, declinedGuests = 0, pendingGuests = 0, attendedGuests = 0, checkedIn = 0;
        int invited = 0, expected = 0, confirmed = 0, actual = 0;
        for (Invitation i : invs) {
            if (i.getGuest().getSide() == Side.GROOM) groom++;
            else if (i.getGuest().getSide() == Side.BRIDE) bride++;
            invited += i.getInvitedCount();
            expected += i.getExpectedCount();
            confirmed += i.getConfirmedCount();
            actual += i.getActualCount();
            switch (i.getStatus()) {
                case CONFIRMED -> confirmedGuests++;
                case DECLINED -> declinedGuests++;
                case ATTENDED -> attendedGuests++;
                case NOT_SENT, SENT, PENDING -> pendingGuests++;
                default -> {
                }
            }
            if (i.isCheckedIn()) checkedIn++;
        }
        BigDecimal giftTotal = BigDecimal.ZERO;
        long giftCount = 0;
        for (Gift g : allGifts) {
            if (g.getEvent() != null && g.getEvent().getId().equals(e.getId())) {
                giftCount++;
                if (g.getAmount() != null) giftTotal = giftTotal.add(g.getAmount());
            }
        }
        BigDecimal estimated = BigDecimal.ZERO, actualExp = BigDecimal.ZERO, paid = BigDecimal.ZERO;
        for (Expense x : allExpenses) {
            if (x.getEvent() != null && x.getEvent().getId().equals(e.getId())) {
                estimated = estimated.add(x.getEstimatedAmount());
                actualExp = actualExp.add(x.getActualAmount());
                paid = paid.add(x.getPaidAmount());
            }
        }
        List<WeddingTable> tables = tableRepository.findByEventIdWithArea(e.getId());
        int totalSeats = tables.stream().mapToInt(WeddingTable::getCapacity).sum();
        List<TableAssignment> assignments = tableAssignmentRepository.findByEventIdWithGuest(e.getId());
        int seated = assignments.stream().mapToInt(TableAssignment::getSeats).sum();
        Set<Long> seatedGuestIds = new HashSet<>();
        assignments.forEach(a -> seatedGuestIds.add(a.getGuest().getId()));
        long unassigned = invs.stream()
                .filter(i -> i.getStatus() != InvitationStatus.DECLINED)
                .filter(i -> !seatedGuestIds.contains(i.getGuest().getId())).count();
        return new EventStats(invs.size(), groom, bride, invited, expected, confirmed, actual,
                confirmedGuests, declinedGuests, pendingGuests, attendedGuests, checkedIn,
                giftTotal, giftCount, estimated, actualExp, paid, tables.size(), totalSeats, seated, unassigned);
    }

    public static EventResponse toResponse(WeddingEvent e, EventStats stats) {
        Long days = ChronoUnit.DAYS.between(LocalDate.now(SettingsService.ZONE), e.getEventDate());
        return new EventResponse(e.getId(), e.getName(), e.getEventDate(), e.getStartTime(), e.getEndTime(), e.getVenue(),
                e.getAddress(), e.getSide(), e.getDescription(), e.getBudget(), e.getStatus(), e.isMain(), e.getSortOrder(),
                days, stats, e.getCreatedAt(), e.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public Map<Long, String> namesById() {
        Map<Long, String> m = new HashMap<>();
        eventRepository.findAll().forEach(e -> m.put(e.getId(), e.getName()));
        return m;
    }
}
