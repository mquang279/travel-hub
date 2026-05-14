-- Add invite_code_expired_at column to trips table
ALTER TABLE trips
    ADD COLUMN invite_code_expired_at TIMESTAMP;
