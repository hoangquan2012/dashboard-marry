-- =====================================================================
-- Wedding Management System - Initial schema
-- Currency: VND stored as NUMERIC(18,0) (no fractional units)
-- Timezone: all TIMESTAMPTZ columns are stored in UTC, displayed in Asia/Ho_Chi_Minh
-- =====================================================================

-- ---------------------------------------------------------------------
-- Users (private system: accounts are created by the administrator only)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- Wedding settings (single row, id = 1)
-- ---------------------------------------------------------------------
CREATE TABLE wedding_settings (
    id                 BIGINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    groom_name         VARCHAR(150) NOT NULL DEFAULT 'Chú rể',
    bride_name         VARCHAR(150) NOT NULL DEFAULT 'Cô dâu',
    wedding_date       DATE,
    total_budget       NUMERIC(18,0) NOT NULL DEFAULT 0,
    currency           VARCHAR(3)   NOT NULL DEFAULT 'VND',
    main_venue         VARCHAR(255),
    groom_family_info  TEXT,
    bride_family_info  TEXT,
    note               TEXT,
    demo_seeded        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
INSERT INTO wedding_settings (id) VALUES (1);

-- ---------------------------------------------------------------------
-- Guest groups / households
-- ---------------------------------------------------------------------
CREATE TABLE guest_groups (
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(150) NOT NULL,
    side                 VARCHAR(10)  NOT NULL,                 -- GROOM | BRIDE | BOTH
    category             VARCHAR(30),                           -- FAMILY, RELATIVE, FRIEND, COLLEAGUE, NEIGHBOR, PARENTS_GUEST, OTHER
    representative_name  VARCHAR(150),
    representative_phone VARCHAR(30),
    description          TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ
);
CREATE INDEX idx_guest_groups_side ON guest_groups (side);
CREATE INDEX idx_guest_groups_name ON guest_groups (lower(name));

-- ---------------------------------------------------------------------
-- Guests
-- ---------------------------------------------------------------------
CREATE SEQUENCE guest_code_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE guests (
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR(20)  NOT NULL UNIQUE,
    full_name      VARCHAR(150) NOT NULL,
    gender         VARCHAR(10),                                  -- MALE | FEMALE | OTHER
    phone          VARCHAR(30),
    email          VARCHAR(150),
    address        VARCHAR(255),
    side           VARCHAR(10)  NOT NULL,                        -- GROOM | BRIDE | BOTH
    relationship   VARCHAR(150),
    category       VARCHAR(30)  NOT NULL DEFAULT 'OTHER',
    group_id       BIGINT REFERENCES guest_groups (id) ON DELETE SET NULL,
    contact_person VARCHAR(150),
    is_vip         BOOLEAN      NOT NULL DEFAULT FALSE,
    note           TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ
);
CREATE INDEX idx_guests_full_name ON guests (lower(full_name));
CREATE INDEX idx_guests_phone     ON guests (phone);
CREATE INDEX idx_guests_side      ON guests (side);
CREATE INDEX idx_guests_group     ON guests (group_id);
CREATE INDEX idx_guests_category  ON guests (category);
CREATE INDEX idx_guests_deleted   ON guests (deleted_at);

-- ---------------------------------------------------------------------
-- Wedding events (lễ dạm ngõ, ăn hỏi, tiệc cưới ...)
-- ---------------------------------------------------------------------
CREATE TABLE wedding_events (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    event_date  DATE         NOT NULL,
    start_time  TIME,
    end_time    TIME,
    venue       VARCHAR(255),
    address     VARCHAR(255),
    side        VARCHAR(10)  NOT NULL DEFAULT 'BOTH',
    description TEXT,
    budget      NUMERIC(18,0) NOT NULL DEFAULT 0,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',        -- PLANNED | CONFIRMED | COMPLETED | CANCELLED
    is_main     BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_wedding_events_date ON wedding_events (event_date);

-- ---------------------------------------------------------------------
-- Invitations: one row per (guest, event)
-- ---------------------------------------------------------------------
CREATE TABLE invitations (
    id              BIGSERIAL PRIMARY KEY,
    guest_id        BIGINT NOT NULL REFERENCES guests (id) ON DELETE CASCADE,
    event_id        BIGINT NOT NULL REFERENCES wedding_events (id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL DEFAULT 'NOT_SENT',    -- NOT_SENT | SENT | PENDING | CONFIRMED | DECLINED | ATTENDED | ABSENT
    invited_count   INT NOT NULL DEFAULT 1 CHECK (invited_count >= 0),
    expected_count  INT NOT NULL DEFAULT 1 CHECK (expected_count >= 0),
    confirmed_count INT NOT NULL DEFAULT 0 CHECK (confirmed_count >= 0),
    actual_count    INT NOT NULL DEFAULT 0 CHECK (actual_count >= 0),
    qr_token        VARCHAR(64) NOT NULL UNIQUE,
    sent_at         TIMESTAMPTZ,
    responded_at    TIMESTAMPTZ,
    checked_in_at   TIMESTAMPTZ,
    checked_in_by   VARCHAR(100),
    note            TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_invitation_guest_event UNIQUE (guest_id, event_id)
);
CREATE INDEX idx_invitations_event  ON invitations (event_id);
CREATE INDEX idx_invitations_guest  ON invitations (guest_id);
CREATE INDEX idx_invitations_status ON invitations (status);

-- ---------------------------------------------------------------------
-- Seating: areas, tables, assignments
-- ---------------------------------------------------------------------
CREATE TABLE table_areas (
    id          BIGSERIAL PRIMARY KEY,
    event_id    BIGINT NOT NULL REFERENCES wedding_events (id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_table_areas_event ON table_areas (event_id);

CREATE TABLE wedding_tables (
    id          BIGSERIAL PRIMARY KEY,
    event_id    BIGINT NOT NULL REFERENCES wedding_events (id) ON DELETE CASCADE,
    area_id     BIGINT REFERENCES table_areas (id) ON DELETE SET NULL,
    name        VARCHAR(100) NOT NULL,
    capacity    INT NOT NULL DEFAULT 10 CHECK (capacity > 0),
    side        VARCHAR(10) NOT NULL DEFAULT 'BOTH',
    position_x  INT NOT NULL DEFAULT 0,
    position_y  INT NOT NULL DEFAULT 0,
    sort_order  INT NOT NULL DEFAULT 0,
    note        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_table_event_name UNIQUE (event_id, name)
);
CREATE INDEX idx_wedding_tables_event ON wedding_tables (event_id);
CREATE INDEX idx_wedding_tables_area  ON wedding_tables (area_id);

CREATE TABLE table_assignments (
    id         BIGSERIAL PRIMARY KEY,
    table_id   BIGINT NOT NULL REFERENCES wedding_tables (id) ON DELETE CASCADE,
    guest_id   BIGINT NOT NULL REFERENCES guests (id) ON DELETE CASCADE,
    seats      INT NOT NULL DEFAULT 1 CHECK (seats > 0),
    note       TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_assignment_table_guest UNIQUE (table_id, guest_id)
);
CREATE INDEX idx_table_assignments_guest ON table_assignments (guest_id);

-- ---------------------------------------------------------------------
-- Vendors
-- ---------------------------------------------------------------------
CREATE TABLE vendors (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(150) NOT NULL,
    service_type        VARCHAR(100),
    contact_person      VARCHAR(150),
    phone               VARCHAR(30),
    email               VARCHAR(150),
    address             VARCHAR(255),
    service_description TEXT,
    agreed_price        NUMERIC(18,0),
    deposit_amount      NUMERIC(18,0) NOT NULL DEFAULT 0,
    contract_status     VARCHAR(20) NOT NULL DEFAULT 'NEGOTIATING',  -- NEGOTIATING | CONTRACTED | DEPOSITED | COMPLETED | CANCELLED
    contract_date       DATE,
    rating              INT,
    note                TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ
);
CREATE INDEX idx_vendors_name  ON vendors (lower(name));
CREATE INDEX idx_vendors_phone ON vendors (phone);

-- ---------------------------------------------------------------------
-- Expense categories (with per-category budget)
-- ---------------------------------------------------------------------
CREATE TABLE expense_categories (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(150) NOT NULL UNIQUE,
    icon          VARCHAR(50),
    budget_amount NUMERIC(18,0) NOT NULL DEFAULT 0,
    sort_order    INT NOT NULL DEFAULT 0,
    is_system     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- Expenses and payments (one expense may have many payments)
-- ---------------------------------------------------------------------
CREATE TABLE expenses (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(200) NOT NULL,
    category_id      BIGINT NOT NULL REFERENCES expense_categories (id),
    vendor_id        BIGINT REFERENCES vendors (id) ON DELETE SET NULL,
    event_id         BIGINT REFERENCES wedding_events (id) ON DELETE SET NULL,
    side             VARCHAR(10) NOT NULL DEFAULT 'BOTH',
    estimated_amount NUMERIC(18,0) NOT NULL DEFAULT 0,
    actual_amount    NUMERIC(18,0) NOT NULL DEFAULT 0,
    paid_amount      NUMERIC(18,0) NOT NULL DEFAULT 0,          -- denormalized: sum(expense_payments.amount)
    deposit_amount   NUMERIC(18,0) NOT NULL DEFAULT 0,          -- denormalized: sum of DEPOSIT payments
    payment_status   VARCHAR(20) NOT NULL DEFAULT 'UNPAID',     -- UNPAID | DEPOSITED | PARTIAL | PAID
    expense_date     DATE,
    due_date         DATE,
    paid_by          VARCHAR(150),
    note             TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ
);
CREATE INDEX idx_expenses_category ON expenses (category_id);
CREATE INDEX idx_expenses_vendor   ON expenses (vendor_id);
CREATE INDEX idx_expenses_event    ON expenses (event_id);
CREATE INDEX idx_expenses_status   ON expenses (payment_status);
CREATE INDEX idx_expenses_due      ON expenses (due_date);
CREATE INDEX idx_expenses_name     ON expenses (lower(name));

CREATE TABLE expense_payments (
    id           BIGSERIAL PRIMARY KEY,
    expense_id   BIGINT NOT NULL REFERENCES expenses (id) ON DELETE CASCADE,
    amount       NUMERIC(18,0) NOT NULL CHECK (amount > 0),
    payment_date DATE NOT NULL,
    payment_type VARCHAR(20) NOT NULL DEFAULT 'PARTIAL',        -- DEPOSIT | PARTIAL | FINAL
    method       VARCHAR(20) NOT NULL DEFAULT 'CASH',           -- CASH | BANK_TRANSFER | OTHER
    paid_by      VARCHAR(150),
    note         TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_expense_payments_expense ON expense_payments (expense_id);
CREATE INDEX idx_expense_payments_date    ON expense_payments (payment_date);

-- ---------------------------------------------------------------------
-- Gifts (tiền mừng) and in-kind items
-- ---------------------------------------------------------------------
CREATE TABLE gifts (
    id          BIGSERIAL PRIMARY KEY,
    guest_id    BIGINT REFERENCES guests (id) ON DELETE RESTRICT,
    giver_name  VARCHAR(150),                                    -- used when the giver is not in the guest list
    side        VARCHAR(10) NOT NULL,
    event_id    BIGINT REFERENCES wedding_events (id) ON DELETE SET NULL,
    gift_type   VARCHAR(20) NOT NULL,                            -- CASH | BANK_TRANSFER | GOLD | IN_KIND | OTHER
    amount      NUMERIC(18,0),                                   -- cash amount, or optional converted value for GOLD/IN_KIND
    received_at DATE NOT NULL,
    recorded_by VARCHAR(100),
    note        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);
CREATE INDEX idx_gifts_guest    ON gifts (guest_id);
CREATE INDEX idx_gifts_event    ON gifts (event_id);
CREATE INDEX idx_gifts_type     ON gifts (gift_type);
CREATE INDEX idx_gifts_received ON gifts (received_at);
CREATE INDEX idx_gifts_deleted  ON gifts (deleted_at);

CREATE TABLE gift_items (
    id              BIGSERIAL PRIMARY KEY,
    gift_id         BIGINT NOT NULL REFERENCES gifts (id) ON DELETE CASCADE,
    item_name       VARCHAR(150) NOT NULL,
    quantity        NUMERIC(10,2) NOT NULL DEFAULT 1,
    unit            VARCHAR(30),
    estimated_value NUMERIC(18,0),
    note            TEXT
);
CREATE INDEX idx_gift_items_gift ON gift_items (gift_id);

-- ---------------------------------------------------------------------
-- Other incomes (đóng góp từ gia đình, nguồn thu khác)
-- ---------------------------------------------------------------------
CREATE TABLE other_incomes (
    id          BIGSERIAL PRIMARY KEY,
    source      VARCHAR(200) NOT NULL,
    side        VARCHAR(10) NOT NULL DEFAULT 'BOTH',
    amount      NUMERIC(18,0) NOT NULL CHECK (amount >= 0),
    received_at DATE NOT NULL,
    method      VARCHAR(20) NOT NULL DEFAULT 'CASH',
    note        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- Tasks / checklist
-- ---------------------------------------------------------------------
CREATE TABLE tasks (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    assignee     VARCHAR(150),
    start_date   DATE,
    due_date     DATE,
    priority     VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',          -- LOW | MEDIUM | HIGH | URGENT
    status       VARCHAR(20) NOT NULL DEFAULT 'TODO',            -- TODO | IN_PROGRESS | DONE
    completed_at TIMESTAMPTZ,
    sort_order   INT NOT NULL DEFAULT 0,
    event_id     BIGINT REFERENCES wedding_events (id) ON DELETE SET NULL,
    note         TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_tasks_status ON tasks (status);
CREATE INDEX idx_tasks_due    ON tasks (due_date);

-- ---------------------------------------------------------------------
-- Attachments (files are stored on disk, never as base64 in the DB)
-- ---------------------------------------------------------------------
CREATE TABLE attachments (
    id            BIGSERIAL PRIMARY KEY,
    entity_type   VARCHAR(30) NOT NULL,                          -- EXPENSE | VENDOR | GIFT | EVENT | TASK | OTHER
    entity_id     BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name   VARCHAR(255) NOT NULL,
    storage_path  VARCHAR(500) NOT NULL,
    content_type  VARCHAR(100),
    size_bytes    BIGINT NOT NULL,
    description   VARCHAR(255),
    uploaded_by   VARCHAR(100),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_attachments_entity ON attachments (entity_type, entity_id);

-- ---------------------------------------------------------------------
-- Audit logs
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    entity_type  VARCHAR(30) NOT NULL,
    entity_id    BIGINT,
    entity_label VARCHAR(255),
    action       VARCHAR(20) NOT NULL,                           -- CREATE | UPDATE | DELETE | RESTORE | CHECKIN | LOGIN | IMPORT | RESTORE_BACKUP ...
    summary      VARCHAR(500),
    changes      JSONB,
    performed_by VARCHAR(100),
    performed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_time   ON audit_logs (performed_at DESC);
