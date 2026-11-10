package com.wedding.management.web;

import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.SeatingDtos.AreaRequest;
import com.wedding.management.dto.SeatingDtos.AreaResponse;
import com.wedding.management.dto.SeatingDtos.AssignRequest;
import com.wedding.management.dto.SeatingDtos.BulkCreateTablesRequest;
import com.wedding.management.dto.SeatingDtos.MoveRequest;
import com.wedding.management.dto.SeatingDtos.SeatLocation;
import com.wedding.management.dto.SeatingDtos.SeatingOverview;
import com.wedding.management.dto.SeatingDtos.TableRequest;
import com.wedding.management.dto.SeatingDtos.TableResponse;
import com.wedding.management.dto.SeatingDtos.UnassignedGuest;
import com.wedding.management.service.SeatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seating")
@RequiredArgsConstructor
@Tag(name = "Seating", description = "Sơ đồ bàn tiệc")
public class SeatingController {

    private final SeatingService seatingService;

    @GetMapping("/overview")
    @Operation(summary = "Sơ đồ bàn của một sự kiện (khu vực, bàn, khách đã xếp)")
    public SeatingOverview overview(@RequestParam Long eventId,
                                    @RequestParam(required = false) Side side,
                                    @RequestParam(required = false) String q) {
        return seatingService.overview(eventId, side, q);
    }

    @GetMapping("/unassigned")
    @Operation(summary = "Khách được mời nhưng chưa xếp bàn")
    public List<UnassignedGuest> unassigned(@RequestParam Long eventId,
                                            @RequestParam(required = false) Side side,
                                            @RequestParam(required = false) String q) {
        return seatingService.unassigned(eventId, side, q);
    }

    @GetMapping("/find")
    @Operation(summary = "Tìm khách đang ngồi ở bàn nào")
    public List<SeatLocation> find(@RequestParam Long eventId, @RequestParam String q) {
        return seatingService.find(eventId, q);
    }

    // areas
    @GetMapping("/areas")
    public List<AreaResponse> areas(@RequestParam Long eventId) {
        return seatingService.areas(eventId);
    }

    @PostMapping("/areas")
    @ResponseStatus(HttpStatus.CREATED)
    public AreaResponse createArea(@Valid @RequestBody AreaRequest request) {
        return seatingService.createArea(request);
    }

    @PutMapping("/areas/{id}")
    public AreaResponse updateArea(@PathVariable Long id, @Valid @RequestBody AreaRequest request) {
        return seatingService.updateArea(id, request);
    }

    @DeleteMapping("/areas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArea(@PathVariable Long id) {
        seatingService.deleteArea(id);
    }

    // tables
    @GetMapping("/tables/{id}")
    public TableResponse table(@PathVariable Long id) {
        return seatingService.tableDetail(id);
    }

    @PostMapping("/tables")
    @ResponseStatus(HttpStatus.CREATED)
    public TableResponse createTable(@Valid @RequestBody TableRequest request) {
        return seatingService.createTable(request);
    }

    @PostMapping("/tables/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo nhanh nhiều bàn (Bàn 01, Bàn 02, ...)")
    public List<TableResponse> bulkCreate(@Valid @RequestBody BulkCreateTablesRequest request) {
        return seatingService.bulkCreate(request);
    }

    @PutMapping("/tables/{id}")
    public TableResponse updateTable(@PathVariable Long id, @Valid @RequestBody TableRequest request) {
        return seatingService.updateTable(id, request);
    }

    @DeleteMapping("/tables/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTable(@PathVariable Long id) {
        seatingService.deleteTable(id);
    }

    // assignments
    @PostMapping("/tables/{id}/assign")
    @Operation(summary = "Xếp khách vào bàn (trả về 409 OVER_CAPACITY nếu vượt sức chứa và chưa xác nhận)")
    public TableResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        return seatingService.assign(id, request);
    }

    @PostMapping("/assignments/{id}/move")
    @Operation(summary = "Chuyển khách sang bàn khác")
    public TableResponse move(@PathVariable Long id, @Valid @RequestBody MoveRequest request) {
        return seatingService.move(id, request);
    }

    @PatchMapping("/assignments/{id}/seats")
    public TableResponse updateSeats(@PathVariable Long id, @RequestParam int seats,
                                     @RequestParam(defaultValue = "false") boolean force) {
        return seatingService.updateSeats(id, seats, force);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable Long id) {
        seatingService.unassign(id);
    }
}
