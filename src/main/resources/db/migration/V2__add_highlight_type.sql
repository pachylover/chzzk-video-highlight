-- Flyway V2: add highlight_type column to highlights table
CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE highlights
    ADD COLUMN highlight_type text;
