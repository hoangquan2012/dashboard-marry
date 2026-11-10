package com.wedding.management.repository;

import com.wedding.management.domain.entity.OtherIncome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OtherIncomeRepository extends JpaRepository<OtherIncome, Long> {
    List<OtherIncome> findAllByOrderByReceivedAtDescIdDesc();
}
