package com.wedding.management.service;

import com.wedding.management.common.excel.ExcelUtil;
import com.wedding.management.common.exception.BusinessException;
import com.wedding.management.domain.entity.Guest;
import com.wedding.management.domain.entity.GuestGroup;
import com.wedding.management.domain.entity.WeddingEvent;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.domain.enums.Gender;
import com.wedding.management.domain.enums.GuestCategory;
import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.GuestDtos.GuestResponse;
import com.wedding.management.dto.GuestDtos.ImportResult;
import com.wedding.management.dto.GuestDtos.InvitationInput;
import com.wedding.management.dto.GuestDtos.InvitationResponse;
import com.wedding.management.dto.GuestDtos.RowMessage;
import com.wedding.management.dto.GuestDtos.TableInfo;
import com.wedding.management.repository.GuestGroupRepository;
import com.wedding.management.repository.GuestRepository;
import com.wedding.management.repository.WeddingEventRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Excel import / export / template for the guest list.
 */
@Service
@RequiredArgsConstructor
public class GuestExcelService {

    private static final List<String> IMPORT_COLUMNS = List.of(
            "Họ và tên (*)", "Giới tính", "Số điện thoại", "Địa chỉ", "Bên (*)", "Quan hệ", "Nhóm khách",
            "Gia đình / Nhóm", "Người liên hệ", "Số người mời", "Sự kiện mời", "Ghi chú");

    private final GuestService guestService;
    private final GuestRepository guestRepository;
    private final GuestGroupRepository groupRepository;
    private final WeddingEventRepository eventRepository;
    private final InvitationService invitationService;
    private final AuditService auditService;

    // ---- template -----------------------------------------------------------

    public byte[] template() {
        Workbook wb = ExcelUtil.newWorkbook();
        ExcelUtil.Styles styles = ExcelUtil.styles(wb);
        Sheet sheet = wb.createSheet("Khách mời");
        ExcelUtil.header(sheet, 0, styles, IMPORT_COLUMNS);
        List<WeddingEvent> events = eventRepository.findAllByOrderByEventDateAscSortOrderAsc();
        String eventSample = events.isEmpty() ? "" : events.get(0).getName();
        Row r1 = sheet.createRow(1);
        String[] sample1 = {"Nguyễn Văn A", "Nam", "0912345678", "Hà Nội", "Nhà trai", "Bạn của chú rể", "Bạn bè",
                "Nhóm bạn đại học", "", "2", eventSample, "Đi cùng vợ"};
        for (int i = 0; i < sample1.length; i++) ExcelUtil.text(r1, i, sample1[i]);
        Row r2 = sheet.createRow(2);
        String[] sample2 = {"Trần Thị B", "Nữ", "0987654321", "Hải Phòng", "Nhà gái", "Đồng nghiệp của cô dâu", "Đồng nghiệp",
                "", "", "1", eventSample, ""};
        for (int i = 0; i < sample2.length; i++) ExcelUtil.text(r2, i, sample2[i]);
        ExcelUtil.autosize(sheet, IMPORT_COLUMNS.size());

        Sheet guide = wb.createSheet("Hướng dẫn");
        int row = 0;
        ExcelUtil.title(guide, row++, "Hướng dẫn nhập danh sách khách mời", styles);
        row++;
        ExcelUtil.bold(guide.createRow(row++), 0, "Các cột đánh dấu (*) là bắt buộc. Xóa 2 dòng mẫu trước khi nhập dữ liệu thật.", styles);
        ExcelUtil.text(guide.createRow(row++), 0, "Bên (*): Nhà trai | Nhà gái | Chung");
        ExcelUtil.text(guide.createRow(row++), 0, "Giới tính: Nam | Nữ | Khác (có thể bỏ trống)");
        ExcelUtil.text(guide.createRow(row++), 0, "Nhóm khách: " + java.util.Arrays.stream(GuestCategory.values())
                .map(GuestCategory::label).collect(Collectors.joining(" | ")));
        ExcelUtil.text(guide.createRow(row++), 0, "Gia đình / Nhóm: tên hộ gia đình hoặc nhóm (ví dụ: Gia đình bác A). Nhóm chưa có sẽ được tạo tự động.");
        ExcelUtil.text(guide.createRow(row++), 0, "Số người mời: số người được mời theo thiệp (mặc định 1).");
        ExcelUtil.text(guide.createRow(row++), 0, "Sự kiện mời: tên sự kiện, nhiều sự kiện cách nhau bằng dấu chấm phẩy (;). Bỏ trống = sự kiện chính.");
        row++;
        ExcelUtil.bold(guide.createRow(row++), 0, "Các sự kiện hiện có:", styles);
        for (WeddingEvent e : events) {
            ExcelUtil.text(guide.createRow(row++), 0, "- " + e.getName() + (e.isMain() ? " (sự kiện chính)" : ""));
        }
        guide.setColumnWidth(0, 25000);
        return ExcelUtil.toBytes(wb);
    }

    // ---- import -------------------------------------------------------------

    @Transactional
    public ImportResult importGuests(MultipartFile file, boolean skipDuplicates) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn tệp Excel (.xlsx)");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new BusinessException("Chỉ hỗ trợ tệp Excel (.xlsx)");
        }

        List<WeddingEvent> events = eventRepository.findAllByOrderByEventDateAscSortOrderAsc();
        Map<String, WeddingEvent> eventByName = new HashMap<>();
        events.forEach(e -> eventByName.put(e.getName().trim().toLowerCase(), e));
        WeddingEvent mainEvent = events.stream().filter(WeddingEvent::isMain).findFirst().orElse(null);

        Map<String, GuestGroup> groupByName = new HashMap<>();
        groupRepository.findByDeletedAtIsNullOrderByNameAsc().forEach(g -> groupByName.put(g.getName().trim().toLowerCase(), g));

        Set<String> existingPhones = guestRepository.findByDeletedAtIsNull().stream()
                .map(Guest::getPhone).filter(p -> p != null && !p.isBlank())
                .map(GuestService::normalizePhone).collect(Collectors.toCollection(HashSet::new));

        List<RowMessage> skipped = new ArrayList<>();
        List<RowMessage> errors = new ArrayList<>();
        int imported = 0;
        int totalRows = 0;

        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (ExcelUtil.isRowEmpty(row, IMPORT_COLUMNS.size())) continue;
                totalRows++;
                int rowNumber = r + 1;
                String fullName = ExcelUtil.readString(row, 0);
                try {
                    if (fullName == null) {
                        errors.add(new RowMessage(rowNumber, null, "Thiếu họ và tên"));
                        continue;
                    }
                    Side side = parseSide(ExcelUtil.readString(row, 4));
                    if (side == null) {
                        errors.add(new RowMessage(rowNumber, fullName, "Cột Bên phải là Nhà trai / Nhà gái / Chung"));
                        continue;
                    }
                    String phone = GuestService.normalizePhone(ExcelUtil.readString(row, 2));
                    if (phone != null && existingPhones.contains(phone)) {
                        if (skipDuplicates) {
                            skipped.add(new RowMessage(rowNumber, fullName, "Trùng số điện thoại " + phone + " với khách đã có"));
                            continue;
                        }
                    }
                    Gender gender = Gender.fromLabel(ExcelUtil.readString(row, 1));
                    GuestCategory category = GuestCategory.fromLabel(ExcelUtil.readString(row, 6));
                    String groupName = ExcelUtil.readString(row, 7);
                    GuestGroup group = null;
                    if (groupName != null) {
                        group = groupByName.get(groupName.trim().toLowerCase());
                        if (group == null) {
                            group = new GuestGroup();
                            group.setName(groupName.trim());
                            group.setSide(side);
                            group.setCategory(category);
                            groupRepository.save(group);
                            groupByName.put(groupName.trim().toLowerCase(), group);
                        }
                    }
                    Integer invitedCount = ExcelUtil.readInt(row, 9);
                    if (invitedCount == null || invitedCount < 0) invitedCount = 1;

                    // resolve events before persisting anything for this row
                    List<InvitationInput> invitations = new ArrayList<>();
                    String eventNames = ExcelUtil.readString(row, 10);
                    if (eventNames != null) {
                        for (String part : eventNames.split("[;,]")) {
                            if (part.isBlank()) continue;
                            WeddingEvent ev = eventByName.get(part.trim().toLowerCase());
                            if (ev == null) {
                                throw new BusinessException("Không tìm thấy sự kiện \"" + part.trim() + "\"");
                            }
                            invitations.add(new InvitationInput(ev.getId(), invitedCount, invitedCount, InvitationStatus.NOT_SENT, null));
                        }
                    } else if (mainEvent != null) {
                        invitations.add(new InvitationInput(mainEvent.getId(), invitedCount, invitedCount, InvitationStatus.NOT_SENT, null));
                    }

                    Guest guest = new Guest();
                    guest.setCode(guestService.nextCode());
                    guest.setFullName(fullName.trim());
                    guest.setGender(gender);
                    guest.setPhone(phone);
                    guest.setAddress(ExcelUtil.readString(row, 3));
                    guest.setSide(side);
                    guest.setRelationship(ExcelUtil.readString(row, 5));
                    guest.setCategory(category == null ? GuestCategory.OTHER : category);
                    guest.setGroup(group);
                    guest.setContactPerson(ExcelUtil.readString(row, 8));
                    guest.setNote(ExcelUtil.readString(row, 11));
                    guestRepository.save(guest);
                    invitationService.syncInvitations(guest, invitations);
                    if (phone != null) existingPhones.add(phone);
                    imported++;
                } catch (BusinessException e) {
                    errors.add(new RowMessage(rowNumber, fullName, e.getMessage()));
                } catch (Exception e) {
                    errors.add(new RowMessage(rowNumber, fullName, "Dòng không hợp lệ: " + e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new BusinessException("Không đọc được tệp Excel: " + e.getMessage());
        }

        auditService.log("GUEST", null, null, AuditAction.IMPORT,
                "Nhập Excel: " + imported + " khách mới, bỏ qua " + skipped.size() + ", lỗi " + errors.size());
        return new ImportResult(totalRows, imported, skipped.size(), errors.size(), skipped, errors);
    }

    private static Side parseSide(String text) {
        if (text == null) return null;
        String t = text.trim().toLowerCase();
        if (t.contains("trai") || t.equals("groom")) return Side.GROOM;
        if (t.contains("gái") || t.contains("gai") || t.equals("bride")) return Side.BRIDE;
        if (t.contains("chung") || t.equals("both")) return Side.BOTH;
        return null;
    }

    // ---- export -------------------------------------------------------------

    public byte[] export(List<GuestResponse> guests, List<WeddingEvent> events) {
        Workbook wb = ExcelUtil.newWorkbook();
        ExcelUtil.Styles styles = ExcelUtil.styles(wb);
        Sheet sheet = wb.createSheet("Khách mời");
        List<String> columns = new ArrayList<>(List.of("Mã", "Họ và tên", "Giới tính", "Số điện thoại", "Địa chỉ", "Bên",
                "Quan hệ", "Nhóm khách", "Gia đình / Nhóm", "Người liên hệ", "Trạng thái chung",
                "Số người mời", "Số người xác nhận", "Số người thực tế", "Tổng tiền mừng", "Số lượt mừng", "Bàn tiệc", "Ghi chú"));
        for (WeddingEvent e : events) {
            columns.add(e.getName());
        }
        ExcelUtil.header(sheet, 0, styles, columns);
        int r = 1;
        for (GuestResponse g : guests) {
            Row row = sheet.createRow(r++);
            int c = 0;
            ExcelUtil.text(row, c++, g.code());
            ExcelUtil.text(row, c++, g.fullName());
            ExcelUtil.text(row, c++, g.gender() == null ? "" : g.gender().label());
            ExcelUtil.text(row, c++, g.phone());
            ExcelUtil.text(row, c++, g.address());
            ExcelUtil.text(row, c++, g.side().label());
            ExcelUtil.text(row, c++, g.relationship());
            ExcelUtil.text(row, c++, g.category() == null ? "" : g.category().label());
            ExcelUtil.text(row, c++, g.groupName());
            ExcelUtil.text(row, c++, g.contactPerson());
            ExcelUtil.text(row, c++, g.overallStatus().label());
            ExcelUtil.number(row, c++, g.totalInvited());
            ExcelUtil.number(row, c++, g.totalConfirmed());
            ExcelUtil.number(row, c++, g.totalActual());
            ExcelUtil.money(row, c++, g.giftTotal(), styles);
            ExcelUtil.number(row, c++, g.giftCount());
            ExcelUtil.text(row, c++, g.tables().stream().map(TableInfo::tableName).collect(Collectors.joining(", ")));
            ExcelUtil.text(row, c++, g.note());
            for (WeddingEvent e : events) {
                InvitationResponse inv = g.invitations().stream().filter(i -> i.eventId().equals(e.getId())).findFirst().orElse(null);
                ExcelUtil.text(row, c++, inv == null ? "Không mời" : inv.statusLabel()
                        + " (" + inv.invitedCount() + " mời / " + inv.confirmedCount() + " XN / " + inv.actualCount() + " đến)");
            }
        }
        ExcelUtil.autosize(sheet, columns.size());
        return ExcelUtil.toBytes(wb);
    }
}
