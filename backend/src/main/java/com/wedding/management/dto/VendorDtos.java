package com.wedding.management.dto;

import com.wedding.management.domain.enums.ContractStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class VendorDtos {
    private VendorDtos() {
    }

    public record VendorRequest(
            @NotBlank(message = "Vui lòng nhập tên nhà cung cấp") @Size(max = 150) String name,
            @Size(max = 100) String serviceType,
            @Size(max = 150) String contactPerson,
            @Size(max = 30) String phone,
            @Size(max = 150) String email,
            @Size(max = 255) String address,
            @Size(max = 4000) String serviceDescription,
            @DecimalMin(value = "0", message = "Giá không được âm") BigDecimal agreedPrice,
            @DecimalMin(value = "0", message = "Tiền cọc không được âm") BigDecimal depositAmount,
            ContractStatus contractStatus,
            LocalDate contractDate,
            @Min(1) @Max(5) Integer rating,
            @Size(max = 2000) String note
    ) {
    }

    public record VendorResponse(
            Long id,
            String name,
            String serviceType,
            String contactPerson,
            String phone,
            String email,
            String address,
            String serviceDescription,
            BigDecimal agreedPrice,
            BigDecimal depositAmount,
            ContractStatus contractStatus,
            String contractStatusLabel,
            LocalDate contractDate,
            Integer rating,
            String note,
            long expenseCount,
            BigDecimal expenseActual,
            BigDecimal expensePaid,
            BigDecimal expenseRemaining,
            long attachmentCount,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
