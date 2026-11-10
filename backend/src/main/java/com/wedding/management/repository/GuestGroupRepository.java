package com.wedding.management.repository;

import com.wedding.management.domain.entity.GuestGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface GuestGroupRepository extends JpaRepository<GuestGroup, Long>, JpaSpecificationExecutor<GuestGroup> {

    List<GuestGroup> findByDeletedAtIsNullOrderByNameAsc();

    Optional<GuestGroup> findByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
