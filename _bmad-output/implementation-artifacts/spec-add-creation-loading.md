---
title: '为创建实验对话增加加载状态'
type: 'feature'
created: '2026-09-11'
status: 'done'
route: 'one-shot'
---

# 为创建实验对话增加加载状态

## Intent

**Problem:** 创建实验期间调用 Harness 和后端接口需要等待，界面没有反馈，用户无法判断请求是否仍在处理。

**Approach:** 在创建弹窗中增加可访问的实时加载提示和旋转指示器，按会话建立、需求整理、方向确认、周期保存、方案生成、节点创建显示对应文案，并在请求期间锁定输入，结束后恢复。

## Suggested Review Order

- 加载元素位于创建表单进度区，兼容三步流程。
  [`index.html:71`](../../index.html#L71)

- 加载提示使用状态语义、旋转动画和现有视觉语言。
  [`styles.css:107`](../../styles.css#L107)

- 请求状态统一控制 aria-busy、控件禁用和阶段文案。
  [`app.js:534`](../../app.js#L534)

- 各 Harness 请求分支显示具体阶段提示并在 finally 恢复。
  [`app.js:666`](../../app.js#L666)
