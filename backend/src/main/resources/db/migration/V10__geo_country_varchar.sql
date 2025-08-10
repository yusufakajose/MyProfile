-- Fix type mismatch: change country from CHAR(2) to VARCHAR(2)
ALTER TABLE link_geo_daily_aggregate
    ALTER COLUMN country TYPE VARCHAR(2);


