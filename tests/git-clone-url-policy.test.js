import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

test('HTTP clone URLs are restricted to the same HTTP host and never embed credentials', async () => {
  const appPath = process.env.BMHS_GIT_APP_FILE ?? new URL('../app.js', import.meta.url);
  const app = await readFile(appPath, 'utf8');
  const start = app.indexOf('function isAllowedGitRepositoryUrl(');
  const end = app.indexOf('\nasync function loadGitRepository', start);
  assert.ok(start >= 0 && end > start, 'clone URL policy helper must exist before repository loading');
  const helper = app.slice(start, end);
  const isAllowed = new Function('window', `${helper}; return isAllowedGitRepositoryUrl;`)({
    location: { href: 'http://47.109.176.135/bm-agent/' },
  });

  assert.equal(isAllowed('http://47.109.176.135/gitea/student/repo.git'), true);
  assert.equal(isAllowed('http://other.example/gitea/student/repo.git'), false);
  assert.equal(isAllowed('https://git.example.edu/student/repo.git'), true);
  assert.equal(isAllowed('http://user:token@47.109.176.135/gitea/student/repo.git'), false);
  assert.equal(isAllowed('ftp://47.109.176.135/student/repo.git'), false);
});
