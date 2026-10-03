-- BE-07 bug cases and RAG metadata migration.
-- Safe to run repeatedly against a compatible BM-sql installation.
USE `BM-sql`;

CREATE TABLE IF NOT EXISTS `bug_cases` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `run_id` BIGINT UNSIGNED NOT NULL,
  `node_id` BIGINT UNSIGNED NULL,
  `course_id` BIGINT UNSIGNED NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `title` VARCHAR(200) NOT NULL,
  `problem` MEDIUMTEXT NOT NULL,
  `solution` MEDIUMTEXT NULL,
  `technology_stack` VARCHAR(100) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
  `visibility` VARCHAR(20) NOT NULL DEFAULT 'personal',
  `vector_status` VARCHAR(20) NOT NULL DEFAULT 'not_indexed',
  `chroma_document_id` VARCHAR(120) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_bug_cases_student_created` (`student_id`, `created_at`),
  KEY `idx_bug_cases_course_status` (`course_id`, `status`),
  CONSTRAINT `fk_bug_cases_run`
    FOREIGN KEY (`run_id`, `experiment_id`) REFERENCES `experiment_runs` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_bug_cases_node`
    FOREIGN KEY (`node_id`, `experiment_id`) REFERENCES `experiment_nodes` (`id`, `experiment_id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_bug_cases_course`
    FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
    ON UPDATE RESTRICT ON DELETE SET NULL,
  CONSTRAINT `fk_bug_cases_student`
    FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_bug_cases_status` CHECK (`status` IN ('pending', 'approved', 'rejected')),
  CONSTRAINT `ck_bug_cases_visibility` CHECK (`visibility` IN ('personal', 'course')),
  CONSTRAINT `ck_bug_cases_vector_status` CHECK (`vector_status` IN ('not_indexed', 'indexed', 'failed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @bug_technology_stack_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bug_cases' AND COLUMN_NAME = 'technology_stack'
);
SET @bug_technology_stack_sql = IF(
  @bug_technology_stack_exists = 0,
  'ALTER TABLE `bug_cases` ADD COLUMN `technology_stack` VARCHAR(100) NULL AFTER `solution`',
  'SELECT 1'
);
PREPARE bug_technology_stack_stmt FROM @bug_technology_stack_sql;
EXECUTE bug_technology_stack_stmt;
DEALLOCATE PREPARE bug_technology_stack_stmt;

CREATE TABLE IF NOT EXISTS `bug_case_reviews` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `bug_case_id` BIGINT UNSIGNED NOT NULL,
  `teacher_id` BIGINT UNSIGNED NOT NULL,
  `decision` VARCHAR(20) NOT NULL,
  `feedback` VARCHAR(1000) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_bug_case_reviews_case_created` (`bug_case_id`, `created_at`),
  CONSTRAINT `fk_bug_case_reviews_case`
    FOREIGN KEY (`bug_case_id`) REFERENCES `bug_cases` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_bug_case_reviews_teacher`
    FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_bug_case_reviews_decision` CHECK (`decision` IN ('approved', 'rejected'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
