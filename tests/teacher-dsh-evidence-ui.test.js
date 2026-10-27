import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const read = (path) => readFile(new URL(path, import.meta.url), 'utf8');

test('student Git run restoration uses the Git run API instead of the legacy workspace response', async () => {
  const app = await read('../app.js');
  const restoreStart = app.indexOf('async function restoreExistingRun');
  const restoreEnd = app.indexOf('\nasync function startGitExperimentRun', restoreStart);
  const restoreSource = app.slice(restoreStart, restoreEnd);

  assert.match(restoreSource, /apiRequest\(`\/student\/runs\/\$\{encodeURIComponent\(runId\)\}`/);
  assert.match(restoreSource, /run\.executionMode \|\| run\.mode/);
  assert.match(restoreSource, /activeNodeId = nodeProgress\.currentNodeId \|\| activeNodeId/);
  assert.doesNotMatch(restoreSource, /apiRequest\(`\/runs\/\$\{encodeURIComponent\(runId\)\}`/);
});

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

test('teacher can self-assign review of a personal experiment in their course scope', async () => {
  const [html, app] = await Promise.all([read('../index.html'), read('../app.js')]);
  assert.match(html, /id="teacherReviewableExperiments"/);
  assert.match(html, /仅显示你所负责课程学生/);
  assert.match(app, /apiRequest\('\/teacher\/personal-experiments\/reviewable'/);
  assert.match(app, /\/teacher\/experiments\/\$\{encodeURIComponent\(experiment\.experimentId\)\}\/reviewer/);
  assert.match(app, /teacherId: Number\(user\.id\)/);
  assert.doesNotMatch(app, /\/admin\/(?:reviewable-experiments|teachers|experiments)/);
});
