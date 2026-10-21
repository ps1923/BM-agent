import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');

test('student workspace exposes DSH launch and stop controls with accessible live status', () => {
  assert.match(html, /id="studentDshLaunchButton"[^>]*>打开 DSH/);
  assert.match(html, /id="studentDshStopButton"[^>]*>停止工作区/);
  assert.match(html, /id="studentDshState" role="status" aria-live="polite"/);
  assert.match(app, /apiRequest\('\/student\/dsh\/status'/);
  assert.match(app, /apiRequest\('\/student\/dsh\/launch'/);
  assert.match(app, /apiRequest\('\/student\/dsh\/stop'/);
});

test('published student experiments open DSH rather than the legacy BM-HS editor', () => {
  assert.match(app, /openButton\.textContent = '在 DSH 中打开'/);
  assert.match(app, /studentCourses\.addEventListener\('click',[\s\S]*?openStudentDsh\(button\)/);
  assert.doesNotMatch(app, /openExperiment\(experimentIdValue\)/);
});

test('student navigation accepts only HTTPS DSH URLs except loopback development', () => {
  assert.match(app, /url\.protocol !== 'https:'/);
  assert.match(app, /url\.username \|\| url\.password/);
  assert.match(app, /blankTab\.opener = null/);
});
