-- Adds cost basis tracking to existing positions. Added nullable first so
-- existing rows don't fail the ALTER, backfilled to 0 for every row that
-- predates this column (sync will overwrite it with the real weighted
-- average on the next /positions/sync), then locked down to NOT NULL with
-- a matching default so future inserts can't skip it either.
ALTER TABLE position ADD COLUMN cost_basis NUMERIC;

UPDATE position SET cost_basis = 0 WHERE cost_basis IS NULL;

ALTER TABLE position ALTER COLUMN cost_basis SET DEFAULT 0;
ALTER TABLE position ALTER COLUMN cost_basis SET NOT NULL;
