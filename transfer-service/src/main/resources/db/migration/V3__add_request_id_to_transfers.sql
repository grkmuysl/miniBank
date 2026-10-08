-- ID of the request that created the transfer. Lets every later retry log with the same ID.
-- Nullable: transfers created before this migration have none.
ALTER TABLE transfers ADD COLUMN request_id VARCHAR(64);