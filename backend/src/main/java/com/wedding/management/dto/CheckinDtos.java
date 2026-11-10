package com.wedding.management.dto;

import com.wedding.management.dto.GuestDtos.InvitationResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class CheckinDtos {
    private CheckinDtos() {
    }

    public record QrCheckinRequest(
            @NotBlank(message = "Thiếu mã QR") String code,
            Long eventId,
            @Min(1) @Max(50) Integer actualCount,
            Boolean force
    ) {
    }

    public record ManualCheckinRequest(@Min(1) @Max(50) Integer actualCount, Boolean force) {
    }

    public record CheckinResult(boolean alreadyCheckedIn, String message, InvitationResponse invitation) {
    }

    public record CheckinStats(Long eventId, String eventName, long totalInvitations, long checkedInGuests,
                               int checkedInPeople, int expectedPeople, int confirmedPeople, long notArrived,
                               long declined, List<InvitationResponse> recent) {
    }
}
