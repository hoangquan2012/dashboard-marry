package com.wedding.management.repository;

import com.wedding.management.domain.entity.TableAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TableAssignmentRepository extends JpaRepository<TableAssignment, Long> {

    @Query("select a from TableAssignment a join fetch a.guest g left join fetch g.group join fetch a.table t where t.event.id = :eventId")
    List<TableAssignment> findByEventIdWithGuest(@Param("eventId") Long eventId);

    @Query("select a from TableAssignment a join fetch a.table t join fetch t.event where a.guest.id in :guestIds")
    List<TableAssignment> findByGuestIdInWithTable(@Param("guestIds") Collection<Long> guestIds);

    @Query("select a from TableAssignment a join fetch a.table t where a.guest.id = :guestId and t.event.id = :eventId")
    Optional<TableAssignment> findByGuestIdAndEventId(@Param("guestId") Long guestId, @Param("eventId") Long eventId);

    @Query("select coalesce(sum(a.seats), 0) from TableAssignment a where a.table.id = :tableId")
    int sumSeatsByTableId(@Param("tableId") Long tableId);

    List<TableAssignment> findByTableId(Long tableId);

    @Query("select a from TableAssignment a join fetch a.table t join fetch t.event where a.guest.id = :guestId")
    List<TableAssignment> findByGuestIdWithTable(@Param("guestId") Long guestId);
}
