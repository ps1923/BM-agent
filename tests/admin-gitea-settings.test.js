import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const read = (path) => readFile(new URL(path, import.meta.url), 'utf8');

test('student and teacher share a personal center backed by the current-user profile API', async () => {
  const [html, app] = await Promise.all([read('../index.html'), read('../app.js')]);
  assert.match(html, /id="personalCenterView"/);
  assert.match(html, /id="studentPersonalCenterButton"/);
  assert.match(html, /id="teacherPersonalCenterButton"/);
  assert.match(html, /id="profileDisplayName"/);
  assert.match(html, /id="profileGitAuthorName"/);
  assert.match(html, /id="profileGitAuthorEmail"/);
  assert.match(app, /apiRequest\('\/me\/profile', \{ method: 'GET' \}\)/);
  assert.match(app, /apiRequest\('\/me\/profile', \{/);
  assert.match(app, /body: JSON\.stringify\(\{ displayName, gitAuthorName, gitAuthorEmail \}\)/);
  assert.doesNotMatch(app, /\['student', 'teacher', 'admin'\]/);
});

test('admin-only routes and global Gitea secret controls are removed from the UI', async () => {
  const [html, app] = await Promise.all([read('../index.html'), read('../app.js')]);
  assert.doesNotMatch(html, /id="adminView"|id="adminPersonalCenterView"|adminGitSettingsForm/);
  assert.doesNotMatch(html, /Gitea 管理令牌|Webhook 签名密钥|admin-secret-clear/);
  assert.doesNotMatch(app, /admin-personal-center|adminView|adminPersonalCenterView/);
  assert.doesNotMatch(app, /\/admin\/integrations\/gitea/);
  assert.doesNotMatch(app, /user\.role\s*===\s*'admin'/);
});

test('teacher course cards configure a course URL without replacing per-run repositories', async () => {
  const [html, app] = await Promise.all([read('../index.html'), read('../app.js')]);
  assert.match(html, /每门课程可关联一个仓库地址/);
  assert.match(app, /\/courses\/\$\{encodeURIComponent\(course\.id\)\}\/git-repository/);
  assert.match(app, /method: 'PUT', body: JSON\.stringify\(\{ repositoryUrl \}\)/);
  assert.match(app, /学生提交和 AI 验收仍使用各自独立的实验仓库/);
  assert.match(app, /\/student\/runs\/\$\{encodeURIComponent\(runId\)\}\/repository/);
  assert.match(app, /courseId: Number\(experiment\.courseId\)/);
  assert.match(app, /所属课程：\$\{experiment\.courseName/);
});

test('HTTP Git repositories are allowed only on the same HTTP host as the BM-HS page', async () => {
  const app = await read('../app.js');
  assert.match(app, /const isHttps = parsed\.protocol === 'https:'/);
  assert.match(app, /parsed\.protocol === 'http:'\s*&&\s*page\.protocol === 'http:'\s*&&\s*parsed\.host === page\.host/);
  assert.match(app, /\(isHttps \|\| isSameHostHttpDemo\) && !parsed\.username && !parsed\.password/);
  assert.doesNotMatch(app, /if \(parsed\.protocol !== 'https:' \|\| parsed\.username \|\| parsed\.password\)/);
});
