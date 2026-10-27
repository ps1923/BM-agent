---
status: done
baseline_commit: abff22ffe0029b93bea543eabccac9eed3674c72
---

# RAG 实验记忆相关性与恢复

## 目标

保持 Bug 记忆仅在所属实验内共享，并改善检索相关性、向量索引恢复能力和教师端说明。

## 变更

- 教师端明确标注“本实验记忆池”。
- 代码验收按节点目标分别检索已审核案例，并仅将对应案例传给该节点检查。
- Chroma 检索支持节点与通用案例过滤；按可配置的最大距离阈值剔除弱相关候选。
- 已审核案例索引失败后保存重试次数与下次重试时间，后台退避重试；超过次数上限后需教师重新审核触发重建。
- 保持 MySQL 对实验归属、审核状态和调用学生权限的最终校验。

## 验收

## Tasks & Acceptance

- [x] 教师页面和 Bug 记录流程明确说明实验级记忆池隔离。
- [x] 代码验收与学生指导按节点过滤；检索结果仍经 MySQL 校验实验归属和审核状态。
- [x] 应用可配置最大向量距离，低相关候选不进入模型上下文。
- [x] 向量索引失败保存次数和下次时间，定时按退避重试且最多六次；重新审核重置计数。
- [x] 执行 Java 全套、前端全套、迁移检查和差异安全复核。
- [x] 保留工作区其他未提交更改，不部署生产环境。

## 约束

- 不改变旧实验和 Bug 数据的归属，不删除历史案例。
- 不自动部署生产服务器。

## Suggested Review Order

**节点级检索隔离**

- 先看验收入口如何为每个节点分别构造查询并绑定案例。
  [`GitAcceptanceService.java:290`](../../backend/src/main/java/com/bmhs/git/GitAcceptanceService.java#L290)

- Harness 输入按节点分隔案例，避免跨节点借用经验。
  [`GitCodeAcceptanceHarness.java:37`](../../backend/src/main/java/com/bmhs/git/GitCodeAcceptanceHarness.java#L37)

- Chroma 先筛节点与通用案例，MySQL 再验证实验和审核状态。
  [`ChromaClient.java:105`](../../backend/src/main/java/com/bmhs/rag/ChromaClient.java#L105)

**相关性与失败恢复**

- 弱相关候选在进入验收上下文前按距离阈值剔除。
  [`RagService.java:20`](../../backend/src/main/java/com/bmhs/rag/RagService.java#L20)

- 审核通过的索引失败后按退避重试，次数有并发安全上限。
  [`BugRepository.java:168`](../../backend/src/main/java/com/bmhs/rag/BugRepository.java#L168)

- 迁移增量保存重试状态、查询索引和上限约束。
  [`20261027_rag_index_retry.sql:1`](../../database/migrations/20261027_rag_index_retry.sql#L1)

**配置、教师说明与验证**

- 距离阈值和扫描间隔由服务端配置，便于部署校准。
  [`application.yml:91`](../../backend/src/main/resources/application.yml#L91)

- 教师端说明案例只会进入本实验记忆池。
  [`index.html:244`](../../index.html#L244)

- 单测覆盖相关性、节点过滤、索引失败和重试边界。
  [`RagServiceTest.java:129`](../../backend/src/test/java/com/bmhs/rag/RagServiceTest.java#L129)

- 集成测试贯通案例索引、Chroma 节点检索和 MySQL 授权复核。
  [`RagServiceTest.java:24`](../../backend/src/test/java/com/bmhs/rag/RagServiceTest.java#L24)

  [`BugRepositoryTest.java:25`](../../backend/src/test/java/com/bmhs/rag/BugRepositoryTest.java#L25)
