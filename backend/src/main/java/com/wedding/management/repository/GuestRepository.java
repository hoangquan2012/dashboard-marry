package com.wedding.management.repository;

import com.wedding.management.domain.entity.Guest;
import com.wedding.management.domain.enums.Side;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long>, JpaSpecificationExecutor<Guest> {

    @Query(value = "SELECT nextval('guest_code_seq')", nativeQuery = true)
    long nextCodeSequence();

    Optional<Guest> findByCode(String code);

    List<Guest> findByDeletedAtIsNull();

    List<Guest> findByDeletedAtIsNullAndSide(Side side);

    List<Guest> findByGroupIdAndDeletedAtIsNull(Long groupId);

    long countByDeletedAtIsNull();

    long countByGroupIdAndDeletedAtIsNull(Long groupId);

    @Query("select g from Guest g where g.deletedAt is null and g.phone is not null and g.phone <> '' "
            + "and replace(g.phone, ' ', '') = :phone and (:excludeId is null or g.id <> :excludeId)")
    List<Guest> findActiveByPhone(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    @Query("select g from Guest g where g.deletedAt is null and lower(g.fullName) = lower(:name) "
            + "and (:excludeId is null or g.id <> :excludeId)")
    List<Guest> findActiveByFullName(@Param("name") String name, @Param("excludeId") Long excludeId);

    @Query("select g from Guest g left join fetch g.group where g.id in :ids")
    List<Guest> findAllWithGroupByIdIn(@Param("ids") Collection<Long> ids);

    @Query("select g from Guest g left join fetch g.group where g.deletedAt is null "
            + "and (lower(g.fullName) like lower(concat('%', :q, '%')) or g.phone like concat('%', :q, '%') "
            + "or lower(g.code) like lower(concat('%', :q, '%'))) order by g.fullName")
    List<Guest> search(@Param("q") String q);
}
