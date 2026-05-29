-- V22: Fix tag delimiter format for exact-match boundary search.
--
-- Previous format:  react,java,microservices
-- New format:       ,react,java,microservices,
--
-- Rows that are NULL, empty, or already fenced are handled safely.
-- The application's joinTags() now writes the fenced format on every save,
-- so this migration only needs to back-fill pre-existing rows.

UPDATE adr
SET tags = ',' || tags || ','
WHERE tags IS NOT NULL
  AND tags <> ''
  AND (tags NOT LIKE ',%' OR tags NOT LIKE '%,');
