-- Applied patches must have a timestamp; pending and rejected patches may leave it null.
-- Run only after confirming the target MySQL version enforces CHECK constraints.
USE `BM-sql`;

SET @patch_timestamp_check_exists = (
  SELECT COUNT(*)
  FROM `information_schema`.`CHECK_CONSTRAINTS`
  WHERE `CONSTRAINT_SCHEMA` = 'BM-sql'
    AND `CONSTRAINT_NAME` = 'ck_code_patches_applied_at'
);
SET @drop_patch_timestamp_check_sql = IF(
  @patch_timestamp_check_exists = 1,
  'ALTER TABLE `code_patches` DROP CHECK `ck_code_patches_applied_at`',
  'DO 0'
);
PREPARE drop_patch_timestamp_check FROM @drop_patch_timestamp_check_sql;
EXECUTE drop_patch_timestamp_check;
DEALLOCATE PREPARE drop_patch_timestamp_check;

ALTER TABLE `code_patches`
  ADD CONSTRAINT `ck_code_patches_applied_at`
    CHECK (`status` <> 'applied' OR `applied_at` IS NOT NULL);
