DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'adr'
          AND column_name = 'tags'
          AND data_type = 'ARRAY'
    ) THEN
        ALTER TABLE adr ADD COLUMN IF NOT EXISTS tags_new TEXT DEFAULT '';
        UPDATE adr SET tags_new = array_to_string(tags::text[], ',') WHERE tags IS NOT NULL;
        ALTER TABLE adr DROP COLUMN tags;
        ALTER TABLE adr RENAME COLUMN tags_new TO tags;
    ELSE
        ALTER TABLE adr ALTER COLUMN tags TYPE TEXT;
        ALTER TABLE adr ALTER COLUMN tags SET DEFAULT '';
    END IF;
END $$;
