package com.wedding.management.seed;

import com.wedding.management.config.AppProperties;
import com.wedding.management.domain.entity.Expense;
import com.wedding.management.domain.entity.ExpenseCategory;
import com.wedding.management.domain.entity.ExpensePayment;
import com.wedding.management.domain.entity.Gift;
import com.wedding.management.domain.entity.GiftItem;
import com.wedding.management.domain.entity.Guest;
import com.wedding.management.domain.entity.GuestGroup;
import com.wedding.management.domain.entity.Invitation;
import com.wedding.management.domain.entity.OtherIncome;
import com.wedding.management.domain.entity.TableArea;
import com.wedding.management.domain.entity.TableAssignment;
import com.wedding.management.domain.entity.Task;
import com.wedding.management.domain.entity.Vendor;
import com.wedding.management.domain.entity.WeddingEvent;
import com.wedding.management.domain.entity.WeddingSettings;
import com.wedding.management.domain.entity.WeddingTable;
import com.wedding.management.domain.enums.ContractStatus;
import com.wedding.management.domain.enums.EventStatus;
import com.wedding.management.domain.enums.Gender;
import com.wedding.management.domain.enums.GiftType;
import com.wedding.management.domain.enums.GuestCategory;
import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.domain.enums.PaymentMethod;
import com.wedding.management.domain.enums.PaymentType;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.domain.enums.TaskPriority;
import com.wedding.management.domain.enums.TaskStatus;
import com.wedding.management.repository.ExpenseCategoryRepository;
import com.wedding.management.repository.ExpensePaymentRepository;
import com.wedding.management.repository.ExpenseRepository;
import com.wedding.management.repository.GiftRepository;
import com.wedding.management.repository.GuestGroupRepository;
import com.wedding.management.repository.GuestRepository;
import com.wedding.management.repository.InvitationRepository;
import com.wedding.management.repository.OtherIncomeRepository;
import com.wedding.management.repository.TableAreaRepository;
import com.wedding.management.repository.TableAssignmentRepository;
import com.wedding.management.repository.TaskRepository;
import com.wedding.management.repository.VendorRepository;
import com.wedding.management.repository.WeddingEventRepository;
import com.wedding.management.repository.WeddingTableRepository;
import com.wedding.management.service.ExpenseService;
import com.wedding.management.service.InvitationService;
import com.wedding.management.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds realistic demo data once (dev profile / APP_SEED_DEMO_DATA=true).
 * It is idempotent: it runs only when the database has never been seeded and has no guests.
 * Production starts with a clean database.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private final AppProperties properties;
    private final SettingsService settingsService;
    private final GuestGroupRepository groupRepository;
    private final GuestRepository guestRepository;
    private final WeddingEventRepository eventRepository;
    private final InvitationRepository invitationRepository;
    private final InvitationService invitationService;
    private final TableAreaRepository areaRepository;
    private final WeddingTableRepository tableRepository;
    private final TableAssignmentRepository assignmentRepository;
    private final GiftRepository giftRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpensePaymentRepository paymentRepository;
    private final VendorRepository vendorRepository;
    private final TaskRepository taskRepository;
    private final OtherIncomeRepository otherIncomeRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.getSeed().isDemoData()) {
            return;
        }
        WeddingSettings settings = settingsService.getEntity();
        if (settings.isDemoSeeded() || guestRepository.count() > 0) {
            log.info("Demo data already present - seeding skipped");
            return;
        }
        log.info("Seeding demo data for development...");
        LocalDate today = LocalDate.now(SettingsService.ZONE);
        LocalDate weddingDate = LocalDate.of(2026, 12, 19);

        settings.setGroomName("Hoàng Quân");
        settings.setBrideName("Hoàng Thị Diệu Anh");
        settings.setWeddingDate(weddingDate);
        settings.setTotalBudget(bd(520_000_000));
        settings.setMainVenue("Trung tâm tiệc cưới Trống Đồng Palace, Hà Nội");
        settings.setGroomFamilyInfo("Ông Nguyễn Văn Bình & Bà Lê Thị Hoa - Số 12 Trần Duy Hưng, Cầu Giấy, Hà Nội");
        settings.setBrideFamilyInfo("Ông Trần Quốc Tuấn & Bà Phạm Thị Mai - Số 45 Lạch Tray, Ngô Quyền, Hải Phòng");
        settings.setDemoSeeded(true);

        // ---- events ----
        WeddingEvent damNgo = event("Lễ dạm ngõ", today.minusDays(75), LocalTime.of(9, 0), "Nhà gái", "45 Lạch Tray, Hải Phòng", Side.BOTH, EventStatus.COMPLETED, false, bd(15_000_000), 1);
        WeddingEvent anHoi = event("Lễ ăn hỏi", today.minusDays(5), LocalTime.of(8, 30), "Nhà gái", "45 Lạch Tray, Hải Phòng", Side.BOTH, EventStatus.COMPLETED, false, bd(60_000_000), 2);
        WeddingEvent tiecNhaGai = event("Tiệc cưới nhà gái", weddingDate.minusDays(1), LocalTime.of(17, 30), "Nhà hàng Hoa Sen Hải Phòng", "Số 1 Lê Hồng Phong, Hải Phòng", Side.BRIDE, EventStatus.CONFIRMED, false, bd(90_000_000), 3);
        WeddingEvent thanhHon = event("Lễ thành hôn & tiệc cưới nhà trai", weddingDate, LocalTime.of(11, 0), "Trống Đồng Palace", "Số 2 Trần Thái Tông, Cầu Giấy, Hà Nội", Side.BOTH, EventStatus.CONFIRMED, true, bd(180_000_000), 4);

        // ---- groups ----
        GuestGroup gdBacHung = group("Gia đình bác Hùng", Side.GROOM, GuestCategory.FAMILY, "Nguyễn Văn Hùng", "0912000001");
        GuestGroup gdChuThanh = group("Gia đình chú Thành", Side.GROOM, GuestCategory.RELATIVE, "Nguyễn Văn Thành", "0912000002");
        GuestGroup banDaiHoc = group("Nhóm bạn đại học Bách Khoa", Side.GROOM, GuestCategory.FRIEND, "Lê Hoàng Nam", "0988000003");
        GuestGroup hangXom = group("Hàng xóm khu Trần Duy Hưng", Side.GROOM, GuestCategory.NEIGHBOR, null, null);
        GuestGroup gdCoLan = group("Gia đình cô Lan", Side.BRIDE, GuestCategory.RELATIVE, "Trần Thị Lan", "0903000004");
        GuestGroup dongNghiep = group("Đồng nghiệp công ty FPT Software", Side.BRIDE, GuestCategory.COLLEAGUE, "Phạm Thu Trang", "0903000005");
        GuestGroup banCap3 = group("Bạn cấp 3 Ngô Quyền", Side.BRIDE, GuestCategory.FRIEND, "Vũ Minh Anh", "0903000006");
        GuestGroup khachBoMe = group("Khách của bố mẹ cô dâu", Side.BRIDE, GuestCategory.PARENTS_GUEST, null, null);

        // ---- guests (20 groom, 20 bride) ----
        List<Guest> groomGuests = new ArrayList<>();
        groomGuests.add(guest("Nguyễn Văn Hùng", Gender.MALE, "0912000001", "Cầu Giấy, Hà Nội", Side.GROOM, "Bác ruột chú rể", GuestCategory.FAMILY, gdBacHung, true));
        groomGuests.add(guest("Lê Thị Thu", Gender.FEMALE, "0912000011", "Cầu Giấy, Hà Nội", Side.GROOM, "Bác gái chú rể", GuestCategory.FAMILY, gdBacHung, false));
        groomGuests.add(guest("Nguyễn Hùng Anh", Gender.MALE, "0912000012", "Cầu Giấy, Hà Nội", Side.GROOM, "Anh họ chú rể", GuestCategory.FAMILY, gdBacHung, false));
        groomGuests.add(guest("Nguyễn Văn Thành", Gender.MALE, "0912000002", "Thanh Xuân, Hà Nội", Side.GROOM, "Chú ruột chú rể", GuestCategory.RELATIVE, gdChuThanh, true));
        groomGuests.add(guest("Phạm Thị Hằng", Gender.FEMALE, "0912000021", "Thanh Xuân, Hà Nội", Side.GROOM, "Thím chú rể", GuestCategory.RELATIVE, gdChuThanh, false));
        groomGuests.add(guest("Nguyễn Thành Đạt", Gender.MALE, "0912000022", "Thanh Xuân, Hà Nội", Side.GROOM, "Em họ chú rể", GuestCategory.RELATIVE, gdChuThanh, false));
        groomGuests.add(guest("Lê Hoàng Nam", Gender.MALE, "0988000003", "Đống Đa, Hà Nội", Side.GROOM, "Bạn thân đại học", GuestCategory.FRIEND, banDaiHoc, false));
        groomGuests.add(guest("Trần Quang Huy", Gender.MALE, "0988000031", "Hai Bà Trưng, Hà Nội", Side.GROOM, "Bạn đại học", GuestCategory.FRIEND, banDaiHoc, false));
        groomGuests.add(guest("Đỗ Minh Tuấn", Gender.MALE, "0988000032", "Hoàng Mai, Hà Nội", Side.GROOM, "Bạn đại học", GuestCategory.FRIEND, banDaiHoc, false));
        groomGuests.add(guest("Phan Văn Khoa", Gender.MALE, "0988000033", "Long Biên, Hà Nội", Side.GROOM, "Bạn đại học", GuestCategory.FRIEND, banDaiHoc, false));
        groomGuests.add(guest("Bùi Thị Ngọc", Gender.FEMALE, "0988000034", "Hà Đông, Hà Nội", Side.GROOM, "Bạn đại học", GuestCategory.FRIEND, banDaiHoc, false));
        groomGuests.add(guest("Hoàng Văn Sơn", Gender.MALE, "0977000041", "Cầu Giấy, Hà Nội", Side.GROOM, "Hàng xóm", GuestCategory.NEIGHBOR, hangXom, false));
        groomGuests.add(guest("Vũ Thị Hường", Gender.FEMALE, "0977000042", "Cầu Giấy, Hà Nội", Side.GROOM, "Hàng xóm", GuestCategory.NEIGHBOR, hangXom, false));
        groomGuests.add(guest("Đặng Quốc Việt", Gender.MALE, "0977000043", "Cầu Giấy, Hà Nội", Side.GROOM, "Hàng xóm", GuestCategory.NEIGHBOR, hangXom, false));
        groomGuests.add(guest("Ngô Đức Thắng", Gender.MALE, "0966000051", "Nam Từ Liêm, Hà Nội", Side.GROOM, "Đồng nghiệp chú rể (VNPT)", GuestCategory.COLLEAGUE, null, false));
        groomGuests.add(guest("Trịnh Thu Phương", Gender.FEMALE, "0966000052", "Bắc Từ Liêm, Hà Nội", Side.GROOM, "Đồng nghiệp chú rể (VNPT)", GuestCategory.COLLEAGUE, null, false));
        groomGuests.add(guest("Lương Văn Tài", Gender.MALE, "0966000053", "Tây Hồ, Hà Nội", Side.GROOM, "Sếp của chú rể", GuestCategory.COLLEAGUE, null, true));
        groomGuests.add(guest("Mai Xuân Trường", Gender.MALE, "0955000061", "Ba Đình, Hà Nội", Side.GROOM, "Bạn của bố chú rể", GuestCategory.PARENTS_GUEST, null, false));
        groomGuests.add(guest("Cao Thị Bích", Gender.FEMALE, "0955000062", "Ba Đình, Hà Nội", Side.GROOM, "Bạn của mẹ chú rể", GuestCategory.PARENTS_GUEST, null, false));
        groomGuests.add(guest("Đinh Công Minh", Gender.MALE, "0955000063", "Gia Lâm, Hà Nội", Side.GROOM, "Bạn cấp 3 chú rể", GuestCategory.FRIEND, null, false));

        List<Guest> brideGuests = new ArrayList<>();
        brideGuests.add(guest("Trần Thị Lan", Gender.FEMALE, "0903000004", "Ngô Quyền, Hải Phòng", Side.BRIDE, "Cô ruột cô dâu", GuestCategory.RELATIVE, gdCoLan, true));
        brideGuests.add(guest("Lê Văn Dũng", Gender.MALE, "0903000041", "Ngô Quyền, Hải Phòng", Side.BRIDE, "Chú (chồng cô Lan)", GuestCategory.RELATIVE, gdCoLan, false));
        brideGuests.add(guest("Lê Thu Trang", Gender.FEMALE, "0903000042", "Ngô Quyền, Hải Phòng", Side.BRIDE, "Em họ cô dâu", GuestCategory.RELATIVE, gdCoLan, false));
        brideGuests.add(guest("Trần Quốc Hưng", Gender.MALE, "0903000043", "Lê Chân, Hải Phòng", Side.BRIDE, "Bác ruột cô dâu", GuestCategory.FAMILY, null, true));
        brideGuests.add(guest("Phạm Thị Nga", Gender.FEMALE, "0903000044", "Lê Chân, Hải Phòng", Side.BRIDE, "Dì ruột cô dâu", GuestCategory.FAMILY, null, false));
        brideGuests.add(guest("Phạm Thu Trang", Gender.FEMALE, "0903000005", "Cầu Giấy, Hà Nội", Side.BRIDE, "Đồng nghiệp cô dâu", GuestCategory.COLLEAGUE, dongNghiep, false));
        brideGuests.add(guest("Nguyễn Hải Yến", Gender.FEMALE, "0903000051", "Cầu Giấy, Hà Nội", Side.BRIDE, "Đồng nghiệp cô dâu", GuestCategory.COLLEAGUE, dongNghiep, false));
        brideGuests.add(guest("Đào Văn Long", Gender.MALE, "0903000052", "Nam Từ Liêm, Hà Nội", Side.BRIDE, "Đồng nghiệp cô dâu", GuestCategory.COLLEAGUE, dongNghiep, false));
        brideGuests.add(guest("Hồ Thị Kim Chi", Gender.FEMALE, "0903000053", "Thanh Xuân, Hà Nội", Side.BRIDE, "Quản lý của cô dâu", GuestCategory.COLLEAGUE, dongNghiep, true));
        brideGuests.add(guest("Vũ Minh Anh", Gender.FEMALE, "0903000006", "Hồng Bàng, Hải Phòng", Side.BRIDE, "Bạn thân cấp 3", GuestCategory.FRIEND, banCap3, false));
        brideGuests.add(guest("Nguyễn Thùy Linh", Gender.FEMALE, "0903000061", "Hồng Bàng, Hải Phòng", Side.BRIDE, "Bạn cấp 3", GuestCategory.FRIEND, banCap3, false));
        brideGuests.add(guest("Bùi Đức Anh", Gender.MALE, "0903000062", "Kiến An, Hải Phòng", Side.BRIDE, "Bạn cấp 3", GuestCategory.FRIEND, banCap3, false));
        brideGuests.add(guest("Lý Thanh Tùng", Gender.MALE, "0903000063", "Hải An, Hải Phòng", Side.BRIDE, "Bạn cấp 3", GuestCategory.FRIEND, banCap3, false));
        brideGuests.add(guest("Trương Thị Hạnh", Gender.FEMALE, "0903000064", "Ngô Quyền, Hải Phòng", Side.BRIDE, "Bạn đại học cô dâu", GuestCategory.FRIEND, null, false));
        brideGuests.add(guest("Đoàn Văn Phúc", Gender.MALE, "0903000071", "Ngô Quyền, Hải Phòng", Side.BRIDE, "Bạn của bố cô dâu", GuestCategory.PARENTS_GUEST, khachBoMe, false));
        brideGuests.add(guest("Lại Thị Thanh", Gender.FEMALE, "0903000072", "Ngô Quyền, Hải Phòng", Side.BRIDE, "Bạn của mẹ cô dâu", GuestCategory.PARENTS_GUEST, khachBoMe, false));
        brideGuests.add(guest("Tô Văn Kiên", Gender.MALE, "0903000073", "Ngô Quyền, Hải Phòng", Side.BRIDE, "Đồng nghiệp bố cô dâu", GuestCategory.PARENTS_GUEST, khachBoMe, false));
        brideGuests.add(guest("Phùng Thị Dung", Gender.FEMALE, "0903000081", "Ngô Quyền, Hải Phòng", Side.BRIDE, "Hàng xóm", GuestCategory.NEIGHBOR, null, false));
        brideGuests.add(guest("Chu Văn Lâm", Gender.MALE, "0903000082", "Ngô Quyền, Hải Phòng", Side.BRIDE, "Hàng xóm", GuestCategory.NEIGHBOR, null, false));
        brideGuests.add(guest("Dương Thị Hoa", Gender.FEMALE, "0903000083", "Ngô Quyền, Hải Phòng", Side.BRIDE, "Bạn cũ của cô dâu", GuestCategory.FRIEND, null, false));

        // ---- invitations ----
        InvitationStatus[] mainStatuses = {InvitationStatus.CONFIRMED, InvitationStatus.CONFIRMED, InvitationStatus.SENT, InvitationStatus.CONFIRMED,
                InvitationStatus.PENDING, InvitationStatus.CONFIRMED, InvitationStatus.DECLINED, InvitationStatus.CONFIRMED, InvitationStatus.NOT_SENT,
                InvitationStatus.CONFIRMED};
        int idx = 0;
        List<Guest> all = new ArrayList<>(groomGuests);
        all.addAll(brideGuests);
        Map<Long, Invitation> mainInvitations = new HashMap<>();
        for (Guest g : all) {
            int invited = g.getCategory() == GuestCategory.FAMILY || g.getCategory() == GuestCategory.RELATIVE || g.getGroup() == gdBacHung ? 2 : 1;
            if (g.getFullName().equals("Lê Hoàng Nam") || g.getFullName().equals("Vũ Minh Anh")) invited = 2;
            InvitationStatus status = mainStatuses[idx++ % mainStatuses.length];
            Invitation inv = invitation(g, thanhHon, invited, status, status == InvitationStatus.CONFIRMED ? invited : null);
            mainInvitations.put(g.getId(), inv);
            if (g.getSide() == Side.BRIDE) {
                // bride side is also invited to the bride's party
                invitation(g, tiecNhaGai, invited, status == InvitationStatus.DECLINED ? InvitationStatus.DECLINED : InvitationStatus.CONFIRMED, invited);
            }
            boolean family = g.getCategory() == GuestCategory.FAMILY || g.getCategory() == GuestCategory.RELATIVE;
            if (family) {
                // family attended the engagement ceremony
                Invitation anHoiInv = invitation(g, anHoi, invited, InvitationStatus.ATTENDED, invited);
                anHoiInv.setActualCount(invited);
                anHoiInv.setCheckedInAt(anHoi.getEventDate().atTime(8, 15 + (idx % 40)).atZone(SettingsService.ZONE).toInstant());
                anHoiInv.setCheckedInBy("admin");
                invitationRepository.save(anHoiInv);
                if (g.isVip()) {
                    Invitation dn = invitation(g, damNgo, invited, InvitationStatus.ATTENDED, invited);
                    dn.setCheckedInAt(damNgo.getEventDate().atTime(9, 5).atZone(SettingsService.ZONE).toInstant());
                    invitationRepository.save(dn);
                }
            }
        }

        // ---- seating for the main event ----
        TableArea khuA = area(thanhHon, "Khu A - Nhà trai", "Dãy bàn bên trái sân khấu", 1);
        TableArea khuB = area(thanhHon, "Khu B - Nhà gái", "Dãy bàn bên phải sân khấu", 2);
        List<WeddingTable> groomTables = new ArrayList<>();
        List<WeddingTable> brideTables = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            groomTables.add(table(thanhHon, khuA, String.format("Bàn A%02d", i), 10, Side.GROOM, i, 1));
            brideTables.add(table(thanhHon, khuB, String.format("Bàn B%02d", i), 10, Side.BRIDE, i, 2));
        }
        groomTables.add(table(thanhHon, khuA, "Bàn VIP gia đình", 12, Side.BOTH, 0, 0));
        seat(groomGuests, groomTables, mainInvitations, 4);
        seat(brideGuests, brideTables, mainInvitations, 4);

        // ---- gifts ----
        LocalDate anHoiDate = anHoi.getEventDate();
        gift(groomGuests.get(0), anHoi, GiftType.CASH, 5_000_000, anHoiDate, "Bác Hùng mừng ăn hỏi");
        gift(groomGuests.get(3), anHoi, GiftType.CASH, 3_000_000, anHoiDate, null);
        gift(brideGuests.get(0), anHoi, GiftType.GOLD, null, anHoiDate, "Cô Lan tặng kiềng vàng", item("Kiềng vàng 24K", "2", "chỉ", 15_000_000));
        gift(brideGuests.get(3), anHoi, GiftType.CASH, 5_000_000, anHoiDate, "Bác Hưng mừng ăn hỏi");
        gift(brideGuests.get(4), anHoi, GiftType.GOLD, null, anHoiDate, "Dì Nga tặng nhẫn", item("Nhẫn vàng 9999", "1", "chỉ", null));
        gift(groomGuests.get(1), anHoi, GiftType.BANK_TRANSFER, 2_000_000, anHoiDate.plusDays(1), null);
        // early transfers for the wedding
        gift(groomGuests.get(6), thanhHon, GiftType.BANK_TRANSFER, 1_000_000, today.minusDays(3), "Nam chuyển khoản trước vì đi công tác");
        gift(groomGuests.get(7), thanhHon, GiftType.BANK_TRANSFER, 1_000_000, today.minusDays(2), null);
        gift(groomGuests.get(16), thanhHon, GiftType.BANK_TRANSFER, 3_000_000, today.minusDays(1), "Sếp mừng trước");
        gift(brideGuests.get(8), thanhHon, GiftType.BANK_TRANSFER, 2_000_000, today.minusDays(2), null);
        gift(brideGuests.get(5), thanhHon, GiftType.BANK_TRANSFER, 1_000_000, today.minusDays(4), null);
        gift(brideGuests.get(9), thanhHon, GiftType.IN_KIND, null, today.minusDays(6), "Minh Anh tặng quà", item("Bộ chăn ga gối Everon", "1", "bộ", 3_500_000));
        gift(groomGuests.get(14), thanhHon, GiftType.CASH, 500_000, today.minusDays(1), "Gửi trước qua đồng nghiệp");
        gift(groomGuests.get(0), thanhHon, GiftType.CASH, 10_000_000, today.minusDays(1), "Bác Hùng mừng cưới (lần 2, khác lễ ăn hỏi)");
        giftFromOutsider("Công ty VNPT Hà Nội", Side.GROOM, thanhHon, GiftType.BANK_TRANSFER, 5_000_000, today.minusDays(2), "Quà mừng của cơ quan chú rể");

        // ---- vendors ----
        Vendor nhaHang = vendor("Trống Đồng Palace", "Nhà hàng tiệc cưới", "Chị Thu Hiền", "02437851234", "Số 2 Trần Thái Tông, Cầu Giấy, Hà Nội",
                "Tiệc cưới 45 bàn, menu 4.200.000đ/bàn, miễn phí trang trí cơ bản", 189_000_000, 50_000_000, ContractStatus.DEPOSITED, today.minusDays(60), 5);
        Vendor studio = vendor("Tuấn Nguyễn Studio", "Chụp ảnh & quay phim", "Anh Tuấn", "0912345678", "Hoàn Kiếm, Hà Nội",
                "Gói chụp ảnh cưới ngoại cảnh + phóng sự cưới 2 ngày", 32_000_000, 10_000_000, ContractStatus.DEPOSITED, today.minusDays(50), 4);
        Vendor trangTri = vendor("Hoa & Decor Lavender", "Trang trí", "Chị Lan Anh", "0987654321", "Đống Đa, Hà Nội",
                "Trang trí sân khấu, cổng hoa, bàn gallery", 28_000_000, 5_000_000, ContractStatus.CONTRACTED, today.minusDays(30), null);
        Vendor makeup = vendor("Linh Makeup Artist", "Makeup", "Linh", "0978111222", "Hai Bà Trưng, Hà Nội",
                "Makeup cô dâu 2 ngày + 2 mẹ", 9_000_000, 2_000_000, ContractStatus.DEPOSITED, today.minusDays(25), 5);
        Vendor thiep = vendor("In ấn Thiệp Cưới Hồng Phúc", "In thiệp", "Anh Phúc", "0966333444", "Thanh Xuân, Hà Nội",
                "In 300 thiệp cưới giấy mỹ thuật", 4_500_000, 0, ContractStatus.COMPLETED, today.minusDays(40), 4);
        Vendor xe = vendor("Xe cưới Hà Thành", "Thuê xe", "Anh Hải", "0909555666", "Long Biên, Hà Nội",
                "Xe hoa Mercedes E300 + 2 xe 45 chỗ đưa đón Hải Phòng - Hà Nội", 14_000_000, 0, ContractStatus.NEGOTIATING, null, null);
        Vendor nhaHangHP = vendor("Nhà hàng Hoa Sen Hải Phòng", "Nhà hàng tiệc cưới", "Anh Hoàng", "02253123456", "Lê Hồng Phong, Hải Phòng",
                "Tiệc nhà gái 25 bàn", 95_000_000, 20_000_000, ContractStatus.DEPOSITED, today.minusDays(45), null);

        // ---- category budgets ----
        Map<String, ExpenseCategory> cats = new HashMap<>();
        categoryRepository.findAll().forEach(c -> cats.put(c.getName(), c));
        budget(cats, "Nhà hàng / tiệc cưới", 290_000_000);
        budget(cats, "Chụp ảnh cưới", 22_000_000);
        budget(cats, "Quay phim", 15_000_000);
        budget(cats, "Trang trí sân khấu", 25_000_000);
        budget(cats, "Trang trí gia đình", 8_000_000);
        budget(cats, "Váy cưới", 15_000_000);
        budget(cats, "Vest chú rể", 8_000_000);
        budget(cats, "Makeup", 10_000_000);
        budget(cats, "Nhẫn cưới", 28_000_000);
        budget(cats, "Thiệp cưới", 6_000_000);
        budget(cats, "Xe đưa đón", 15_000_000);
        budget(cats, "Lễ ăn hỏi", 15_000_000);
        budget(cats, "Tráp cưới", 10_000_000);
        budget(cats, "Quà cảm ơn", 5_000_000);
        budget(cats, "Chi phí phát sinh", 10_000_000);

        // ---- expenses & payments ----
        Expense tiecNhaTrai = expense("Tiệc cưới nhà trai (45 bàn)", cats.get("Nhà hàng / tiệc cưới"), nhaHang, thanhHon, Side.GROOM, 180_000_000, 189_000_000, weddingDate.plusDays(1), null, "Đã chốt 45 bàn, menu Trống Đồng 2");
        pay(tiecNhaTrai, 50_000_000, today.minusDays(60), PaymentType.DEPOSIT, PaymentMethod.BANK_TRANSFER, "Bố chú rể");
        pay(tiecNhaTrai, 40_000_000, today.minusDays(10), PaymentType.PARTIAL, PaymentMethod.BANK_TRANSFER, "Chú rể");
        Expense tiecGai = expense("Tiệc cưới nhà gái (25 bàn)", cats.get("Nhà hàng / tiệc cưới"), nhaHangHP, tiecNhaGai, Side.BRIDE, 90_000_000, 95_000_000, weddingDate, null, null);
        pay(tiecGai, 20_000_000, today.minusDays(45), PaymentType.DEPOSIT, PaymentMethod.CASH, "Bố cô dâu");
        Expense anh = expense("Chụp ảnh cưới ngoại cảnh", cats.get("Chụp ảnh cưới"), studio, null, Side.BOTH, 18_000_000, 20_000_000, today.minusDays(5), today.minusDays(20), "Chụp tại Hồ Gươm & Ninh Bình");
        pay(anh, 10_000_000, today.minusDays(50), PaymentType.DEPOSIT, PaymentMethod.BANK_TRANSFER, "Chú rể");
        pay(anh, 10_000_000, today.minusDays(5), PaymentType.FINAL, PaymentMethod.BANK_TRANSFER, "Chú rể");
        Expense phim = expense("Quay phóng sự cưới", cats.get("Quay phim"), studio, thanhHon, Side.BOTH, 12_000_000, 12_000_000, weddingDate.plusDays(3), null, null);
        Expense decor = expense("Trang trí sân khấu & cổng hoa", cats.get("Trang trí sân khấu"), trangTri, thanhHon, Side.GROOM, 25_000_000, 28_000_000, weddingDate.minusDays(2), null, "Tông màu champagne - burgundy");
        pay(decor, 5_000_000, today.minusDays(30), PaymentType.DEPOSIT, PaymentMethod.BANK_TRANSFER, "Chú rể");
        Expense decorNha = expense("Trang trí nhà trai & nhà gái", cats.get("Trang trí gia đình"), trangTri, null, Side.BOTH, 8_000_000, 7_500_000, weddingDate.minusDays(2), null, null);
        Expense vay = expense("Thuê váy cưới (2 váy)", cats.get("Váy cưới"), null, null, Side.BRIDE, 15_000_000, 16_500_000, today.minusDays(15), today.minusDays(15), "Váy Hacchic Bridal");
        pay(vay, 16_500_000, today.minusDays(15), PaymentType.FINAL, PaymentMethod.BANK_TRANSFER, "Cô dâu");
        Expense vest = expense("May vest chú rể", cats.get("Vest chú rể"), null, null, Side.GROOM, 8_000_000, 7_200_000, today.minusDays(20), today.minusDays(20), null);
        pay(vest, 7_200_000, today.minusDays(20), PaymentType.FINAL, PaymentMethod.CASH, "Chú rể");
        Expense mk = expense("Makeup cô dâu & 2 mẹ", cats.get("Makeup"), makeup, null, Side.BRIDE, 9_000_000, 9_000_000, weddingDate, null, null);
        pay(mk, 2_000_000, today.minusDays(25), PaymentType.DEPOSIT, PaymentMethod.BANK_TRANSFER, "Cô dâu");
        Expense nhan = expense("Nhẫn cưới PNJ", cats.get("Nhẫn cưới"), null, null, Side.BOTH, 25_000_000, 27_800_000, today.minusDays(35), today.minusDays(35), "Cặp nhẫn vàng trắng 18K");
        pay(nhan, 27_800_000, today.minusDays(35), PaymentType.FINAL, PaymentMethod.BANK_TRANSFER, "Chú rể");
        Expense thiepE = expense("In 300 thiệp cưới", cats.get("Thiệp cưới"), thiep, null, Side.BOTH, 4_500_000, 4_500_000, today.minusDays(40), today.minusDays(40), null);
        pay(thiepE, 4_500_000, today.minusDays(40), PaymentType.FINAL, PaymentMethod.CASH, "Chú rể");
        Expense xeE = expense("Xe hoa + 2 xe 45 chỗ", cats.get("Xe đưa đón"), xe, thanhHon, Side.GROOM, 12_000_000, 14_000_000, weddingDate, null, "Đang thương lượng giảm giá");
        Expense anHoiE = expense("Lễ ăn hỏi: tráp, đội bê tráp, trầu cau", cats.get("Lễ ăn hỏi"), null, anHoi, Side.GROOM, 15_000_000, 13_200_000, anHoiDate, anHoiDate, null);
        pay(anHoiE, 13_200_000, anHoiDate, PaymentType.FINAL, PaymentMethod.CASH, "Mẹ chú rể");
        Expense trap = expense("7 tráp ăn hỏi", cats.get("Tráp cưới"), null, anHoi, Side.GROOM, 10_000_000, 9_500_000, anHoiDate.minusDays(1), anHoiDate.minusDays(1), null);
        pay(trap, 9_500_000, anHoiDate.minusDays(1), PaymentType.FINAL, PaymentMethod.CASH, "Mẹ chú rể");
        Expense qua = expense("Quà cảm ơn khách (300 hộp)", cats.get("Quà cảm ơn"), null, thanhHon, Side.BOTH, 5_000_000, 0, weddingDate.minusDays(5), null, "Chưa đặt");
        Expense phatSinh = expense("Tiệc cảm ơn bạn bè sau cưới", cats.get("Chi phí phát sinh"), null, null, Side.BOTH, 0, 6_000_000, weddingDate.plusDays(2), null, null);

        // ---- other incomes ----
        income("Bố mẹ chú rể hỗ trợ", Side.GROOM, 100_000_000, today.minusDays(70), PaymentMethod.BANK_TRANSFER);
        income("Bố mẹ cô dâu hỗ trợ", Side.BRIDE, 50_000_000, today.minusDays(50), PaymentMethod.CASH);
        income("Tiền tiết kiệm của cô dâu chú rể", Side.BOTH, 80_000_000, today.minusDays(90), PaymentMethod.BANK_TRANSFER);

        // ---- tasks ----
        task("Chọn ngày cưới", "Xem ngày với hai gia đình", "Chú rể", today.minusDays(120), today.minusDays(110), TaskPriority.HIGH, TaskStatus.DONE, null, 1);
        task("Đặt nhà hàng tiệc cưới", "Chốt Trống Đồng Palace, đặt cọc 50 triệu", "Chú rể", today.minusDays(90), today.minusDays(60), TaskPriority.URGENT, TaskStatus.DONE, thanhHon, 2);
        task("Chụp ảnh cưới", "Ngoại cảnh Hồ Gươm & Ninh Bình", "Cô dâu", today.minusDays(40), today.minusDays(5), TaskPriority.HIGH, TaskStatus.DONE, null, 3);
        task("Chọn trang phục", "Váy cưới, vest, áo dài ăn hỏi", "Cô dâu", today.minusDays(45), today.minusDays(15), TaskPriority.MEDIUM, TaskStatus.DONE, null, 4);
        task("Đặt in thiệp cưới", "300 thiệp giấy mỹ thuật", "Chú rể", today.minusDays(45), today.minusDays(40), TaskPriority.MEDIUM, TaskStatus.DONE, null, 5);
        task("Gửi thiệp mời", "Gửi thiệp cho toàn bộ khách, ưu tiên khách ở xa", "Hai bên gia đình", today.minusDays(14), today.plusDays(3), TaskPriority.URGENT, TaskStatus.IN_PROGRESS, thanhHon, 6);
        task("Chốt danh sách khách mời", "Xác nhận số lượng với từng nhóm khách", "Chú rể", today.minusDays(7), today.plusDays(7), TaskPriority.HIGH, TaskStatus.IN_PROGRESS, thanhHon, 7);
        task("Chuẩn bị nhẫn cưới", "Đã mua tại PNJ, lấy nhẫn trước 3 ngày", "Chú rể", today.minusDays(35), today.plusDays(15), TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, null, 8);
        task("Chuẩn bị lễ vật ăn hỏi", "7 tráp, đội bê tráp 7 nam 7 nữ", "Mẹ chú rể", today.minusDays(20), today.minusDays(6), TaskPriority.HIGH, TaskStatus.DONE, anHoi, 9);
        task("Đặt xe hoa và xe đưa đón", "Chốt giá với Xe cưới Hà Thành", "Bố chú rể", today.minusDays(5), today.plusDays(5), TaskPriority.HIGH, TaskStatus.TODO, thanhHon, 10);
        task("Xác nhận số lượng bàn với nhà hàng", "Báo số bàn chính thức trước 5 ngày", "Chú rể", today.plusDays(10), today.plusDays(15), TaskPriority.URGENT, TaskStatus.TODO, thanhHon, 11);
        task("Xác nhận thực đơn", "Chốt menu và đồ uống", "Cô dâu", today.plusDays(5), today.plusDays(12), TaskPriority.MEDIUM, TaskStatus.TODO, thanhHon, 12);
        task("Xếp sơ đồ bàn tiệc", "Xếp bàn theo nhóm khách, in sơ đồ dán tại sảnh", "Cô dâu", today.plusDays(8), today.plusDays(17), TaskPriority.HIGH, TaskStatus.TODO, thanhHon, 13);
        task("Thanh toán các khoản còn lại", "Nhà hàng, trang trí, makeup, xe", "Chú rể", today.plusDays(18), today.plusDays(22), TaskPriority.HIGH, TaskStatus.TODO, null, 14);
        task("Đặt quà cảm ơn khách", "300 hộp bánh kèm thiệp cảm ơn", "Cô dâu", today.minusDays(3), today.minusDays(1), TaskPriority.MEDIUM, TaskStatus.TODO, thanhHon, 15);
        task("Tập luyện tiết mục & kịch bản MC", "Duyệt kịch bản với MC và ban nhạc", "Chú rể", today.plusDays(12), today.plusDays(18), TaskPriority.LOW, TaskStatus.TODO, thanhHon, 16);

        log.info("Demo data seeded: {} guests, {} events, {} gifts, {} expenses, {} tasks",
                guestRepository.count(), eventRepository.count(), giftRepository.count(), expenseRepository.count(), taskRepository.count());
    }

    // ---- helpers --------------------------------------------------------------

    private static BigDecimal bd(long v) {
        return BigDecimal.valueOf(v);
    }

    private WeddingEvent event(String name, LocalDate date, LocalTime time, String venue, String address, Side side, EventStatus status,
                               boolean main, BigDecimal budget, int order) {
        WeddingEvent e = new WeddingEvent();
        e.setName(name);
        e.setEventDate(date);
        e.setStartTime(time);
        e.setVenue(venue);
        e.setAddress(address);
        e.setSide(side);
        e.setStatus(status);
        e.setMain(main);
        e.setBudget(budget);
        e.setSortOrder(order);
        return eventRepository.save(e);
    }

    private GuestGroup group(String name, Side side, GuestCategory category, String rep, String phone) {
        GuestGroup g = new GuestGroup();
        g.setName(name);
        g.setSide(side);
        g.setCategory(category);
        g.setRepresentativeName(rep);
        g.setRepresentativePhone(phone);
        return groupRepository.save(g);
    }

    private Guest guest(String name, Gender gender, String phone, String address, Side side, String relationship, GuestCategory category,
                        GuestGroup group, boolean vip) {
        Guest g = new Guest();
        g.setCode(String.format("KM-%04d", guestRepository.nextCodeSequence()));
        g.setFullName(name);
        g.setGender(gender);
        g.setPhone(phone);
        g.setAddress(address);
        g.setSide(side);
        g.setRelationship(relationship);
        g.setCategory(category);
        g.setGroup(group);
        g.setVip(vip);
        g.setContactPerson(group != null ? group.getRepresentativeName() : null);
        return guestRepository.save(g);
    }

    private Invitation invitation(Guest g, WeddingEvent e, int invited, InvitationStatus status, Integer confirmed) {
        Invitation inv = new Invitation();
        inv.setGuest(g);
        inv.setEvent(e);
        inv.setInvitedCount(invited);
        inv.setExpectedCount(invited);
        invitationService.applyStatus(inv, status, confirmed, status == InvitationStatus.ATTENDED ? invited : null);
        if (status != InvitationStatus.NOT_SENT && inv.getSentAt() != null) {
            inv.setSentAt(Instant.now().minus(12, ChronoUnit.DAYS));
        }
        return invitationRepository.save(inv);
    }

    private TableArea area(WeddingEvent e, String name, String description, int order) {
        TableArea a = new TableArea();
        a.setEvent(e);
        a.setName(name);
        a.setDescription(description);
        a.setSortOrder(order);
        return areaRepository.save(a);
    }

    private WeddingTable table(WeddingEvent e, TableArea area, String name, int capacity, Side side, int x, int y) {
        WeddingTable t = new WeddingTable();
        t.setEvent(e);
        t.setArea(area);
        t.setName(name);
        t.setCapacity(capacity);
        t.setSide(side);
        t.setPositionX(x);
        t.setPositionY(y);
        t.setSortOrder(y * 10 + x);
        return tableRepository.save(t);
    }

    private void seat(List<Guest> guests, List<WeddingTable> tables, Map<Long, Invitation> invitations, int tableCount) {
        int tableIndex = 0;
        int[] used = new int[tables.size()];
        int seatedCount = 0;
        for (Guest g : guests) {
            Invitation inv = invitations.get(g.getId());
            if (inv == null || inv.getStatus() == InvitationStatus.DECLINED || inv.getStatus() == InvitationStatus.NOT_SENT) continue;
            if (seatedCount >= 13) break; // leave some guests unassigned for the demo
            int seats = Math.max(1, inv.getConfirmedCount() > 0 ? inv.getConfirmedCount() : inv.getExpectedCount());
            while (tableIndex < tableCount && used[tableIndex] + seats > tables.get(tableIndex).getCapacity()) tableIndex++;
            if (tableIndex >= tableCount) break;
            TableAssignment a = new TableAssignment();
            a.setTable(tables.get(tableIndex));
            a.setGuest(g);
            a.setSeats(seats);
            assignmentRepository.save(a);
            used[tableIndex] += seats;
            seatedCount++;
        }
    }

    private GiftItem item(String name, String qty, String unit, Integer value) {
        GiftItem i = new GiftItem();
        i.setItemName(name);
        i.setQuantity(new BigDecimal(qty));
        i.setUnit(unit);
        i.setEstimatedValue(value == null ? null : bd(value));
        return i;
    }

    private void gift(Guest g, WeddingEvent e, GiftType type, Integer amount, LocalDate date, String note, GiftItem... items) {
        Gift gift = new Gift();
        gift.setGuest(g);
        gift.setSide(g.getSide());
        gift.setEvent(e);
        gift.setGiftType(type);
        gift.setReceivedAt(date);
        gift.setNote(note);
        gift.setRecordedBy("admin");
        BigDecimal total = null;
        for (GiftItem item : items) {
            item.setGift(gift);
            gift.getItems().add(item);
            if (item.getEstimatedValue() != null) total = (total == null ? BigDecimal.ZERO : total).add(item.getEstimatedValue());
        }
        gift.setAmount(amount != null ? bd(amount) : total);
        giftRepository.save(gift);
    }

    private void giftFromOutsider(String name, Side side, WeddingEvent e, GiftType type, int amount, LocalDate date, String note) {
        Gift gift = new Gift();
        gift.setGiverName(name);
        gift.setSide(side);
        gift.setEvent(e);
        gift.setGiftType(type);
        gift.setAmount(bd(amount));
        gift.setReceivedAt(date);
        gift.setNote(note);
        gift.setRecordedBy("admin");
        giftRepository.save(gift);
    }

    private Vendor vendor(String name, String type, String contact, String phone, String address, String desc, long price, long deposit,
                          ContractStatus status, LocalDate contractDate, Integer rating) {
        Vendor v = new Vendor();
        v.setName(name);
        v.setServiceType(type);
        v.setContactPerson(contact);
        v.setPhone(phone);
        v.setAddress(address);
        v.setServiceDescription(desc);
        v.setAgreedPrice(bd(price));
        v.setDepositAmount(bd(deposit));
        v.setContractStatus(status);
        v.setContractDate(contractDate);
        v.setRating(rating);
        return vendorRepository.save(v);
    }

    private void budget(Map<String, ExpenseCategory> cats, String name, long amount) {
        ExpenseCategory c = cats.get(name);
        if (c != null) {
            c.setBudgetAmount(bd(amount));
            categoryRepository.save(c);
        }
    }

    private Expense expense(String name, ExpenseCategory cat, Vendor vendor, WeddingEvent event, Side side, long estimated, long actual,
                            LocalDate dueDate, LocalDate expenseDate, String note) {
        Expense e = new Expense();
        e.setName(name);
        e.setCategory(cat);
        e.setVendor(vendor);
        e.setEvent(event);
        e.setSide(side);
        e.setEstimatedAmount(bd(estimated));
        e.setActualAmount(bd(actual));
        e.setDueDate(dueDate);
        e.setExpenseDate(expenseDate);
        e.setNote(note);
        ExpenseService.recomputeStatus(e, List.of());
        return expenseRepository.save(e);
    }

    private void pay(Expense e, long amount, LocalDate date, PaymentType type, PaymentMethod method, String paidBy) {
        ExpensePayment p = new ExpensePayment();
        p.setExpense(e);
        p.setAmount(bd(amount));
        p.setPaymentDate(date);
        p.setPaymentType(type);
        p.setMethod(method);
        p.setPaidBy(paidBy);
        paymentRepository.save(p);
        e.setPaidBy(paidBy);
        ExpenseService.recomputeStatus(e, paymentRepository.findByExpenseIdOrderByPaymentDateAscIdAsc(e.getId()));
        expenseRepository.save(e);
    }

    private void income(String source, Side side, long amount, LocalDate date, PaymentMethod method) {
        OtherIncome i = new OtherIncome();
        i.setSource(source);
        i.setSide(side);
        i.setAmount(bd(amount));
        i.setReceivedAt(date);
        i.setMethod(method);
        otherIncomeRepository.save(i);
    }

    private void task(String title, String desc, String assignee, LocalDate start, LocalDate due, TaskPriority priority, TaskStatus status,
                      WeddingEvent event, int order) {
        Task t = new Task();
        t.setTitle(title);
        t.setDescription(desc);
        t.setAssignee(assignee);
        t.setStartDate(start);
        t.setDueDate(due);
        t.setPriority(priority);
        t.setStatus(status);
        t.setEvent(event);
        t.setSortOrder(order);
        if (status == TaskStatus.DONE) t.setCompletedAt(due.atStartOfDay(SettingsService.ZONE).toInstant());
        taskRepository.save(t);
    }
}
