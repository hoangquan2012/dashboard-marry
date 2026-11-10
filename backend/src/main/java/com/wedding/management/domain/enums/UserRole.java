package com.wedding.management.domain.enums;

public enum UserRole {
    /** Full access: settings, users, deletions, backup/restore. */
    ADMIN,
    /** Helper account (e.g. family member at the reception desk): can view, record gifts, check-in, update tasks. */
    STAFF
}
