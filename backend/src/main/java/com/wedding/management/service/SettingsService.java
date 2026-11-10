package com.wedding.management.service;

import com.wedding.management.domain.entity.WeddingSettings;
import com.wedding.management.domain.enums.AuditAction;
import com.wedding.management.dto.SettingsDtos.SettingsRequest;
import com.wedding.management.dto.SettingsDtos.SettingsResponse;
import com.wedding.management.repository.WeddingSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingsService {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final WeddingSettingsRepository settingsRepository;
    private final AuditService auditService;

    @Transactional
    public WeddingSettings getEntity() {
        return settingsRepository.findById(WeddingSettings.SINGLETON_ID).orElseGet(() -> {
            WeddingSettings s = new WeddingSettings();
            s.setId(WeddingSettings.SINGLETON_ID);
            return settingsRepository.save(s);
        });
    }

    @Transactional
    public SettingsResponse get() {
        return toResponse(getEntity());
    }

    @Transactional
    public SettingsResponse update(SettingsRequest request) {
        WeddingSettings s = getEntity();
        Map<String, Object> before = snapshot(s);
        s.setGroomName(request.groomName().trim());
        s.setBrideName(request.brideName().trim());
        s.setWeddingDate(request.weddingDate());
        s.setTotalBudget(request.totalBudget() == null ? BigDecimal.ZERO : request.totalBudget());
        s.setMainVenue(request.mainVenue());
        s.setGroomFamilyInfo(request.groomFamilyInfo());
        s.setBrideFamilyInfo(request.brideFamilyInfo());
        s.setNote(request.note());
        settingsRepository.save(s);
        auditService.log("SETTINGS", s.getId(), "Thông tin đám cưới", AuditAction.UPDATE,
                "Cập nhật thông tin đám cưới", AuditService.diff(before, snapshot(s)));
        return toResponse(s);
    }

    private Map<String, Object> snapshot(WeddingSettings s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("groomName", s.getGroomName());
        m.put("brideName", s.getBrideName());
        m.put("weddingDate", s.getWeddingDate());
        m.put("totalBudget", s.getTotalBudget());
        m.put("mainVenue", s.getMainVenue());
        m.put("note", s.getNote());
        return m;
    }

    public static SettingsResponse toResponse(WeddingSettings s) {
        Long days = null;
        if (s.getWeddingDate() != null) {
            days = ChronoUnit.DAYS.between(LocalDate.now(ZONE), s.getWeddingDate());
        }
        return new SettingsResponse(s.getGroomName(), s.getBrideName(), s.getWeddingDate(), s.getTotalBudget(),
                s.getCurrency(), s.getMainVenue(), s.getGroomFamilyInfo(), s.getBrideFamilyInfo(), s.getNote(),
                days, s.isDemoSeeded(), s.getUpdatedAt());
    }
}
