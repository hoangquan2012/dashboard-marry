package com.wedding.management.domain.entity;

import com.wedding.management.domain.enums.Gender;
import com.wedding.management.domain.enums.GuestCategory;
import com.wedding.management.domain.enums.Side;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "guests")
public class Guest extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Side side;

    @Column(length = 150)
    private String relationship;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GuestCategory category = GuestCategory.OTHER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private GuestGroup group;

    @Column(name = "contact_person", length = 150)
    private String contactPerson;

    @Column(name = "is_vip", nullable = false)
    private boolean vip = false;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isArchived() {
        return deletedAt != null;
    }
}
