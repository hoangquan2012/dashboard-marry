package com.wedding.management.web;

import com.wedding.management.domain.enums.InvitationStatus;
import com.wedding.management.domain.enums.Side;
import com.wedding.management.dto.EventDtos.EventRequest;
import com.wedding.management.dto.EventDtos.EventResponse;
import com.wedding.management.dto.EventDtos.InviteGuestsRequest;
import com.wedding.management.dto.GuestDtos.InvitationResponse;
import com.wedding.management.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Các sự kiện của đám cưới")
public class EventController {

    private final EventService eventService;

    @GetMapping
    @Operation(summary = "Danh sách sự kiện kèm thống kê khách, tiền mừng, chi phí")
    public List<EventResponse> list() {
        return eventService.list();
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return eventService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody EventRequest request) {
        return eventService.create(request);
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return eventService.update(id, request);
    }

    @PostMapping("/{id}/set-main")
    @Operation(summary = "Đặt làm sự kiện chính (khách mới sẽ mặc định được mời)")
    public EventResponse setMain(@PathVariable Long id) {
        return eventService.setMain(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        eventService.delete(id);
    }

    @GetMapping("/{id}/guests")
    @Operation(summary = "Danh sách lời mời (khách) của sự kiện")
    public List<InvitationResponse> guests(@PathVariable Long id,
                                           @RequestParam(required = false) InvitationStatus status,
                                           @RequestParam(required = false) Side side,
                                           @RequestParam(required = false) String q) {
        return eventService.guests(id, status, side, q);
    }

    @PostMapping("/{id}/guests")
    @Operation(summary = "Mời thêm khách vào sự kiện")
    public Map<String, Integer> invite(@PathVariable Long id, @Valid @RequestBody InviteGuestsRequest request) {
        return Map.of("invited", eventService.inviteGuests(id, request));
    }

    @DeleteMapping("/{id}/guests/{guestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Bỏ khách khỏi sự kiện")
    public void removeGuest(@PathVariable Long id, @PathVariable Long guestId) {
        eventService.removeGuest(id, guestId);
    }
}
