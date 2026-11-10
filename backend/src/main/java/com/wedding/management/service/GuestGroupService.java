package com.wedding.management.service;

import com.wedding.management.common.exception.BusinessException;
import com.wedding.management.common.exception.ResourceNotFoundException;
import com.wedding.management.domain.entity.Gift;
import com.wedding.management.domain.entity.Guest;
import com.wedding.management.domain.entity.GuestGroup;
import com.wedding.management.domain.entity.Invitation;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.GuestDtos.GuestGroupRequest;
import com.wedding.management.dto.GuestDtos.GuestGroupResponse;
import com.wedding.management.repository.GiftRepository;
import com.wedding.management.repository.GuestGroupRepository;
import com.wedding.management.repository.GuestRepository;
import com.wedding.management.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GuestGroupService {

    private final GuestGroupRepository groupRepository;
    private final GuestRepository guestRepository;
    private final InvitationRepository invitationRepository;
    private final GiftRepository giftRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<GuestGroupResponse> list(Side side, String q) {
        List<GuestGroup> groups = groupRepository.findByDeletedAtIsNullOrderByNameAsc();
        if (side != null) groups = groups.stream().filter(g -> g.getSide() == side).toList();
        if (q != null && !q.isBlank()) {
            String needle = q.trim().toLowerCase();
            groups = groups.stream().filter(g -> g.getName().toLowerCase().contains(needle)
                    || (g.getRepresentativeName() != null && g.getRepresentativeName().toLowerCase().contains(needle))).toList();
        }
        return withStats(groups);
    }

    @Transactional(readOnly = true)
    public GuestGroupResponse get(Long id) {
        GuestGroup group = find(id);
        return withStats(List.of(group)).get(0);
    }

    @Transactional
    public GuestGroupResponse create(GuestGroupRequest request) {
        groupRepository.findByNameIgnoreCaseAndDeletedAtIsNull(request.name().trim()).ifPresent(g -> {
            throw BusinessException.conflict("Đã tồn tại nhóm khách có tên \"" + request.name().trim() + "\"");
        });
        GuestGroup group = new GuestGroup();
        apply(group, request);
        groupRepository.save(group);
        auditService.log("GUEST_GROUP", group.getId(), group.getName(), AuditAction.CREATE, "Tạo nhóm khách " + group.getName());
        return withStats(List.of(group)).get(0);
    }

    @Transactional
    public GuestGroupResponse update(Long id, GuestGroupRequest request) {
        GuestGroup group = find(id);
        groupRepository.findByNameIgnoreCaseAndDeletedAtIsNull(request.name().trim())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(g -> {
                    throw BusinessException.conflict("Đã tồn tại nhóm khách có tên \"" + request.name().trim() + "\"");
                });
        apply(group, request);
        groupRepository.save(group);
        auditService.log("GUEST_GROUP", group.getId(), group.getName(), AuditAction.UPDATE, "Cập nhật nhóm khách " + group.getName());
        return withStats(List.of(group)).get(0);
    }

    @Transactional
    public void delete(Long id) {
        GuestGroup group = find(id);
        // members keep existing, they simply become ungrouped
        List<Guest> members = guestRepository.findByGroupIdAndDeletedAtIsNull(id);
        members.forEach(m -> m.setGroup(null));
        guestRepository.saveAll(members);
        group.setDeletedAt(Instant.now());
        groupRepository.save(group);
        auditService.log("GUEST_GROUP", group.getId(), group.getName(), AuditAction.DELETE, "Xóa nhóm khách " + group.getName());
    }

    public GuestGroup find(Long id) {
        return groupRepository.findById(id).filter(g -> g.getDeletedAt() == null)
                .orElseThrow(() -> ResourceNotFoundException.of("Nhóm khách", id));
    }

    private void apply(GuestGroup group, GuestGroupRequest r) {
        group.setName(r.name().trim());
        group.setSide(r.side());
        group.setCategory(r.category());
        group.setRepresentativeName(r.representativeName());
        group.setRepresentativePhone(r.representativePhone());
        group.setDescription(r.description());
    }

    private List<GuestGroupResponse> withStats(List<GuestGroup> groups) {
        if (groups.isEmpty()) return List.of();
        List<Guest> guests = guestRepository.findByDeletedAtIsNull();
        Map<Long, List<Guest>> byGroup = new HashMap<>();
        for (Guest g : guests) {
            if (g.getGroup() != null) byGroup.computeIfAbsent(g.getGroup().getId(), k -> new ArrayList<>()).add(g);
        }
        List<Invitation> invitations = invitationRepository.findAllActiveWithGuestAndEvent();
        Map<Long, int[]> invByGuest = new HashMap<>();
        for (Invitation i : invitations) {
            int[] acc = invByGuest.computeIfAbsent(i.getGuest().getId(), k -> new int[3]);
            acc[0] += i.getInvitedCount();
            acc[1] += i.getConfirmedCount();
            acc[2] += i.getActualCount();
        }
        List<Gift> gifts = giftRepository.findAllActiveWithRelations();
        Map<Long, BigDecimal> giftByGuest = new HashMap<>();
        Map<Long, Long> giftCountByGuest = new HashMap<>();
        for (Gift gift : gifts) {
            if (gift.getGuest() == null) continue;
            Long gid = gift.getGuest().getId();
            giftCountByGuest.merge(gid, 1L, Long::sum);
            if (gift.getAmount() != null) giftByGuest.merge(gid, gift.getAmount(), BigDecimal::add);
        }

        List<GuestGroupResponse> out = new ArrayList<>();
        for (GuestGroup group : groups) {
            List<Guest> members = byGroup.getOrDefault(group.getId(), List.of());
            int invited = 0, confirmed = 0, actual = 0;
            BigDecimal giftTotal = BigDecimal.ZERO;
            long giftCount = 0;
            for (Guest m : members) {
                int[] acc = invByGuest.get(m.getId());
                if (acc != null) {
                    invited += acc[0];
                    confirmed += acc[1];
                    actual += acc[2];
                }
                giftTotal = giftTotal.add(giftByGuest.getOrDefault(m.getId(), BigDecimal.ZERO));
                giftCount += giftCountByGuest.getOrDefault(m.getId(), 0L);
            }
            out.add(new GuestGroupResponse(group.getId(), group.getName(), group.getSide(), group.getCategory(),
                    group.getRepresentativeName(), group.getRepresentativePhone(), group.getDescription(),
                    members.size(), invited, confirmed, actual, giftTotal, giftCount, group.getCreatedAt()));
        }
        return out;
    }
}
