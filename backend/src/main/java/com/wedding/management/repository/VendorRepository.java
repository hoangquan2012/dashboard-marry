package com.wedding.management.repository;

import com.wedding.management.domain.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VendorRepository extends JpaRepository<Vendor, Long>, JpaSpecificationExecutor<Vendor> {

    List<Vendor> findByDeletedAtIsNullOrderByNameAsc();

    @Query("select v from Vendor v where v.deletedAt is null and (lower(v.name) like lower(concat('%', :q, '%')) "
            + "or v.phone like concat('%', :q, '%') or lower(coalesce(v.contactPerson, '')) like lower(concat('%', :q, '%'))) order by v.name")
    List<Vendor> search(@Param("q") String q);
}
