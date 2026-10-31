CREATE TABLE refresh_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    subject     VARCHAR(255) NOT NULL,          -- user's email (same as JWT sub)
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,   -- SHA-256 hex of the raw token
    family_id   UUID         NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    user_agent  VARCHAR(512),
    ip          VARCHAR(45)                     -- fits IPv6
);

CREATE INDEX idx_refresh_tokens_family_id  ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_subject    ON refresh_tokens (subject);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);