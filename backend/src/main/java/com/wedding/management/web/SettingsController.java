package com.wedding.management.web;

import com.wedding.management.dto.SettingsDtos.SettingsRequest;
import com.wedding.management.dto.SettingsDtos.SettingsResponse;
import com.wedding.management.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Tag(name = "Settings", description = "Thông tin chung của đám cưới")
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @Operation(summary = "Lấy thông tin đám cưới (tên cô dâu chú rể, ngày cưới, ngân sách)")
    public SettingsResponse get() {
        return settingsService.get();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật thông tin đám cưới (ADMIN)")
    public SettingsResponse update(@Valid @RequestBody SettingsRequest request) {
        return settingsService.update(request);
    }
}
