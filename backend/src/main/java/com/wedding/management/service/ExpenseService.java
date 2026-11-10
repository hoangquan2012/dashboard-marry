package com.wedding.management.service;

import com.wedding.management.common.api.PageResponse;
import com.wedding.management.common.excel.ExcelUtil;
import com.wedding.management.common.exception.BusinessException;
import com.wedding.management.common.exception.ResourceNotFoundException;
import com.wedding.management.domain.entity.Expense;
import com.wedding.management.domain.entity.ExpenseCategory;
import com.wedding.management.domain.entity.ExpensePayment;
import com.wedding.management.domain.entity.Vendor;
import com.wedding.management.domain.entity.WeddingEvent;
import com.wedding.management.domain.enums.AttachmentEntityType;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.domain.enums.PaymentMethod;
import com.wedding.management.domain.enums.PaymentStatus;
import com.wedding.management.domain.enums.PaymentType;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.ExpenseDtos.CategoryStat;
import com.wedding.management.dto.ExpenseDtos.DebtItem;
import com.wedding.management.dto.ExpenseDtos.DebtSummary;
import com.wedding.management.dto.ExpenseDtos.ExpenseRequest;
import com.wedding.management.dto.ExpenseDtos.ExpenseResponse;
import com.wedding.management.dto.ExpenseDtos.ExpenseStats;
import com.wedding.management.dto.ExpenseDtos.PaymentRequest;
import com.wedding.management.dto.ExpenseDtos.PaymentResponse;
import com.wedding.management.dto.ExpenseDtos.SideStat;
import com.wedding.management.dto.ExpenseDtos.StatusStat;
import com.wedding.management.dto.ExpenseDtos.VendorStat;
import com.wedding.management.repository.AttachmentRepository;
import com.wedding.management.repository.ExpenseCategoryRepository;
import com.wedding.management.repository.ExpensePaymentRepository;
import com.wedding.management.repository.ExpenseRepository;
import com.wedding.management.repository.VendorRepository;
import com.wedding.management.repository.WeddingEventRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpensePaymentRepository paymentRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final VendorRepository vendorRepository;
    private final WeddingEventRepository eventRepository;
    private final AttachmentRepository attachmentRepository;
    private final SettingsService settingsService;
    private final AuditService auditService;

    public record ExpenseFilter(String q, Long categoryId, Long vendorId, Long eventId, Side side, PaymentStatus status,
                                Boolean overBudget, Boolean unpaid, LocalDate from, LocalDate to) {
    }

    // ---- queries ------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(ExpenseFilter filter, int page, int size, String sortBy, String direction) {
        Set<String> allowed = Set.of("createdAt", "name", "actualAmount", "estimatedAmount", "dueDate", "expenseDate", "paymentStatus");
        String property = allowed.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<Expense> result = expenseRepository.findAll(specification(filter),
                PageRequest.of(page, Math.min(Math.max(size, 1), 200), Sort.by(dir, property).and(Sort.by(Sort.Direction.DESC, "id"))));
        Map<Long, Long> attachments = attachmentCounts(result.getContent());
        return PageResponse.of(result, result.getContent().stream().map(e -> toResponse(e, null, attachments.getOrDefault(e.getId(), 0L))).toList());
    }

    @Transactional(readOnly = true)
    public List<Expense> findAll(ExpenseFilter filter) {
        return expenseRepository.findAll(specification(filter), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(Long id) {
        Expense e = findActive(id);
        List<ExpensePayment> payments = paymentRepository.findByExpenseIdOrderByPaymentDateAscIdAsc(id);
        return toResponse(e, payments, attachmentRepository.countByEntityTypeAndEntityId(AttachmentEntityType.EXPENSE, id));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> byVendor(Long vendorId) {
        return expenseRepository.findActiveByVendorId(vendorId).stream().map(e -> toResponse(e, null, 0)).toList();
    }

    @Transactional(readOnly = true)
    public ExpenseStats stats(ExpenseFilter filter) {
        List<Expense> expenses = findAll(filter);
        return computeStats(expenses, categoryRepository.findAllByOrderBySortOrderAscNameAsc(), settingsService.getEntity().getTotalBudget());
    }

    public static ExpenseStats computeStats(List<Expense> expenses, List<ExpenseCategory> categories, BigDecimal totalBudget) {
        BigDecimal estimated = BigDecimal.ZERO, actual = BigDecimal.ZERO, paid = BigDecimal.ZERO, deposit = BigDecimal.ZERO, overAmount = BigDecimal.ZERO;
        long unpaid = 0, over = 0;
        Map<Long, BigDecimal[]> byCat = new LinkedHashMap<>();
        Map<Long, Long> catCount = new HashMap<>();
        Map<Side, BigDecimal[]> bySide = new LinkedHashMap<>();
        Map<Side, Long> sideCount = new LinkedHashMap<>();
        Map<Long, BigDecimal[]> byVendor = new LinkedHashMap<>();
        Map<Long, String> vendorNames = new HashMap<>();
        Map<Long, Long> vendorCount = new HashMap<>();
        Map<PaymentStatus, BigDecimal> statusAmount = new LinkedHashMap<>();
        Map<PaymentStatus, Long> statusCount = new LinkedHashMap<>();
        List<ExpenseResponse> overList = new ArrayList<>();

        for (Expense e : expenses) {
            estimated = estimated.add(e.getEstimatedAmount());
            actual = actual.add(e.getActualAmount());
            paid = paid.add(e.getPaidAmount());
            deposit = deposit.add(e.getDepositAmount());
            if (e.getRemainingAmount().signum() > 0) unpaid++;
            if (e.isOverBudget()) {
                over++;
                overAmount = overAmount.add(e.getActualAmount().subtract(e.getEstimatedAmount()));
                overList.add(toResponse(e, null, 0));
            }
            BigDecimal[] c = byCat.computeIfAbsent(e.getCategory().getId(), k -> zeros());
            c[0] = c[0].add(e.getEstimatedAmount());
            c[1] = c[1].add(e.getActualAmount());
            c[2] = c[2].add(e.getPaidAmount());
            catCount.merge(e.getCategory().getId(), 1L, Long::sum);
            BigDecimal[] s = bySide.computeIfAbsent(e.getSide(), k -> zeros());
            s[0] = s[0].add(e.getEstimatedAmount());
            s[1] = s[1].add(e.getActualAmount());
            s[2] = s[2].add(e.getPaidAmount());
            sideCount.merge(e.getSide(), 1L, Long::sum);
            if (e.getVendor() != null) {
                BigDecimal[] v = byVendor.computeIfAbsent(e.getVendor().getId(), k -> zeros());
                v[1] = v[1].add(e.getActualAmount());
                v[2] = v[2].add(e.getPaidAmount());
                vendorNames.put(e.getVendor().getId(), e.getVendor().getName());
                vendorCount.merge(e.getVendor().getId(), 1L, Long::sum);
            }
            statusAmount.merge(e.getPaymentStatus(), e.getActualAmount(), BigDecimal::add);
            statusCount.merge(e.getPaymentStatus(), 1L, Long::sum);
        }

        BigDecimal categoryBudgetTotal = BigDecimal.ZERO;
        List<CategoryStat> categoryStats = new ArrayList<>();
        for (ExpenseCategory cat : categories) {
            categoryBudgetTotal = categoryBudgetTotal.add(cat.getBudgetAmount());
            BigDecimal[] c = byCat.getOrDefault(cat.getId(), zeros());
            long count = catCount.getOrDefault(cat.getId(), 0L);
            if (count == 0 && cat.getBudgetAmount().signum() == 0) continue;
            double percent = actual.signum() == 0 ? 0 : c[1].multiply(BigDecimal.valueOf(100)).divide(actual, 1, RoundingMode.HALF_UP).doubleValue();
            boolean overBudget = cat.getBudgetAmount().signum() > 0 && c[1].compareTo(cat.getBudgetAmount()) > 0;
            categoryStats.add(new CategoryStat(cat.getId(), cat.getName(), cat.getIcon(), count, cat.getBudgetAmount(),
                    c[0], c[1], c[2], c[1].subtract(c[2]).max(BigDecimal.ZERO), percent, overBudget));
        }
        categoryStats.sort(Comparator.comparing(CategoryStat::actual).reversed());
        CategoryStat largest = categoryStats.stream().filter(c -> c.actual().signum() > 0).findFirst().orElse(null);

        List<SideStat> sideStats = new ArrayList<>();
        for (Side side : Side.values()) {
            BigDecimal[] s = bySide.getOrDefault(side, zeros());
            sideStats.add(new SideStat(side, side.label(), sideCount.getOrDefault(side, 0L), s[0], s[1], s[2]));
        }
        List<VendorStat> vendorStats = byVendor.entrySet().stream()
                .map(en -> new VendorStat(en.getKey(), vendorNames.get(en.getKey()), vendorCount.get(en.getKey()),
                        en.getValue()[1], en.getValue()[2], en.getValue()[1].subtract(en.getValue()[2]).max(BigDecimal.ZERO)))
                .sorted(Comparator.comparing(VendorStat::actual).reversed()).toList();
        List<StatusStat> statusStats = new ArrayList<>();
        for (PaymentStatus st : PaymentStatus.values()) {
            statusStats.add(new StatusStat(st, st.label(), statusCount.getOrDefault(st, 0L), statusAmount.getOrDefault(st, BigDecimal.ZERO)));
        }
        overList.sort(Comparator.comparing(ExpenseResponse::overBudgetAmount).reversed());
        return new ExpenseStats(totalBudget, categoryBudgetTotal, estimated, actual, paid, actual.subtract(paid).max(BigDecimal.ZERO), deposit,
                expenses.size(), unpaid, over, overAmount, largest, categoryStats, sideStats, vendorStats, statusStats, overList);
    }

    private static BigDecimal[] zeros() {
        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
    }

    @Transactional(readOnly = true)
    public DebtSummary debts(Long vendorId) {
        LocalDate today = LocalDate.now(SettingsService.ZONE);
        List<DebtItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO, overdueAmount = BigDecimal.ZERO;
        long overdueCount = 0, dueSoon = 0;
        for (Expense e : expenseRepository.findAllActiveWithRelations()) {
            if (vendorId != null && (e.getVendor() == null || !e.getVendor().getId().equals(vendorId))) continue;
            BigDecimal remaining = e.getRemainingAmount();
            if (remaining.signum() <= 0) continue;
            boolean overdue = e.getDueDate() != null && e.getDueDate().isBefore(today);
            Long days = e.getDueDate() == null ? null : ChronoUnit.DAYS.between(today, e.getDueDate());
            if (overdue) {
                overdueCount++;
                overdueAmount = overdueAmount.add(remaining);
            } else if (days != null && days <= 7) {
                dueSoon++;
            }
            total = total.add(remaining);
            items.add(new DebtItem(e.getId(), e.getName(), e.getCategory().getName(),
                    e.getVendor() != null ? e.getVendor().getId() : null, e.getVendor() != null ? e.getVendor().getName() : null,
                    e.getActualAmount(), e.getPaidAmount(), remaining, e.getDueDate(), e.getPaymentStatus(), overdue, days));
        }
        items.sort(Comparator.comparing((DebtItem d) -> d.dueDate() == null ? LocalDate.MAX : d.dueDate()));
        return new DebtSummary(total, items.size(), overdueCount, overdueAmount, dueSoon, items);
    }

    // ---- commands -----------------------------------------------------------

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Expense e = new Expense();
        apply(e, request);
        expenseRepository.save(e);
        syncPaidAmount(e, request.paidAmount());
        expenseRepository.save(e);
        auditService.log("EXPENSE", e.getId(), e.getName(), AuditAction.CREATE, "Thêm khoản chi " + e.getName() + " (" + money(e.getActualAmount()) + ")");
        return get(e.getId());
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense e = findActive(id);
        Map<String, Object> before = snapshot(e);
        apply(e, request);
        syncPaidAmount(e, request.paidAmount());
        expenseRepository.save(e);
        auditService.log("EXPENSE", e.getId(), e.getName(), AuditAction.UPDATE, "Cập nhật khoản chi " + e.getName(),
                AuditService.diff(before, snapshot(e)));
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        Expense e = findActive(id);
        e.setDeletedAt(Instant.now());
        expenseRepository.save(e);
        auditService.log("EXPENSE", e.getId(), e.getName(), AuditAction.DELETE, "Xóa khoản chi " + e.getName());
    }

    @Transactional
    public ExpenseResponse addPayment(Long expenseId, PaymentRequest request) {
        Expense e = findActive(expenseId);
        ExpensePayment p = new ExpensePayment();
        p.setExpense(e);
        p.setAmount(request.amount().setScale(0, RoundingMode.HALF_UP));
        p.setPaymentDate(request.paymentDate());
        p.setPaymentType(request.paymentType() == null ? PaymentType.PARTIAL : request.paymentType());
        p.setMethod(request.method() == null ? PaymentMethod.CASH : request.method());
        p.setPaidBy(request.paidBy());
        p.setNote(request.note());
        paymentRepository.save(p);
        List<ExpensePayment> payments = paymentRepository.findByExpenseIdOrderByPaymentDateAscIdAsc(expenseId);
        BigDecimal paidTotal = payments.stream().map(ExpensePayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (e.getActualAmount().signum() == 0 && paidTotal.signum() > 0) {
            // no actual amount entered yet: assume paid total is the cost so far
            e.setActualAmount(paidTotal);
        }
        if (paidTotal.compareTo(e.getActualAmount()) > 0) {
            throw new BusinessException("OVERPAID", "Tổng thanh toán (" + money(paidTotal) + ") vượt quá số tiền thực tế của khoản chi ("
                    + money(e.getActualAmount()) + "). Hãy cập nhật số tiền thực tế trước.");
        }
        recomputeStatus(e, payments);
        if (request.paidBy() != null && !request.paidBy().isBlank()) e.setPaidBy(request.paidBy());
        expenseRepository.save(e);
        auditService.log("EXPENSE", e.getId(), e.getName(), AuditAction.UPDATE,
                "Thanh toán " + money(p.getAmount()) + " (" + p.getPaymentType().label() + ") cho " + e.getName());
        return get(expenseId);
    }

    @Transactional
    public ExpenseResponse deletePayment(Long expenseId, Long paymentId) {
        Expense e = findActive(expenseId);
        ExpensePayment p = paymentRepository.findById(paymentId)
                .filter(x -> x.getExpense().getId().equals(expenseId))
                .orElseThrow(() -> ResourceNotFoundException.of("Lần thanh toán", paymentId));
        paymentRepository.delete(p);
        paymentRepository.flush();
        recomputeStatus(e, paymentRepository.findByExpenseIdOrderByPaymentDateAscIdAsc(expenseId));
        expenseRepository.save(e);
        auditService.log("EXPENSE", e.getId(), e.getName(), AuditAction.UPDATE, "Xóa lần thanh toán " + money(p.getAmount()) + " của " + e.getName());
        return get(expenseId);
    }

    /** Recalculates paid / deposit totals and the derived payment status. */
    public static void recomputeStatus(Expense e, List<ExpensePayment> payments) {
        BigDecimal paid = BigDecimal.ZERO, deposit = BigDecimal.ZERO;
        for (ExpensePayment p : payments) {
            paid = paid.add(p.getAmount());
            if (p.getPaymentType() == PaymentType.DEPOSIT) deposit = deposit.add(p.getAmount());
        }
        e.setPaidAmount(paid);
        e.setDepositAmount(deposit);
        if (paid.signum() == 0) {
            e.setPaymentStatus(PaymentStatus.UNPAID);
        } else if (e.getActualAmount().signum() > 0 && paid.compareTo(e.getActualAmount()) >= 0) {
            e.setPaymentStatus(PaymentStatus.PAID);
        } else if (deposit.compareTo(paid) == 0) {
            e.setPaymentStatus(PaymentStatus.DEPOSITED);
        } else {
            e.setPaymentStatus(PaymentStatus.PARTIAL);
        }
    }

    /**
     * Sets the paid total to the amount entered on the expense form.
     * When the value is omitted, existing payments are left unchanged.
     */
    private void syncPaidAmount(Expense e, BigDecimal desiredPaid) {
        List<ExpensePayment> payments = paymentRepository.findByExpenseIdOrderByPaymentDateAscIdAsc(e.getId());
        if (desiredPaid == null) {
            recomputeStatus(e, payments);
            return;
        }
        BigDecimal paid = desiredPaid.setScale(0, RoundingMode.HALF_UP);
        if (paid.signum() < 0) {
            paid = BigDecimal.ZERO;
        }
        if (paid.compareTo(e.getActualAmount()) > 0) {
            throw new BusinessException("OVERPAID", "Số tiền đã trả không được lớn hơn số tiền khoản chi.");
        }
        BigDecimal current = payments.stream().map(ExpensePayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (current.compareTo(paid) == 0) {
            recomputeStatus(e, payments);
            return;
        }
        paymentRepository.deleteByExpenseId(e.getId());
        paymentRepository.flush();
        List<ExpensePayment> next = new ArrayList<>();
        if (paid.signum() > 0) {
            ExpensePayment p = new ExpensePayment();
            p.setExpense(e);
            p.setAmount(paid);
            p.setPaymentDate(LocalDate.now(SettingsService.ZONE));
            p.setPaymentType(e.getActualAmount().signum() > 0 && paid.compareTo(e.getActualAmount()) >= 0
                    ? PaymentType.FINAL : PaymentType.PARTIAL);
            p.setMethod(PaymentMethod.CASH);
            paymentRepository.save(p);
            next.add(p);
        }
        recomputeStatus(e, next);
    }

    private void apply(Expense e, ExpenseRequest r) {
        e.setName(r.name().trim());
        e.setCategory(categoryRepository.findById(r.categoryId()).orElseThrow(() -> ResourceNotFoundException.of("Danh mục chi phí", r.categoryId())));
        Vendor vendor = r.vendorId() == null ? null : vendorRepository.findById(r.vendorId()).orElseThrow(() -> ResourceNotFoundException.of("Nhà cung cấp", r.vendorId()));
        e.setVendor(vendor);
        WeddingEvent event = r.eventId() == null ? null : eventRepository.findById(r.eventId()).orElseThrow(() -> ResourceNotFoundException.of("Sự kiện", r.eventId()));
        e.setEvent(event);
        e.setSide(r.side() == null ? Side.BOTH : r.side());
        e.setEstimatedAmount(r.estimatedAmount() == null ? BigDecimal.ZERO : r.estimatedAmount().setScale(0, RoundingMode.HALF_UP));
        e.setActualAmount(r.actualAmount() == null ? BigDecimal.ZERO : r.actualAmount().setScale(0, RoundingMode.HALF_UP));
        e.setExpenseDate(r.expenseDate());
        e.setDueDate(r.dueDate());
        e.setPaidBy(r.paidBy());
        e.setNote(r.note());
    }

    public Expense findActive(Long id) {
        return expenseRepository.findByIdWithRelations(id).filter(e -> e.getDeletedAt() == null)
                .orElseThrow(() -> ResourceNotFoundException.of("Khoản chi", id));
    }

    private Map<Long, Long> attachmentCounts(List<Expense> expenses) {
        Map<Long, Long> counts = new HashMap<>();
        if (expenses.isEmpty()) return counts;
        attachmentRepository.findByEntityTypeAndEntityIdIn(AttachmentEntityType.EXPENSE, expenses.stream().map(Expense::getId).toList())
                .forEach(a -> counts.merge(a.getEntityId(), 1L, Long::sum));
        return counts;
    }

    private Map<String, Object> snapshot(Expense e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", e.getName());
        m.put("category", e.getCategory().getName());
        m.put("vendor", e.getVendor() != null ? e.getVendor().getName() : null);
        m.put("side", e.getSide());
        m.put("estimatedAmount", e.getEstimatedAmount());
        m.put("actualAmount", e.getActualAmount());
        m.put("paidAmount", e.getPaidAmount());
        m.put("dueDate", e.getDueDate());
        m.put("paymentStatus", e.getPaymentStatus());
        return m;
    }

    private static String money(BigDecimal v) {
        return v == null ? "0 ₫" : String.format("%,d ₫", v.longValue()).replace(',', '.');
    }

    private Specification<Expense> specification(ExpenseFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("note"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("paidBy"), "")), like)));
            }
            if (f.categoryId() != null) predicates.add(cb.equal(root.get("category").get("id"), f.categoryId()));
            if (f.vendorId() != null) predicates.add(cb.equal(root.get("vendor").get("id"), f.vendorId()));
            if (f.eventId() != null) predicates.add(cb.equal(root.get("event").get("id"), f.eventId()));
            if (f.side() != null) predicates.add(cb.equal(root.get("side"), f.side()));
            if (f.status() != null) predicates.add(cb.equal(root.get("paymentStatus"), f.status()));
            if (Boolean.TRUE.equals(f.overBudget())) {
                predicates.add(cb.greaterThan(root.get("estimatedAmount"), BigDecimal.ZERO));
                predicates.add(cb.greaterThan(root.get("actualAmount"), root.get("estimatedAmount")));
            }
            if (Boolean.TRUE.equals(f.unpaid())) {
                predicates.add(cb.greaterThan(root.get("actualAmount"), root.get("paidAmount")));
            }
            if (f.from() != null) predicates.add(cb.greaterThanOrEqualTo(cb.coalesce(root.get("expenseDate"), root.get("dueDate")), f.from()));
            if (f.to() != null) predicates.add(cb.lessThanOrEqualTo(cb.coalesce(root.get("expenseDate"), root.get("dueDate")), f.to()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static ExpenseResponse toResponse(Expense e, List<ExpensePayment> payments, long attachmentCount) {
        LocalDate today = LocalDate.now(SettingsService.ZONE);
        List<PaymentResponse> paymentResponses = payments == null ? List.of() : payments.stream()
                .map(p -> new PaymentResponse(p.getId(), e.getId(), p.getAmount(), p.getPaymentDate(), p.getPaymentType(),
                        p.getPaymentType().label(), p.getMethod(), p.getMethod().label(), p.getPaidBy(), p.getNote(), p.getCreatedAt()))
                .toList();
        LocalDate lastPayment = payments == null ? null : payments.stream().map(ExpensePayment::getPaymentDate).max(Comparator.naturalOrder()).orElse(null);
        boolean overdue = e.getRemainingAmount().signum() > 0 && e.getDueDate() != null && e.getDueDate().isBefore(today);
        return new ExpenseResponse(e.getId(), e.getName(), e.getCategory().getId(), e.getCategory().getName(), e.getCategory().getIcon(),
                e.getVendor() != null ? e.getVendor().getId() : null, e.getVendor() != null ? e.getVendor().getName() : null,
                e.getEvent() != null ? e.getEvent().getId() : null, e.getEvent() != null ? e.getEvent().getName() : null,
                e.getSide(), e.getEstimatedAmount(), e.getActualAmount(), e.getPaidAmount(), e.getDepositAmount(), e.getRemainingAmount(),
                e.getPaymentStatus(), e.getPaymentStatus().label(), e.isOverBudget(),
                e.isOverBudget() ? e.getActualAmount().subtract(e.getEstimatedAmount()) : BigDecimal.ZERO,
                e.getExpenseDate(), e.getDueDate(), lastPayment, overdue, e.getPaidBy(), e.getNote(), attachmentCount, paymentResponses,
                e.getCreatedAt(), e.getUpdatedAt());
    }

    // ---- export -------------------------------------------------------------

    public byte[] export(List<Expense> expenses) {
        Workbook wb = ExcelUtil.newWorkbook();
        ExcelUtil.Styles styles = ExcelUtil.styles(wb);
        Sheet sheet = wb.createSheet("Chi phí");
        List<String> columns = List.of("Tên khoản chi", "Danh mục", "Nhà cung cấp", "Sự kiện", "Bên", "Dự kiến", "Thực tế",
                "Đã thanh toán", "Đã đặt cọc", "Còn phải trả", "Trạng thái", "Ngày chi", "Hạn thanh toán", "Người thanh toán", "Vượt ngân sách", "Ghi chú");
        ExcelUtil.header(sheet, 0, styles, columns);
        int r = 1;
        BigDecimal est = BigDecimal.ZERO, act = BigDecimal.ZERO, paid = BigDecimal.ZERO, remaining = BigDecimal.ZERO;
        for (Expense e : expenses) {
            Row row = sheet.createRow(r++);
            ExcelUtil.text(row, 0, e.getName());
            ExcelUtil.text(row, 1, e.getCategory().getName());
            ExcelUtil.text(row, 2, e.getVendor() != null ? e.getVendor().getName() : "");
            ExcelUtil.text(row, 3, e.getEvent() != null ? e.getEvent().getName() : "");
            ExcelUtil.text(row, 4, e.getSide().label());
            ExcelUtil.money(row, 5, e.getEstimatedAmount(), styles);
            ExcelUtil.money(row, 6, e.getActualAmount(), styles);
            ExcelUtil.money(row, 7, e.getPaidAmount(), styles);
            ExcelUtil.money(row, 8, e.getDepositAmount(), styles);
            ExcelUtil.money(row, 9, e.getRemainingAmount(), styles);
            ExcelUtil.text(row, 10, e.getPaymentStatus().label());
            ExcelUtil.date(row, 11, e.getExpenseDate(), styles);
            ExcelUtil.date(row, 12, e.getDueDate(), styles);
            ExcelUtil.text(row, 13, e.getPaidBy());
            ExcelUtil.text(row, 14, e.isOverBudget() ? "Có" : "");
            ExcelUtil.text(row, 15, e.getNote());
            est = est.add(e.getEstimatedAmount());
            act = act.add(e.getActualAmount());
            paid = paid.add(e.getPaidAmount());
            remaining = remaining.add(e.getRemainingAmount());
        }
        Row total = sheet.createRow(r + 1);
        ExcelUtil.bold(total, 4, "Tổng cộng", styles);
        ExcelUtil.boldMoney(total, 5, est, styles);
        ExcelUtil.boldMoney(total, 6, act, styles);
        ExcelUtil.boldMoney(total, 7, paid, styles);
        ExcelUtil.boldMoney(total, 9, remaining, styles);
        ExcelUtil.autosize(sheet, columns.size());
        return ExcelUtil.toBytes(wb);
    }
}
