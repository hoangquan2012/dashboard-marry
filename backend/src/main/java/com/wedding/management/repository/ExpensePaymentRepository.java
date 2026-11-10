package com.wedding.management.repository;

import com.wedding.management.domain.entity.ExpensePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpensePaymentRepository extends JpaRepository<ExpensePayment, Long> {

    List<ExpensePayment> findByExpenseIdOrderByPaymentDateAscIdAsc(Long expenseId);

    void deleteByExpenseId(Long expenseId);

    @Query("select p from ExpensePayment p join fetch p.expense e where e.deletedAt is null")
    List<ExpensePayment> findAllActive();
}
