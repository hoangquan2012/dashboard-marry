package com.wedding.management.service;

import com.wedding.management.common.exception.BusinessException;
import com.wedding.management.common.exception.ResourceNotFoundException;
import com.wedding.management.domain.entity.Expense;
import com.wedding.management.domain.entity.ExpenseCategory;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.dto.ExpenseDtos.CategoryRequest;
import com.wedding.management.dto.ExpenseDtos.CategoryResponse;
import com.wedding.management.repository.ExpenseCategoryRepository;
import com.wedding.management.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        List<ExpenseCategory> categories = categoryRepository.findAllByOrderBySortOrderAscNameAsc();
        List<Expense> expenses = expenseRepository.findAllActiveWithRelations();
        Map<Long, BigDecimal[]> totals = new HashMap<>();
        Map<Long, Long> counts = new HashMap<>();
        for (Expense e : expenses) {
            BigDecimal[] acc = totals.computeIfAbsent(e.getCategory().getId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            acc[0] = acc[0].add(e.getEstimatedAmount());
            acc[1] = acc[1].add(e.getActualAmount());
            acc[2] = acc[2].add(e.getPaidAmount());
            counts.merge(e.getCategory().getId(), 1L, Long::sum);
        }
        List<CategoryResponse> out = new ArrayList<>();
        for (ExpenseCategory c : categories) {
            BigDecimal[] acc = totals.getOrDefault(c.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            boolean over = c.getBudgetAmount().signum() > 0 && acc[1].compareTo(c.getBudgetAmount()) > 0;
            out.add(new CategoryResponse(c.getId(), c.getName(), c.getIcon(), c.getBudgetAmount(), c.getSortOrder(), c.isSystem(),
                    counts.getOrDefault(c.getId(), 0L), acc[0], acc[1], acc[2], over));
        }
        return out;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        categoryRepository.findByNameIgnoreCase(request.name().trim()).ifPresent(c -> {
            throw BusinessException.conflict("Đã tồn tại danh mục \"" + request.name().trim() + "\"");
        });
        ExpenseCategory c = new ExpenseCategory();
        c.setName(request.name().trim());
        c.setIcon(request.icon() == null || request.icon().isBlank() ? "payments" : request.icon());
        c.setBudgetAmount(request.budgetAmount() == null ? BigDecimal.ZERO : request.budgetAmount());
        c.setSortOrder(request.sortOrder() == null ? 100 : request.sortOrder());
        categoryRepository.save(c);
        auditService.log("EXPENSE_CATEGORY", c.getId(), c.getName(), AuditAction.CREATE, "Tạo danh mục chi " + c.getName());
        return list().stream().filter(r -> r.id().equals(c.getId())).findFirst().orElseThrow();
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        ExpenseCategory c = find(id);
        categoryRepository.findByNameIgnoreCase(request.name().trim()).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
            throw BusinessException.conflict("Đã tồn tại danh mục \"" + request.name().trim() + "\"");
        });
        BigDecimal oldBudget = c.getBudgetAmount();
        c.setName(request.name().trim());
        if (request.icon() != null && !request.icon().isBlank()) c.setIcon(request.icon());
        c.setBudgetAmount(request.budgetAmount() == null ? BigDecimal.ZERO : request.budgetAmount());
        if (request.sortOrder() != null) c.setSortOrder(request.sortOrder());
        categoryRepository.save(c);
        auditService.log("EXPENSE_CATEGORY", c.getId(), c.getName(), AuditAction.UPDATE,
                "Cập nhật danh mục " + c.getName() + (oldBudget.compareTo(c.getBudgetAmount()) != 0 ? " (ngân sách " + oldBudget + " → " + c.getBudgetAmount() + ")" : ""));
        return list().stream().filter(r -> r.id().equals(c.getId())).findFirst().orElseThrow();
    }

    @Transactional
    public void updateBudgets(Map<Long, BigDecimal> budgets) {
        for (Map.Entry<Long, BigDecimal> e : budgets.entrySet()) {
            ExpenseCategory c = find(e.getKey());
            c.setBudgetAmount(e.getValue() == null ? BigDecimal.ZERO : e.getValue());
            categoryRepository.save(c);
        }
        auditService.log("EXPENSE_CATEGORY", null, null, AuditAction.UPDATE, "Cập nhật ngân sách cho " + budgets.size() + " danh mục");
    }

    @Transactional
    public void delete(Long id) {
        ExpenseCategory c = find(id);
        if (expenseRepository.countByCategoryId(id) > 0) {
            throw new BusinessException("Danh mục đang có khoản chi, không thể xóa. Hãy chuyển các khoản chi sang danh mục khác trước.");
        }
        categoryRepository.delete(c);
        auditService.log("EXPENSE_CATEGORY", id, c.getName(), AuditAction.DELETE, "Xóa danh mục chi " + c.getName());
    }

    public ExpenseCategory find(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Danh mục chi phí", id));
    }
}
