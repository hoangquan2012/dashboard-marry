package com.wedding.management.web;

import com.wedding.management.common.api.PageResponse;
import com.wedding.management.common.web.Downloads;
import com.wedding.management.domain.enums.PaymentStatus;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.ExpenseDtos.CategoryRequest;
import com.wedding.management.dto.ExpenseDtos.CategoryResponse;
import com.wedding.management.dto.ExpenseDtos.DebtSummary;
import com.wedding.management.dto.ExpenseDtos.ExpenseRequest;
import com.wedding.management.dto.ExpenseDtos.ExpenseResponse;
import com.wedding.management.dto.ExpenseDtos.ExpenseStats;
import com.wedding.management.dto.ExpenseDtos.PaymentRequest;
import com.wedding.management.service.ExpenseCategoryService;
import com.wedding.management.service.ExpenseService;
import com.wedding.management.service.ExpenseService.ExpenseFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Chi phí đám cưới, thanh toán, danh mục, công nợ")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseCategoryService categoryService;

    // ---- categories ---------------------------------------------------------

    @GetMapping("/expense-categories")
    @Operation(summary = "Danh mục chi phí kèm ngân sách và tổng chi")
    public List<CategoryResponse> categories() {
        return categoryService.list();
    }

    @PostMapping("/expense-categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/expense-categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @PutMapping("/expense-categories/budgets")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật ngân sách cho nhiều danh mục cùng lúc {categoryId: amount}")
    public List<CategoryResponse> updateBudgets(@RequestBody Map<Long, BigDecimal> budgets) {
        categoryService.updateBudgets(budgets);
        return categoryService.list();
    }

    @DeleteMapping("/expense-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
    }

    // ---- expenses -----------------------------------------------------------

    @GetMapping("/expenses")
    @Operation(summary = "Danh sách khoản chi (tìm kiếm, lọc, phân trang)")
    public PageResponse<ExpenseResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Side side,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) Boolean overBudget,
            @RequestParam(required = false) Boolean unpaid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return expenseService.list(new ExpenseFilter(q, categoryId, vendorId, eventId, side, status, overBudget, unpaid, from, to),
                page, size, sortBy, direction);
    }

    @GetMapping("/expenses/stats")
    @Operation(summary = "Thống kê chi phí theo danh mục, bên, nhà cung cấp, trạng thái")
    public ExpenseStats stats(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Side side,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return expenseService.stats(new ExpenseFilter(null, categoryId, vendorId, eventId, side, null, null, null, from, to));
    }

    @GetMapping("/expenses/debts")
    @Operation(summary = "Công nợ: các khoản còn phải thanh toán")
    public DebtSummary debts(@RequestParam(required = false) Long vendorId) {
        return expenseService.debts(vendorId);
    }

    @GetMapping("/expenses/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Side side,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) Boolean overBudget,
            @RequestParam(required = false) Boolean unpaid) {
        return Downloads.excel("chi-phi-dam-cuoi", expenseService.export(
                expenseService.findAll(new ExpenseFilter(q, categoryId, vendorId, eventId, side, status, overBudget, unpaid, null, null))));
    }

    @GetMapping("/expenses/{id}")
    public ExpenseResponse get(@PathVariable Long id) {
        return expenseService.get(id);
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(request);
    }

    @PutMapping("/expenses/{id}")
    public ExpenseResponse update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(id, request);
    }

    @DeleteMapping("/expenses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        expenseService.delete(id);
    }

    @PostMapping("/expenses/{id}/payments")
    @Operation(summary = "Ghi nhận một lần thanh toán (đặt cọc / một phần / nốt)")
    public ExpenseResponse addPayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return expenseService.addPayment(id, request);
    }

    @DeleteMapping("/expenses/{id}/payments/{paymentId}")
    public ExpenseResponse deletePayment(@PathVariable Long id, @PathVariable Long paymentId) {
        return expenseService.deletePayment(id, paymentId);
    }
}
