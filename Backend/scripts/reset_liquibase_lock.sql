DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'databasechangeloglock') THEN
        UPDATE databasechangeloglock
        SET locked = FALSE,
            lockgranted = NULL,
            lockedby = NULL
        WHERE id = 1;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'databasechangeloglock_adr') THEN
        UPDATE databasechangeloglock_adr
        SET locked = FALSE,
            lockgranted = NULL,
            lockedby = NULL
        WHERE id = 1;
    END IF;
END $$;
