
'use strict';

import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

test('student experiment plan and node inspector display generated acceptance criteria', async () => {
  const [html, app] = await Promise.all([
    readFile(new URL('../index.html', import.meta.url), 'utf8'),
    readFile(new URL('../app.js', import.meta.url), 'utf8'),
  ]);

  assert.match(html, /id="nodeAcceptanceCriteria"/);
  assert.match(html, /id="planDurationNotice"/);
  assert.match(app, /function renderNodeAcceptanceCriteria\(node\)/);
  assert.match(app, /criterion\.expectedEvidence/);
  assert.match(app, /totalEstimatedMinutes/);
  assert.match(app, /高于填写的/);
});

test('teacher acceptance criteria editor loads history and writes a new revision', async () => {
  const [html, app] = await Promise.all([
    readFile(new URL('../index.html', import.meta.url), 'utf8'),
    readFile(new URL('../app.js', import.meta.url), 'utf8'),
  ]);

  assert.match(html, /id="teacherCriteriaPanel"/);
  assert.match(html, /id="teacherCriteriaHistory"/);
  assert.match(app, /\/acceptance-criteria\/history/);
  assert.match(app, /method: 'PUT', body: JSON\.stringify\(\{ criteria \}\)/);
  assert.match(app, /已有验收报告保留原准则快照/);
});

test('teacher Bug review explains experiment-level memory isolation', async () => {
  const [html, app] = await Promise.all([
    readFile(new URL('../index.html', import.meta.url), 'utf8'),
    readFile(new URL('../app.js', import.meta.url), 'utf8'),
  ]);
  assert.match(html, /案例进入所属实验的记忆池；不同实验之间不会共享/);
  assert.match(app, /入库失败；自动重试次数有限，可手动重试/);
  assert.match(app, /重新审核并重试入库/);
  assert.match(app, /重试清理记忆索引/);
  assert.match(app, /正在从实验记忆池清理/);
  assert.match(app, /已进入实验记忆池/);
});
