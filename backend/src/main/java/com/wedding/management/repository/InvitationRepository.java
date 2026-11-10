package com.wedding.management.repository;

import com.wedding.management.domain.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long>, JpaSpecificationExecutor<Invitation> {

    @Query("select i from Invitation i join fetch i.event where i.guest.id = :guestId order by i.event.eventDate")
    List<Invitation> findByGuestIdWithEvent(@Param("guestId") Long guestId);

    @Query("select i from Invitation i join fetch i.event join fetch i.guest where i.guest.id in :guestIds")
    List<Invitation> findByGuestIdInWithEvent(@Param("guestIds") Collection<Long> guestIds);

    @Query("select i from Invitation i join fetch i.guest g left join fetch g.group where i.event.id = :eventId and g.deletedAt is null")
    List<Invitation> findByEventIdWithGuest(@Param("eventId") Long eventId);

    @Query("select i from Invitation i join fetch i.guest g join fetch i.event where g.deletedAt is null")
    List<Invitation> findAllActiveWithGuestAndEvent();

    @Query("select i from Invitation i join fetch i.guest g join fetch i.event where i.qrToken = :token")
    Optional<Invitation> findByQrToken(@Param("token") String token);

    @Query("select i from Invitation i join fetch i.guest g left join fetch g.group join fetch i.event where i.id = :id")
    Optional<Invitation> findByIdWithGuestAndEvent(@Param("id") Long id);

    Optional<Invitation> findByGuestIdAndEventId(Long guestId, Long eventId);

    boolean existsByGuestIdAndEventId(Long guestId, Long eventId);

    long countByEventId(Long eventId);

    @Query("select count(i) from Invitation i where i.guest.id = :guestId and i.checkedInAt is not null")
    long countCheckedInByGuest(@Param("guestId") Long guestId);

    void deleteByGuestIdAndEventId(Long guestId, Long eventId);
}
