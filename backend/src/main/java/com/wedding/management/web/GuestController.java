package com.wedding.management.web;

import com.wedding.management.common.api.PageResponse;
import com.wedding.management.common.web.Downloads;
import com.wedding.management.domain.enums.GuestCategory;
import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.GiftDtos.GiftResponse;
import com.wedding.management.dto.GuestDtos.DuplicateCheckResponse;
import com.wedding.management.dto.GuestDtos.GuestRequest;
import com.wedding.management.dto.GuestDtos.GuestResponse;
import com.wedding.management.dto.GuestDtos.GuestStats;
import com.wedding.management.dto.GuestDtos.GuestSummary;
import com.wedding.management.dto.GuestDtos.ImportResult;
import com.wedding.management.service.GiftService;
import com.wedding.management.service.GuestExcelService;
import com.wedding.management.service.GuestService;
import com.wedding.management.service.GuestService.GuestFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/guests")
@RequiredArgsConstructor
@Tag(name = "Guests", description = "Quản lý khách mời")
public class GuestController {

    private final GuestService guestService;
    private final GuestExcelService guestExcelService;
    private final GiftService giftService;

    @GetMapping
    @Operation(summary = "Danh sách khách mời (tìm kiếm, lọc, phân trang)")
    public PageResponse<GuestResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Side side,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) GuestCategory category,
            @RequestParam(required = false) InvitationStatus status,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Boolean vip,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) Boolean hasGift,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return guestService.list(new GuestFilter(q, side, groupId, category, status, eventId, vip, archived, unassigned, hasGift),
                page, size, sortBy, direction);
    }

    @GetMapping("/stats")
    @Operation(summary = "Thống kê nhanh khách mời theo bên và trạng thái")
    public GuestStats stats() {
        return guestService.stats();
    }

    @GetMapping("/lookup")
    @Operation(summary = "Tìm nhanh khách theo tên / SĐT / mã (autocomplete)")
    public List<GuestSummary> lookup(@RequestParam String q, @RequestParam(defaultValue = "15") int limit) {
        return guestService.lookup(q, Math.min(limit, 50));
    }

    @GetMapping("/check-duplicate")
    @Operation(summary = "Kiểm tra khách trùng số điện thoại hoặc trùng tên")
    public DuplicateCheckResponse checkDuplicate(@RequestParam(required = false) String phone,
                                                 @RequestParam(required = false) String fullName,
                                                 @RequestParam(required = false) Long excludeId) {
        return guestService.checkDuplicates(phone, fullName, excludeId);
    }

    @GetMapping("/export")
    @Operation(summary = "Xuất danh sách khách mời ra Excel (áp dụng bộ lọc hiện tại)")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Side side,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) GuestCategory category,
            @RequestParam(required = false) InvitationStatus status,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        List<GuestResponse> guests = guestService.listAll(
                new GuestFilter(q, side, groupId, category, status, eventId, null, archived, null, null), sortBy, direction);
        return Downloads.excel("danh-sach-khach-moi", guestExcelService.export(guests, guestService.events()));
    }

    @GetMapping("/import-template")
    @Operation(summary = "Tải file Excel mẫu để nhập khách mời")
    public ResponseEntity<byte[]> template() {
        return Downloads.file("mau-nhap-khach-moi.xlsx", Downloads.XLSX, guestExcelService.template());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Nhập danh sách khách mời từ Excel")
    public ImportResult importGuests(@RequestPart("file") MultipartFile file,
                                     @RequestParam(defaultValue = "true") boolean skipDuplicates) {
        return guestExcelService.importGuests(file, skipDuplicates);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết khách mời")
    public GuestResponse get(@PathVariable Long id) {
        return guestService.get(id);
    }

    @GetMapping("/{id}/gifts")
    @Operation(summary = "Lịch sử tiền mừng của khách")
    public List<GiftResponse> gifts(@PathVariable Long id) {
        return giftService.historyOfGuest(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm khách mời")
    public GuestResponse create(@Valid @RequestBody GuestRequest request) {
        return guestService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật khách mời")
    public GuestResponse update(@PathVariable Long id, @Valid @RequestBody GuestRequest request) {
        return guestService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Lưu trữ (xóa mềm) khách mời - lịch sử tiền mừng được giữ nguyên")
    public void archive(@PathVariable Long id) {
        guestService.archive(id);
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Khôi phục khách mời đã lưu trữ")
    public GuestResponse restore(@PathVariable Long id) {
        return guestService.restore(id);
    }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa vĩnh viễn khách mời (chỉ khi chưa có tiền mừng)")
    public void hardDelete(@PathVariable Long id) {
        guestService.hardDelete(id);
    }
}
