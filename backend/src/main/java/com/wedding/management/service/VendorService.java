package com.wedding.management.service;

import com.wedding.management.common.exception.ResourceNotFoundException;
import com.wedding.management.domain.entity.Attachment;
import com.wedding.management.domain.entity.Expense;
import com.wedding.management.domain.entity.Vendor;
import com.wedding.management.domain.enums.AttachmentEntityType;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.domain.enums.ContractStatus;
import com.wedding.management.dto.VendorDtos.VendorRequest;
import com.wedding.management.dto.VendorDtos.VendorResponse;
import com.wedding.management.repository.AttachmentRepository;
import com.wedding.management.repository.ExpenseRepository;
import com.wedding.management.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final ExpenseRepository expenseRepository;
    private final AttachmentRepository attachmentRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<VendorResponse> list(String q, ContractStatus status, String serviceType) {
        List<Vendor> vendors = vendorRepository.findByDeletedAtIsNullOrderByNameAsc();
        String needle = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        vendors = vendors.stream()
                .filter(v -> status == null || v.getContractStatus() == status)
                .filter(v -> serviceType == null || serviceType.isBlank() || (v.getServiceType() != null && v.getServiceType().equalsIgnoreCase(serviceType)))
                .filter(v -> needle == null || v.getName().toLowerCase().contains(needle)
                        || (v.getPhone() != null && v.getPhone().contains(needle))
                        || (v.getContactPerson() != null && v.getContactPerson().toLowerCase().contains(needle))
                        || (v.getServiceType() != null && v.getServiceType().toLowerCase().contains(needle)))
                .toList();
        return withStats(vendors);
    }

    @Transactional(readOnly = true)
    public VendorResponse get(Long id) {
        return withStats(List.of(find(id))).get(0);
    }

    @Transactional
    public VendorResponse create(VendorRequest request) {
        Vendor v = new Vendor();
        apply(v, request);
        vendorRepository.save(v);
        auditService.log("VENDOR", v.getId(), v.getName(), AuditAction.CREATE, "Thêm nhà cung cấp " + v.getName());
        return get(v.getId());
    }

    @Transactional
    public VendorResponse update(Long id, VendorRequest request) {
        Vendor v = find(id);
        apply(v, request);
        vendorRepository.save(v);
        auditService.log("VENDOR", v.getId(), v.getName(), AuditAction.UPDATE, "Cập nhật nhà cung cấp " + v.getName());
        return get(id);
    }

    @Transactional
    public void delete(Long id) {
        Vendor v = find(id);
        v.setDeletedAt(Instant.now());
        vendorRepository.save(v);
        // expenses keep their reference for history; the vendor simply disappears from active lists
        auditService.log("VENDOR", v.getId(), v.getName(), AuditAction.DELETE, "Xóa nhà cung cấp " + v.getName());
    }

    public Vendor find(Long id) {
        return vendorRepository.findById(id).filter(v -> v.getDeletedAt() == null)
                .orElseThrow(() -> ResourceNotFoundException.of("Nhà cung cấp", id));
    }

    private void apply(Vendor v, VendorRequest r) {
        v.setName(r.name().trim());
        v.setServiceType(r.serviceType());
        v.setContactPerson(r.contactPerson());
        v.setPhone(r.phone() == null ? null : r.phone().replace(" ", "").trim());
        v.setEmail(r.email());
        v.setAddress(r.address());
        v.setServiceDescription(r.serviceDescription());
        v.setAgreedPrice(r.agreedPrice());
        v.setDepositAmount(r.depositAmount() == null ? BigDecimal.ZERO : r.depositAmount());
        v.setContractStatus(r.contractStatus() == null ? ContractStatus.NEGOTIATING : r.contractStatus());
        v.setContractDate(r.contractDate());
        v.setRating(r.rating());
        v.setNote(r.note());
    }

    private List<VendorResponse> withStats(List<Vendor> vendors) {
        if (vendors.isEmpty()) return List.of();
        Map<Long, BigDecimal[]> totals = new HashMap<>();
        Map<Long, Long> counts = new HashMap<>();
        for (Expense e : expenseRepository.findAllActiveWithRelations()) {
            if (e.getVendor() == null) continue;
            BigDecimal[] acc = totals.computeIfAbsent(e.getVendor().getId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            acc[0] = acc[0].add(e.getActualAmount());
            acc[1] = acc[1].add(e.getPaidAmount());
            counts.merge(e.getVendor().getId(), 1L, Long::sum);
        }
        Map<Long, Long> attachments = new HashMap<>();
        for (Attachment a : attachmentRepository.findByEntityTypeAndEntityIdIn(AttachmentEntityType.VENDOR, vendors.stream().map(Vendor::getId).toList())) {
            attachments.merge(a.getEntityId(), 1L, Long::sum);
        }
        List<VendorResponse> out = new ArrayList<>();
        for (Vendor v : vendors) {
            BigDecimal[] acc = totals.getOrDefault(v.getId(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            out.add(new VendorResponse(v.getId(), v.getName(), v.getServiceType(), v.getContactPerson(), v.getPhone(), v.getEmail(),
                    v.getAddress(), v.getServiceDescription(), v.getAgreedPrice(), v.getDepositAmount(), v.getContractStatus(),
                    v.getContractStatus().label(), v.getContractDate(), v.getRating(), v.getNote(),
                    counts.getOrDefault(v.getId(), 0L), acc[0], acc[1], acc[0].subtract(acc[1]).max(BigDecimal.ZERO),
                    attachments.getOrDefault(v.getId(), 0L), v.getCreatedAt(), v.getUpdatedAt()));
        }
        return out;
    }
}
