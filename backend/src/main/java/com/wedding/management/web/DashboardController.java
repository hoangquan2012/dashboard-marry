package com.wedding.management.web;

import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.DashboardDtos.DashboardSummary;
import com.wedding.management.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Tổng quan đám cưới")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Toàn bộ số liệu tổng quan: thẻ thống kê, biểu đồ, danh sách cần chú ý",
            description = "Bộ lọc: side (GROOM/BRIDE), eventId, from/to (yyyy-MM-dd). Mọi phép tính đều thực hiện ở backend.")
    public DashboardSummary summary(
            @RequestParam(required = false) Side side,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardService.summary(side == Side.BOTH ? null : side, eventId, from, to);
    }
}
