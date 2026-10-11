-- LeaveWise initial schema. Source of truth: docs/data-model.md
-- Timestamps are UTC (hibernate.jdbc.time_zone=UTC); defaults use now() AT TIME ZONE 'UTC'.

CREATE TABLE company (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP,
    CONSTRAINT uk_company_name UNIQUE (name)
);

CREATE TABLE municipality (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name     VARCHAR(150) NOT NULL,
    district VARCHAR(150) NOT NULL,
    country  VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX uk_municipality_name_district_country
    ON municipality (lower(name), lower(district), lower(country));

CREATE TABLE company_location (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id      BIGINT       NOT NULL,
    municipality_id BIGINT       NOT NULL,
    name            VARCHAR(150) NOT NULL,
    address         VARCHAR(500),
    CONSTRAINT uk_company_location_company_name UNIQUE (company_id, name),
    -- Target of the composite FK from lw_user; id alone is already unique
    CONSTRAINT uk_company_location_company_id UNIQUE (company_id, id),
    CONSTRAINT fk_company_location_company FOREIGN KEY (company_id) REFERENCES company (id) ON DELETE CASCADE,
    CONSTRAINT fk_company_location_municipality FOREIGN KEY (municipality_id) REFERENCES municipality (id) ON DELETE RESTRICT
);

CREATE TABLE lw_user (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                VARCHAR(200) NOT NULL,
    nif                 VARCHAR(9),
    email               VARCHAR(320) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    role                VARCHAR(30)  NOT NULL DEFAULT 'USER',
    contact             VARCHAR(50),
    company_id          BIGINT       NOT NULL,
    company_location_id BIGINT       NOT NULL,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMP,
    CONSTRAINT uk_lw_user_email UNIQUE (email),
    CONSTRAINT uk_lw_user_nif UNIQUE (nif),
    CONSTRAINT ck_lw_user_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT fk_lw_user_company FOREIGN KEY (company_id) REFERENCES company (id) ON DELETE RESTRICT,
    CONSTRAINT fk_lw_user_company_location FOREIGN KEY (company_id, company_location_id)
        REFERENCES company_location (company_id, id) ON DELETE RESTRICT
);

CREATE TABLE work_schedule (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id BIGINT       NOT NULL,
    name       VARCHAR(150) NOT NULL,
    CONSTRAINT uk_work_schedule_company UNIQUE (company_id),
    CONSTRAINT fk_work_schedule_company FOREIGN KEY (company_id) REFERENCES company (id) ON DELETE CASCADE
);

CREATE TABLE work_schedule_day (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    work_schedule_id BIGINT   NOT NULL,
    day_of_week      SMALLINT NOT NULL,
    is_working_day   BOOLEAN  NOT NULL,
    CONSTRAINT uk_work_schedule_day UNIQUE (work_schedule_id, day_of_week),
    CONSTRAINT ck_work_schedule_day_day_of_week CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT fk_work_schedule_day_schedule FOREIGN KEY (work_schedule_id) REFERENCES work_schedule (id) ON DELETE CASCADE
);

CREATE TABLE holiday (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    date            DATE         NOT NULL,
    name            VARCHAR(200) NOT NULL,
    type            VARCHAR(30)  NOT NULL,
    municipality_id BIGINT,
    CONSTRAINT uk_holiday_date_municipality UNIQUE NULLS NOT DISTINCT (date, municipality_id),
    CONSTRAINT ck_holiday_type CHECK (type IN ('NATIONAL', 'MUNICIPAL', 'OTHER')),
    CONSTRAINT fk_holiday_municipality FOREIGN KEY (municipality_id) REFERENCES municipality (id) ON DELETE RESTRICT
);

CREATE TABLE vacation_policy (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id             BIGINT   NOT NULL,
    year                   SMALLINT NOT NULL,
    annual_days            INTEGER  NOT NULL,
    carry_over_allowed     BOOLEAN  NOT NULL DEFAULT TRUE,
    carry_over_expiry_date DATE,
    CONSTRAINT uk_vacation_policy_company_year UNIQUE (company_id, year),
    CONSTRAINT ck_vacation_policy_year CHECK (year > 2020),
    CONSTRAINT ck_vacation_policy_annual_days CHECK (annual_days > 0),
    CONSTRAINT fk_vacation_policy_company FOREIGN KEY (company_id) REFERENCES company (id) ON DELETE CASCADE
);

CREATE TABLE vacation_entitlement (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id                  BIGINT   NOT NULL,
    year                     SMALLINT NOT NULL,
    annual_days              INTEGER  NOT NULL,
    carried_over_days        INTEGER  NOT NULL DEFAULT 0,
    carried_over_expiry_date DATE,
    CONSTRAINT uk_vacation_entitlement_user_year UNIQUE (user_id, year),
    CONSTRAINT ck_vacation_entitlement_year CHECK (year > 2020),
    CONSTRAINT ck_vacation_entitlement_annual_days CHECK (annual_days >= 0),
    CONSTRAINT ck_vacation_entitlement_carried_over_days CHECK (carried_over_days >= 0),
    CONSTRAINT fk_vacation_entitlement_user FOREIGN KEY (user_id) REFERENCES lw_user (id) ON DELETE CASCADE
);

CREATE TABLE vacation (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    days       INTEGER     NOT NULL,
    status     VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP   NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP,
    CONSTRAINT ck_vacation_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_vacation_days CHECK (days > 0),
    CONSTRAINT ck_vacation_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT fk_vacation_user FOREIGN KEY (user_id) REFERENCES lw_user (id) ON DELETE CASCADE
);

CREATE TABLE vacation_suggestion (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT      NOT NULL,
    year             SMALLINT    NOT NULL,
    start_date       DATE        NOT NULL,
    end_date         DATE        NOT NULL,
    block_start_date DATE        NOT NULL,
    block_end_date   DATE        NOT NULL,
    days             INTEGER     NOT NULL,
    reason           TEXT,
    status           VARCHAR(30) NOT NULL DEFAULT 'SUGGESTED',
    created_at       TIMESTAMP   NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    CONSTRAINT ck_vacation_suggestion_year CHECK (year > 2020),
    CONSTRAINT ck_vacation_suggestion_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_vacation_suggestion_block CHECK (block_start_date <= start_date AND block_end_date >= end_date),
    CONSTRAINT ck_vacation_suggestion_days CHECK (days > 0),
    CONSTRAINT ck_vacation_suggestion_status CHECK (status IN ('SUGGESTED', 'ACCEPTED', 'DISMISSED')),
    CONSTRAINT fk_vacation_suggestion_user FOREIGN KEY (user_id) REFERENCES lw_user (id) ON DELETE CASCADE
);

CREATE TABLE file (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    storage_key  VARCHAR(500) NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    CONSTRAINT fk_file_user FOREIGN KEY (user_id) REFERENCES lw_user (id) ON DELETE RESTRICT
);

CREATE TABLE vacation_import (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    file_id       BIGINT      NOT NULL,
    status        VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    processed_at  TIMESTAMP,
    created_at    TIMESTAMP   NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    total_rows    INTEGER,
    imported_rows INTEGER,
    CONSTRAINT ck_vacation_import_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_vacation_import_total_rows CHECK (total_rows >= 0),
    CONSTRAINT ck_vacation_import_imported_rows CHECK (imported_rows >= 0),
    CONSTRAINT fk_vacation_import_user FOREIGN KEY (user_id) REFERENCES lw_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_vacation_import_file FOREIGN KEY (file_id) REFERENCES file (id) ON DELETE CASCADE
);

CREATE TABLE vacation_import_error (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vacation_import_id BIGINT  NOT NULL,
    row_number         INTEGER NOT NULL,
    message            TEXT    NOT NULL,
    CONSTRAINT ck_vacation_import_error_row_number CHECK (row_number > 0),
    CONSTRAINT fk_vacation_import_error_import FOREIGN KEY (vacation_import_id) REFERENCES vacation_import (id) ON DELETE CASCADE
);

CREATE TABLE refresh_token (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    -- SHA-256 hex of the opaque token; the token itself is never stored
    token_hash CHAR(64)  NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    CONSTRAINT uk_refresh_token_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES lw_user (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);
CREATE INDEX idx_lw_user_company ON lw_user (company_id);
CREATE INDEX idx_vacation_user_start ON vacation (user_id, start_date);
CREATE INDEX idx_vacation_suggestion_user_year ON vacation_suggestion (user_id, year);
CREATE INDEX idx_vacation_import_user ON vacation_import (user_id);
CREATE INDEX idx_holiday_date ON holiday (date);
