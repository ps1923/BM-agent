import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import vm from 'node:vm';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');
const styles = await readFile(new URL('../styles.css', import.meta.url), 'utf8');

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

test('experiment start opens DSH and never creates a legacy BM-HS workspace', () => {
  const start = app.indexOf("startNodeButton.addEventListener('click'");
  const end = app.indexOf("saveFileButton.addEventListener('click'", start);
  const handler = app.slice(start, end);
  assert.match(html, /在 DSH 中攻克节点/);
  assert.match(html, /id="startNodeStatus" class="start-node-status" role="status" aria-live="polite"/);
  assert.match(handler, /openStudentDsh\(startNodeButton,[\s\S]*?experiment: \{ id: activeExperimentId, name: experimentName \}/);
  assert.doesNotMatch(handler, /startWorkspaceRun|\/experiments\/\$\{activeExperimentId\}\/runs|navigateTo\(\{ type: 'editor'/);
  assert.doesNotMatch(app, /async function startWorkspaceRun\(/);
  assert.match(styles, /\.start-node-status\.is-error\s*\{[^}]*color:\s*#b91c1c/);
});

test('DSH launch tells the student how to attach this experiment and retains explicit plugin confirmation', async () => {
  const start = app.indexOf('async function openStudentDsh(');
  const end = app.indexOf('\nasync function renderAdminDashboard', start);
  const context = {
    studentDshState: { textContent: '' },
    studentMessage: {},
    currentUser: { id: 5, role: 'student' },
    getCurrentUser() { return context.currentUser; },
    startNodeButton: null,
    calls: [],
    window: {
      open() { return context.popup; },
      setTimeout,
      location: { assign(url) { context.navigatedUrl = url; } },
    },
    popup: { opener: null, closed: false, location: { replace(url) { context.openedUrl = url; } }, close() { this.closed = true; } },
    apiRequest: async (path, options) => {
      context.calls.push([path, options.method]);
      return { status: 'running', launchUrl: 'http://47.109.176.135/dsh/session' };
    },
    describeDshStatus: (status) => `DSH 状态：${status}`,
    validateDshLaunchUrl: (url) => url,
    loadStudentDshStatus() {},
    setMessage() {},
    Date,
    Promise,
  };
  context.startNodeButton = {
    disabled: false,
    ariaLabel: '',
    label: { textContent: '在 DSH 中攻克节点' },
    querySelector() { return this.label; },
    setAttribute(_name, value) { this.ariaLabel = value; },
  };
  const inlineStatus = { textContent: '', hidden: true, classList: { toggle() {} } };
  vm.runInNewContext(`${app.slice(start, end)}\nglobalThis.launch = openStudentDsh;`, context);

  assert.equal(await context.launch(context.startNodeButton, {
    experiment: { id: '42', name: 'Django 博客' },
    statusElement: inlineStatus,
  }), true);
  assert.deepEqual(context.calls, [['/student/dsh/launch', 'POST']]);
  assert.equal(context.openedUrl, 'http://47.109.176.135/dsh/session');
  assert.match(inlineStatus.textContent, /请连接实验“Django 博客”（编号 42）/);
  assert.match(inlineStatus.textContent, /插件会在绑定前请求你确认/);
  assert.equal(context.startNodeButton.disabled, false);
  assert.equal(context.startNodeButton.label.textContent, '在 DSH 中攻克节点');

  context.apiRequest = async () => { throw new Error('DSH 服务暂时不可用'); };
  assert.equal(await context.launch(context.startNodeButton, {
    experiment: { id: '42', name: 'Django 博客' },
    statusElement: inlineStatus,
  }), false);
  assert.equal(inlineStatus.textContent, 'DSH 服务暂时不可用');
  assert.equal(inlineStatus.hidden, false);
  assert.equal(context.startNodeButton.disabled, false);
});

test('student navigation accepts platform-host HTTP or HTTPS DSH URLs without credentials', () => {
  const start = app.indexOf('function validateDshLaunchUrl(');
  const end = app.indexOf('\nasync function openStudentDsh', start);
  assert.ok(start >= 0 && end > start, 'DSH URL validator remains isolated and testable');
  const context = {
    URL,
    Set,
    Error,
    API_BASE_URL: 'http://47.109.176.135/bm-agent/api',
    window: { location: { origin: 'http://47.109.176.135', href: 'http://47.109.176.135/bm-agent/' } },
  };
  vm.runInNewContext(`${app.slice(start, end)}\nglobalThis.validate = validateDshLaunchUrl;`, context);
  const route = 'bm-agent/dsh/12345678-1234-4234-8234-123456789abc/';
  assert.equal(context.validate(`http://47.109.176.135/${route}`), `http://47.109.176.135/${route}`);
  assert.throws(() => context.validate('http://user:pass@47.109.176.135/dsh'), /凭据/);
  assert.throws(() => context.validate(`http://attacker.example/${route}`), /专属工作区路由/);
  assert.throws(() => context.validate(`http://47.109.176.135:8080/${route}`), /专属工作区路由/);
  assert.throws(() => context.validate('https://47.109.176.135/bm-agent/'), /专属工作区路由/);
  assert.throws(() => context.validate('javascript:alert(1)'), /HTTP 或 HTTPS/);
  assert.match(app, /blankTab\.opener = null/);
  assert.doesNotMatch(app, /DSH 访问地址必须使用 HTTPS/);
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
