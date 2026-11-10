package com.wedding.management.repository;

import com.wedding.management.domain.entity.WeddingTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WeddingTableRepository extends JpaRepository<WeddingTable, Long> {

    @Query("select t from WeddingTable t left join fetch t.area where t.event.id = :eventId order by t.sortOrder, t.name")
    List<WeddingTable> findByEventIdWithArea(@Param("eventId") Long eventId);

    Optional<WeddingTable> findByEventIdAndNameIgnoreCase(Long eventId, String name);

    long countByEventId(Long eventId);
}
