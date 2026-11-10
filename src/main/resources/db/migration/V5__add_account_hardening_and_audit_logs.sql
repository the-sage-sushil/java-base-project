ALTER TABLE users
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0;

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    event_type  VARCHAR(64) NOT NULL,
    subject     VARCHAR(255),
    ip          VARCHAR(45),
    user_agent  VARCHAR(512),
    details     VARCHAR(2048),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_subject ON audit_logs (subject);
