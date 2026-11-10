package com.wedding.management.web;

import com.wedding.management.domain.enums.ContractStatus;
import com.wedding.management.dto.ExpenseDtos.ExpenseResponse;
import com.wedding.management.dto.VendorDtos.VendorRequest;
import com.wedding.management.dto.VendorDtos.VendorResponse;
import com.wedding.management.service.ExpenseService;
import com.wedding.management.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "Nhà cung cấp dịch vụ")
public class VendorController {

    private final VendorService vendorService;
    private final ExpenseService expenseService;

    @GetMapping
    @Operation(summary = "Danh sách nhà cung cấp kèm tổng chi phí liên quan")
    public List<VendorResponse> list(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) ContractStatus status,
                                     @RequestParam(required = false) String serviceType) {
        return vendorService.list(q, status, serviceType);
    }

    @GetMapping("/{id}")
    public VendorResponse get(@PathVariable Long id) {
        return vendorService.get(id);
    }

    @GetMapping("/{id}/expenses")
    @Operation(summary = "Các khoản chi liên kết với nhà cung cấp")
    public List<ExpenseResponse> expenses(@PathVariable Long id) {
        vendorService.find(id);
        return expenseService.byVendor(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendorResponse create(@Valid @RequestBody VendorRequest request) {
        return vendorService.create(request);
    }

    @PutMapping("/{id}")
    public VendorResponse update(@PathVariable Long id, @Valid @RequestBody VendorRequest request) {
        return vendorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        vendorService.delete(id);
    }
}
