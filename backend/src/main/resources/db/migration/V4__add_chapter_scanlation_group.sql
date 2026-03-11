DO $$
BEGIN
  IF to_regclass('public.chapter') IS NOT NULL THEN
    ALTER TABLE chapter ADD COLUMN IF NOT EXISTS scanlation_group VARCHAR(255);
  END IF;
END $$;
