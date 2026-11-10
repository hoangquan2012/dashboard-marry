package com.wedding.management.repository;

import com.wedding.management.domain.entity.WeddingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeddingSettingsRepository extends JpaRepository<WeddingSettings, Long> {
}
