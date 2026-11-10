package com.wedding.management.web;

import com.wedding.management.dto.BudgetDtos.BudgetOverview;
import com.wedding.management.dto.BudgetDtos.OtherIncomeRequest;
import com.wedding.management.dto.BudgetDtos.OtherIncomeResponse;
import com.wedding.management.service.BudgetService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Budget", description = "Ngân sách, tổng thu - tổng chi, nguồn thu khác")
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping("/budget/overview")
    @Operation(summary = "Tổng quan tài chính: ngân sách, chi phí, tiền mừng, số dư, cảnh báo vượt ngân sách")
    public BudgetOverview overview() {
        return budgetService.overview();
    }

    @GetMapping("/other-incomes")
    public List<OtherIncomeResponse> incomes() {
        return budgetService.incomes();
    }

    @PostMapping("/other-incomes")
    @ResponseStatus(HttpStatus.CREATED)
    public OtherIncomeResponse createIncome(@Valid @RequestBody OtherIncomeRequest request) {
        return budgetService.createIncome(request);
    }

    @PutMapping("/other-incomes/{id}")
    public OtherIncomeResponse updateIncome(@PathVariable Long id, @Valid @RequestBody OtherIncomeRequest request) {
        return budgetService.updateIncome(id, request);
    }

    @DeleteMapping("/other-incomes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIncome(@PathVariable Long id) {
        budgetService.deleteIncome(id);
    }
}
