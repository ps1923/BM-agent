-- BE-09 teacher progress and review migration.
-- Safe to run repeatedly against a compatible BM-sql installation.
USE `BM-sql`;

CREATE TABLE IF NOT EXISTS `teacher_reviews` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT UNSIGNED NOT NULL,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `run_id` BIGINT UNSIGNED NOT NULL,
  `teacher_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `rating` TINYINT UNSIGNED NULL,
  `feedback` VARCHAR(2000) NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_teacher_reviews_run_teacher` (`run_id`, `teacher_id`),
  KEY `idx_teacher_reviews_course_updated` (`course_id`, `updated_at`),
  CONSTRAINT `fk_teacher_reviews_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_teacher_reviews_run` FOREIGN KEY (`run_id`, `experiment_id`) REFERENCES `experiment_runs` (`id`, `experiment_id`) ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_teacher_reviews_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `fk_teacher_reviews_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_teacher_reviews_rating` CHECK (`rating` IS NULL OR (`rating` >= 0 AND `rating` <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
