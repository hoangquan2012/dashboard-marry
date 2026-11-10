package com.wedding.management.repository;

import com.wedding.management.domain.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    @Override
    @EntityGraph(attributePaths = {"category", "vendor", "event"})
    Page<Expense> findAll(Specification<Expense> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "vendor", "event"})
    List<Expense> findAll(Specification<Expense> spec, Sort sort);

    @Query("select e from Expense e join fetch e.category left join fetch e.vendor left join fetch e.event where e.deletedAt is null")
    List<Expense> findAllActiveWithRelations();

    @Query("select e from Expense e join fetch e.category left join fetch e.vendor left join fetch e.event where e.id = :id")
    Optional<Expense> findByIdWithRelations(@Param("id") Long id);

    @Query("select e from Expense e join fetch e.category left join fetch e.vendor where e.deletedAt is null and e.vendor.id = :vendorId order by e.createdAt desc")
    List<Expense> findActiveByVendorId(@Param("vendorId") Long vendorId);

    long countByCategoryId(Long categoryId);

    @Query("select e from Expense e join fetch e.category left join fetch e.vendor where e.deletedAt is null and "
            + "(lower(e.name) like lower(concat('%', :q, '%')) or lower(e.category.name) like lower(concat('%', :q, '%'))) order by e.createdAt desc")
    List<Expense> search(@Param("q") String q);
}
