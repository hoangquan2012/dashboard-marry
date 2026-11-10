package com.wedding.management.web;

import com.wedding.management.common.api.PageResponse;
import com.wedding.management.common.web.Downloads;
import com.wedding.management.domain.enums.GiftType;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.GiftDtos.GiftRequest;
import com.wedding.management.dto.GiftDtos.GiftResponse;
import com.wedding.management.dto.GiftDtos.GiftStats;
import com.wedding.management.service.GiftService;
import com.wedding.management.service.GiftService.GiftFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/gifts")
@RequiredArgsConstructor
@Tag(name = "Gifts", description = "Tiền mừng cưới")
public class GiftController {

    private final GiftService giftService;

    @GetMapping
    @Operation(summary = "Danh sách khoản mừng (tìm kiếm, lọc, phân trang)")
    public PageResponse<GiftResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Side side,
            @RequestParam(required = false) GiftType type,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long guestId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "receivedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return giftService.list(new GiftFilter(q, side, type, eventId, guestId, groupId, from, to), page, size, sortBy, direction);
    }

    @GetMapping("/stats")
    @Operation(summary = "Thống kê tiền mừng (tổng, theo bên, hình thức, sự kiện, trung bình)")
    public GiftStats stats(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Side side,
            @RequestParam(required = false) GiftType type,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return giftService.stats(new GiftFilter(q, side, type, eventId, null, groupId, from, to));
    }

    @GetMapping("/export")
    @Operation(summary = "Xuất Excel danh sách tiền mừng")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Side side,
            @RequestParam(required = false) GiftType type,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Downloads.excel("tien-mung-cuoi", giftService.export(giftService.findAll(new GiftFilter(q, side, type, eventId, null, groupId, from, to))));
    }

    @GetMapping("/{id}")
    public GiftResponse get(@PathVariable Long id) {
        return giftService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ghi nhận khoản mừng (một khách có thể có nhiều khoản ở nhiều sự kiện)")
    public GiftResponse create(@Valid @RequestBody GiftRequest request) {
        return giftService.create(request);
    }

    @PutMapping("/{id}")
    public GiftResponse update(@PathVariable Long id, @Valid @RequestBody GiftRequest request) {
        return giftService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa khoản mừng ghi nhầm (xóa mềm, có lưu nhật ký)")
    public void delete(@PathVariable Long id) {
        giftService.delete(id);
    }
}
