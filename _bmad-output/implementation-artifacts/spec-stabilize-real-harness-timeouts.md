---
title: '稳定真实 Harness 的计划生成超时'
type: 'bugfix'
created: '2026-09-11'
status: 'done'
route: 'one-shot'
---

# 稳定真实 Harness 的计划生成超时

## Intent

**Problem:** 真实模型生成树状实验计划约需 45 秒，原 MCP 45 秒和浏览器 60 秒超时缺少安全余量，可能在模型正常返回前中断请求。

**Approach:** 将 MCP 等待上限提高到 120 秒，并只为前端计划生成请求设置 150 秒上限；其他 API 仍保持 60 秒默认值，同时校验自定义超时必须为正有限数。

## Suggested Review Order

**计划请求预算**

- 计划生成单独获得 150 秒浏览器等待窗口。
  [`app.js:701`](../../app.js#L701)

- MCP 留出 120 秒真实模型响应时间。
  [`application.yml:19`](../../backend/src/main/resources/application.yml#L19)

**通用请求边界**

- 默认超时保持 60 秒并拒绝无效覆盖值。
  [`app.js:496`](../../app.js#L496)

