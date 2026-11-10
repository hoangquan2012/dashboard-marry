package com.wedding.management.dto;

import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.domain.enums.Side;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class SeatingDtos {
    private SeatingDtos() {
    }

    public record AreaRequest(
            @NotNull(message = "Thiếu sự kiện") Long eventId,
            @NotBlank(message = "Vui lòng nhập tên khu vực") @Size(max = 150) String name,
            @Size(max = 1000) String description,
            Integer sortOrder
    ) {
    }

    public record AreaResponse(Long id, Long eventId, String name, String description, int sortOrder, long tableCount) {
    }

    public record TableRequest(
            @NotNull(message = "Thiếu sự kiện") Long eventId,
            Long areaId,
            @NotBlank(message = "Vui lòng nhập tên bàn") @Size(max = 100) String name,
            @NotNull(message = "Vui lòng nhập số chỗ") @Min(value = 1, message = "Số chỗ tối thiểu là 1") @Max(100) Integer capacity,
            Side side,
            Integer positionX,
            Integer positionY,
            Integer sortOrder,
            @Size(max = 1000) String note
    ) {
    }

    public record SeatResponse(Long assignmentId, Long guestId, String guestCode, String guestName, String phone,
                               Side side, String groupName, int seats, InvitationStatus status, boolean checkedIn, String note) {
    }

    public record TableResponse(Long id, Long eventId, Long areaId, String areaName, String name, int capacity, Side side,
                                int positionX, int positionY, int sortOrder, String note,
                                int seatsTaken, int seatsAvailable, List<SeatResponse> seats) {
    }

    public record AssignRequest(
            @NotNull(message = "Thiếu khách mời") Long guestId,
            @Min(1) @Max(50) Integer seats,
            @Size(max = 500) String note,
            /** Allow exceeding the table capacity after an explicit confirmation. */
            Boolean force
    ) {
    }

    public record MoveRequest(@NotNull(message = "Thiếu bàn đích") Long tableId, Boolean force) {
    }

    public record BulkCreateTablesRequest(
            @NotNull Long eventId,
            Long areaId,
            @NotBlank @Size(max = 50) String prefix,
            @Min(1) @Max(200) int count,
            @Min(1) @Max(100) int capacity,
            Side side
    ) {
    }

    public record UnassignedGuest(Long guestId, String code, String fullName, String phone, Side side, String groupName,
                                  InvitationStatus status, int expectedCount, int confirmedCount) {
    }

    public record SeatingOverview(Long eventId, String eventName, long tableCount, int totalSeats, int seatsTaken,
                                  long seatedGuests, long unassignedGuests, List<AreaResponse> areas, List<TableResponse> tables) {
    }

    public record SeatLocation(Long guestId, String guestName, Long tableId, String tableName, String areaName, int seats) {
    }
}
