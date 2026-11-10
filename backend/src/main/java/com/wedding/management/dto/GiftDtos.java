package com.wedding.management.dto;

import com.wedding.management.domain.enums.GiftType;
import com.wedding.management.domain.enums.Side;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class GiftDtos {
    private GiftDtos() {
    }

    public record GiftItemRequest(
            @NotBlank(message = "Vui lòng nhập tên hiện vật") @Size(max = 150) String itemName,
            @DecimalMin(value = "0", message = "Số lượng không hợp lệ") BigDecimal quantity,
            @Size(max = 30) String unit,
            @DecimalMin(value = "0", message = "Giá trị không hợp lệ") BigDecimal estimatedValue,
            @Size(max = 500) String note
    ) {
    }

    public record GiftRequest(
            Long guestId,
            @Size(max = 150) String giverName,
            Side side,
            Long eventId,
            @NotNull(message = "Vui lòng chọn hình thức mừng") GiftType giftType,
            @DecimalMin(value = "0", message = "Số tiền không được âm") BigDecimal amount,
            @NotNull(message = "Vui lòng chọn ngày nhận") LocalDate receivedAt,
            @Size(max = 2000) String note,
            @Valid List<GiftItemRequest> items
    ) {
    }

    public record GiftItemResponse(Long id, String itemName, BigDecimal quantity, String unit, BigDecimal estimatedValue, String note) {
    }

    public record GiftResponse(
            Long id,
            Long guestId,
            String guestCode,
            String giverName,
            String guestPhone,
            String groupName,
            Side side,
            Long eventId,
            String eventName,
            GiftType giftType,
            String giftTypeLabel,
            BigDecimal amount,
            boolean valueKnown,
            LocalDate receivedAt,
            String recordedBy,
            String note,
            List<GiftItemResponse> items,
            String itemsSummary,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record TypeStat(GiftType type, String label, long count, BigDecimal amount) {
    }

    public record SideStat(Side side, String label, long count, long guests, BigDecimal amount) {
    }

    public record EventStat(Long eventId, String eventName, long count, long guests, BigDecimal amount) {
    }

    public record InKindSummary(String itemName, String unit, BigDecimal quantity, long count, BigDecimal estimatedValue) {
    }

    public record GiftStats(
            BigDecimal totalAmount,
            BigDecimal cashAmount,
            BigDecimal transferAmount,
            BigDecimal inKindValue,
            long giftCount,
            long inKindCount,
            long unknownValueCount,
            long giverCount,
            long guestsWithoutGift,
            long activeGuests,
            BigDecimal averagePerGiver,
            BigDecimal averagePerGroup,
            long groupsWithGifts,
            List<SideStat> bySide,
            List<TypeStat> byType,
            List<EventStat> byEvent,
            List<InKindSummary> inKindItems,
            Map<String, BigDecimal> byCategory
    ) {
    }
}
