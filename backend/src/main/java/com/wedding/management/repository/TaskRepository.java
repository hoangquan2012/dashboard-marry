package com.wedding.management.repository;

import com.wedding.management.domain.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Query("select t from Task t left join fetch t.event order by t.sortOrder, t.dueDate nulls last, t.id")
    List<Task> findAllWithEvent();

    @Query("select t from Task t where lower(t.title) like lower(concat('%', :q, '%')) or lower(coalesce(t.assignee, '')) like lower(concat('%', :q, '%')) order by t.dueDate nulls last")
    List<Task> search(@Param("q") String q);
}
