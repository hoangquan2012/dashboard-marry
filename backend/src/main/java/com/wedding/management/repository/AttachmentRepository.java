package com.wedding.management.repository;

import com.wedding.management.domain.entity.Attachment;
import com.wedding.management.domain.enums.AttachmentEntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(AttachmentEntityType entityType, Long entityId);

    long countByEntityTypeAndEntityId(AttachmentEntityType entityType, Long entityId);

    List<Attachment> findByEntityTypeAndEntityIdIn(AttachmentEntityType entityType, Collection<Long> entityIds);
}
