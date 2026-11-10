package com.wedding.management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class SettingsDtos {
    private SettingsDtos() {
    }

    public record SettingsResponse(
            String groomName,
            String brideName,
            LocalDate weddingDate,
            BigDecimal totalBudget,
            String currency,
            String mainVenue,
            String groomFamilyInfo,
            String brideFamilyInfo,
            String note,
            Long daysUntilWedding,
            boolean demoSeeded,
            Instant updatedAt
    ) {
    }

    public record SettingsRequest(
            @NotBlank(message = "Vui lòng nhập tên chú rể") @Size(max = 150) String groomName,
            @NotBlank(message = "Vui lòng nhập tên cô dâu") @Size(max = 150) String brideName,
            LocalDate weddingDate,
            @DecimalMin(value = "0", message = "Ngân sách không được âm") BigDecimal totalBudget,
            @Size(max = 255) String mainVenue,
            String groomFamilyInfo,
            String brideFamilyInfo,
            String note
    ) {
    }
}
