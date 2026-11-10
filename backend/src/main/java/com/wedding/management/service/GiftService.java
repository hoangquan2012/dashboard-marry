package com.wedding.management.service;

import com.wedding.management.common.api.PageResponse;
import com.wedding.management.common.excel.ExcelUtil;
import com.wedding.management.common.exception.BusinessException;
import com.wedding.management.common.exception.ResourceNotFoundException;
import com.wedding.management.domain.entity.Gift;
import com.wedding.management.domain.entity.GiftItem;
import com.wedding.management.domain.entity.Guest;
import com.wedding.management.domain.entity.WeddingEvent;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.domain.enums.GiftType;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.GiftDtos.EventStat;
import com.wedding.management.dto.GiftDtos.GiftItemRequest;
import com.wedding.management.dto.GiftDtos.GiftItemResponse;
import com.wedding.management.dto.GiftDtos.GiftRequest;
import com.wedding.management.dto.GiftDtos.GiftResponse;
import com.wedding.management.dto.GiftDtos.GiftStats;
import com.wedding.management.dto.GiftDtos.InKindSummary;
import com.wedding.management.dto.GiftDtos.SideStat;
import com.wedding.management.dto.GiftDtos.TypeStat;
import com.wedding.management.repository.GiftRepository;
import com.wedding.management.repository.GuestRepository;
import com.wedding.management.repository.WeddingEventRepository;
import com.wedding.management.security.CurrentUser;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GiftService {

    private final GiftRepository giftRepository;
    private final GuestRepository guestRepository;
    private final WeddingEventRepository eventRepository;
    private final AuditService auditService;

    public record GiftFilter(String q, Side side, GiftType type, Long eventId, Long guestId, Long groupId,
                             LocalDate from, LocalDate to) {
    }

    // ---- queries ------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<GiftResponse> list(GiftFilter filter, int page, int size, String sortBy, String direction) {
        Set<String> allowed = Set.of("receivedAt", "amount", "createdAt", "giftType", "side");
        String property = allowed.contains(sortBy) ? sortBy : "receivedAt";
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<Gift> result = giftRepository.findAll(specification(filter),
                PageRequest.of(page, Math.min(Math.max(size, 1), 200), Sort.by(dir, property).and(Sort.by(Sort.Direction.DESC, "id"))));
        return PageResponse.of(result, result.getContent().stream().map(GiftService::toResponse).toList());
    }

    @Transactional(readOnly = true)
    public List<Gift> findAll(GiftFilter filter) {
        return giftRepository.findAll(specification(filter), Sort.by(Sort.Direction.DESC, "receivedAt", "id"));
    }

    @Transactional(readOnly = true)
    public GiftResponse get(Long id) {
        return toResponse(giftRepository.findByIdWithRelations(id).filter(g -> g.getDeletedAt() == null)
                .orElseThrow(() -> ResourceNotFoundException.of("Khoản mừng", id)));
    }

    @Transactional(readOnly = true)
    public List<GiftResponse> historyOfGuest(Long guestId) {
        return giftRepository.findActiveByGuestId(guestId).stream().map(GiftService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GiftStats stats(GiftFilter filter) {
        List<Gift> gifts = findAll(filter);
        List<Guest> activeGuests = guestRepository.findByDeletedAtIsNull();
        if (filter.side() != null) activeGuests = activeGuests.stream().filter(g -> g.getSide() == filter.side()).toList();
        return computeStats(gifts, activeGuests);
    }

    public static GiftStats computeStats(List<Gift> gifts, List<Guest> activeGuests) {
        BigDecimal total = BigDecimal.ZERO, cash = BigDecimal.ZERO, transfer = BigDecimal.ZERO, inKindValue = BigDecimal.ZERO;
        long inKindCount = 0, unknownValue = 0;
        Set<String> givers = new HashSet<>();
        Set<Long> giverGuestIds = new HashSet<>();
        Map<Long, BigDecimal> byGroup = new HashMap<>();
        Map<Side, BigDecimal> sideAmount = new LinkedHashMap<>();
        Map<Side, Long> sideCount = new LinkedHashMap<>();
        Map<Side, Set<String>> sideGivers = new LinkedHashMap<>();
        Map<GiftType, BigDecimal> typeAmount = new LinkedHashMap<>();
        Map<GiftType, Long> typeCount = new LinkedHashMap<>();
        Map<Long, BigDecimal> eventAmount = new LinkedHashMap<>();
        Map<Long, Long> eventCount = new LinkedHashMap<>();
        Map<Long, Set<String>> eventGivers = new LinkedHashMap<>();
        Map<Long, String> eventNames = new HashMap<>();
        Map<String, InKindAcc> inKind = new TreeMap<>();
        Map<String, BigDecimal> byCategory = new TreeMap<>();

        for (Gift g : gifts) {
            String giverKey = g.getGuest() != null ? "G" + g.getGuest().getId() : "N" + (g.getGiverName() == null ? g.getId() : g.getGiverName().trim().toLowerCase());
            givers.add(giverKey);
            if (g.getGuest() != null) giverGuestIds.add(g.getGuest().getId());
            BigDecimal amount = g.getAmount() == null ? BigDecimal.ZERO : g.getAmount();
            total = total.add(amount);
            switch (g.getGiftType()) {
                case CASH -> cash = cash.add(amount);
                case BANK_TRANSFER -> transfer = transfer.add(amount);
                default -> {
                    inKindCount++;
                    inKindValue = inKindValue.add(amount);
                    if (g.getAmount() == null) unknownValue++;
                }
            }
            sideAmount.merge(g.getSide(), amount, BigDecimal::add);
            sideCount.merge(g.getSide(), 1L, Long::sum);
            sideGivers.computeIfAbsent(g.getSide(), k -> new HashSet<>()).add(giverKey);
            typeAmount.merge(g.getGiftType(), amount, BigDecimal::add);
            typeCount.merge(g.getGiftType(), 1L, Long::sum);
            Long eventId = g.getEvent() == null ? 0L : g.getEvent().getId();
            eventNames.putIfAbsent(eventId, g.getEvent() == null ? "Không gắn sự kiện" : g.getEvent().getName());
            eventAmount.merge(eventId, amount, BigDecimal::add);
            eventCount.merge(eventId, 1L, Long::sum);
            eventGivers.computeIfAbsent(eventId, k -> new HashSet<>()).add(giverKey);
            if (g.getGuest() != null && g.getGuest().getGroup() != null) {
                byGroup.merge(g.getGuest().getGroup().getId(), amount, BigDecimal::add);
            }
            if (g.getGuest() != null) {
                byCategory.merge(g.getGuest().getCategory().label(), amount, BigDecimal::add);
            } else {
                byCategory.merge("Khách ngoài danh sách", amount, BigDecimal::add);
            }
            for (GiftItem item : g.getItems()) {
                String key = item.getItemName().trim() + "|" + (item.getUnit() == null ? "" : item.getUnit().trim());
                InKindAcc acc = inKind.computeIfAbsent(key, k -> new InKindAcc(item.getItemName().trim(), item.getUnit()));
                acc.quantity = acc.quantity.add(item.getQuantity() == null ? BigDecimal.ONE : item.getQuantity());
                acc.count++;
                if (item.getEstimatedValue() != null) acc.value = acc.value.add(item.getEstimatedValue());
            }
        }

        long guestsWithoutGift = activeGuests.stream().filter(g -> !giverGuestIds.contains(g.getId())).count();
        BigDecimal avgPerGiver = givers.isEmpty() ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(givers.size()), 0, RoundingMode.HALF_UP);
        BigDecimal groupTotal = byGroup.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgPerGroup = byGroup.isEmpty() ? BigDecimal.ZERO : groupTotal.divide(BigDecimal.valueOf(byGroup.size()), 0, RoundingMode.HALF_UP);

        List<SideStat> bySide = new ArrayList<>();
        for (Side s : Side.values()) {
            bySide.add(new SideStat(s, s.label(), sideCount.getOrDefault(s, 0L),
                    sideGivers.getOrDefault(s, Set.of()).size(), sideAmount.getOrDefault(s, BigDecimal.ZERO)));
        }
        List<TypeStat> byType = new ArrayList<>();
        for (GiftType t : GiftType.values()) {
            byType.add(new TypeStat(t, t.label(), typeCount.getOrDefault(t, 0L), typeAmount.getOrDefault(t, BigDecimal.ZERO)));
        }
        List<EventStat> byEvent = eventAmount.keySet().stream()
                .map(id -> new EventStat(id == 0L ? null : id, eventNames.get(id), eventCount.get(id), eventGivers.get(id).size(), eventAmount.get(id)))
                .sorted((a, b) -> b.amount().compareTo(a.amount()))
                .toList();
        List<InKindSummary> inKindItems = inKind.values().stream()
                .map(a -> new InKindSummary(a.name, a.unit, a.quantity, a.count, a.value)).toList();

        return new GiftStats(total, cash, transfer, inKindValue, gifts.size(), inKindCount, unknownValue, givers.size(),
                guestsWithoutGift, activeGuests.size(), avgPerGiver, avgPerGroup, byGroup.size(),
                bySide, byType, byEvent, inKindItems, byCategory);
    }

    private static final class InKindAcc {
        final String name;
        final String unit;
        BigDecimal quantity = BigDecimal.ZERO;
        long count = 0;
        BigDecimal value = BigDecimal.ZERO;

        InKindAcc(String name, String unit) {
            this.name = name;
            this.unit = unit;
        }
    }

    // ---- commands -----------------------------------------------------------

    @Transactional
    public GiftResponse create(GiftRequest request) {
        Gift gift = new Gift();
        apply(gift, request);
        gift.setRecordedBy(CurrentUser.username());
        giftRepository.save(gift);
        auditService.log("GIFT", gift.getId(), gift.displayGiverName(), AuditAction.CREATE,
                "Ghi nhận tiền mừng của " + gift.displayGiverName() + ": " + describe(gift));
        return get(gift.getId());
    }

    @Transactional
    public GiftResponse update(Long id, GiftRequest request) {
        Gift gift = giftRepository.findByIdWithRelations(id).filter(g -> g.getDeletedAt() == null)
                .orElseThrow(() -> ResourceNotFoundException.of("Khoản mừng", id));
        Map<String, Object> before = snapshot(gift);
        apply(gift, request);
        giftRepository.save(gift);
        auditService.log("GIFT", gift.getId(), gift.displayGiverName(), AuditAction.UPDATE,
                "Sửa khoản mừng của " + gift.displayGiverName() + ": " + describe(gift), AuditService.diff(before, snapshot(gift)));
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        Gift gift = giftRepository.findByIdWithRelations(id).filter(g -> g.getDeletedAt() == null)
                .orElseThrow(() -> ResourceNotFoundException.of("Khoản mừng", id));
        gift.setDeletedAt(Instant.now());
        giftRepository.save(gift);
        auditService.log("GIFT", gift.getId(), gift.displayGiverName(), AuditAction.DELETE,
                "Xóa khoản mừng của " + gift.displayGiverName() + ": " + describe(gift));
    }

    private void apply(Gift gift, GiftRequest r) {
        Guest guest = null;
        if (r.guestId() != null) {
            guest = guestRepository.findById(r.guestId()).orElseThrow(() -> ResourceNotFoundException.of("Khách mời", r.guestId()));
        }
        if (guest == null && (r.giverName() == null || r.giverName().isBlank())) {
            throw new BusinessException("Vui lòng chọn khách mời hoặc nhập tên người mừng");
        }
        gift.setGuest(guest);
        gift.setGiverName(guest == null ? r.giverName().trim() : null);
        Side side = guest != null ? guest.getSide() : r.side();
        if (side == null) {
            throw new BusinessException("Vui lòng chọn nhà trai hoặc nhà gái cho khoản mừng");
        }
        gift.setSide(side);
        WeddingEvent event = r.eventId() == null ? null
                : eventRepository.findById(r.eventId()).orElseThrow(() -> ResourceNotFoundException.of("Sự kiện", r.eventId()));
        gift.setEvent(event);
        gift.setGiftType(r.giftType());
        gift.setReceivedAt(r.receivedAt());
        gift.setNote(r.note());

        gift.getItems().clear();
        if (r.items() != null && !r.giftType().isMonetary()) {
            for (GiftItemRequest ir : r.items()) {
                GiftItem item = new GiftItem();
                item.setGift(gift);
                item.setItemName(ir.itemName().trim());
                item.setQuantity(ir.quantity() == null || ir.quantity().signum() <= 0 ? BigDecimal.ONE : ir.quantity());
                item.setUnit(ir.unit());
                item.setEstimatedValue(ir.estimatedValue());
                item.setNote(ir.note());
                gift.getItems().add(item);
            }
        }

        if (r.giftType().isMonetary()) {
            if (r.amount() == null || r.amount().signum() <= 0) {
                throw new BusinessException("Vui lòng nhập số tiền mừng lớn hơn 0");
            }
            gift.setAmount(r.amount().setScale(0, RoundingMode.HALF_UP));
        } else {
            // in-kind: value is optional. Use item values when provided, otherwise the optional manual value.
            BigDecimal fromItems = gift.getItems().stream().map(GiftItem::getEstimatedValue)
                    .filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean anyItemValue = gift.getItems().stream().anyMatch(i -> i.getEstimatedValue() != null);
            if (anyItemValue) {
                gift.setAmount(fromItems.setScale(0, RoundingMode.HALF_UP));
            } else if (r.amount() != null && r.amount().signum() > 0) {
                gift.setAmount(r.amount().setScale(0, RoundingMode.HALF_UP));
            } else {
                gift.setAmount(null);
            }
            if (gift.getItems().isEmpty() && (r.note() == null || r.note().isBlank())) {
                throw new BusinessException("Vui lòng mô tả hiện vật / vàng (tên, số lượng) hoặc ghi chú");
            }
        }
    }

    private static String describe(Gift g) {
        String type = g.getGiftType().label();
        String amount = g.getAmount() == null ? "chưa định giá" : String.format("%,d ₫", g.getAmount().longValue()).replace(',', '.');
        return type + " - " + amount;
    }

    private Map<String, Object> snapshot(Gift g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("giver", g.displayGiverName());
        m.put("side", g.getSide());
        m.put("event", g.getEvent() != null ? g.getEvent().getName() : null);
        m.put("giftType", g.getGiftType());
        m.put("amount", g.getAmount());
        m.put("receivedAt", g.getReceivedAt());
        m.put("items", g.getItems().stream().map(i -> i.getItemName() + " x" + i.getQuantity()).collect(Collectors.joining(", ")));
        return m;
    }

    private Specification<Gift> specification(GiftFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            Join<Gift, Guest> guest = root.join("guest", JoinType.LEFT);
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(guest.get("fullName"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("giverName"), "")), like),
                        cb.like(cb.coalesce(guest.get("phone"), ""), "%" + f.q().trim() + "%"),
                        cb.like(cb.lower(cb.coalesce(guest.get("code"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("note"), "")), like)));
            }
            if (f.side() != null) predicates.add(cb.equal(root.get("side"), f.side()));
            if (f.type() != null) predicates.add(cb.equal(root.get("giftType"), f.type()));
            if (f.eventId() != null) predicates.add(cb.equal(root.get("event").get("id"), f.eventId()));
            if (f.guestId() != null) predicates.add(cb.equal(guest.get("id"), f.guestId()));
            if (f.groupId() != null) predicates.add(cb.equal(guest.get("group").get("id"), f.groupId()));
            if (f.from() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("receivedAt"), f.from()));
            if (f.to() != null) predicates.add(cb.lessThanOrEqualTo(root.get("receivedAt"), f.to()));
            if (query != null) query.distinct(false);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static GiftResponse toResponse(Gift g) {
        Guest guest = g.getGuest();
        List<GiftItemResponse> items = g.getItems().stream()
                .map(i -> new GiftItemResponse(i.getId(), i.getItemName(), i.getQuantity(), i.getUnit(), i.getEstimatedValue(), i.getNote()))
                .toList();
        String summary = g.getItems().stream()
                .map(i -> i.getItemName() + " x" + i.getQuantity().stripTrailingZeros().toPlainString() + (i.getUnit() == null ? "" : " " + i.getUnit()))
                .collect(Collectors.joining(", "));
        return new GiftResponse(g.getId(), guest != null ? guest.getId() : null, guest != null ? guest.getCode() : null,
                g.displayGiverName(), guest != null ? guest.getPhone() : null,
                guest != null && guest.getGroup() != null ? guest.getGroup().getName() : null,
                g.getSide(), g.getEvent() != null ? g.getEvent().getId() : null, g.getEvent() != null ? g.getEvent().getName() : null,
                g.getGiftType(), g.getGiftType().label(), g.getAmount(), g.getAmount() != null, g.getReceivedAt(), g.getRecordedBy(),
                g.getNote(), items, summary, g.getCreatedAt(), g.getUpdatedAt());
    }

    // ---- export -------------------------------------------------------------

    public byte[] export(List<Gift> gifts) {
        Workbook wb = ExcelUtil.newWorkbook();
        ExcelUtil.Styles styles = ExcelUtil.styles(wb);
        Sheet sheet = wb.createSheet("Tiền mừng");
        List<String> columns = List.of("Ngày nhận", "Khách mời", "Mã KM", "Số điện thoại", "Nhóm", "Bên", "Sự kiện",
                "Hình thức", "Số tiền / Giá trị quy đổi", "Hiện vật", "Người ghi nhận", "Ghi chú");
        ExcelUtil.header(sheet, 0, styles, columns);
        int r = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (Gift g : gifts) {
            GiftResponse gr = toResponse(g);
            Row row = sheet.createRow(r++);
            ExcelUtil.date(row, 0, gr.receivedAt(), styles);
            ExcelUtil.text(row, 1, gr.giverName());
            ExcelUtil.text(row, 2, gr.guestCode());
            ExcelUtil.text(row, 3, gr.guestPhone());
            ExcelUtil.text(row, 4, gr.groupName());
            ExcelUtil.text(row, 5, gr.side().label());
            ExcelUtil.text(row, 6, gr.eventName());
            ExcelUtil.text(row, 7, gr.giftTypeLabel());
            ExcelUtil.money(row, 8, gr.amount(), styles);
            ExcelUtil.text(row, 9, gr.itemsSummary());
            ExcelUtil.text(row, 10, gr.recordedBy());
            ExcelUtil.text(row, 11, gr.note());
            if (gr.amount() != null) total = total.add(gr.amount());
        }
        Row totalRow = sheet.createRow(r + 1);
        ExcelUtil.bold(totalRow, 7, "Tổng cộng", styles);
        ExcelUtil.boldMoney(totalRow, 8, total, styles);
        ExcelUtil.autosize(sheet, columns.size());
        return ExcelUtil.toBytes(wb);
    }
}
