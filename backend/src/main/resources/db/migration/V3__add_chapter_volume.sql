DO $$
BEGIN
  IF to_regclass('public.chapter') IS NOT NULL THEN
    ALTER TABLE chapter ADD COLUMN IF NOT EXISTS volume VARCHAR(20) NULL;
  END IF;
END $$;
