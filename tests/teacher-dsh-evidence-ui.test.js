import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const read = (path) => readFile(new URL(path, import.meta.url), 'utf8');

test('teacher DSH view is explicitly limited to student-submitted snapshots', async () => {
  const [html, app] = await Promise.all([read('../index.html'), read('../app.js')]);
  assert.match(html, /DSH 学习记录与主动提交/);
  assert.match(html, /不读取实时工作区、对话或终端/);
  assert.match(app, /apiRequest\('\/teacher\/experiments'/);
  assert.match(app, /\/teacher\/experiments\/\$\{encodeURIComponent\(experimentId\)\}\/runs/);
  assert.match(app, /\/teacher\/runs\/\$\{encodeURIComponent\(runId\)\}\/submissions/);
  assert.match(app, /\/teacher\/runs\/\$\{encodeURIComponent\(runId\)\}\/submissions\/\$\{encodeURIComponent\(submissionId\)\}/);
  assert.match(app, /\/teacher\/dsh\/runs\/\$\{encodeURIComponent\(runId\)\}\/review/);
  assert.match(app, /teacherDshCode\.textContent = files\.map/);
  assert.doesNotMatch(app, /teacherDshCode\.innerHTML/);
});

test('admin reviewer assignment UI is role-routed and uses the protected binding endpoint', async () => {
  const [html, app] = await Promise.all([read('../index.html'), read('../app.js')]);
  assert.match(html, /id="adminView"/);
  assert.match(html, /个人实验审核教师/);
  assert.match(html, /id="adminExperiment"/);
  assert.match(html, /id="adminReviewer"/);
  assert.match(app, /\['student', 'teacher', 'admin'\]/);
  assert.match(app, /route === 'admin' && user\.role === 'admin'/);
  assert.match(app, /apiRequest\('\/admin\/reviewable-experiments'/);
  assert.match(app, /apiRequest\('\/admin\/teachers'/);
  assert.match(app, /\/admin\/experiments\/\$\{encodeURIComponent\(experimentId\)\}\/reviewer/);
});
