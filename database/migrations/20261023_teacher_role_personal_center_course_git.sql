-- Remove the standalone admin role, add personal Git author fields and course repository URLs.
-- Existing account and assignment rows are preserved; safe to rerun on the supported schema.
USE `BM-sql`;

SET @has_git_author_name = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'BM-sql' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'git_author_name'
);
SET @ddl = IF(@has_git_author_name = 0,
  'ALTER TABLE `users` ADD COLUMN `git_author_name` VARCHAR(100) NULL AFTER `display_name`', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_git_author_email = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'BM-sql' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'git_author_email'
);
SET @ddl = IF(@has_git_author_email = 0,
  'ALTER TABLE `users` ADD COLUMN `git_author_email` VARCHAR(320) NULL AFTER `git_author_name`', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_course_repository_url = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'BM-sql' AND TABLE_NAME = 'courses' AND COLUMN_NAME = 'git_repository_url'
);
SET @ddl = IF(@has_course_repository_url = 0,
  'ALTER TABLE `courses` ADD COLUMN `git_repository_url` VARCHAR(1000) NULL AFTER `invite_code`', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop the old role check before converting data, then re-add the student/teacher-only check.
SET @has_role_check = (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = 'BM-sql' AND TABLE_NAME = 'users'
    AND CONSTRAINT_NAME = 'ck_users_role' AND CONSTRAINT_TYPE = 'CHECK'
);
SET @ddl = IF(@has_role_check > 0, 'ALTER TABLE `users` DROP CHECK `ck_users_role`', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `users` SET `role` = 'teacher' WHERE `role` = 'admin';

SET @has_role_check = (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = 'BM-sql' AND TABLE_NAME = 'users'
    AND CONSTRAINT_NAME = 'ck_users_role' AND CONSTRAINT_TYPE = 'CHECK'
);
SET @ddl = IF(@has_role_check = 0,
  'ALTER TABLE `users` ADD CONSTRAINT `ck_users_role` CHECK (`role` IN (''student'', ''teacher''))', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add the teacher audit actor field without dropping the legacy field, so the previous release can roll back.
SET @has_reviewer_course_id = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'BM-sql' AND TABLE_NAME = 'experiment_reviewers' AND COLUMN_NAME = 'course_id'
);
SET @ddl = IF(@has_reviewer_course_id = 0,
  'ALTER TABLE `experiment_reviewers` ADD COLUMN `course_id` BIGINT UNSIGNED NULL COMMENT ''NULL indicates an untrusted legacy assignment that cannot grant access'' AFTER `experiment_id`', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_reviewer_course_fk = (
  SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = 'BM-sql' AND TABLE_NAME = 'experiment_reviewers'
    AND CONSTRAINT_NAME = 'fk_experiment_reviewers_course'
);
SET @ddl = IF(@has_reviewer_course_fk = 0,
  'ALTER TABLE `experiment_reviewers` ADD CONSTRAINT `fk_experiment_reviewers_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Trust only historical assignments where the assigned teacher owns the student's active course.
UPDATE experiment_reviewers er
SET er.course_id = (
  SELECT MIN(c.id)
  FROM experiments e
  JOIN course_members m ON m.student_id = e.creator_id AND m.status = 'active'
  JOIN courses c ON c.id = m.course_id AND c.status = 'active' AND c.teacher_id = er.teacher_id
  WHERE e.id = er.experiment_id
)
WHERE er.course_id IS NULL
  AND EXISTS (
    SELECT 1 FROM experiments e
    JOIN course_members m ON m.student_id = e.creator_id AND m.status = 'active'
    JOIN courses c ON c.id = m.course_id AND c.status = 'active' AND c.teacher_id = er.teacher_id
    WHERE e.id = er.experiment_id
  );

SET @has_legacy_reviewer_column = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'BM-sql' AND TABLE_NAME = 'experiment_reviewers'
    AND COLUMN_NAME = 'assigned_by_admin_id'
);
SET @has_teacher_reviewer_column = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'BM-sql' AND TABLE_NAME = 'experiment_reviewers'
    AND COLUMN_NAME = 'assigned_by_teacher_id'
);
SET @ddl = IF(@has_teacher_reviewer_column = 0,
  'ALTER TABLE `experiment_reviewers` ADD COLUMN `assigned_by_teacher_id` BIGINT UNSIGNED NULL', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Preserve existing assignment actors; never overwrite a value already written by the new application.
SET @ddl = IF(@has_legacy_reviewer_column > 0,
  'UPDATE `experiment_reviewers` SET `assigned_by_teacher_id` = `assigned_by_admin_id` WHERE `assigned_by_teacher_id` IS NULL', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Keep the legacy actor for rollback/history, but allow new rows to use the teacher actor only.
SET @ddl = IF(@has_legacy_reviewer_column > 0,
  'ALTER TABLE `experiment_reviewers` MODIFY COLUMN `assigned_by_admin_id` BIGINT UNSIGNED NULL', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE `experiment_reviewers` MODIFY COLUMN `assigned_by_teacher_id` BIGINT UNSIGNED NOT NULL;

SET @has_teacher_reviewer_fk = (
  SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = 'BM-sql' AND TABLE_NAME = 'experiment_reviewers'
    AND CONSTRAINT_NAME = 'fk_experiment_reviewers_assigned_by_teacher'
);
SET @has_teacher_reviewer_column = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'BM-sql' AND TABLE_NAME = 'experiment_reviewers'
    AND COLUMN_NAME = 'assigned_by_teacher_id'
);
SET @ddl = IF(@has_teacher_reviewer_column > 0 AND @has_teacher_reviewer_fk = 0,
  'ALTER TABLE `experiment_reviewers` ADD CONSTRAINT `fk_experiment_reviewers_assigned_by_teacher` FOREIGN KEY (`assigned_by_teacher_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
