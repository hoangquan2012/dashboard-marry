package com.wedding.management.web;

import com.wedding.management.common.web.Downloads;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.ReportDtos.ExpenseReport;
import com.wedding.management.dto.ReportDtos.FinancialReport;
import com.wedding.management.dto.ReportDtos.GiftReport;
import com.wedding.management.dto.ReportDtos.GuestReport;
import com.wedding.management.dto.ReportDtos.ReportFilter;
import com.wedding.management.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Báo cáo và thống kê tổng kết")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/guests")
    @Operation(summary = "Báo cáo khách mời")
    public GuestReport guests(@RequestParam(required = false) Side side,
                              @RequestParam(required = false) Long eventId,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.guestReport(filter(side, eventId, from, to));
    }

    @GetMapping("/gifts")
    @Operation(summary = "Báo cáo tiền mừng")
    public GiftReport gifts(@RequestParam(required = false) Side side,
                            @RequestParam(required = false) Long eventId,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.giftReport(filter(side, eventId, from, to));
    }

    @GetMapping("/expenses")
    @Operation(summary = "Báo cáo chi phí")
    public ExpenseReport expenses(@RequestParam(required = false) Side side,
                                  @RequestParam(required = false) Long eventId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.expenseReport(filter(side, eventId, from, to));
    }

    @GetMapping("/financial")
    @Operation(summary = "Báo cáo tổng kết tài chính (tổng thu, tổng chi, số dư, trung bình)")
    public FinancialReport financial(@RequestParam(required = false) Side side,
                                     @RequestParam(required = false) Long eventId,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.financialReport(filter(side, eventId, from, to));
    }

    @GetMapping("/export")
    @Operation(summary = "Xuất toàn bộ báo cáo ra Excel (4 sheet)")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) Side side,
                                         @RequestParam(required = false) Long eventId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Downloads.excel("bao-cao-dam-cuoi", reportService.exportAll(filter(side, eventId, from, to)));
    }

    private static ReportFilter filter(Side side, Long eventId, LocalDate from, LocalDate to) {
        return new ReportFilter(side == Side.BOTH ? null : side, eventId, from, to);
    }
}
