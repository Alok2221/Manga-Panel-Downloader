DO $$
BEGIN
  -- Only run this migration if the legacy "panel" table already exists.
  -- On fresh databases, Hibernate (ddl-auto=update) will create the schema,
  -- including panel and panel_text_segment, so we skip this Flyway step.
  IF to_regclass('public.panel') IS NOT NULL THEN
    CREATE TABLE IF NOT EXISTS panel_text_segment (
        id BIGSERIAL PRIMARY KEY,
        panel_id BIGINT NOT NULL REFERENCES panel (id) ON DELETE CASCADE,
        sequence_index INTEGER NOT NULL,
        source_language VARCHAR(10),
        target_language VARCHAR(10),
        source_text TEXT NOT NULL,
        translated_text TEXT,
        bbox_x INTEGER,
        bbox_y INTEGER,
        bbox_w INTEGER,
        bbox_h INTEGER,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
        updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    );

    CREATE INDEX IF NOT EXISTS idx_panel_text_segment_panel_id
        ON panel_text_segment (panel_id);
  END IF;
END $$;

