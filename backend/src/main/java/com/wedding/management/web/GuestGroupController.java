package com.wedding.management.web;

import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.GuestDtos.GuestGroupRequest;
import com.wedding.management.dto.GuestDtos.GuestGroupResponse;
import com.wedding.management.service.GuestGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

import java.util.List;

@RestController
@RequestMapping("/api/guest-groups")
@RequiredArgsConstructor
@Tag(name = "Guest groups", description = "Nhóm khách / hộ gia đình")
public class GuestGroupController {

    private final GuestGroupService guestGroupService;

    @GetMapping
    @Operation(summary = "Danh sách nhóm khách kèm thống kê thành viên, số người và tiền mừng")
    public List<GuestGroupResponse> list(@RequestParam(required = false) Side side,
                                         @RequestParam(required = false) String q) {
        return guestGroupService.list(side, q);
    }

    @GetMapping("/{id}")
    public GuestGroupResponse get(@PathVariable Long id) {
        return guestGroupService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuestGroupResponse create(@Valid @RequestBody GuestGroupRequest request) {
        return guestGroupService.create(request);
    }

    @PutMapping("/{id}")
    public GuestGroupResponse update(@PathVariable Long id, @Valid @RequestBody GuestGroupRequest request) {
        return guestGroupService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Xóa nhóm (các thành viên được giữ lại, chỉ bỏ liên kết nhóm)")
    public void delete(@PathVariable Long id) {
        guestGroupService.delete(id);
    }
}
