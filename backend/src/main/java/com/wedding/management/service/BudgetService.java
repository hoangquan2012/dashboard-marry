package com.wedding.management.service;

import com.wedding.management.common.exception.ResourceNotFoundException;
import com.wedding.management.domain.entity.Expense;
import com.wedding.management.domain.entity.ExpenseCategory;
import com.wedding.management.domain.entity.ExpensePayment;
import com.wedding.management.domain.entity.Gift;
import com.wedding.management.domain.entity.OtherIncome;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.domain.enums.GiftType;
import com.wedding.management.domain.enums.PaymentMethod;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.BudgetDtos.BudgetAlert;
import com.wedding.management.dto.BudgetDtos.BudgetOverview;
import com.wedding.management.dto.BudgetDtos.OtherIncomeRequest;
import com.wedding.management.dto.BudgetDtos.OtherIncomeResponse;
import com.wedding.management.dto.BudgetDtos.SideFinance;
import com.wedding.management.dto.ExpenseDtos.CategoryStat;
import com.wedding.management.dto.ExpenseDtos.ExpenseStats;
import com.wedding.management.repository.ExpenseCategoryRepository;
import com.wedding.management.repository.ExpensePaymentRepository;
import com.wedding.management.repository.ExpenseRepository;
import com.wedding.management.repository.GiftRepository;
import com.wedding.management.repository.OtherIncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Financial overview. Gifts (income) and expenses are two independent flows:
 * gifts are never used as payments for expenses, they are only compared in the balance.
 */
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final SettingsService settingsService;
    private final ExpenseRepository expenseRepository;
    private final ExpensePaymentRepository paymentRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final GiftRepository giftRepository;
    private final OtherIncomeRepository otherIncomeRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public BudgetOverview overview() {
        BigDecimal totalBudget = settingsService.getEntity().getTotalBudget();
        List<Expense> expenses = expenseRepository.findAllActiveWithRelations();
        List<ExpenseCategory> categories = categoryRepository.findAllByOrderBySortOrderAscNameAsc();
        List<Gift> gifts = giftRepository.findAllActiveWithRelations();
        List<OtherIncome> incomes = otherIncomeRepository.findAll();
        List<ExpensePayment> payments = paymentRepository.findAllActive();

        ExpenseStats expenseStats = ExpenseService.computeStats(expenses, categories, totalBudget);

        BigDecimal giftTotal = BigDecimal.ZERO, cashIncome = BigDecimal.ZERO, transferIncome = BigDecimal.ZERO;
        for (Gift g : gifts) {
            if (g.getAmount() == null) continue;
            giftTotal = giftTotal.add(g.getAmount());
            if (g.getGiftType() == GiftType.CASH) cashIncome = cashIncome.add(g.getAmount());
            else if (g.getGiftType() == GiftType.BANK_TRANSFER) transferIncome = transferIncome.add(g.getAmount());
        }
        BigDecimal otherTotal = BigDecimal.ZERO;
        for (OtherIncome i : incomes) {
            otherTotal = otherTotal.add(i.getAmount());
            if (i.getMethod() == PaymentMethod.CASH) cashIncome = cashIncome.add(i.getAmount());
            else if (i.getMethod() == PaymentMethod.BANK_TRANSFER) transferIncome = transferIncome.add(i.getAmount());
        }
        BigDecimal cashExpense = BigDecimal.ZERO, transferExpense = BigDecimal.ZERO;
        for (ExpensePayment p : payments) {
            if (p.getMethod() == PaymentMethod.CASH) cashExpense = cashExpense.add(p.getAmount());
            else if (p.getMethod() == PaymentMethod.BANK_TRANSFER) transferExpense = transferExpense.add(p.getAmount());
        }

        BigDecimal totalIncome = giftTotal.add(otherTotal);
        BigDecimal actualTotal = expenseStats.actualTotal();
        BigDecimal estimatedTotal = expenseStats.estimatedTotal();
        BigDecimal projectedCost = actualTotal.max(estimatedTotal);
        BigDecimal budgetRemaining = totalBudget.subtract(actualTotal);
        double usedPercent = totalBudget.signum() == 0 ? 0 :
                actualTotal.multiply(BigDecimal.valueOf(100)).divide(totalBudget, 1, RoundingMode.HALF_UP).doubleValue();

        List<SideFinance> bySide = new ArrayList<>();
        for (Side side : Side.values()) {
            BigDecimal gift = gifts.stream().filter(g -> g.getSide() == side && g.getAmount() != null).map(Gift::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal other = incomes.stream().filter(i -> i.getSide() == side).map(OtherIncome::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal exp = expenses.stream().filter(e -> e.getSide() == side).map(Expense::getActualAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal paid = expenses.stream().filter(e -> e.getSide() == side).map(Expense::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            bySide.add(new SideFinance(side, side.label(), gift, other, exp, paid, gift.add(other).subtract(exp)));
        }

        List<BudgetAlert> alerts = new ArrayList<>();
        if (totalBudget.signum() > 0 && actualTotal.compareTo(totalBudget) > 0) {
            alerts.add(new BudgetAlert("TOTAL_BUDGET", "error", "Tổng chi phí thực tế đã vượt ngân sách dự kiến "
                    + money(actualTotal.subtract(totalBudget)), null, actualTotal.subtract(totalBudget)));
        } else if (totalBudget.signum() > 0 && usedPercent >= 90) {
            alerts.add(new BudgetAlert("TOTAL_BUDGET", "warning", "Đã sử dụng " + usedPercent + "% ngân sách tổng", null, budgetRemaining));
        }
        if (totalBudget.signum() > 0 && expenseStats.categoryBudgetTotal().compareTo(totalBudget) > 0) {
            alerts.add(new BudgetAlert("CATEGORY_ALLOCATION", "warning", "Tổng ngân sách các hạng mục ("
                    + money(expenseStats.categoryBudgetTotal()) + ") lớn hơn ngân sách tổng (" + money(totalBudget) + ")", null,
                    expenseStats.categoryBudgetTotal().subtract(totalBudget)));
        }
        for (CategoryStat c : expenseStats.byCategory()) {
            if (c.overBudget()) {
                alerts.add(new BudgetAlert("CATEGORY", "error", "Hạng mục \"" + c.categoryName() + "\" vượt ngân sách "
                        + money(c.actual().subtract(c.budget())), c.categoryId(), c.actual().subtract(c.budget())));
            } else if (c.budget().signum() > 0 && c.actual().multiply(BigDecimal.valueOf(100)).divide(c.budget(), 0, RoundingMode.HALF_UP).intValue() >= 90) {
                alerts.add(new BudgetAlert("CATEGORY", "warning", "Hạng mục \"" + c.categoryName() + "\" đã dùng trên 90% ngân sách",
                        c.categoryId(), c.budget().subtract(c.actual())));
            }
        }
        if (totalIncome.subtract(actualTotal).signum() < 0) {
            alerts.add(new BudgetAlert("BALANCE", "warning", "Tổng thu hiện tại thấp hơn tổng chi " + money(actualTotal.subtract(totalIncome)),
                    null, actualTotal.subtract(totalIncome)));
        }

        return new BudgetOverview(totalBudget, expenseStats.categoryBudgetTotal(), totalBudget.subtract(expenseStats.categoryBudgetTotal()),
                estimatedTotal, actualTotal, expenseStats.paidTotal(), expenseStats.remainingTotal(), expenseStats.depositTotal(),
                budgetRemaining, usedPercent, giftTotal, otherTotal, totalIncome,
                totalIncome.subtract(projectedCost), totalIncome.subtract(actualTotal),
                cashIncome.subtract(cashExpense), cashIncome, transferIncome, cashExpense, transferExpense,
                expenseStats.byCategory(), bySide, alerts);
    }

    // ---- other incomes ------------------------------------------------------

    @Transactional(readOnly = true)
    public List<OtherIncomeResponse> incomes() {
        return otherIncomeRepository.findAllByOrderByReceivedAtDescIdDesc().stream().map(BudgetService::toResponse).toList();
    }

    @Transactional
    public OtherIncomeResponse createIncome(OtherIncomeRequest request) {
        OtherIncome i = new OtherIncome();
        apply(i, request);
        otherIncomeRepository.save(i);
        auditService.log("OTHER_INCOME", i.getId(), i.getSource(), AuditAction.CREATE, "Thêm nguồn thu " + i.getSource() + " " + money(i.getAmount()));
        return toResponse(i);
    }

    @Transactional
    public OtherIncomeResponse updateIncome(Long id, OtherIncomeRequest request) {
        OtherIncome i = otherIncomeRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Nguồn thu", id));
        apply(i, request);
        otherIncomeRepository.save(i);
        auditService.log("OTHER_INCOME", i.getId(), i.getSource(), AuditAction.UPDATE, "Cập nhật nguồn thu " + i.getSource());
        return toResponse(i);
    }

    @Transactional
    public void deleteIncome(Long id) {
        OtherIncome i = otherIncomeRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Nguồn thu", id));
        otherIncomeRepository.delete(i);
        auditService.log("OTHER_INCOME", id, i.getSource(), AuditAction.DELETE, "Xóa nguồn thu " + i.getSource());
    }

    private void apply(OtherIncome i, OtherIncomeRequest r) {
        i.setSource(r.source().trim());
        i.setSide(r.side() == null ? Side.BOTH : r.side());
        i.setAmount(r.amount().setScale(0, RoundingMode.HALF_UP));
        i.setReceivedAt(r.receivedAt() == null ? LocalDate.now(SettingsService.ZONE) : r.receivedAt());
        i.setMethod(r.method() == null ? PaymentMethod.CASH : r.method());
        i.setNote(r.note());
    }

    private static OtherIncomeResponse toResponse(OtherIncome i) {
        return new OtherIncomeResponse(i.getId(), i.getSource(), i.getSide(), i.getAmount(), i.getReceivedAt(), i.getMethod(), i.getNote(), i.getCreatedAt());
    }

    private static String money(BigDecimal v) {
        return String.format("%,d ₫", v.longValue()).replace(',', '.');
    }
}
