package com.wedding.management.service;

import com.wedding.management.dto.MiscDtos.SearchHit;
import com.wedding.management.dto.MiscDtos.SearchResponse;
import com.wedding.management.repository.ExpenseRepository;
import com.wedding.management.repository.GiftRepository;
import com.wedding.management.repository.GuestRepository;
import com.wedding.management.repository.TaskRepository;
import com.wedding.management.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final GuestRepository guestRepository;
    private final GiftRepository giftRepository;
    private final ExpenseRepository expenseRepository;
    private final VendorRepository vendorRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public SearchResponse search(String q, int limit) {
        String needle = q == null ? "" : q.trim();
        if (needle.length() < 1) {
            return new SearchResponse(needle, List.of(), List.of(), List.of(), List.of(), List.of(), 0);
        }
        List<SearchHit> guests = guestRepository.search(needle).stream().limit(limit)
                .map(g -> new SearchHit("GUEST", g.getId(), g.getFullName(),
                        join(g.getCode(), g.getPhone(), g.getSide().label(), g.getGroup() != null ? g.getGroup().getName() : null),
                        "/guests/" + g.getId(), g.getSide(), null)).toList();
        List<SearchHit> gifts = giftRepository.search(needle).stream().limit(limit)
                .map(g -> new SearchHit("GIFT", g.getId(), g.displayGiverName(),
                        join(g.getGiftType().label(), g.getEvent() != null ? g.getEvent().getName() : null, "Ngày " + g.getReceivedAt()),
                        "/gifts?guestId=" + (g.getGuest() != null ? g.getGuest().getId() : ""), g.getSide(), g.getAmount())).toList();
        List<SearchHit> expenses = expenseRepository.search(needle).stream().limit(limit)
                .map(e -> new SearchHit("EXPENSE", e.getId(), e.getName(),
                        join(e.getCategory().getName(), e.getVendor() != null ? e.getVendor().getName() : null, e.getPaymentStatus().label()),
                        "/expenses/" + e.getId(), e.getSide(), e.getActualAmount())).toList();
        List<SearchHit> vendors = vendorRepository.search(needle).stream().limit(limit)
                .map(v -> new SearchHit("VENDOR", v.getId(), v.getName(),
                        join(v.getServiceType(), v.getContactPerson(), v.getPhone()), "/vendors/" + v.getId(), null, v.getAgreedPrice())).toList();
        List<SearchHit> tasks = taskRepository.search(needle).stream().limit(limit)
                .map(t -> new SearchHit("TASK", t.getId(), t.getTitle(),
                        join(t.getStatus().label(), t.getAssignee(), t.getDueDate() != null ? "Hạn " + t.getDueDate() : null),
                        "/tasks?taskId=" + t.getId(), null, null)).toList();
        int total = guests.size() + gifts.size() + expenses.size() + vendors.size() + tasks.size();
        return new SearchResponse(needle, guests, gifts, expenses, vendors, tasks, total);
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append(p);
        }
        return sb.toString();
    }
}
