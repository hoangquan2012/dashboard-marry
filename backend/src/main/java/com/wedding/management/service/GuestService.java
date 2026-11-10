package com.wedding.management.service;

import com.wedding.management.common.api.PageResponse;
import com.wedding.management.common.exception.BusinessException;
import com.wedding.management.common.exception.ResourceNotFoundException;
import com.wedding.management.domain.entity.Gift;
import com.wedding.management.domain.entity.Guest;
import com.wedding.management.domain.entity.GuestGroup;
import com.wedding.management.domain.entity.Invitation;
import com.wedding.management.domain.entity.TableAssignment;
import com.wedding.management.domain.entity.WeddingEvent;
import com.wedding.management.domain.entity.WeddingTable;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.domain.enums.GuestCategory;
import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.GuestDtos.DuplicateCheckResponse;
import com.wedding.management.dto.GuestDtos.GuestRequest;
import com.wedding.management.dto.GuestDtos.GuestResponse;
import com.wedding.management.dto.GuestDtos.GuestStats;
import com.wedding.management.dto.GuestDtos.GuestSummary;
import com.wedding.management.dto.GuestDtos.InvitationInput;
import com.wedding.management.dto.GuestDtos.InvitationResponse;
import com.wedding.management.dto.GuestDtos.TableInfo;
import com.wedding.management.repository.GiftRepository;
import com.wedding.management.repository.GuestRepository;
import com.wedding.management.repository.InvitationRepository;
import com.wedding.management.repository.TableAssignmentRepository;
import com.wedding.management.repository.WeddingEventRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;
    private final GuestGroupService guestGroupService;
    private final InvitationRepository invitationRepository;
    private final InvitationService invitationService;
    private final WeddingEventRepository eventRepository;
    private final TableAssignmentRepository tableAssignmentRepository;
    private final GiftRepository giftRepository;
    private final AuditService auditService;

    public record GuestFilter(String q, Side side, Long groupId, GuestCategory category, InvitationStatus status,
                              Long eventId, Boolean vip, Boolean archived, Boolean unassigned, Boolean hasGift) {
    }

    // ---- queries ------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<GuestResponse> list(GuestFilter filter, int page, int size, String sortBy, String direction) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 200), sort(sortBy, direction));
        Page<Guest> result = guestRepository.findAll(specification(filter), pageable);
        return PageResponse.of(result, assemble(result.getContent()));
    }

    @Transactional(readOnly = true)
    public List<GuestResponse> listAll(GuestFilter filter, String sortBy, String direction) {
        List<Guest> guests = guestRepository.findAll(specification(filter), sort(sortBy, direction));
        return assemble(guests);
    }

    @Transactional(readOnly = true)
    public GuestResponse get(Long id) {
        Guest guest = guestRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Khách mời", id));
        return assemble(List.of(guest)).get(0);
    }

    @Transactional(readOnly = true)
    public List<GuestSummary> lookup(String q, int limit) {
        if (q == null || q.isBlank()) return List.of();
        return guestRepository.search(q.trim()).stream().limit(limit).map(GuestService::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public GuestStats stats() {
        List<Guest> guests = guestRepository.findAll();
        List<Invitation> invitations = invitationRepository.findAllActiveWithGuestAndEvent();
        Map<Long, List<Invitation>> byGuest = new HashMap<>();
        invitations.forEach(i -> byGuest.computeIfAbsent(i.getGuest().getId(), k -> new ArrayList<>()).add(i));
        long total = 0, groom = 0, bride = 0, both = 0, archived = 0;
        Map<InvitationStatus, Long> statusCounts = new LinkedHashMap<>();
        for (InvitationStatus s : InvitationStatus.values()) statusCounts.put(s, 0L);
        for (Guest g : guests) {
            if (g.isArchived()) {
                archived++;
                continue;
            }
            total++;
            switch (g.getSide()) {
                case GROOM -> groom++;
                case BRIDE -> bride++;
                case BOTH -> both++;
            }
            InvitationStatus overall = InvitationService.overallStatus(byGuest.getOrDefault(g.getId(), List.of()));
            statusCounts.merge(overall, 1L, Long::sum);
        }
        return new GuestStats(total, groom, bride, both, archived,
                statusCounts.get(InvitationStatus.NOT_SENT), statusCounts.get(InvitationStatus.SENT),
                statusCounts.get(InvitationStatus.PENDING), statusCounts.get(InvitationStatus.CONFIRMED),
                statusCounts.get(InvitationStatus.DECLINED), statusCounts.get(InvitationStatus.ATTENDED),
                statusCounts.get(InvitationStatus.ABSENT));
    }

    @Transactional(readOnly = true)
    public DuplicateCheckResponse checkDuplicates(String phone, String fullName, Long excludeId) {
        List<GuestSummary> matches = new ArrayList<>();
        String normalizedPhone = normalizePhone(phone);
        if (normalizedPhone != null) {
            guestRepository.findActiveByPhone(normalizedPhone, excludeId).forEach(g -> matches.add(toSummary(g)));
        }
        if (fullName != null && !fullName.isBlank()) {
            guestRepository.findActiveByFullName(fullName.trim(), excludeId).stream()
                    .filter(g -> matches.stream().noneMatch(m -> m.id().equals(g.getId())))
                    .forEach(g -> matches.add(toSummary(g)));
        }
        return new DuplicateCheckResponse(!matches.isEmpty(), matches);
    }

    // ---- commands -----------------------------------------------------------

    @Transactional
    public GuestResponse create(GuestRequest request) {
        guardDuplicatePhone(request.phone(), null, Boolean.TRUE.equals(request.ignoreDuplicates()));
        Guest guest = new Guest();
        guest.setCode(nextCode());
        apply(guest, request);
        guestRepository.save(guest);
        List<InvitationInput> invitations = request.invitations();
        if (invitations == null || invitations.isEmpty()) {
            // default: invite to the main event when one exists
            invitations = eventRepository.findFirstByMainTrue()
                    .map(e -> List.of(new InvitationInput(e.getId(), 1, 1, InvitationStatus.NOT_SENT, null)))
                    .orElse(List.of());
        }
        invitationService.syncInvitations(guest, invitations);
        auditService.log("GUEST", guest.getId(), guest.getFullName(), AuditAction.CREATE,
                "Thêm khách mời " + guest.getFullName() + " (" + guest.getSide().label() + ")");
        return get(guest.getId());
    }

    @Transactional
    public GuestResponse update(Long id, GuestRequest request) {
        Guest guest = guestRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Khách mời", id));
        guardDuplicatePhone(request.phone(), id, Boolean.TRUE.equals(request.ignoreDuplicates()));
        Map<String, Object> before = snapshot(guest);
        Side previousSide = guest.getSide();
        apply(guest, request);
        guestRepository.save(guest);
        if (previousSide != guest.getSide()) {
            // keep gift statistics consistent with the guest's side
            giftRepository.findActiveByGuestId(id).forEach(g -> g.setSide(guest.getSide()));
        }
        if (request.invitations() != null) {
            invitationService.syncInvitations(guest, request.invitations());
        }
        auditService.log("GUEST", guest.getId(), guest.getFullName(), AuditAction.UPDATE,
                "Cập nhật khách mời " + guest.getFullName(), AuditService.diff(before, snapshot(guest)));
        return get(guest.getId());
    }

    /** Soft delete: the guest is archived, gifts and history are preserved. */
    @Transactional
    public void archive(Long id) {
        Guest guest = guestRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Khách mời", id));
        if (guest.isArchived()) return;
        guest.setDeletedAt(Instant.now());
        guestRepository.save(guest);
        // free the seats of an archived guest
        tableAssignmentRepository.deleteAll(tableAssignmentRepository.findByGuestIdWithTable(id));
        auditService.log("GUEST", guest.getId(), guest.getFullName(), AuditAction.DELETE,
                "Lưu trữ khách mời " + guest.getFullName());
    }

    @Transactional
    public GuestResponse restore(Long id) {
        Guest guest = guestRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Khách mời", id));
        guest.setDeletedAt(null);
        guestRepository.save(guest);
        auditService.log("GUEST", guest.getId(), guest.getFullName(), AuditAction.RESTORE,
                "Khôi phục khách mời " + guest.getFullName());
        return get(id);
    }

    /** Permanent delete, only allowed when the guest has no gift history. */
    @Transactional
    public void hardDelete(Long id) {
        Guest guest = guestRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Khách mời", id));
        if (giftRepository.countByGuestIdAndDeletedAtIsNull(id) > 0) {
            throw new BusinessException("HAS_GIFTS",
                    "Khách này đã có lịch sử tiền mừng nên không thể xóa vĩnh viễn. Hãy dùng chức năng lưu trữ.");
        }
        String name = guest.getFullName();
        guestRepository.delete(guest);
        auditService.log("GUEST", id, name, AuditAction.DELETE, "Xóa vĩnh viễn khách mời " + name);
    }

    // ---- helpers ------------------------------------------------------------

    private void guardDuplicatePhone(String phone, Long excludeId, boolean ignore) {
        if (ignore) return;
        String normalized = normalizePhone(phone);
        if (normalized == null) return;
        List<Guest> dup = guestRepository.findActiveByPhone(normalized, excludeId);
        if (!dup.isEmpty()) {
            Map<String, Object> details = new HashMap<>();
            details.put("matches", dup.stream().map(GuestService::toSummary).toList());
            throw BusinessException.conflict("DUPLICATE_PHONE",
                    "Số điện thoại này đã tồn tại cho khách \"" + dup.get(0).getFullName() + "\"", details);
        }
    }

    public static String normalizePhone(String phone) {
        if (phone == null) return null;
        String p = phone.replace(" ", "").trim();
        return p.isEmpty() ? null : p;
    }

    private void apply(Guest guest, GuestRequest r) {
        guest.setFullName(r.fullName().trim());
        guest.setGender(r.gender());
        guest.setPhone(normalizePhone(r.phone()));
        guest.setEmail(r.email() == null || r.email().isBlank() ? null : r.email().trim());
        guest.setAddress(r.address());
        guest.setSide(r.side());
        guest.setRelationship(r.relationship());
        guest.setCategory(r.category() == null ? GuestCategory.OTHER : r.category());
        guest.setGroup(r.groupId() == null ? null : guestGroupService.find(r.groupId()));
        guest.setContactPerson(r.contactPerson());
        guest.setVip(Boolean.TRUE.equals(r.vip()));
        guest.setNote(r.note());
    }

    public String nextCode() {
        return String.format("KM-%04d", guestRepository.nextCodeSequence());
    }

    private Map<String, Object> snapshot(Guest g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fullName", g.getFullName());
        m.put("phone", g.getPhone());
        m.put("side", g.getSide());
        m.put("category", g.getCategory());
        m.put("relationship", g.getRelationship());
        m.put("group", g.getGroup() != null ? g.getGroup().getName() : null);
        m.put("address", g.getAddress());
        m.put("vip", g.isVip());
        return m;
    }

    private Sort sort(String sortBy, String direction) {
        Set<String> allowed = Set.of("fullName", "createdAt", "code", "side", "category", "updatedAt");
        String property = allowed.contains(sortBy) ? sortBy : "fullName";
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, property).and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private Specification<Guest> specification(GuestFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (Boolean.TRUE.equals(f.archived())) {
                predicates.add(cb.isNotNull(root.get("deletedAt")));
            } else {
                predicates.add(cb.isNull(root.get("deletedAt")));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.coalesce(root.get("phone"), ""), "%" + f.q().trim() + "%"),
                        cb.like(cb.lower(root.get("code")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("relationship"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("address"), "")), like)));
            }
            if (f.side() != null) predicates.add(cb.equal(root.get("side"), f.side()));
            if (f.groupId() != null) predicates.add(cb.equal(root.get("group").get("id"), f.groupId()));
            if (f.category() != null) predicates.add(cb.equal(root.get("category"), f.category()));
            if (f.vip() != null) predicates.add(cb.equal(root.get("vip"), f.vip()));

            if (f.status() != null || f.eventId() != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Invitation> inv = sub.from(Invitation.class);
                List<Predicate> subPreds = new ArrayList<>();
                subPreds.add(cb.equal(inv.get("guest"), root));
                if (f.eventId() != null) subPreds.add(cb.equal(inv.get("event").get("id"), f.eventId()));
                if (f.status() != null) subPreds.add(cb.equal(inv.get("status"), f.status()));
                sub.select(inv.get("id")).where(subPreds.toArray(new Predicate[0]));
                if (f.status() == InvitationStatus.NOT_SENT && f.eventId() == null) {
                    // guests with no invitation at all also count as "not sent"
                    Subquery<Long> any = query.subquery(Long.class);
                    Root<Invitation> anyInv = any.from(Invitation.class);
                    any.select(anyInv.get("id")).where(cb.equal(anyInv.get("guest"), root));
                    predicates.add(cb.or(cb.exists(sub), cb.not(cb.exists(any))));
                } else {
                    predicates.add(cb.exists(sub));
                }
            }
            if (Boolean.TRUE.equals(f.unassigned()) && f.eventId() != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<TableAssignment> ta = sub.from(TableAssignment.class);
                Join<TableAssignment, WeddingTable> table = ta.join("table", JoinType.INNER);
                sub.select(ta.get("id")).where(cb.equal(ta.get("guest"), root), cb.equal(table.get("event").get("id"), f.eventId()));
                predicates.add(cb.not(cb.exists(sub)));
            }
            if (f.hasGift() != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Gift> gift = sub.from(Gift.class);
                sub.select(gift.get("id")).where(cb.equal(gift.get("guest"), root), cb.isNull(gift.get("deletedAt")));
                predicates.add(f.hasGift() ? cb.exists(sub) : cb.not(cb.exists(sub)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Builds full responses for a list of guests with a constant number of queries.
     */
    @Transactional(readOnly = true)
    public List<GuestResponse> assemble(List<Guest> guests) {
        if (guests.isEmpty()) return List.of();
        List<Long> ids = guests.stream().map(Guest::getId).toList();
        Map<Long, Guest> withGroup = new HashMap<>();
        guestRepository.findAllWithGroupByIdIn(ids).forEach(g -> withGroup.put(g.getId(), g));

        Map<Long, List<Invitation>> invByGuest = new HashMap<>();
        invitationRepository.findByGuestIdInWithEvent(ids)
                .forEach(i -> invByGuest.computeIfAbsent(i.getGuest().getId(), k -> new ArrayList<>()).add(i));

        List<TableAssignment> assignments = tableAssignmentRepository.findByGuestIdInWithTable(ids);
        Map<Long, List<TableAssignment>> tablesByGuest = new HashMap<>();
        assignments.forEach(a -> tablesByGuest.computeIfAbsent(a.getGuest().getId(), k -> new ArrayList<>()).add(a));

        Map<Long, BigDecimal> giftTotal = new HashMap<>();
        Map<Long, Long> giftCount = new HashMap<>();
        for (Object[] row : giftRepository.sumByGuestIds(ids)) {
            Long gid = (Long) row[0];
            giftTotal.put(gid, row[1] == null ? BigDecimal.ZERO : new BigDecimal(row[1].toString()));
            giftCount.put(gid, ((Number) row[2]).longValue());
        }

        List<GuestResponse> out = new ArrayList<>(guests.size());
        for (Guest g0 : guests) {
            Guest g = withGroup.getOrDefault(g0.getId(), g0);
            List<Invitation> invitations = invByGuest.getOrDefault(g.getId(), List.of());
            invitations.sort((a, b) -> a.getEvent().getEventDate().compareTo(b.getEvent().getEventDate()));
            List<TableAssignment> tables = tablesByGuest.getOrDefault(g.getId(), List.of());
            List<InvitationResponse> invResponses = InvitationService.toResponses(invitations, tables);
            int invited = 0, expected = 0, confirmed = 0, actual = 0;
            for (Invitation i : invitations) {
                invited += i.getInvitedCount();
                expected += i.getExpectedCount();
                confirmed += i.getConfirmedCount();
                actual += i.getActualCount();
            }
            GuestGroup group = g.getGroup();
            out.add(new GuestResponse(g.getId(), g.getCode(), g.getFullName(), g.getGender(), g.getPhone(), g.getEmail(),
                    g.getAddress(), g.getSide(), g.getRelationship(), g.getCategory(),
                    group != null ? group.getId() : null, group != null ? group.getName() : null,
                    g.getContactPerson(), g.isVip(), g.getNote(),
                    InvitationService.overallStatus(invitations), invited, expected, confirmed, actual,
                    giftTotal.getOrDefault(g.getId(), BigDecimal.ZERO), giftCount.getOrDefault(g.getId(), 0L),
                    invResponses,
                    tables.stream().map(a -> new TableInfo(a.getTable().getId(), a.getTable().getName(),
                            a.getTable().getEvent().getId(), a.getTable().getEvent().getName(), a.getSeats())).toList(),
                    g.isArchived(), g.getCreatedAt(), g.getUpdatedAt()));
        }
        return out;
    }

    public static GuestSummary toSummary(Guest g) {
        return new GuestSummary(g.getId(), g.getCode(), g.getFullName(), g.getPhone(), g.getSide(),
                g.getGroup() != null ? g.getGroup().getName() : null);
    }

    @Transactional(readOnly = true)
    public List<WeddingEvent> events() {
        return eventRepository.findAllByOrderByEventDateAscSortOrderAsc();
    }
}
