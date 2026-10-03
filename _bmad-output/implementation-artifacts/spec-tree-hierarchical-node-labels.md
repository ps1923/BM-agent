---
title: '实验节点层级编号显示'
type: 'feature'
created: '2026-10-03'
status: 'done'
route: 'one-shot'
baseline_commit: '8454527b846d8add4f39178501c9fa81ac7f91a0'
---

# 实验节点层级编号显示

## Intent

**Problem:** 画布曾直接展示内部节点 key，创建预览使用平铺序号；多级分支难以阅读，也无法直观看出 `3.1.1` 的父子层次。

**Approach:** 在前端根据真实父子关系统一推导显示编号，供创建预览、实验画布和节点详情共用；保留 API key 不变，并为历史多根树提供不重复编号与正确画布定位。

## Suggested Review Order

**编号与树布局**

- 用单一稳定排序规则推导层级标签，并保留内部 key。
  [`app.js:1008`](../../app.js#L1008)

- 让旧多根树的画布定位锚定实际中心节点。
  [`app.js:1055`](../../app.js#L1055)

- 画布气泡与选中详情都显示同一层级编号。
  [`app.js:1096`](../../app.js#L1096)

- 创建预览复用同一编号和兄弟排序规则。
  [`app.js:2426`](../../app.js#L2426)

**回归覆盖**

- 覆盖分支、`3.1.1`、坐标缺失、多根兼容及预览/画布集成。
  [`tree-node-numbering.test.js:15`](../../tests/tree-node-numbering.test.js#L15)
