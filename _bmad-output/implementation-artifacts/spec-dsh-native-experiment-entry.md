---
title: '从实验节点进入 DSH 原生工作区'
type: 'feature'
created: '2026-10-22'
status: 'done'
route: 'one-shot'
baseline_commit: 'b3f4bb4877ad22d0eb23ef7ac4df33559923ead5'
---

# 从实验节点进入 DSH 原生工作区

## Intent

**Problem:** 学生从实验画布点击“开始攻克节点”时，旧流程会创建 BM-HS 自有运行实例并进入旧编辑器，和项目选定的 DSH 原生工作区基线冲突。

**Approach:** 按钮改为启动或重连学生个人 DSH，并在返回的平台页面显示可复制的实验名称和编号提示。学生在 DSH 对话中显式请求插件连接实验，插件继续在绑定前要求学生确认；不把实验身份塞入启动 URL，也不自动推进节点。

## Suggested Review Order

**学生入口与平台路由**

- 先看节点入口如何转向 DSH，避免建立旧工作区。
  [`app.js:3059`](../../app.js#L3059)

- 验证启动链接受限于平台来源和 DSH 专属路由。
  [`app.js:385`](../../app.js#L385)

- 检查 DSH 就绪提示保留实验绑定与学生确认边界。
  [`app.js:402`](../../app.js#L402)

**验收证据**

- 测试覆盖旧接口不再调用、HTTP 启动与失败反馈。
  [`tests/dsh-launch-ui.test.js:22`](../../tests/dsh-launch-ui.test.js#L22)
