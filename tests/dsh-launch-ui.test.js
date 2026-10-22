import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');
const styles = await readFile(new URL('../styles.css', import.meta.url), 'utf8');

test('student flow uses local IDE guidance and does not expose a DSH launch control', () => {
  assert.match(html, /在本地 IDE 编码/);
  assert.match(html, /id="studentLocalCodingButton"[^>]*>查看实验/);
  assert.match(html, /id="studentLocalCodingState" role="status" aria-live="polite"/);
  assert.doesNotMatch(html, /studentDshLaunchButton|studentDshStopButton|打开 DSH/);
  assert.doesNotMatch(app, /apiRequest\('\/student\/dsh\/(?:status|launch|stop)'/);
});

test('published experiments open their learning path, and starting a node creates a Git-only run', () => {
  assert.match(app, /openButton\.textContent = '查看学习路径'/);
  assert.match(app, /studentCourses\.addEventListener\('click',[\s\S]*?openExperiment\(experimentId\)/);
  assert.match(html, /查看节点并提交代码/);
  assert.match(html, /节点树是学习建议和验收清单，不限制先后/);
  assert.match(app, /POST[\s\S]*?\/student\/experiments\/\$\{encodeURIComponent\(experimentId\)\}\/runs/);
  assert.match(app, /navigateTo\(\{ type: 'editor', runId: String\(runId\) \}\)/);
  assert.doesNotMatch(app, /openStudentDsh|validateDshLaunchUrl|\/student\/dsh\/launch/);
});

test('Git submission screen supports multiple node checks and reads only pushed commits', () => {
  assert.match(html, /id="gitCloneCommand"/);
  assert.match(html, /id="gitReviewNodeList"/);
  assert.match(html, /id="gitCommitSha"/);
  assert.match(html, /id="selectAllReviewNodes"[^>]*>全选节点/);
  assert.match(app, /checkbox\.name = 'nodeIds'/);
  assert.match(app, /nodeIds, description:/);
  assert.match(app, /\/student\/runs\/\$\{encodeURIComponent\(runId\)\}\/repository/);
  assert.match(app, /\/student\/runs\/\$\{encodeURIComponent\(runId\)\}\/submissions/);
  assert.match(app, /async function submitGitCommitForReview/);
  assert.match(app, /retry\.textContent = '重新尝试验收'/);
  assert.match(app, /已恢复失败提交的 SHA 和节点选择/);
  assert.match(app, /Git commit SHA/);
  assert.match(app, /我已完成/);
  assert.match(app, /已记录你的自我确认；这不代表 AI 验收或教师评分/);
  assert.match(styles, /\.git-submission-mode \.editor-workspace, \.git-submission-mode > \.terminal-panel/);
});

test('creation dialog preserves in-flight work on close and ignores backdrop clicks', () => {
  const start = app.indexOf('function closeCreateDialogPanel()');
  const end = app.indexOf('\nfunction updateCreateStep', start);
  const source = app.slice(start, end);
  assert.match(source, /createDialog\.classList\.remove\('is-open'\)/);
  assert.doesNotMatch(source, /cancelCreateRequest\(\)|createDialogEpoch\s*\+\+/);
  assert.match(app, /if \(event\.target === createDialog\) event\.stopPropagation\(\)/);
  assert.match(app, /event\.key === 'Escape'[\s\S]*?closeCreateDialogPanel\(\)/);
});

test('creation turns show the user message before awaiting and retry without appending another bubble', () => {
  const start = app.indexOf('async function sendCreationDirection(');
  const end = app.indexOf('\nfunction renderPlanPreview', start);
  const source = app.slice(start, end);
  assert.match(source, /existingBubble \|\| appendCreationMessage\('user', message\)/);
  assert.match(source, /appendCreationPending\(\)/);
  assert.ok(source.indexOf("appendCreationMessage('user', message)") < source.indexOf('await createApiRequest('));
  assert.match(source, /appendCreationRetry\(error\.message[\s\S]*userBubble/);
  assert.match(app, /retry\.textContent = '重试发送'/);
});

test('library exposes draft recovery through the planned session and ordered-message APIs', () => {
  assert.match(html, /id="creationDraftList"/);
  assert.match(app, /apiRequest\('\/experiment-creation\/sessions\/drafts'/);
  assert.match(app, /\/experiment-creation\/sessions\/\$\{encodedId\}\/messages/);
  assert.match(app, /\/experiment-creation\/sessions\/\$\{encodedId\}`/);
  assert.match(app, /function resumeCreationDraft\(sessionId\)/);
  assert.match(app, /formatDifficulty\(session\.difficulty\)/);
  assert.match(app, /renderPlanPreview\(session\.plan\)/);
  assert.match(app, /message\.role === 'user' \? 'user' : 'assistant'/);
  assert.match(app, /session\.status === 'plan_ready'/);
});

test('creation conversation persists between steps and plan confirmation remains outside scroll areas', () => {
  assert.match(html, /id="creationConversation"/);
  assert.doesNotMatch(html, /<div class="harness-message"/);
  assert.match(html, /class="create-dialog-scroll"/);
  assert.match(html, /class="create-dialog-actions"><button id="createStepButton"/);
  assert.match(styles, /\.create-dialog-actions\s*\{[^}]*flex: 0 0 auto/);
  assert.match(styles, /\.plan-node-list\s*\{[^}]*max-height[^}]*overflow: auto/);
  assert.match(styles, /\.create-dialog-card\s*\{[^}]*height: min\(760px/);
});
