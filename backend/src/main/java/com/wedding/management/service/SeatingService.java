package com.wedding.management.service;

import com.wedding.management.common.exception.BusinessException;
import com.wedding.management.common.exception.ResourceNotFoundException;
import com.wedding.management.domain.entity.Guest;
import com.wedding.management.domain.entity.Invitation;
import com.wedding.management.domain.entity.TableArea;
import com.wedding.management.domain.entity.TableAssignment;
import com.wedding.management.domain.entity.WeddingEvent;
import com.wedding.management.domain.entity.WeddingTable;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.SeatingDtos.AreaRequest;
import com.wedding.management.dto.SeatingDtos.AreaResponse;
import com.wedding.management.dto.SeatingDtos.AssignRequest;
import com.wedding.management.dto.SeatingDtos.BulkCreateTablesRequest;
import com.wedding.management.dto.SeatingDtos.MoveRequest;
import com.wedding.management.dto.SeatingDtos.SeatLocation;
import com.wedding.management.dto.SeatingDtos.SeatResponse;
import com.wedding.management.dto.SeatingDtos.SeatingOverview;
import com.wedding.management.dto.SeatingDtos.TableRequest;
import com.wedding.management.dto.SeatingDtos.TableResponse;
import com.wedding.management.dto.SeatingDtos.UnassignedGuest;
import com.wedding.management.repository.GuestRepository;
import com.wedding.management.repository.InvitationRepository;
import com.wedding.management.repository.TableAreaRepository;
import com.wedding.management.repository.TableAssignmentRepository;
import com.wedding.management.repository.WeddingEventRepository;
import com.wedding.management.repository.WeddingTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SeatingService {

    private final WeddingEventRepository eventRepository;
    private final TableAreaRepository areaRepository;
    private final WeddingTableRepository tableRepository;
    private final TableAssignmentRepository assignmentRepository;
    private final InvitationRepository invitationRepository;
    private final GuestRepository guestRepository;
    private final AuditService auditService;

    // ---- overview -----------------------------------------------------------

    @Transactional(readOnly = true)
    public SeatingOverview overview(Long eventId, Side side, String q) {
        WeddingEvent event = eventRepository.findById(eventId).orElseThrow(() -> ResourceNotFoundException.of("Sự kiện", eventId));
        List<TableArea> areas = areaRepository.findByEventIdOrderBySortOrderAscNameAsc(eventId);
        List<WeddingTable> tables = tableRepository.findByEventIdWithArea(eventId);
        List<TableAssignment> assignments = assignmentRepository.findByEventIdWithGuest(eventId);
        Map<Long, Invitation> invByGuest = new HashMap<>();
        invitationRepository.findByEventIdWithGuest(eventId).forEach(i -> invByGuest.put(i.getGuest().getId(), i));

        Map<Long, List<TableAssignment>> byTable = new HashMap<>();
        assignments.forEach(a -> byTable.computeIfAbsent(a.getTable().getId(), k -> new ArrayList<>()).add(a));
        Map<Long, Long> tableCountByArea = new HashMap<>();
        tables.forEach(t -> {
            if (t.getArea() != null) tableCountByArea.merge(t.getArea().getId(), 1L, Long::sum);
        });

        String needle = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        List<TableResponse> tableResponses = new ArrayList<>();
        int totalSeats = 0, seatsTaken = 0;
        Set<Long> seatedGuests = new HashSet<>();
        for (WeddingTable t : tables) {
            List<TableAssignment> seats = byTable.getOrDefault(t.getId(), List.of());
            totalSeats += t.getCapacity();
            int taken = seats.stream().mapToInt(TableAssignment::getSeats).sum();
            seatsTaken += taken;
            seats.forEach(a -> seatedGuests.add(a.getGuest().getId()));
            boolean matchesSide = side == null || t.getSide() == side || t.getSide() == Side.BOTH
                    || seats.stream().anyMatch(a -> a.getGuest().getSide() == side);
            boolean matchesQuery = needle == null || t.getName().toLowerCase().contains(needle)
                    || seats.stream().anyMatch(a -> a.getGuest().getFullName().toLowerCase().contains(needle));
            if (matchesSide && matchesQuery) {
                tableResponses.add(toTableResponse(t, seats, invByGuest));
            }
        }
        long unassigned = invByGuest.values().stream()
                .filter(i -> i.getStatus() != InvitationStatus.DECLINED)
                .filter(i -> !seatedGuests.contains(i.getGuest().getId())).count();

        List<AreaResponse> areaResponses = areas.stream().map(a -> new AreaResponse(a.getId(), eventId, a.getName(),
                a.getDescription(), a.getSortOrder(), tableCountByArea.getOrDefault(a.getId(), 0L))).toList();
        return new SeatingOverview(eventId, event.getName(), tables.size(), totalSeats, seatsTaken, seatedGuests.size(),
                unassigned, areaResponses, tableResponses);
    }

    @Transactional(readOnly = true)
    public List<UnassignedGuest> unassigned(Long eventId, Side side, String q) {
        List<Invitation> invitations = invitationRepository.findByEventIdWithGuest(eventId);
        Set<Long> seated = new HashSet<>();
        assignmentRepository.findByEventIdWithGuest(eventId).forEach(a -> seated.add(a.getGuest().getId()));
        String needle = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        return invitations.stream()
                .filter(i -> !seated.contains(i.getGuest().getId()))
                .filter(i -> i.getStatus() != InvitationStatus.DECLINED)
                .filter(i -> side == null || i.getGuest().getSide() == side)
                .filter(i -> needle == null || i.getGuest().getFullName().toLowerCase().contains(needle)
                        || (i.getGuest().getPhone() != null && i.getGuest().getPhone().contains(needle)))
                .sorted((a, b) -> a.getGuest().getFullName().compareToIgnoreCase(b.getGuest().getFullName()))
                .map(i -> {
                    Guest g = i.getGuest();
                    return new UnassignedGuest(g.getId(), g.getCode(), g.getFullName(), g.getPhone(), g.getSide(),
                            g.getGroup() != null ? g.getGroup().getName() : null, i.getStatus(), i.getExpectedCount(), i.getConfirmedCount());
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<SeatLocation> find(Long eventId, String q) {
        if (q == null || q.isBlank()) return List.of();
        String needle = q.trim().toLowerCase();
        return assignmentRepository.findByEventIdWithGuest(eventId).stream()
                .filter(a -> a.getGuest().getFullName().toLowerCase().contains(needle)
                        || (a.getGuest().getPhone() != null && a.getGuest().getPhone().contains(needle)))
                .map(a -> new SeatLocation(a.getGuest().getId(), a.getGuest().getFullName(), a.getTable().getId(),
                        a.getTable().getName(), a.getTable().getArea() != null ? a.getTable().getArea().getName() : null, a.getSeats()))
                .toList();
    }

    // ---- areas --------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AreaResponse> areas(Long eventId) {
        return areaRepository.findByEventIdOrderBySortOrderAscNameAsc(eventId).stream()
                .map(a -> new AreaResponse(a.getId(), eventId, a.getName(), a.getDescription(), a.getSortOrder(), 0)).toList();
    }

    @Transactional
    public AreaResponse createArea(AreaRequest request) {
        WeddingEvent event = eventRepository.findById(request.eventId()).orElseThrow(() -> ResourceNotFoundException.of("Sự kiện", request.eventId()));
        TableArea area = new TableArea();
        area.setEvent(event);
        area.setName(request.name().trim());
        area.setDescription(request.description());
        area.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        areaRepository.save(area);
        return new AreaResponse(area.getId(), event.getId(), area.getName(), area.getDescription(), area.getSortOrder(), 0);
    }

    @Transactional
    public AreaResponse updateArea(Long id, AreaRequest request) {
        TableArea area = areaRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Khu vực", id));
        area.setName(request.name().trim());
        area.setDescription(request.description());
        if (request.sortOrder() != null) area.setSortOrder(request.sortOrder());
        areaRepository.save(area);
        return new AreaResponse(area.getId(), area.getEvent().getId(), area.getName(), area.getDescription(), area.getSortOrder(), 0);
    }

    @Transactional
    public void deleteArea(Long id) {
        TableArea area = areaRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Khu vực", id));
        // tables stay, they just lose their area
        tableRepository.findByEventIdWithArea(area.getEvent().getId()).stream()
                .filter(t -> t.getArea() != null && t.getArea().getId().equals(id))
                .forEach(t -> t.setArea(null));
        areaRepository.delete(area);
    }

    // ---- tables -------------------------------------------------------------

    @Transactional
    public TableResponse createTable(TableRequest request) {
        WeddingEvent event = eventRepository.findById(request.eventId()).orElseThrow(() -> ResourceNotFoundException.of("Sự kiện", request.eventId()));
        tableRepository.findByEventIdAndNameIgnoreCase(event.getId(), request.name().trim()).ifPresent(t -> {
            throw BusinessException.conflict("Đã có bàn tên \"" + request.name().trim() + "\" trong sự kiện này");
        });
        WeddingTable table = new WeddingTable();
        table.setEvent(event);
        applyTable(table, request);
        tableRepository.save(table);
        auditService.log("TABLE", table.getId(), table.getName(), AuditAction.CREATE, "Tạo bàn " + table.getName());
        return toTableResponse(table, List.of(), Map.of());
    }

    @Transactional
    public List<TableResponse> bulkCreate(BulkCreateTablesRequest request) {
        WeddingEvent event = eventRepository.findById(request.eventId()).orElseThrow(() -> ResourceNotFoundException.of("Sự kiện", request.eventId()));
        TableArea area = request.areaId() == null ? null : areaRepository.findById(request.areaId()).orElse(null);
        long existing = tableRepository.countByEventId(event.getId());
        List<TableResponse> out = new ArrayList<>();
        int created = 0, index = 1;
        while (created < request.count() && index < 1000) {
            String name = String.format("%s %02d", request.prefix().trim(), index++);
            if (tableRepository.findByEventIdAndNameIgnoreCase(event.getId(), name).isPresent()) continue;
            WeddingTable table = new WeddingTable();
            table.setEvent(event);
            table.setArea(area);
            table.setName(name);
            table.setCapacity(request.capacity());
            table.setSide(request.side() == null ? Side.BOTH : request.side());
            table.setSortOrder((int) existing + created);
            tableRepository.save(table);
            out.add(toTableResponse(table, List.of(), Map.of()));
            created++;
        }
        auditService.log("TABLE", null, null, AuditAction.CREATE, "Tạo nhanh " + created + " bàn cho " + event.getName());
        return out;
    }

    @Transactional
    public TableResponse updateTable(Long id, TableRequest request) {
        WeddingTable table = tableRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Bàn", id));
        tableRepository.findByEventIdAndNameIgnoreCase(table.getEvent().getId(), request.name().trim())
                .filter(t -> !t.getId().equals(id)).ifPresent(t -> {
                    throw BusinessException.conflict("Đã có bàn tên \"" + request.name().trim() + "\" trong sự kiện này");
                });
        int taken = assignmentRepository.sumSeatsByTableId(id);
        if (request.capacity() < taken) {
            throw new BusinessException("Bàn đang có " + taken + " người, không thể giảm số chỗ xuống " + request.capacity());
        }
        applyTable(table, request);
        tableRepository.save(table);
        auditService.log("TABLE", table.getId(), table.getName(), AuditAction.UPDATE, "Cập nhật bàn " + table.getName());
        return tableDetail(id);
    }

    @Transactional
    public void deleteTable(Long id) {
        WeddingTable table = tableRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Bàn", id));
        String name = table.getName();
        tableRepository.delete(table);
        auditService.log("TABLE", id, name, AuditAction.DELETE, "Xóa bàn " + name);
    }

    @Transactional(readOnly = true)
    public TableResponse tableDetail(Long id) {
        WeddingTable table = tableRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Bàn", id));
        List<TableAssignment> seats = assignmentRepository.findByEventIdWithGuest(table.getEvent().getId()).stream()
                .filter(a -> a.getTable().getId().equals(id)).toList();
        Map<Long, Invitation> invByGuest = new HashMap<>();
        invitationRepository.findByEventIdWithGuest(table.getEvent().getId()).forEach(i -> invByGuest.put(i.getGuest().getId(), i));
        return toTableResponse(table, seats, invByGuest);
    }

    // ---- assignments --------------------------------------------------------

    @Transactional
    public TableResponse assign(Long tableId, AssignRequest request) {
        WeddingTable table = tableRepository.findById(tableId).orElseThrow(() -> ResourceNotFoundException.of("Bàn", tableId));
        Guest guest = guestRepository.findById(request.guestId()).orElseThrow(() -> ResourceNotFoundException.of("Khách mời", request.guestId()));
        Long eventId = table.getEvent().getId();
        Invitation invitation = invitationRepository.findByGuestIdAndEventId(guest.getId(), eventId).orElse(null);
        if (invitation == null) {
            throw new BusinessException("NOT_INVITED", "Khách \"" + guest.getFullName() + "\" chưa được mời tới sự kiện này");
        }
        assignmentRepository.findByGuestIdAndEventId(guest.getId(), eventId).ifPresent(existing -> {
            throw BusinessException.conflict("ALREADY_SEATED",
                    "Khách đã được xếp ở bàn " + existing.getTable().getName() + ". Hãy dùng chức năng chuyển bàn.",
                    Map.of("tableId", existing.getTable().getId(), "tableName", existing.getTable().getName()));
        });
        int seats = request.seats() != null ? request.seats()
                : Math.max(1, invitation.getConfirmedCount() > 0 ? invitation.getConfirmedCount() : invitation.getExpectedCount());
        ensureCapacity(table, seats, Boolean.TRUE.equals(request.force()));

        TableAssignment a = new TableAssignment();
        a.setTable(table);
        a.setGuest(guest);
        a.setSeats(seats);
        a.setNote(request.note());
        assignmentRepository.save(a);
        auditService.log("GUEST", guest.getId(), guest.getFullName(), AuditAction.ASSIGN,
                "Xếp " + guest.getFullName() + " vào bàn " + table.getName() + " (" + seats + " chỗ)");
        return tableDetail(tableId);
    }

    @Transactional
    public TableResponse move(Long assignmentId, MoveRequest request) {
        TableAssignment a = assignmentRepository.findById(assignmentId).orElseThrow(() -> ResourceNotFoundException.of("Vị trí ngồi", assignmentId));
        WeddingTable target = tableRepository.findById(request.tableId()).orElseThrow(() -> ResourceNotFoundException.of("Bàn", request.tableId()));
        if (!target.getEvent().getId().equals(a.getTable().getEvent().getId())) {
            throw new BusinessException("Chỉ có thể chuyển bàn trong cùng một sự kiện");
        }
        if (target.getId().equals(a.getTable().getId())) return tableDetail(target.getId());
        ensureCapacity(target, a.getSeats(), Boolean.TRUE.equals(request.force()));
        String from = a.getTable().getName();
        a.setTable(target);
        assignmentRepository.save(a);
        auditService.log("GUEST", a.getGuest().getId(), a.getGuest().getFullName(), AuditAction.ASSIGN,
                "Chuyển " + a.getGuest().getFullName() + " từ bàn " + from + " sang bàn " + target.getName());
        return tableDetail(target.getId());
    }

    @Transactional
    public TableResponse updateSeats(Long assignmentId, int seats, boolean force) {
        TableAssignment a = assignmentRepository.findById(assignmentId).orElseThrow(() -> ResourceNotFoundException.of("Vị trí ngồi", assignmentId));
        int others = assignmentRepository.sumSeatsByTableId(a.getTable().getId()) - a.getSeats();
        if (!force && others + seats > a.getTable().getCapacity()) {
            throw BusinessException.conflict("OVER_CAPACITY", "Bàn " + a.getTable().getName() + " chỉ còn "
                    + (a.getTable().getCapacity() - others) + " chỗ trống", Map.of("available", a.getTable().getCapacity() - others));
        }
        a.setSeats(Math.max(1, seats));
        assignmentRepository.save(a);
        return tableDetail(a.getTable().getId());
    }

    @Transactional
    public void unassign(Long assignmentId) {
        TableAssignment a = assignmentRepository.findById(assignmentId).orElseThrow(() -> ResourceNotFoundException.of("Vị trí ngồi", assignmentId));
        assignmentRepository.delete(a);
        auditService.log("GUEST", a.getGuest().getId(), a.getGuest().getFullName(), AuditAction.UNASSIGN,
                "Bỏ " + a.getGuest().getFullName() + " khỏi bàn " + a.getTable().getName());
    }

    private void ensureCapacity(WeddingTable table, int seats, boolean force) {
        int taken = assignmentRepository.sumSeatsByTableId(table.getId());
        int available = table.getCapacity() - taken;
        if (seats > available && !force) {
            throw BusinessException.conflict("OVER_CAPACITY",
                    "Bàn " + table.getName() + " chỉ còn " + Math.max(available, 0) + " chỗ trống (cần " + seats + "). Xác nhận để xếp vượt sức chứa.",
                    Map.of("available", Math.max(available, 0), "requested", seats, "capacity", table.getCapacity()));
        }
    }

    private void applyTable(WeddingTable table, TableRequest r) {
        table.setArea(r.areaId() == null ? null : areaRepository.findById(r.areaId()).orElse(null));
        table.setName(r.name().trim());
        table.setCapacity(r.capacity());
        table.setSide(r.side() == null ? Side.BOTH : r.side());
        if (r.positionX() != null) table.setPositionX(r.positionX());
        if (r.positionY() != null) table.setPositionY(r.positionY());
        if (r.sortOrder() != null) table.setSortOrder(r.sortOrder());
        table.setNote(r.note());
    }

    private TableResponse toTableResponse(WeddingTable t, List<TableAssignment> seats, Map<Long, Invitation> invByGuest) {
        int taken = seats.stream().mapToInt(TableAssignment::getSeats).sum();
        List<SeatResponse> seatResponses = seats.stream()
                .sorted((a, b) -> a.getGuest().getFullName().compareToIgnoreCase(b.getGuest().getFullName()))
                .map(a -> {
                    Guest g = a.getGuest();
                    Invitation inv = invByGuest.get(g.getId());
                    return new SeatResponse(a.getId(), g.getId(), g.getCode(), g.getFullName(), g.getPhone(), g.getSide(),
                            g.getGroup() != null ? g.getGroup().getName() : null, a.getSeats(),
                            inv != null ? inv.getStatus() : null, inv != null && inv.isCheckedIn(), a.getNote());
                }).toList();
        return new TableResponse(t.getId(), t.getEvent().getId(), t.getArea() != null ? t.getArea().getId() : null,
                t.getArea() != null ? t.getArea().getName() : null, t.getName(), t.getCapacity(), t.getSide(),
                t.getPositionX(), t.getPositionY(), t.getSortOrder(), t.getNote(), taken, t.getCapacity() - taken, seatResponses);
    }
}
