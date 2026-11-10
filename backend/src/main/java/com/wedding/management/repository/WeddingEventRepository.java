package com.wedding.management.repository;

import com.wedding.management.domain.entity.WeddingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeddingEventRepository extends JpaRepository<WeddingEvent, Long> {

    List<WeddingEvent> findAllByOrderByEventDateAscSortOrderAsc();

    Optional<WeddingEvent> findFirstByMainTrue();

    Optional<WeddingEvent> findByNameIgnoreCase(String name);
}
