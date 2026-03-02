DO $$
BEGIN
  IF to_regclass('public.panel') IS NOT NULL THEN
    IF EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = 'public'
        AND table_name = 'panel'
        AND column_name = 'data'
        AND udt_name = 'oid'
    ) THEN
      ALTER TABLE public.panel
        ALTER COLUMN data TYPE bytea
        USING lo_get(data);
    END IF;
  END IF;
END $$;

