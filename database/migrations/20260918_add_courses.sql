-- BE/FE-02 course, membership and published experiment migration.
-- Safe to run repeatedly against a compatible BM-sql installation.
USE `BM-sql`;

CREATE TABLE IF NOT EXISTS `courses` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `teacher_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(150) NOT NULL,
  `invite_code` VARCHAR(16) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'active',
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_courses_invite_code` (`invite_code`),
  KEY `idx_courses_teacher_status` (`teacher_id`, `status`),
  CONSTRAINT `fk_courses_teacher`
    FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_courses_status` CHECK (`status` IN ('active', 'archived'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_members` (
  `course_id` BIGINT UNSIGNED NOT NULL,
  `student_id` BIGINT UNSIGNED NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'active',
  `joined_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`course_id`, `student_id`),
  KEY `idx_course_members_student_status` (`student_id`, `status`),
  CONSTRAINT `fk_course_members_course`
    FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_course_members_student`
    FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_course_members_status` CHECK (`status` IN ('active', 'left'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_experiment_assignments` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT UNSIGNED NOT NULL,
  `experiment_id` BIGINT UNSIGNED NOT NULL,
  `publisher_id` BIGINT UNSIGNED NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'published',
  `published_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_course_experiment_assignment` (`course_id`, `experiment_id`),
  KEY `idx_assignments_course_status` (`course_id`, `status`),
  CONSTRAINT `fk_assignments_course`
    FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_assignments_experiment`
    FOREIGN KEY (`experiment_id`) REFERENCES `experiments` (`id`)
    ON UPDATE RESTRICT ON DELETE CASCADE,
  CONSTRAINT `fk_assignments_publisher`
    FOREIGN KEY (`publisher_id`) REFERENCES `users` (`id`)
    ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT `ck_assignments_status` CHECK (`status` IN ('published', 'archived'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
