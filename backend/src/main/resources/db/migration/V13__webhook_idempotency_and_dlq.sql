-- Add idempotency key to webhook deliveries and a partial unique index
ALTER TABLE IF EXISTS webhook_deliveries
  ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relname = 'ux_webhook_idempotency_key' AND n.nspname = 'public'
    ) THEN
        CREATE UNIQUE INDEX ux_webhook_idempotency_key ON webhook_deliveries(idempotency_key) WHERE idempotency_key IS NOT NULL;
    END IF;
END$$;


