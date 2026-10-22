import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const read = (path) => readFile(new URL(path, import.meta.url), 'utf8');

test('teacher Git review shows submitted commits, diffs, evidence, and keeps legacy snapshots historical', async () => {
  const [html, app] = await Promise.all([read('../index.html'), read('../app.js')]);
  assert.match(html, /学生提交与节点验收/);
  assert.match(html, /只查看学生 push 后提交检查的 commit/);
  assert.match(html, /历史工作区快照/);
  assert.match(app, /apiRequest\('\/teacher\/experiments'/);
  assert.match(app, /\/teacher\/experiments\/\$\{encodeURIComponent\(experimentId\)\}\/runs/);
  assert.match(app, /\/teacher\/runs\/\$\{encodeURIComponent\(runId\)\}\/submissions/);
  assert.match(app, /\/teacher\/submissions\/\$\{encodeURIComponent\(submissionId\)\}/);
  assert.match(app, /\/teacher\/runs\/\$\{encodeURIComponent\(runId\)\}\/reviews/);
  assert.match(app, /Array\.isArray\(detail\.diff\)/);
  assert.match(app, /Array\.isArray\(detail\.results\)/);
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
