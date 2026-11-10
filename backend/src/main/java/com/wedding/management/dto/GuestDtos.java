package com.wedding.management.dto;

import com.wedding.management.domain.enums.Gender;
import com.wedding.management.domain.enums.GuestCategory;
import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.domain.enums.Side;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class GuestDtos {
    private GuestDtos() {
    }

    // ---- requests -----------------------------------------------------------

    public record InvitationInput(
            @NotNull(message = "Thiếu sự kiện") Long eventId,
            @Min(value = 0, message = "Số người mời không hợp lệ") @Max(50) Integer invitedCount,
            @Min(value = 0, message = "Số người dự kiến không hợp lệ") @Max(50) Integer expectedCount,
            InvitationStatus status,
            @Size(max = 1000) String note
    ) {
    }

    public record GuestRequest(
            @NotBlank(message = "Vui lòng nhập họ và tên") @Size(max = 150) String fullName,
            Gender gender,
            @Size(max = 30) @Pattern(regexp = "^[0-9+ .()-]*$", message = "Số điện thoại không hợp lệ") String phone,
            @Size(max = 150) String email,
            @Size(max = 255) String address,
            @NotNull(message = "Vui lòng chọn nhà trai hoặc nhà gái") Side side,
            @Size(max = 150) String relationship,
            GuestCategory category,
            Long groupId,
            @Size(max = 150) String contactPerson,
            Boolean vip,
            @Size(max = 2000) String note,
            @Valid List<InvitationInput> invitations,
            /** When true, the duplicate-phone check is bypassed (user confirmed). */
            Boolean ignoreDuplicates
    ) {
    }

    public record InvitationUpdateRequest(
            InvitationStatus status,
            @Min(0) @Max(50) Integer invitedCount,
            @Min(0) @Max(50) Integer expectedCount,
            @Min(0) @Max(50) Integer confirmedCount,
            @Min(0) @Max(50) Integer actualCount,
            @Size(max = 1000) String note
    ) {
    }

    public record BulkStatusRequest(
            @NotEmpty(message = "Chưa chọn lời mời nào") List<Long> invitationIds,
            @NotNull(message = "Thiếu trạng thái") InvitationStatus status
    ) {
    }

    public record GuestGroupRequest(
            @NotBlank(message = "Vui lòng nhập tên nhóm") @Size(max = 150) String name,
            @NotNull(message = "Vui lòng chọn bên") Side side,
            GuestCategory category,
            @Size(max = 150) String representativeName,
            @Size(max = 30) String representativePhone,
            @Size(max = 2000) String description
    ) {
    }

    // ---- responses ----------------------------------------------------------

    public record GuestSummary(Long id, String code, String fullName, String phone, Side side, String groupName) {
    }

    public record TableInfo(Long tableId, String tableName, Long eventId, String eventName, int seats) {
    }

    public record InvitationResponse(
            Long id,
            Long guestId,
            String guestCode,
            String guestName,
            String guestPhone,
            Side guestSide,
            String groupName,
            Long eventId,
            String eventName,
            LocalDate eventDate,
            InvitationStatus status,
            String statusLabel,
            int invitedCount,
            int expectedCount,
            int confirmedCount,
            int actualCount,
            String qrToken,
            Instant sentAt,
            Instant respondedAt,
            Instant checkedInAt,
            String checkedInBy,
            String note,
            Long tableId,
            String tableName
    ) {
    }

    public record GuestResponse(
            Long id,
            String code,
            String fullName,
            Gender gender,
            String phone,
            String email,
            String address,
            Side side,
            String relationship,
            GuestCategory category,
            Long groupId,
            String groupName,
            String contactPerson,
            boolean vip,
            String note,
            InvitationStatus overallStatus,
            int totalInvited,
            int totalExpected,
            int totalConfirmed,
            int totalActual,
            BigDecimal giftTotal,
            long giftCount,
            List<InvitationResponse> invitations,
            List<TableInfo> tables,
            boolean archived,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record GuestGroupResponse(
            Long id,
            String name,
            Side side,
            GuestCategory category,
            String representativeName,
            String representativePhone,
            String description,
            long memberCount,
            int totalInvited,
            int totalConfirmed,
            int totalActual,
            BigDecimal giftTotal,
            long giftCount,
            Instant createdAt
    ) {
    }

    public record DuplicateCheckResponse(boolean duplicate, List<GuestSummary> matches) {
    }

    public record RowMessage(int row, String name, String message) {
    }

    public record ImportResult(int totalRows, int imported, int skipped, int failed,
                               List<RowMessage> skippedRows, List<RowMessage> errors) {
    }

    public record GuestStats(long total, long groom, long bride, long both, long archived,
                             long notSent, long sent, long pending, long confirmed, long declined, long attended, long absent) {
    }
}
