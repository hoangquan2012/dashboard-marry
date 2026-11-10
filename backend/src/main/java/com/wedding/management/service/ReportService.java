package com.wedding.management.service;

import com.wedding.management.common.excel.ExcelUtil;
import com.wedding.management.domain.entity.Expense;
import com.wedding.management.domain.entity.ExpensePayment;
import com.wedding.management.domain.entity.Gift;
import com.wedding.management.domain.entity.Guest;
import com.wedding.management.domain.entity.Invitation;
import com.wedding.management.domain.entity.OtherIncome;
import com.wedding.management.domain.entity.WeddingEvent;
import com.wedding.management.domain.entity.WeddingSettings;
import com.wedding.management.domain.enums.GiftType;
import com.wedding.management.domain.enums.GuestCategory;
import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.domain.enums.PaymentMethod;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.BudgetDtos.OtherIncomeResponse;
import com.wedding.management.dto.ExpenseDtos.CategoryStat;
import com.wedding.management.dto.ExpenseDtos.ExpenseResponse;
import com.wedding.management.dto.ExpenseDtos.ExpenseStats;
import com.wedding.management.dto.ExpenseDtos.VendorStat;
import com.wedding.management.dto.GiftDtos.GiftResponse;
import com.wedding.management.dto.GiftDtos.GiftStats;
import com.wedding.management.dto.ReportDtos.CountRow;
import com.wedding.management.dto.ReportDtos.ExpenseReport;
import com.wedding.management.dto.ReportDtos.FinancialReport;
import com.wedding.management.dto.ReportDtos.GiftReport;
import com.wedding.management.dto.ReportDtos.GiftSideRow;
import com.wedding.management.dto.ReportDtos.GuestReport;
import com.wedding.management.dto.ReportDtos.ReportFilter;
import com.wedding.management.dto.ReportDtos.ReportHeader;
import com.wedding.management.dto.ReportDtos.SideFinanceRow;
import com.wedding.management.repository.ExpenseCategoryRepository;
import com.wedding.management.repository.ExpensePaymentRepository;
import com.wedding.management.repository.ExpenseRepository;
import com.wedding.management.repository.GiftRepository;
import com.wedding.management.repository.GuestRepository;
import com.wedding.management.repository.InvitationRepository;
import com.wedding.management.repository.OtherIncomeRepository;
import com.wedding.management.repository.WeddingEventRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SettingsService settingsService;
    private final GuestRepository guestRepository;
    private final InvitationRepository invitationRepository;
    private final GiftRepository giftRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpensePaymentRepository paymentRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final OtherIncomeRepository otherIncomeRepository;
    private final WeddingEventRepository eventRepository;

    // ---- data helpers -------------------------------------------------------

    private ReportHeader header(ReportFilter f) {
        WeddingSettings s = settingsService.getEntity();
        String eventName = f.eventId() == null ? null : eventRepository.findById(f.eventId()).map(WeddingEvent::getName).orElse(null);
        return new ReportHeader(s.getGroomName(), s.getBrideName(), s.getWeddingDate(), LocalDate.now(SettingsService.ZONE),
                f.side() == null ? "Toàn bộ đám cưới" : f.side().label(), eventName, f.from(), f.to());
    }

    private List<Invitation> invitations(ReportFilter f) {
        return invitationRepository.findAllActiveWithGuestAndEvent().stream()
                .filter(i -> f.eventId() == null || i.getEvent().getId().equals(f.eventId()))
                .filter(i -> f.side() == null || i.getGuest().getSide() == f.side())
                .toList();
    }

    private List<Gift> gifts(ReportFilter f) {
        return giftRepository.findAllActiveWithRelations().stream()
                .filter(g -> f.side() == null || g.getSide() == f.side())
                .filter(g -> f.eventId() == null || (g.getEvent() != null && g.getEvent().getId().equals(f.eventId())))
                .filter(g -> inRange(g.getReceivedAt(), f))
                .sorted(Comparator.comparing(Gift::getReceivedAt).reversed())
                .toList();
    }

    private List<Expense> expenses(ReportFilter f) {
        return expenseRepository.findAllActiveWithRelations().stream()
                .filter(e -> f.side() == null || e.getSide() == f.side())
                .filter(e -> f.eventId() == null || (e.getEvent() != null && e.getEvent().getId().equals(f.eventId())))
                .filter(e -> (e.getExpenseDate() == null && e.getDueDate() == null) || inRange(e.getExpenseDate() != null ? e.getExpenseDate() : e.getDueDate(), f))
                .toList();
    }

    private static boolean inRange(LocalDate d, ReportFilter f) {
        if (d == null) return true;
        if (f.from() != null && d.isBefore(f.from())) return false;
        return f.to() == null || !d.isAfter(f.to());
    }

    // ---- guest report -------------------------------------------------------

    @Transactional(readOnly = true)
    public GuestReport guestReport(ReportFilter f) {
        List<Invitation> invitations = invitations(f);
        List<Guest> guests;
        if (f.eventId() != null) {
            Set<Long> ids = new HashSet<>();
            invitations.forEach(i -> ids.add(i.getGuest().getId()));
            guests = guestRepository.findByDeletedAtIsNull().stream().filter(g -> ids.contains(g.getId())).toList();
        } else {
            guests = f.side() == null ? guestRepository.findByDeletedAtIsNull() : guestRepository.findByDeletedAtIsNullAndSide(f.side());
        }
        Map<Long, List<Invitation>> byGuest = new HashMap<>();
        invitations.forEach(i -> byGuest.computeIfAbsent(i.getGuest().getId(), k -> new ArrayList<>()).add(i));

        int invited = 0, expected = 0, confirmed = 0, actual = 0;
        for (Invitation i : invitations) {
            invited += i.getInvitedCount();
            expected += i.getExpectedCount();
            confirmed += i.getConfirmedCount();
            actual += i.getActualCount();
        }
        long attended = 0, declined = 0, undecided = 0, groom = 0, bride = 0;
        Map<Side, int[]> sideAgg = new LinkedHashMap<>();
        Map<GuestCategory, int[]> catAgg = new LinkedHashMap<>();
        Map<InvitationStatus, int[]> statusAgg = new LinkedHashMap<>();
        for (Guest g : guests) {
            List<Invitation> list = byGuest.getOrDefault(g.getId(), List.of());
            InvitationStatus overall = InvitationService.overallStatus(list);
            if (overall == InvitationStatus.ATTENDED) attended++;
            else if (overall == InvitationStatus.DECLINED) declined++;
            else if (overall.isUndecided()) undecided++;
            if (g.getSide() == Side.GROOM) groom++;
            if (g.getSide() == Side.BRIDE) bride++;
            int[] s = sideAgg.computeIfAbsent(g.getSide(), k -> new int[4]);
            int[] c = catAgg.computeIfAbsent(g.getCategory(), k -> new int[4]);
            int[] st = statusAgg.computeIfAbsent(overall, k -> new int[4]);
            for (int[] acc : List.of(s, c, st)) {
                acc[0]++;
                for (Invitation i : list) {
                    acc[1] += i.getInvitedCount();
                    acc[2] += i.getConfirmedCount();
                    acc[3] += i.getActualCount();
                }
            }
        }
        List<CountRow> bySide = new ArrayList<>();
        for (Side s : Side.values()) {
            int[] a = sideAgg.getOrDefault(s, new int[4]);
            bySide.add(row(s.name(), s.label(), a));
        }
        List<CountRow> byCategory = new ArrayList<>();
        for (GuestCategory c : GuestCategory.values()) {
            int[] a = catAgg.get(c);
            if (a != null) byCategory.add(row(c.name(), c.label(), a));
        }
        List<CountRow> byStatus = new ArrayList<>();
        for (InvitationStatus s : InvitationStatus.values()) {
            int[] a = statusAgg.get(s);
            if (a != null) byStatus.add(row(s.name(), s.label(), a));
        }
        List<CountRow> byEvent = new ArrayList<>();
        for (WeddingEvent e : eventRepository.findAllByOrderByEventDateAscSortOrderAsc()) {
            if (f.eventId() != null && !e.getId().equals(f.eventId())) continue;
            int[] a = new int[4];
            for (Invitation i : invitations) {
                if (!i.getEvent().getId().equals(e.getId())) continue;
                a[0]++;
                a[1] += i.getInvitedCount();
                a[2] += i.getConfirmedCount();
                a[3] += i.getActualCount();
            }
            byEvent.add(row(String.valueOf(e.getId()), e.getName(), a));
        }
        double ratePeople = invited == 0 ? 0 : round1(actual * 100.0 / invited);
        double rateGuests = guests.isEmpty() ? 0 : round1(attended * 100.0 / guests.size());
        return new GuestReport(header(f), guests.size(), groom, bride, invited, expected, confirmed, actual, attended, declined, undecided,
                ratePeople, rateGuests, bySide, byCategory, byStatus, byEvent);
    }

    private static CountRow row(String key, String label, int[] a) {
        double rate = a[1] == 0 ? 0 : round1(a[3] * 100.0 / a[1]);
        return new CountRow(key, label, a[0], a[1], a[2], a[3], rate);
    }

    // ---- gift report --------------------------------------------------------

    @Transactional(readOnly = true)
    public GiftReport giftReport(ReportFilter f) {
        List<Gift> gifts = gifts(f);
        List<Guest> activeGuests = f.side() == null ? guestRepository.findByDeletedAtIsNull() : guestRepository.findByDeletedAtIsNullAndSide(f.side());
        GiftStats stats = GiftService.computeStats(gifts, activeGuests);
        List<GiftSideRow> bySide = stats.bySide().stream()
                .map(s -> new GiftSideRow(s.side(), s.label(), s.count(), s.guests(), s.amount(),
                        stats.totalAmount().signum() == 0 ? 0 : round1(s.amount().multiply(BigDecimal.valueOf(100)).divide(stats.totalAmount(), 1, RoundingMode.HALF_UP).doubleValue())))
                .toList();
        return new GiftReport(header(f), stats.totalAmount(), stats.giftCount(), stats.giverCount(), stats.cashAmount(), stats.transferAmount(),
                stats.inKindValue(), stats.inKindCount(), stats.averagePerGiver(), bySide, stats.byType(), stats.byEvent(),
                gifts.stream().map(GiftService::toResponse).toList());
    }

    // ---- expense report -----------------------------------------------------

    @Transactional(readOnly = true)
    public ExpenseReport expenseReport(ReportFilter f) {
        List<Expense> expenses = expenses(f);
        ExpenseStats stats = ExpenseService.computeStats(expenses, categoryRepository.findAllByOrderBySortOrderAscNameAsc(),
                settingsService.getEntity().getTotalBudget());
        List<ExpenseResponse> details = expenses.stream()
                .sorted(Comparator.comparing(Expense::getActualAmount).reversed())
                .map(e -> ExpenseService.toResponse(e, null, 0)).toList();
        List<ExpenseResponse> unpaid = details.stream().filter(e -> e.remainingAmount().signum() > 0).toList();
        return new ExpenseReport(header(f), stats.totalBudget(), stats.estimatedTotal(), stats.actualTotal(), stats.paidTotal(),
                stats.remainingTotal(), stats.expenseCount(), stats.overBudgetCount(), stats.overBudgetAmount(), stats.byCategory(),
                stats.bySide(), stats.byVendor(), stats.byStatus(), stats.overBudgetExpenses(), unpaid, details);
    }

    // ---- financial report ---------------------------------------------------

    @Transactional(readOnly = true)
    public FinancialReport financialReport(ReportFilter f) {
        List<Gift> gifts = gifts(f);
        List<Expense> expenses = expenses(f);
        List<Invitation> invitations = invitations(f);
        List<OtherIncome> incomes = otherIncomeRepository.findAllByOrderByReceivedAtDescIdDesc().stream()
                .filter(i -> f.side() == null || i.getSide() == f.side())
                .filter(i -> inRange(i.getReceivedAt(), f)).toList();
        Set<Long> expenseIds = new HashSet<>();
        expenses.forEach(e -> expenseIds.add(e.getId()));
        List<ExpensePayment> payments = paymentRepository.findAllActive().stream()
                .filter(p -> expenseIds.contains(p.getExpense().getId())).toList();

        BigDecimal giftTotal = BigDecimal.ZERO, cashIn = BigDecimal.ZERO, transferIn = BigDecimal.ZERO;
        Set<String> givers = new HashSet<>();
        for (Gift g : gifts) {
            givers.add(g.getGuest() != null ? "G" + g.getGuest().getId() : "N" + g.getGiverName());
            if (g.getAmount() == null) continue;
            giftTotal = giftTotal.add(g.getAmount());
            if (g.getGiftType() == GiftType.CASH) cashIn = cashIn.add(g.getAmount());
            if (g.getGiftType() == GiftType.BANK_TRANSFER) transferIn = transferIn.add(g.getAmount());
        }
        BigDecimal otherTotal = BigDecimal.ZERO;
        for (OtherIncome i : incomes) {
            otherTotal = otherTotal.add(i.getAmount());
            if (i.getMethod() == PaymentMethod.CASH) cashIn = cashIn.add(i.getAmount());
            if (i.getMethod() == PaymentMethod.BANK_TRANSFER) transferIn = transferIn.add(i.getAmount());
        }
        BigDecimal estimated = BigDecimal.ZERO, actual = BigDecimal.ZERO, paid = BigDecimal.ZERO;
        for (Expense e : expenses) {
            estimated = estimated.add(e.getEstimatedAmount());
            actual = actual.add(e.getActualAmount());
            paid = paid.add(e.getPaidAmount());
        }
        BigDecimal cashOut = BigDecimal.ZERO, transferOut = BigDecimal.ZERO;
        for (ExpensePayment p : payments) {
            if (p.getMethod() == PaymentMethod.CASH) cashOut = cashOut.add(p.getAmount());
            if (p.getMethod() == PaymentMethod.BANK_TRANSFER) transferOut = transferOut.add(p.getAmount());
        }
        int actualPeople = invitations.stream().mapToInt(Invitation::getActualCount).sum();
        BigDecimal totalIncome = giftTotal.add(otherTotal);
        BigDecimal totalBudget = settingsService.getEntity().getTotalBudget();

        List<SideFinanceRow> bySide = new ArrayList<>();
        for (Side side : Side.values()) {
            if (f.side() != null && f.side() != side) continue;
            long guests = invitations.stream().filter(i -> i.getGuest().getSide() == side).map(i -> i.getGuest().getId()).distinct().count();
            int people = invitations.stream().filter(i -> i.getGuest().getSide() == side).mapToInt(Invitation::getActualCount).sum();
            BigDecimal gift = gifts.stream().filter(g -> g.getSide() == side && g.getAmount() != null).map(Gift::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal other = incomes.stream().filter(i -> i.getSide() == side).map(OtherIncome::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal exp = expenses.stream().filter(e -> e.getSide() == side).map(Expense::getActualAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            bySide.add(new SideFinanceRow(side, side.label(), guests, people, gift, other, exp, gift.add(other).subtract(exp)));
        }
        return new FinancialReport(header(f), totalBudget, giftTotal, otherTotal, totalIncome, estimated, actual, paid,
                actual.subtract(paid).max(BigDecimal.ZERO), totalIncome.subtract(actual), totalIncome.subtract(actual.max(estimated)),
                totalBudget.subtract(actual), actualPeople, givers.size(),
                actualPeople == 0 ? null : actual.divide(BigDecimal.valueOf(actualPeople), 0, RoundingMode.HALF_UP),
                givers.isEmpty() ? null : giftTotal.divide(BigDecimal.valueOf(givers.size()), 0, RoundingMode.HALF_UP),
                cashIn, transferIn, cashOut, transferOut, bySide,
                incomes.stream().map(i -> new OtherIncomeResponse(i.getId(), i.getSource(), i.getSide(), i.getAmount(), i.getReceivedAt(),
                        i.getMethod(), i.getNote(), i.getCreatedAt())).toList());
    }

    private static double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    // ---- Excel export (all reports in one workbook) -------------------------

    @Transactional(readOnly = true)
    public byte[] exportAll(ReportFilter f) {
        GuestReport guestReport = guestReport(f);
        GiftReport giftReport = giftReport(f);
        ExpenseReport expenseReport = expenseReport(f);
        FinancialReport financial = financialReport(f);

        Workbook wb = ExcelUtil.newWorkbook();
        ExcelUtil.Styles st = ExcelUtil.styles(wb);

        // Sheet 1: financial summary
        Sheet s1 = wb.createSheet("Tổng kết tài chính");
        int r = 0;
        ExcelUtil.title(s1, r++, "BÁO CÁO TỔNG KẾT ĐÁM CƯỚI " + financial.header().groomName() + " & " + financial.header().brideName(), st);
        ExcelUtil.text(s1.createRow(r++), 0, "Phạm vi: " + financial.header().sideLabel()
                + (financial.header().eventName() != null ? " - " + financial.header().eventName() : "")
                + " | Ngày lập: " + financial.header().generatedAt());
        r++;
        r = kv(s1, r, st, "Ngân sách dự kiến", financial.totalBudget());
        r = kv(s1, r, st, "Tổng tiền mừng", financial.giftTotal());
        r = kv(s1, r, st, "Nguồn thu khác", financial.otherIncomeTotal());
        r = kv(s1, r, st, "TỔNG THU", financial.totalIncome());
        r = kv(s1, r, st, "Chi phí dự kiến", financial.expenseEstimated());
        r = kv(s1, r, st, "TỔNG CHI (thực tế)", financial.expenseActual());
        r = kv(s1, r, st, "Đã thanh toán", financial.expensePaid());
        r = kv(s1, r, st, "Còn phải thanh toán", financial.expenseRemaining());
        r = kv(s1, r, st, "SỐ DƯ (Thu - Chi)", financial.balance());
        r = kv(s1, r, st, "Ngân sách còn lại", financial.budgetRemaining());
        r = kv(s1, r, st, "Chi phí trung bình / khách thực tế", financial.costPerAttendee());
        r = kv(s1, r, st, "Tiền mừng trung bình / khách đã mừng", financial.giftPerGiver());
        r = kv(s1, r, st, "Tiền mặt nhận", financial.cashIn());
        r = kv(s1, r, st, "Chuyển khoản nhận", financial.transferIn());
        r = kv(s1, r, st, "Tiền mặt đã chi", financial.cashOut());
        r = kv(s1, r, st, "Chuyển khoản đã chi", financial.transferOut());
        r++;
        ExcelUtil.header(s1, r++, st, List.of("Bên", "Số khách", "Người thực tế", "Tiền mừng", "Thu khác", "Chi phí", "Số dư"));
        for (SideFinanceRow row : financial.bySide()) {
            Row x = s1.createRow(r++);
            ExcelUtil.text(x, 0, row.label());
            ExcelUtil.number(x, 1, row.guests());
            ExcelUtil.number(x, 2, row.actualPeople());
            ExcelUtil.money(x, 3, row.giftTotal(), st);
            ExcelUtil.money(x, 4, row.otherIncome(), st);
            ExcelUtil.money(x, 5, row.expenseActual(), st);
            ExcelUtil.money(x, 6, row.balance(), st);
        }
        ExcelUtil.autosize(s1, 7);

        // Sheet 2: guests
        Sheet s2 = wb.createSheet("Khách mời");
        r = 0;
        ExcelUtil.title(s2, r++, "BÁO CÁO KHÁCH MỜI", st);
        r = kvNum(s2, r, st, "Tổng số khách", guestReport.totalGuests());
        r = kvNum(s2, r, st, "Khách nhà trai", guestReport.groomGuests());
        r = kvNum(s2, r, st, "Khách nhà gái", guestReport.brideGuests());
        r = kvNum(s2, r, st, "Số người được mời", guestReport.invitedPeople());
        r = kvNum(s2, r, st, "Số người xác nhận", guestReport.confirmedPeople());
        r = kvNum(s2, r, st, "Số người thực tế tham dự", guestReport.actualPeople());
        ExcelUtil.text(s2.createRow(r++), 0, "Tỷ lệ tham dự (người): " + guestReport.attendanceRatePeople() + "%");
        r++;
        for (var section : List.of(
                Map.entry("Theo bên", guestReport.bySide()),
                Map.entry("Theo nhóm khách", guestReport.byCategory()),
                Map.entry("Theo trạng thái", guestReport.byStatus()),
                Map.entry("Theo sự kiện", guestReport.byEvent()))) {
            ExcelUtil.bold(s2.createRow(r++), 0, section.getKey(), st);
            ExcelUtil.header(s2, r++, st, List.of("Nhóm", "Số khách", "Người mời", "Người xác nhận", "Người thực tế", "Tỷ lệ tham dự %"));
            for (CountRow row : section.getValue()) {
                Row x = s2.createRow(r++);
                ExcelUtil.text(x, 0, row.label());
                ExcelUtil.number(x, 1, row.guests());
                ExcelUtil.number(x, 2, row.invitedPeople());
                ExcelUtil.number(x, 3, row.confirmedPeople());
                ExcelUtil.number(x, 4, row.actualPeople());
                ExcelUtil.number(x, 5, row.attendanceRate());
            }
            r++;
        }
        ExcelUtil.autosize(s2, 6);

        // Sheet 3: gifts
        Sheet s3 = wb.createSheet("Tiền mừng");
        r = 0;
        ExcelUtil.title(s3, r++, "BÁO CÁO TIỀN MỪNG", st);
        r = kv(s3, r, st, "Tổng tiền mừng", giftReport.totalAmount());
        r = kvNum(s3, r, st, "Số lượt mừng", giftReport.giftCount());
        r = kvNum(s3, r, st, "Số khách đã mừng", giftReport.giverCount());
        r = kv(s3, r, st, "Tiền mặt", giftReport.cashAmount());
        r = kv(s3, r, st, "Chuyển khoản", giftReport.transferAmount());
        r = kv(s3, r, st, "Vàng / hiện vật (giá trị quy đổi)", giftReport.inKindValue());
        r = kv(s3, r, st, "Trung bình / khách đã mừng", giftReport.averagePerGiver());
        r++;
        ExcelUtil.bold(s3.createRow(r++), 0, "Theo bên", st);
        ExcelUtil.header(s3, r++, st, List.of("Bên", "Số lượt", "Số khách", "Số tiền", "Tỷ lệ %"));
        for (GiftSideRow row : giftReport.bySide()) {
            Row x = s3.createRow(r++);
            ExcelUtil.text(x, 0, row.label());
            ExcelUtil.number(x, 1, row.count());
            ExcelUtil.number(x, 2, row.givers());
            ExcelUtil.money(x, 3, row.amount(), st);
            ExcelUtil.number(x, 4, row.percent());
        }
        r++;
        ExcelUtil.bold(s3.createRow(r++), 0, "Theo hình thức", st);
        ExcelUtil.header(s3, r++, st, List.of("Hình thức", "Số lượt", "Số tiền"));
        for (var row : giftReport.byType()) {
            Row x = s3.createRow(r++);
            ExcelUtil.text(x, 0, row.label());
            ExcelUtil.number(x, 1, row.count());
            ExcelUtil.money(x, 2, row.amount(), st);
        }
        r++;
        ExcelUtil.bold(s3.createRow(r++), 0, "Theo sự kiện", st);
        ExcelUtil.header(s3, r++, st, List.of("Sự kiện", "Số lượt", "Số khách", "Số tiền"));
        for (var row : giftReport.byEvent()) {
            Row x = s3.createRow(r++);
            ExcelUtil.text(x, 0, row.eventName());
            ExcelUtil.number(x, 1, row.count());
            ExcelUtil.number(x, 2, row.guests());
            ExcelUtil.money(x, 3, row.amount(), st);
        }
        r++;
        ExcelUtil.bold(s3.createRow(r++), 0, "Chi tiết từng khách", st);
        ExcelUtil.header(s3, r++, st, List.of("Khách mời", "Bên", "Sự kiện", "Hình thức", "Số tiền", "Hiện vật", "Ngày nhận"));
        for (GiftResponse g : giftReport.details()) {
            Row x = s3.createRow(r++);
            ExcelUtil.text(x, 0, g.giverName());
            ExcelUtil.text(x, 1, g.side().label());
            ExcelUtil.text(x, 2, g.eventName());
            ExcelUtil.text(x, 3, g.giftTypeLabel());
            ExcelUtil.money(x, 4, g.amount(), st);
            ExcelUtil.text(x, 5, g.itemsSummary());
            ExcelUtil.date(x, 6, g.receivedAt(), st);
        }
        ExcelUtil.autosize(s3, 7);

        // Sheet 4: expenses
        Sheet s4 = wb.createSheet("Chi phí");
        r = 0;
        ExcelUtil.title(s4, r++, "BÁO CÁO CHI PHÍ", st);
        r = kv(s4, r, st, "Ngân sách tổng", expenseReport.totalBudget());
        r = kv(s4, r, st, "Chi phí dự kiến", expenseReport.estimatedTotal());
        r = kv(s4, r, st, "Chi phí thực tế", expenseReport.actualTotal());
        r = kv(s4, r, st, "Đã thanh toán", expenseReport.paidTotal());
        r = kv(s4, r, st, "Còn phải thanh toán", expenseReport.remainingTotal());
        r = kv(s4, r, st, "Vượt ngân sách", expenseReport.overBudgetAmount());
        r++;
        ExcelUtil.bold(s4.createRow(r++), 0, "Theo danh mục", st);
        ExcelUtil.header(s4, r++, st, List.of("Danh mục", "Số khoản", "Ngân sách", "Dự kiến", "Thực tế", "Đã trả", "Còn nợ", "Tỷ lệ %", "Vượt NS"));
        for (CategoryStat c : expenseReport.byCategory()) {
            Row x = s4.createRow(r++);
            ExcelUtil.text(x, 0, c.categoryName());
            ExcelUtil.number(x, 1, c.count());
            ExcelUtil.money(x, 2, c.budget(), st);
            ExcelUtil.money(x, 3, c.estimated(), st);
            ExcelUtil.money(x, 4, c.actual(), st);
            ExcelUtil.money(x, 5, c.paid(), st);
            ExcelUtil.money(x, 6, c.remaining(), st);
            ExcelUtil.number(x, 7, c.percentOfTotal());
            ExcelUtil.text(x, 8, c.overBudget() ? "Có" : "");
        }
        r++;
        ExcelUtil.bold(s4.createRow(r++), 0, "Theo bên chịu chi phí", st);
        ExcelUtil.header(s4, r++, st, List.of("Bên", "Số khoản", "Dự kiến", "Thực tế", "Đã trả"));
        for (var s : expenseReport.bySide()) {
            Row x = s4.createRow(r++);
            ExcelUtil.text(x, 0, s.label());
            ExcelUtil.number(x, 1, s.count());
            ExcelUtil.money(x, 2, s.estimated(), st);
            ExcelUtil.money(x, 3, s.actual(), st);
            ExcelUtil.money(x, 4, s.paid(), st);
        }
        r++;
        ExcelUtil.bold(s4.createRow(r++), 0, "Theo nhà cung cấp", st);
        ExcelUtil.header(s4, r++, st, List.of("Nhà cung cấp", "Số khoản", "Thực tế", "Đã trả", "Còn nợ"));
        for (VendorStat v : expenseReport.byVendor()) {
            Row x = s4.createRow(r++);
            ExcelUtil.text(x, 0, v.vendorName());
            ExcelUtil.number(x, 1, v.count());
            ExcelUtil.money(x, 2, v.actual(), st);
            ExcelUtil.money(x, 3, v.paid(), st);
            ExcelUtil.money(x, 4, v.remaining(), st);
        }
        r++;
        ExcelUtil.bold(s4.createRow(r++), 0, "Chi tiết khoản chi", st);
        ExcelUtil.header(s4, r++, st, List.of("Khoản chi", "Danh mục", "Nhà cung cấp", "Bên", "Dự kiến", "Thực tế", "Đã trả", "Còn nợ", "Trạng thái", "Hạn thanh toán"));
        for (ExpenseResponse e : expenseReport.details()) {
            Row x = s4.createRow(r++);
            ExcelUtil.text(x, 0, e.name());
            ExcelUtil.text(x, 1, e.categoryName());
            ExcelUtil.text(x, 2, e.vendorName());
            ExcelUtil.text(x, 3, e.side().label());
            ExcelUtil.money(x, 4, e.estimatedAmount(), st);
            ExcelUtil.money(x, 5, e.actualAmount(), st);
            ExcelUtil.money(x, 6, e.paidAmount(), st);
            ExcelUtil.money(x, 7, e.remainingAmount(), st);
            ExcelUtil.text(x, 8, e.paymentStatusLabel());
            ExcelUtil.date(x, 9, e.dueDate(), st);
        }
        ExcelUtil.autosize(s4, 10);
        return ExcelUtil.toBytes(wb);
    }

    private static int kv(Sheet sheet, int r, ExcelUtil.Styles st, String label, BigDecimal value) {
        Row row = sheet.createRow(r);
        ExcelUtil.bold(row, 0, label, st);
        ExcelUtil.money(row, 1, value, st);
        return r + 1;
    }

    private static int kvNum(Sheet sheet, int r, ExcelUtil.Styles st, String label, long value) {
        Row row = sheet.createRow(r);
        ExcelUtil.bold(row, 0, label, st);
        ExcelUtil.number(row, 1, value);
        return r + 1;
    }
}
