CREATE TABLE rate_limit_log (
    key TEXT NOT NULL,
    ts  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_rate_limit_key_ts
ON rate_limit_log (key, ts DESC);