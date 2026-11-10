package com.wedding.management.repository;

import com.wedding.management.domain.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    List<ExpenseCategory> findAllByOrderBySortOrderAscNameAsc();

    Optional<ExpenseCategory> findByNameIgnoreCase(String name);
}
