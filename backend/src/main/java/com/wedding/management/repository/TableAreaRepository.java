package com.wedding.management.repository;

import com.wedding.management.domain.entity.TableArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TableAreaRepository extends JpaRepository<TableArea, Long> {
    List<TableArea> findByEventIdOrderBySortOrderAscNameAsc(Long eventId);
}
