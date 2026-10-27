-- Fresh demonstration data for the node-level AI acceptance flow.
-- Run database/schema.sql first. This script is idempotent and never deletes existing rows.
-- Demo password for both accounts: 12345678

USE `BM-sql`;
START TRANSACTION;

INSERT INTO `users` (`email`, `display_name`, `password_hash`, `role`, `status`)
VALUES
  ('teacher.demo@example.com', '演示教师', '$2b$10$XGVOSD3HML7jG9OaNXToJ.qX0bhGieoseRvkOLOEziTRcxM2k51wC', 'teacher', 'active'),
  ('student.demo@example.com', '演示学生', '$2b$10$XGVOSD3HML7jG9OaNXToJ.qX0bhGieoseRvkOLOEziTRcxM2k51wC', 'student', 'active')
ON DUPLICATE KEY UPDATE
  `display_name` = VALUES(`display_name`),
  `password_hash` = VALUES(`password_hash`),
  `role` = VALUES(`role`),
  `status` = 'active';

SELECT `id` INTO @demo_teacher_id FROM `users` WHERE `email` = 'teacher.demo@example.com';
SELECT `id` INTO @demo_student_id FROM `users` WHERE `email` = 'student.demo@example.com';

INSERT INTO `courses` (`teacher_id`, `name`, `invite_code`, `status`)
VALUES (@demo_teacher_id, '智能编程实验演示课', 'BMHSDEMO26', 'active')
ON DUPLICATE KEY UPDATE `teacher_id` = VALUES(`teacher_id`), `status` = 'active';
SELECT `id` INTO @demo_course_id FROM `courses` WHERE `invite_code` = 'BMHSDEMO26';

INSERT INTO `course_members` (`course_id`, `student_id`, `status`)
VALUES (@demo_course_id, @demo_student_id, 'active')
ON DUPLICATE KEY UPDATE `status` = 'active';

SET @demo_experiment_marker = 'bmhs-node-acceptance-demo-v1';
SET @demo_experiment_id = (
  SELECT `id` FROM `experiments`
  WHERE `creator_id` = @demo_teacher_id
    AND JSON_UNQUOTE(JSON_EXTRACT(`generation_config`, '$.demo_seed')) = @demo_experiment_marker
  ORDER BY `id` LIMIT 1
);

INSERT INTO `experiments`
  (`creator_id`, `name`, `learning_goal`, `description`, `difficulty`, `duration_days`,
   `duration_minutes`, `status`, `visibility`, `generation_config`, `published_at`)
SELECT @demo_teacher_id, 'Django 从零实现课程首页',
       '从空白目录搭建 Django 项目，并实现可通过 URL 访问的课程首页。',
       '演示实验：逐步完成项目初始化、首页应用与路由；学生提交 commit 后按节点验收。',
       'beginner', 7, 180, 'published', 'unlisted',
       JSON_OBJECT('demo_seed', @demo_experiment_marker, 'technology_stack', 'Python/Django'),
       CURRENT_TIMESTAMP(6)
WHERE @demo_experiment_id IS NULL;

SET @demo_experiment_id = COALESCE(@demo_experiment_id, LAST_INSERT_ID());
SET @demo_experiment_id = COALESCE(@demo_experiment_id, (
  SELECT `id` FROM `experiments`
  WHERE `creator_id` = @demo_teacher_id
    AND JSON_UNQUOTE(JSON_EXTRACT(`generation_config`, '$.demo_seed')) = @demo_experiment_marker
  ORDER BY `id` LIMIT 1
));

INSERT INTO `course_experiment_assignments` (`course_id`, `experiment_id`, `publisher_id`, `status`)
VALUES (@demo_course_id, @demo_experiment_id, @demo_teacher_id, 'published')
ON DUPLICATE KEY UPDATE `publisher_id` = VALUES(`publisher_id`), `status` = 'published';

INSERT INTO `experiment_stages` (`experiment_id`, `name`, `description`, `sort_order`)
SELECT @demo_experiment_id, stage.name, stage.description, stage.sort_order
FROM (
  SELECT '准备项目环境' AS name, '建立最小可运行的 Django 项目结构。' AS description, 1 AS sort_order
  UNION ALL SELECT '实现首页应用', '创建应用、视图和页面模板。', 2
  UNION ALL SELECT '配置与验证路由', '将 URL 映射到首页，并准备可核对的验证材料。', 3
) AS stage
WHERE NOT EXISTS (
  SELECT 1 FROM `experiment_stages` existing
  WHERE existing.`experiment_id` = @demo_experiment_id AND existing.`sort_order` = stage.sort_order
);

SELECT `id` INTO @demo_stage_1 FROM `experiment_stages` WHERE `experiment_id` = @demo_experiment_id AND `sort_order` = 1;
SELECT `id` INTO @demo_stage_2 FROM `experiment_stages` WHERE `experiment_id` = @demo_experiment_id AND `sort_order` = 2;
SELECT `id` INTO @demo_stage_3 FROM `experiment_stages` WHERE `experiment_id` = @demo_experiment_id AND `sort_order` = 3;

INSERT INTO `experiment_nodes`
  (`experiment_id`, `stage_id`, `parent_node_id`, `sequence_code`, `name`, `description`,
   `estimated_minutes`, `canvas_x`, `canvas_y`, `sort_order`)
SELECT @demo_experiment_id, @demo_stage_1, NULL, '1', '创建空白 Django 项目',
       '从空白目录建立虚拟环境，安装 Django 并创建可启动的项目骨架。', 40, 120, 80, 1
WHERE NOT EXISTS (SELECT 1 FROM `experiment_nodes` WHERE `experiment_id` = @demo_experiment_id AND `sequence_code` = '1');

SET @demo_node_1 = (SELECT `id` FROM `experiment_nodes` WHERE `experiment_id` = @demo_experiment_id AND `sequence_code` = '1');

INSERT INTO `experiment_nodes`
  (`experiment_id`, `stage_id`, `parent_node_id`, `sequence_code`, `name`, `description`,
   `estimated_minutes`, `canvas_x`, `canvas_y`, `sort_order`)
SELECT @demo_experiment_id, @demo_stage_2, @demo_node_1, '1.1', '创建首页应用与视图',
       '在项目基础上创建 Django 应用，编写首页视图并返回明确的页面响应。', 55, 410, 80, 2
WHERE NOT EXISTS (SELECT 1 FROM `experiment_nodes` WHERE `experiment_id` = @demo_experiment_id AND `sequence_code` = '2');

SET @demo_node_2 = (SELECT `id` FROM `experiment_nodes` WHERE `experiment_id` = @demo_experiment_id AND `sequence_code` = '1.1');

INSERT INTO `experiment_nodes`
  (`experiment_id`, `stage_id`, `parent_node_id`, `sequence_code`, `name`, `description`,
   `estimated_minutes`, `canvas_x`, `canvas_y`, `sort_order`)
SELECT @demo_experiment_id, @demo_stage_3, @demo_node_2, '1.1.1', '配置首页路由并验证响应',
       '完成项目级与应用级 URL 映射，提交能证明路由目标和响应内容的代码或测试证据。', 45, 700, 80, 3
WHERE NOT EXISTS (SELECT 1 FROM `experiment_nodes` WHERE `experiment_id` = @demo_experiment_id AND `sequence_code` = '1.1.1');

SET @demo_node_3 = (SELECT `id` FROM `experiment_nodes` WHERE `experiment_id` = @demo_experiment_id AND `sequence_code` = '1.1.1');

INSERT INTO `node_dependencies` (`experiment_id`, `from_node_id`, `to_node_id`, `relation_type`)
SELECT @demo_experiment_id, edge.from_node_id, edge.to_node_id, 'prerequisite'
FROM (
  SELECT @demo_node_1 AS from_node_id, @demo_node_2 AS to_node_id
  UNION ALL SELECT @demo_node_2, @demo_node_3
) AS edge
WHERE NOT EXISTS (
  SELECT 1 FROM `node_dependencies` existing
  WHERE existing.`experiment_id` = @demo_experiment_id
    AND existing.`from_node_id` = edge.from_node_id AND existing.`to_node_id` = edge.to_node_id
);

INSERT INTO `node_tasks` (`experiment_id`, `node_id`, `title`, `description`, `sort_order`, `validation_type`)
SELECT @demo_experiment_id, task.node_id, task.title, task.description, task.sort_order, task.validation_type
FROM (
  SELECT @demo_node_1 AS node_id, '创建项目目录与虚拟环境' AS title,
         '提交项目依赖清单，并在说明中记录创建虚拟环境和安装 Django 的命令。' AS description, 1 AS sort_order, 'agent' AS validation_type
  UNION ALL SELECT @demo_node_1, '生成 Django 项目骨架', '提交 manage.py、项目配置包及必要的启动配置文件。', 2, 'agent'
  UNION ALL SELECT @demo_node_2, '创建首页应用与视图', '提交应用配置、视图函数及其返回响应的实现。', 1, 'agent'
  UNION ALL SELECT @demo_node_2, '准备首页模板', '提交模板文件，并能从视图中明确定位到该模板。', 2, 'agent'
  UNION ALL SELECT @demo_node_3, '连接项目 URL 与应用 URL', '提交项目级和应用级 URL 配置，映射到首页视图。', 1, 'agent'
  UNION ALL SELECT @demo_node_3, '添加首页响应验证', '提交测试或清晰的验证材料，证明目标 URL 对应首页响应。', 2, 'test'
) AS task
WHERE NOT EXISTS (
  SELECT 1 FROM `node_tasks` existing
  WHERE existing.`node_id` = task.node_id AND existing.`sort_order` = task.sort_order
);

INSERT INTO `experiment_node_acceptance_criteria`
  (`experiment_id`, `node_id`, `version`, `source`, `criteria_json`)
SELECT @demo_experiment_id, criterion.node_id, 1, 'generated', criterion.criteria_json
FROM (
  SELECT @demo_node_1 AS node_id,
         JSON_ARRAY(
           JSON_OBJECT('criterion', '项目依赖中声明 Django，项目入口与配置结构齐全。', 'verification_method', 'static_code', 'expected_evidence', 'requirements.txt 或 pyproject.toml、manage.py、项目 settings 模块。'),
           JSON_OBJECT('criterion', '项目配置指向明确的根 URL 配置，具备启动所需的基础设置。', 'verification_method', 'static_code', 'expected_evidence', '项目 settings.py 中的 ROOT_URLCONF、INSTALLED_APPS 和基础配置。')
         ) AS criteria_json
  UNION ALL
  SELECT @demo_node_2,
         JSON_ARRAY(
           JSON_OBJECT('criterion', '首页应用已注册，视图函数返回可识别的首页内容。', 'verification_method', 'static_code', 'expected_evidence', 'settings.py 中的应用注册，以及 views.py 中的响应或模板渲染代码。'),
           JSON_OBJECT('criterion', '首页模板存在且被视图显式引用。', 'verification_method', 'static_code', 'expected_evidence', '模板文件路径与 render 调用中的模板名称。')
         )
  UNION ALL
  SELECT @demo_node_3,
         JSON_ARRAY(
           JSON_OBJECT('criterion', '项目 URL 配置包含首页路由，并可追溯到首页视图。', 'verification_method', 'static_code', 'expected_evidence', '项目 urls.py、应用 urls.py 及其 view 引用。'),
           JSON_OBJECT('criterion', '提交包含首页 URL 与预期响应之间的可核对验证。', 'verification_method', 'test_artifact', 'expected_evidence', '针对首页路径的 Django 测试及断言，或随 commit 提交的实际验证记录。')
         )
) AS criterion
WHERE NOT EXISTS (
  SELECT 1 FROM `experiment_node_acceptance_criteria` existing
  WHERE existing.`experiment_id` = @demo_experiment_id AND existing.`node_id` = criterion.node_id
);

COMMIT;

SELECT 'teacher.demo@example.com' AS `teacher_login`,
       'student.demo@example.com' AS `student_login`,
       '12345678' AS `demo_password`,
       'BMHSDEMO26' AS `course_invite_code`,
       @demo_experiment_id AS `experiment_id`;
