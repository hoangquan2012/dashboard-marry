package com.wedding.management.dto;

import com.wedding.management.domain.enums.EventStatus;
import com.wedding.management.domain.enums.Side;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class EventDtos {
    private EventDtos() {
    }

    public record EventRequest(
            @NotBlank(message = "Vui lòng nhập tên sự kiện") @Size(max = 150) String name,
            @NotNull(message = "Vui lòng chọn ngày tổ chức") LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            @Size(max = 255) String venue,
            @Size(max = 255) String address,
            Side side,
            @Size(max = 4000) String description,
            @DecimalMin(value = "0", message = "Ngân sách không được âm") BigDecimal budget,
            EventStatus status,
            Boolean main,
            Integer sortOrder
    ) {
    }

    public record EventStats(
            long guestCount,
            long groomGuests,
            long brideGuests,
            int invitedPeople,
            int expectedPeople,
            int confirmedPeople,
            int actualPeople,
            long confirmedGuests,
            long declinedGuests,
            long pendingGuests,
            long attendedGuests,
            long checkedInGuests,
            BigDecimal giftTotal,
            long giftCount,
            BigDecimal expenseEstimated,
            BigDecimal expenseActual,
            BigDecimal expensePaid,
            long tableCount,
            int totalSeats,
            int seatedPeople,
            long unassignedGuests
    ) {
    }

    public record EventResponse(
            Long id,
            String name,
            LocalDate eventDate,
            LocalTime startTime,
            LocalTime endTime,
            String venue,
            String address,
            Side side,
            String description,
            BigDecimal budget,
            EventStatus status,
            boolean main,
            int sortOrder,
            Long daysUntil,
            EventStats stats,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record InviteGuestsRequest(
            @NotEmpty(message = "Chưa chọn khách mời") List<Long> guestIds,
            @Min(0) @Max(50) Integer invitedCount
    ) {
    }
}
