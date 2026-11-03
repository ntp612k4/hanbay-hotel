ALTER TABLE ai_requests
    ADD COLUMN conversation_id UUID,
    ADD COLUMN locale VARCHAR(10),
    ADD COLUMN fallback BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE ai_requests
    ALTER COLUMN feature TYPE VARCHAR(80),
    ALTER COLUMN response DROP NOT NULL,
    ALTER COLUMN conversation_id SET DEFAULT gen_random_uuid(),
    ALTER COLUMN locale SET DEFAULT 'vi';

UPDATE ai_requests
SET conversation_id = COALESCE(conversation_id, id),
    locale = COALESCE(NULLIF(locale, ''), 'vi')
WHERE conversation_id IS NULL
   OR locale IS NULL
   OR locale = '';

ALTER TABLE ai_requests
    ALTER COLUMN conversation_id SET NOT NULL,
    ALTER COLUMN locale SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ai_requests_conversation_id
    ON ai_requests(conversation_id);
