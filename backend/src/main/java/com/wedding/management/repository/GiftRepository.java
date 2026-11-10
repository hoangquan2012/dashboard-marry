package com.wedding.management.repository;

import com.wedding.management.domain.entity.Gift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GiftRepository extends JpaRepository<Gift, Long>, JpaSpecificationExecutor<Gift> {

    @Override
    @EntityGraph(attributePaths = {"guest", "guest.group", "event"})
    Page<Gift> findAll(Specification<Gift> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"guest", "guest.group", "event"})
    List<Gift> findAll(Specification<Gift> spec, Sort sort);

    @Query("select g from Gift g left join fetch g.guest gu left join fetch gu.group left join fetch g.event where g.id = :id")
    Optional<Gift> findByIdWithRelations(@Param("id") Long id);

    @Query("select g from Gift g left join fetch g.guest gu left join fetch g.event where g.deletedAt is null and g.guest.id = :guestId order by g.receivedAt desc, g.id desc")
    List<Gift> findActiveByGuestId(@Param("guestId") Long guestId);

    @Query("select g from Gift g left join fetch g.guest gu left join fetch gu.group left join fetch g.event where g.deletedAt is null")
    List<Gift> findAllActiveWithRelations();

    long countByGuestIdAndDeletedAtIsNull(Long guestId);

    /** Returns [guestId, sumAmount, count] rows for a set of guests (active gifts only). */
    @Query("select g.guest.id, coalesce(sum(g.amount), 0), count(g) from Gift g where g.deletedAt is null and g.guest.id in :guestIds group by g.guest.id")
    List<Object[]> sumByGuestIds(@Param("guestIds") Collection<Long> guestIds);

    @Query("select g from Gift g left join fetch g.guest gu left join fetch g.event where g.deletedAt is null and "
            + "(lower(coalesce(gu.fullName, g.giverName)) like lower(concat('%', :q, '%'))) order by g.receivedAt desc")
    List<Gift> search(@Param("q") String q);
}
