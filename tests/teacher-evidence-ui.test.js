import assert from 'node:assert/strict';
import { webcrypto } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { runInNewContext } from 'node:vm';
import { TextEncoder } from 'node:util';
import test from 'node:test';

const [app, html] = await Promise.all([
  readFile(new URL('../app.js', import.meta.url), 'utf8'),
  readFile(new URL('../index.html', import.meta.url), 'utf8'),
]);
const evidenceUi = app.slice(app.indexOf('async function openTeacherSnapshot'), app.indexOf('async function loadTeacherCourseDashboard'));
const validatorStart = app.indexOf('async function verifyWorkspaceFileResponse');
const validatorEnd = app.indexOf('\nfunction setLibraryState', validatorStart);
const hashFallbackStart = app.indexOf('function sha256HexFallback(');
const sha256HexFallback = runInNewContext(
  `(${app.slice(hashFallbackStart, validatorStart).trim()})`,
  { Uint8Array, DataView, Array, Math },
);
const studentDashboardStart = app.indexOf('async function renderStudentDashboard()');
const studentDashboardEnd = app.indexOf('\nasync function renderAdminDashboard', studentDashboardStart);
const clearStudentDashboardStart = app.indexOf('function clearStudentDashboard()');
const clearStudentDashboardEnd = app.indexOf('\nfunction formatTeacherDate', clearStudentDashboardStart);
const renderSimpleRouteStart = app.indexOf('function renderSimpleRoute(route)');
const renderSimpleRouteEnd = app.indexOf('\nfunction redirectToRoleHome', renderSimpleRouteStart);
const teacherRunDetailStart = app.indexOf('function isCurrentTeacherRunDetail');
const resetTeacherRunDetailStart = app.indexOf('function resetTeacherRunDetail()');
const openTeacherSnapshotStart = app.indexOf('\nasync function openTeacherSnapshot', resetTeacherRunDetailStart);
const teacherRunSummaryStart = app.indexOf('async function loadTeacherRunSummary', teacherRunDetailStart);
const teacherRunTimelineStart = app.indexOf('async function loadTeacherRunTimeline', teacherRunSummaryStart);
const teacherRunDetailLoaderStart = app.indexOf('async function loadTeacherRunDetail', teacherRunTimelineStart);
const teacherRunDashboardStart = app.indexOf('\nasync function loadTeacherCourseDashboard', teacherRunDetailLoaderStart);
const verifyWorkspaceFileResponse = runInNewContext(
  `(${app.slice(validatorStart, validatorEnd).trim()})`,
  { window: { crypto: webcrypto, TextEncoder }, sha256HexFallback, Uint8Array, Array, Error },
);

function createStudentDashboardHarness() {
  const state = { user: { id: 'student-a', role: 'student', name: 'Student A', email: 'a@example.test' }, pending: [] };
  const makeElement = () => ({
    textContent: '',
    children: [],
    append(...children) { this.children.push(...children); },
    replaceChildren(...children) { this.children = [...children]; },
  });
  const studentCourses = makeElement();
  const studentIdentity = makeElement();
  const studentCourseCount = makeElement();
  const context = {
    studentDashboardEpoch: 0,
    getCurrentUser: () => state.user,
    apiRequest: () => new Promise((resolve, reject) => state.pending.push({ resolve, reject })),
    studentIdentity,
    studentCourseCount,
    studentCourses,
    studentLocalCodingState: makeElement(),
    loginView: { classList: { remove() {}, add() {} } },
    studentView: { classList: { remove() {}, add() {} } },
    teacherView: { classList: { remove() {}, add() {} } },
    adminView: { classList: { remove() {}, add() {} } },
    hideProtectedViews() {},
    renderTeacherDashboard() {},
    document: { createElement: makeElement },
    Array,
    Error,
  };
  const clear = runInNewContext(`(${app.slice(clearStudentDashboardStart, clearStudentDashboardEnd).trim()})`, context);
  context.clearStudentDashboard = clear;
  const render = runInNewContext(`(${app.slice(studentDashboardStart, studentDashboardEnd).trim()})`, context);
  const renderSimpleRoute = runInNewContext(
    `(${app.slice(renderSimpleRouteStart, renderSimpleRouteEnd).trim()})`, context,
  );
  return { state, studentCourses, studentIdentity, studentCourseCount, render, clear, renderSimpleRoute };
}

function createTeacherRunDetailHarness() {
  const pending = [];
  const requests = [];
  const elements = new Map();

  class TestElement {
    constructor(tagName = 'div') {
      this.tagName = tagName.toUpperCase();
      this.children = [];
      this.listeners = new Map();
      this._textContent = '';
      this.disabled = false;
      this.hidden = false;
      this.value = '';
      this.type = '';
      this.className = '';
      this.classes = new Set();
      this.classList = {
        add: (name) => this.classes.add(name),
        remove: (name) => this.classes.delete(name),
        contains: (name) => this.classes.has(name),
      };
    }

    get textContent() {
      return this.children.length
        ? this.children.map((child) => child.textContent || '').join('')
        : this._textContent;
    }

    set textContent(value) {
      this.children = [];
      this._textContent = String(value ?? '');
    }

    append(...children) {
      this.children.push(...children);
    }

    replaceChildren(...children) {
      this.children = [...children];
      this._textContent = '';
    }

    addEventListener(type, listener) {
      const listeners = this.listeners.get(type) || [];
      listeners.push(listener);
      this.listeners.set(type, listeners);
    }

    click() {
      if (this.disabled) return;
      for (const listener of this.listeners.get('click') || []) listener({ currentTarget: this });
    }

    pressKey(key) {
      // Native buttons activate their click action with Enter or Space.
      if (this.tagName === 'BUTTON' && this.type === 'button' && ['Enter', ' '].includes(key)) this.click();
    }
  }

  const ids = [
    'teacherRunDetail', 'teacherRunTitle', 'teacherRunStatus', 'teacherRunSummaryState',
    'teacherRunSummary', 'teacherAiSummary', 'teacherRunMetrics', 'teacherTimeline',
    'teacherTimelineState', 'teacherStageEvidence', 'teacherEvidenceFiles', 'teacherEvidenceCode',
    'teacherEvidenceMessage', 'teacherRating', 'teacherFeedback', 'teacherReviewMessage',
  ];
  for (const id of ids) elements.set(id, new TestElement());
  const submitButton = new TestElement('button');
  submitButton.type = 'submit';
  const reviewForm = new TestElement('form');
  reviewForm.querySelector = () => submitButton;
  elements.set('teacherReviewForm', reviewForm);

  const context = {
    teacherEvidenceRequestEpoch: 0,
    teacherEvidenceSelectionEpoch: 0,
    teacherRunDetailRequestEpoch: 0,
    teacherRunSummaryRequestEpoch: 0,
    teacherRunTimelineRequestEpoch: 0,
    selectedTeacherRunId: null,
    teacherDashboardMessage: new TestElement(),
    document: {
      createElement: (tagName) => new TestElement(tagName),
      querySelector: (selector) => elements.get(selector.slice(1)),
    },
    apiRequest(path) {
      requests.push(path);
      return new Promise((resolve, reject) => pending.push({ path, resolve, reject }));
    },
    setMessage(element, message) { element.textContent = message; },
    loadTeacherStageEvidence() {},
    formatTeacherDate: (value) => value || '时间未知',
    Number,
    String,
    Array,
    Error,
  };
  for (const [id, element] of elements) context[id] = element;
  const runDetailFunctions = [
    app.slice(resetTeacherRunDetailStart, openTeacherSnapshotStart),
    app.slice(teacherRunDetailStart, teacherRunSummaryStart),
    app.slice(teacherRunSummaryStart, teacherRunTimelineStart),
    app.slice(teacherRunTimelineStart, teacherRunDetailLoaderStart),
    app.slice(teacherRunDetailLoaderStart, teacherRunDashboardStart),
  ].join('\n');
  const factory = runInNewContext(`(() => {
    ${runDetailFunctions}
    return {
      loadTeacherRunDetail,
      get selectedRunId() { return selectedTeacherRunId; },
      get requestEpoch() { return teacherRunDetailRequestEpoch; },
    };
  })()`, context);

  const get = (id) => elements.get(id);
  const takeRequest = (suffix, occurrence = 0) => {
    const matches = pending.filter((request) => request.path.endsWith(suffix));
    assert.ok(matches[occurrence], `expected pending request ending with ${suffix}`);
    pending.splice(pending.indexOf(matches[occurrence]), 1);
    return matches[occurrence];
  };
  const settle = async () => {
    await Promise.resolve();
    await Promise.resolve();
  };
  const openRun = async (runId, name = `Student ${runId}`) => {
    await factory.loadTeacherRunDetail(String(runId), name);
    return {
      summary: takeRequest(`/runs/${runId}/summary`),
      timeline: takeRequest(`/runs/${runId}/timeline`),
    };
  };
  const resolveSummary = (request, runId, overrides = {}) => request.resolve({
    runId,
    runStatus: 'in_progress',
    experimentName: `Experiment ${runId}`,
    completedNodes: 2,
    totalNodes: 4,
    completedTasks: 3,
    totalTasks: 6,
    commandCount: 5,
    messageCount: 7,
    bugCount: 1,
    aiSummaryAvailable: false,
    aiSummary: null,
    review: { rating: 88, feedback: `Review ${runId}` },
    ...overrides,
  });
  const resolveTimeline = (request, runId, title = `Timeline ${runId}`) => request.resolve([{
    source: 'student',
    title,
    detail: `Details ${runId}`,
    createdAt: '2026-10-03T10:00:00Z',
  }]);
  const text = (id) => get(id).textContent;
  const retryButton = (id) => get(id).children.find((child) => child.tagName === 'BUTTON');
  return {
    context, elements, pending, requests, get, takeRequest, settle, openRun,
    resolveSummary, resolveTimeline, text, retryButton, factory,
  };
}

async function testSha256(content) {
  const digest = await webcrypto.subtle.digest('SHA-256', new TextEncoder().encode(content));
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, '0')).join('');
}

test('teacher evidence uses the authorized read-only endpoints and escapes code as text', () => {
  assert.match(evidenceUi, /`\/teacher\/runs\/\$\{runId\}\/stages`/);
  assert.match(evidenceUi, /`\/teacher\/runs\/\$\{runId\}\/snapshots\/\$\{snapshotId\}\/files`/);
  assert.match(evidenceUi, /teacherEvidenceCode\.textContent = result\.content/);
  assert.doesNotMatch(evidenceUi, /teacherEvidenceCode\.innerHTML/);
});

test('teacher evidence distinguishes missing history and drops stale cross-run responses', () => {
  assert.match(evidenceUi, /该历史节点没有留存代码快照/);
  assert.match(evidenceUi, /requestEpoch !== teacherEvidenceRequestEpoch/);
  assert.match(evidenceUi, /selectionEpoch !== teacherEvidenceSelectionEpoch/);
  assert.match(app, /teacherRunDetailRequestEpoch/);
  assert.match(evidenceUi, /selectedTeacherRunId !== Number\(runId\)/);
  assert.match(html, /id="teacherEvidenceMessage"[^>]*aria-live="polite"/);
  assert.match(html, /id="teacherEvidenceCode"[^>]*tabindex="0"/);
});

test('workspace editor renders actual files and uses content hashes for optimistic saves', () => {
  assert.match(html, /id="codeFileTabs"[^>]*role="tablist"/);
  const tabsStart = html.indexOf('<div class="code-tabs">');
  const tabsEnd = html.indexOf('</div>', tabsStart);
  assert.doesNotMatch(html.slice(tabsStart, tabsEnd), /DemoApplication\.java|pom\.xml/);
  assert.match(app, /function renderWorkspaceFileTabs\(\)/);
  assert.match(app, /function renderCodeLineNumbers\(\)/);
  assert.match(app, /function selectWorkspaceFile\(path\)/);
  assert.match(app, /async function verifyWorkspaceFileResponse\(response, workspaceId, expectedFile\)/);
  assert.match(app, /metadata\.path !== expectedFile\.path/);
  assert.match(app, /crypto\.subtle\.digest\('SHA-256'/);
  assert.match(app, /文件内容校验失败，已阻止显示和编辑/);
  assert.match(app, /expectedHash \? \{ expectedHash \}/);
  assert.match(app, /当前文件有未保存修改。请先保存，再切换文件/);
  assert.match(app, /selectionEpoch !== workspaceFileSelectionEpoch/);
});

test('terminal execution protects unsaved code and refreshes verified workspace files afterward', () => {
  const terminalStart = app.indexOf("terminalInput.addEventListener('keydown'");
  const terminalEnd = app.indexOf("document.querySelectorAll('.task-item')", terminalStart);
  const terminalUi = app.slice(terminalStart, terminalEnd);
  assert.match(terminalUi, /codeEditor\.value !== activeFileSavedContent/);
  assert.match(terminalUi, /terminalCommandsInFlight\.add\(commandLockKey\)/);
  assert.match(terminalUi, /await refreshWorkspaceFilesAfterTerminal\(runId, workspaceId, epoch\)/);
  assert.match(app, /async function refreshWorkspaceFilesAfterTerminal\(runId, workspaceId, epoch\)/);
  assert.match(app, /verifyWorkspaceFileResponse\(response, workspaceId, selected\)/);
});

test('terminal resync loads the selected disk version and ignores a stale run response', async () => {
  const syncStart = app.indexOf('async function refreshWorkspaceFilesAfterTerminal');
  const syncEnd = app.indexOf('\nasync function restoreExistingRun', syncStart);
  const syncSource = app.slice(syncStart, syncEnd).trim();
  const content = 'class Main { }\n';
  const contentHash = await testSha256(content);
  const fileMetadata = { workspaceId: 9, path: 'src/Main.java', contentHash };
  let currentRunId = '42';
  const state = {
    activeRun: { runId: '42', workspaceId: 9 },
    activeFilePath: 'src/Main.java',
    activeWorkspaceFiles: [],
    activeFileHash: null,
    activeFileSavedContent: '',
    codeEditor: { value: 'stale editor content', disabled: true },
    saveFileButton: { disabled: true },
    messages: [],
    renderCount: 0,
  };
  const apiRequest = async (path) => path.endsWith('/files')
    ? [fileMetadata]
    : { file: fileMetadata, content };
  const bindings = {
    apiRequest,
    isRunContextCurrent: (runId) => String(runId) === currentRunId,
    verifyWorkspaceFileResponse,
    workspaceFileUrl: encodeURIComponent,
    encodeURIComponent,
    activeRun: state.activeRun,
    activeFilePath: state.activeFilePath,
    activeWorkspaceFiles: state.activeWorkspaceFiles,
    activeFileHash: state.activeFileHash,
    activeFileSavedContent: state.activeFileSavedContent,
    codeEditor: state.codeEditor,
    saveFileButton: state.saveFileButton,
    appendChatMessage: (...message) => state.messages.push(message),
    renderCodeLineNumbers: () => { state.renderCount += 1; },
    renderWorkspaceFileTabs: () => { state.renderCount += 1; },
  };
  const sync = runInNewContext(`(${syncSource})`, bindings);

  await sync('42', '9', 1);

  assert.equal(bindings.codeEditor.value, content);
  assert.equal(bindings.activeFileSavedContent, content);
  assert.equal(bindings.activeFileHash, contentHash);
  assert.equal(bindings.saveFileButton.disabled, false);
  assert.equal(bindings.activeWorkspaceFiles[0].path, fileMetadata.path);
  assert.equal(state.renderCount, 2);

  let resolveList;
  bindings.apiRequest = () => new Promise((resolve) => { resolveList = resolve; });
  const staleRefresh = sync('42', '9', 2);
  currentRunId = '43';
  bindings.activeRun = { runId: '43', workspaceId: 9 };
  resolveList([{ workspaceId: 9, path: 'src/Other.java', contentHash }]);
  await staleRefresh;

  assert.equal(bindings.activeFilePath, 'src/Main.java');
  assert.equal(bindings.activeWorkspaceFiles[0].path, fileMetadata.path);
});

test('workspace file validator accepts the selected file and rejects mismatched identity or bytes', async () => {
  const content = 'class Main {}\n';
  const contentHash = await testSha256(content);
  const metadata = { workspaceId: 9, path: 'src/Main.java', contentHash };
  const response = { file: metadata, content };

  const validated = await verifyWorkspaceFileResponse(response, 9, { ...metadata });
  assert.equal(validated.content, content);
  assert.equal(validated.file.path, metadata.path);
  assert.equal(validated.file.workspaceId, metadata.workspaceId);
  assert.equal(validated.file.contentHash, contentHash);
  await assert.rejects(
    verifyWorkspaceFileResponse(response, 9, { ...metadata, path: 'src/Other.java' }),
    /代码文件与当前选择不一致/,
  );
  await assert.rejects(
    verifyWorkspaceFileResponse(response, 10, { ...metadata }),
    /代码文件与当前选择不一致/,
  );
  await assert.rejects(
    verifyWorkspaceFileResponse({ file: metadata, content: 'print("wrong language")' }, 9, { ...metadata }),
    /文件内容校验失败/,
  );
});

test('HTTP-compatible SHA-256 fallback preserves file integrity verification without Web Crypto', async () => {
  assert.equal(
    sha256HexFallback(new TextEncoder().encode('abc')),
    'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad',
  );
  const content = '中文文件内容\n';
  const contentHash = await testSha256(content);
  const verifyWithoutWebCrypto = runInNewContext(
    `(${app.slice(validatorStart, validatorEnd).trim()})`,
    { window: { crypto: null, TextEncoder }, sha256HexFallback, Uint8Array, Array, Error },
  );
  const verified = await verifyWithoutWebCrypto(
    { file: { workspaceId: 9, path: 'README.md', contentHash }, content },
    9,
    { workspaceId: 9, path: 'README.md', contentHash },
  );
  assert.equal(verified.content, content);
  await assert.rejects(
    verifyWithoutWebCrypto(
      { file: { workspaceId: 9, path: 'README.md', contentHash }, content: '篡改内容' },
      9,
      { workspaceId: 9, path: 'README.md', contentHash },
    ),
    /文件内容校验失败/,
  );
});

test('teacher dashboards and task updates discard stale request results', () => {
  assert.match(app, /if \(route !== 'student' \|\| user\?\.role !== 'student'\) clearStudentDashboard\(\);/);
  assert.match(app, /requestEpoch !== teacherCourseDashboardEpoch \|\| teacherDashboardCourse\.value !== String\(courseId\)/);
  assert.match(app, /requestEpoch !== teacherDashboardEpoch \|\| getCurrentUser\(\)\?\.id !== user\.id/);
  assert.match(app, /requestEpoch !== taskProgressRequestEpoch/);
  assert.match(app, /taskProgressMutationInFlight = false;\s*if \(epoch === routeResolutionEpoch/);
  assert.match(app, /finally \{[\s\S]*?renderSelectedNodeTasks\(\);\s*updateCompleteNodeControl\(\);/);
  assert.match(app, /clearTeacherBugReview\(\);\s*try \{\s*window\.localStorage\.setItem\(STORAGE_KEYS\.teacherCourse/);
  assert.match(app, /function isRunContextCurrent\(runId, epoch\)/);
  assert.match(app, /if \(!isRunContextCurrent\(runId, epoch\)\) return;/);
  assert.match(app, /canUpdate = Boolean\(activeRun && nodeProgress && nodeProgress\.status !== 'completed'\)/);
  assert.match(app, /completeNodeButton\.disabled = !activeRun \|\| !current\s*\|\| current\.status === 'completed'/);
  assert.doesNotMatch(app, /allTasksComplete/);
  assert.match(app, /本节点没有细分任务。节点顺序仅供参考，不会限制学习。/);
});

test('student dashboard discards a pending response after switching to a teacher account', async () => {
  const { state, studentCourses, studentIdentity, studentCourseCount, render, renderSimpleRoute } = createStudentDashboardHarness();
  const initialRequest = render();
  state.pending[0].resolve([{ name: 'PREVIOUS-COURSE', inviteCode: 'old-code', experiments: [] }]);
  await initialRequest;
  assert.equal(studentCourses.children.length, 1);

  const oldRequest = render();
  state.user = { id: 'teacher-b', role: 'teacher', name: 'Teacher B', email: 'b@example.test' };
  renderSimpleRoute('teacher');
  state.pending[1].resolve([{ name: 'PRIVATE-OLD-COURSE', inviteCode: 'old-code', experiments: [] }]);
  await oldRequest;

  assert.equal(studentCourses.children.length, 0);
  assert.equal(studentIdentity.textContent, '');
  assert.equal(studentCourseCount.textContent, '');
});

test('student dashboard keeps only the new student courses when responses resolve out of order', async () => {
  const { state, studentCourses, studentIdentity, studentCourseCount, render } = createStudentDashboardHarness();
  const earlierRequest = render();
  state.user = { id: 'student-b', role: 'student', name: 'Student B', email: 'b@example.test' };
  const currentRequest = render();
  state.pending[1].resolve([{ name: 'CURRENT-ACCOUNT-COURSE', inviteCode: 'new-code', experiments: [] }]);
  await currentRequest;
  state.pending[0].resolve([{ name: 'STALE-ACCOUNT-COURSE', inviteCode: 'old-code', experiments: [] }]);
  await earlierRequest;

  const flattenText = (element) => [element.textContent, ...element.children.map(flattenText)].join(' ');
  const renderedText = studentCourses.children.map(flattenText).join(' ');
  assert.match(renderedText, /CURRENT-ACCOUNT-COURSE/);
  assert.doesNotMatch(renderedText, /STALE-ACCOUNT-COURSE/);
  assert.equal(studentIdentity.textContent, 'Student B · b@example.test');
  assert.equal(studentCourseCount.textContent, '1 门课程');
});

test('teacher route clears retained student data and invalidates pending dashboard responses', async () => {
  const { state, studentCourses, studentIdentity, studentCourseCount, render, renderSimpleRoute } = createStudentDashboardHarness();
  studentCourses.append({ textContent: 'PRIVATE-COURSE', children: [] });
  studentIdentity.textContent = 'Student A · a@example.test';
  studentCourseCount.textContent = '1 门课程';
  const pendingRequest = render();
  state.user = { id: 'teacher-b', role: 'teacher', name: 'Teacher B', email: 'b@example.test' };

  renderSimpleRoute('teacher');
  state.pending[0].resolve([{ name: 'STALE-PRIVATE-COURSE', inviteCode: 'old-code', experiments: [] }]);
  await pendingRequest;

  assert.equal(studentCourses.children.length, 0);
  assert.equal(studentIdentity.textContent, '');
  assert.equal(studentCourseCount.textContent, '');
  assert.doesNotMatch(JSON.stringify(studentCourses.children), /STALE-PRIVATE-COURSE/);
});

test('logout route ignores a late dashboard failure and preserves the logged-out empty state', async () => {
  const { state, studentCourses, studentIdentity, studentCourseCount, render, renderSimpleRoute } = createStudentDashboardHarness();
  const pendingRequest = render();
  state.user = null;

  renderSimpleRoute('login');
  state.pending[0].reject(new Error('stale private request failure'));
  await pendingRequest;

  assert.equal(studentCourses.children.length, 0);
  assert.equal(studentIdentity.textContent, '');
  assert.equal(studentCourseCount.textContent, '');
});

test('current student dashboard request failure renders a recoverable error state', async () => {
  const { state, studentCourses, studentCourseCount, render } = createStudentDashboardHarness();
  const request = render();
  state.pending[0].reject(new Error('service unavailable'));
  await request;

  assert.equal(studentCourseCount.textContent, '加载失败');
  assert.equal(studentCourses.children.length, 1);
  assert.equal(studentCourses.children[0].textContent, 'service unavailable');
});

test('teacher run keeps a successful summary and saved review when timeline fails, then retries only timeline', async () => {
  assert.match(html, /id="teacherRunSummaryState"[^>]*role="status"[^>]*aria-live="polite"/);
  assert.match(html, /id="teacherTimelineState"[^>]*role="status"[^>]*aria-live="polite"/);
  const harness = createTeacherRunDetailHarness();
  const initial = await harness.openRun(41);
  assert.equal(harness.text('teacherRunSummaryState'), '正在加载学习概况…');
  assert.equal(harness.text('teacherTimelineState'), '正在加载过程记录…');
  assert.match(harness.text('teacherReviewMessage'), /正在读取已保存评价/);
  harness.resolveSummary(initial.summary, 41);
  initial.timeline.reject(new Error('timeline temporarily unavailable'));
  await harness.settle();

  assert.match(harness.text('teacherRunSummary'), /Experiment 41/);
  assert.match(harness.text('teacherRunMetrics'), /2已完成节点/);
  assert.equal(harness.get('teacherRating').value, 88);
  assert.equal(harness.get('teacherFeedback').value, 'Review 41');
  assert.equal(harness.text('teacherReviewMessage'), '');
  assert.match(harness.text('teacherTimelineState'), /timeline temporarily unavailable/);
  assert.equal(harness.text('teacherTimeline'), '');

  const retry = harness.retryButton('teacherTimelineState');
  assert.ok(retry);
  retry.click();
  const timelineRetry = harness.takeRequest('/runs/41/timeline');
  assert.equal(harness.pending.length, 0);
  assert.ok(harness.requests.every((path) => path.endsWith('/summary') || path.endsWith('/timeline')));
  harness.resolveTimeline(timelineRetry, 41, 'Recovered timeline 41');
  await harness.settle();

  assert.match(harness.text('teacherTimeline'), /Recovered timeline 41/);
  assert.match(harness.text('teacherRunSummary'), /Experiment 41/);
  assert.equal(harness.get('teacherRating').value, 88);
  assert.equal(harness.get('teacherFeedback').value, 'Review 41');
  assert.equal(harness.text('teacherTimelineState'), '');
});

test('teacher summary rejects malformed counts rather than presenting fabricated zero metrics', async () => {
  const harness = createTeacherRunDetailHarness();
  const initial = await harness.openRun(47);
  initial.summary.resolve({
    runId: 47,
    runStatus: 'in_progress',
    experimentName: 'Malformed experiment',
    completedNodes: null,
    totalNodes: 4,
    completedTasks: 0,
    totalTasks: 0,
    commandCount: 0,
    messageCount: 0,
    bugCount: 0,
  });
  initial.timeline.resolve([]);
  await harness.settle();

  assert.match(harness.text('teacherRunSummaryState'), /数据格式无效/);
  assert.equal(harness.get('teacherRunMetrics').children.length, 0);
  assert.equal(harness.get('teacherReviewForm').querySelector('button[type="submit"]').disabled, true);
  assert.equal(harness.get('teacherRating').disabled, true);
  assert.match(harness.text('teacherReviewMessage'), /为避免覆盖，评价暂不可编辑/);
});

test('teacher summary rejects impossible totals and malformed review or descriptive fields', async () => {
  const invalidCases = [
    { completedNodes: 5, totalNodes: 4 },
    { completedTasks: 7, totalTasks: 6 },
    { review: { rating: 101, feedback: 'invalid rating' } },
    { review: { rating: 88, feedback: { unexpected: 'object' } } },
    { experimentName: { unexpected: 'object' } },
    { runStatus: '' },
  ];
  for (const [index, overrides] of invalidCases.entries()) {
    const runId = 48 + index;
    const harness = createTeacherRunDetailHarness();
    const initial = await harness.openRun(runId);
    harness.resolveSummary(initial.summary, runId, overrides);
    harness.resolveTimeline(initial.timeline, runId);
    await harness.settle();

    assert.match(harness.text('teacherRunSummaryState'), /数据格式无效/, `case ${index} must be rejected`);
    assert.equal(harness.get('teacherRunMetrics').children.length, 0, `case ${index} must not show metrics`);
    assert.equal(harness.get('teacherReviewForm').querySelector('button[type="submit"]').disabled, true);
  }
});

test('teacher run keeps a successful timeline when summary fails and enables review only after valid summary retry', async () => {
  const harness = createTeacherRunDetailHarness();
  const initial = await harness.openRun(42);
  harness.resolveTimeline(initial.timeline, 42);
  initial.summary.reject(new Error('summary temporarily unavailable'));
  await harness.settle();

  assert.match(harness.text('teacherTimeline'), /Timeline 42/);
  assert.match(harness.text('teacherTimeline'), /Details 42/);
  assert.match(harness.text('teacherRunSummaryState'), /summary temporarily unavailable/);
  assert.match(harness.text('teacherReviewMessage'), /为避免覆盖，评价暂不可编辑/);
  assert.equal(harness.text('teacherRunSummary'), '');
  assert.equal(harness.get('teacherRunMetrics').children.length, 0);
  assert.equal(harness.get('teacherRating').disabled, true);
  assert.equal(harness.get('teacherFeedback').disabled, true);
  assert.equal(harness.get('teacherReviewForm').querySelector('button[type="submit"]').disabled, true);

  harness.retryButton('teacherRunSummaryState').click();
  const summaryRetry = harness.takeRequest('/runs/42/summary');
  assert.equal(harness.pending.length, 0);
  harness.resolveSummary(summaryRetry, 42);
  await harness.settle();

  assert.match(harness.text('teacherRunSummary'), /Experiment 42/);
  assert.match(harness.text('teacherTimeline'), /Timeline 42/);
  assert.equal(harness.get('teacherRating').disabled, false);
  assert.equal(harness.get('teacherFeedback').disabled, false);
  assert.equal(harness.get('teacherReviewForm').querySelector('button[type="submit"]').disabled, false);
  assert.equal(harness.get('teacherRating').value, 88);
  assert.equal(harness.text('teacherReviewMessage'), '');
});

test('teacher run reports both panel failures independently and each retry preserves its sibling', async () => {
  const harness = createTeacherRunDetailHarness();
  const initial = await harness.openRun(43);
  initial.summary.reject(new Error('summary offline'));
  initial.timeline.reject(new Error('timeline offline'));
  await harness.settle();

  assert.match(harness.text('teacherRunSummaryState'), /summary offline/);
  assert.match(harness.text('teacherTimelineState'), /timeline offline/);
  assert.equal(harness.get('teacherReviewForm').querySelector('button[type="submit"]').disabled, true);
  const summaryRetry = harness.retryButton('teacherRunSummaryState');
  const timelineRetry = harness.retryButton('teacherTimelineState');
  assert.ok(summaryRetry);
  assert.ok(timelineRetry);

  summaryRetry.click();
  const summaryRequest = harness.takeRequest('/runs/43/summary');
  assert.equal(harness.pending.length, 0);
  harness.resolveSummary(summaryRequest, 43);
  await harness.settle();
  assert.match(harness.text('teacherRunSummary'), /Experiment 43/);
  assert.match(harness.text('teacherTimelineState'), /timeline offline/);
  assert.equal(harness.get('teacherRating').disabled, false);

  timelineRetry.click();
  const timelineRequest = harness.takeRequest('/runs/43/timeline');
  assert.equal(harness.pending.length, 0);
  harness.resolveTimeline(timelineRequest, 43, 'Timeline recovered independently');
  await harness.settle();
  assert.match(harness.text('teacherTimeline'), /Timeline recovered independently/);
  assert.match(harness.text('teacherRunSummary'), /Experiment 43/);
  assert.equal(harness.text('teacherRunSummaryState'), '');
  assert.equal(harness.text('teacherTimelineState'), '');
});

test('teacher timeline rejects malformed entries and timestamps before rendering any partial event list', async () => {
  const malformedEntries = [
    null,
    { source: 'student', title: { unsafe: 'object' }, detail: 'bad title' },
    { source: 'student', title: 'Bad date', detail: 'invalid date', createdAt: '2026-99-99T10:00:00Z' },
    { source: 'student', title: 'Unparseable date', detail: 'invalid date', createdAt: 'not-a-date' },
  ];
  for (const [index, malformed] of malformedEntries.entries()) {
    const runId = 55 + index;
    const harness = createTeacherRunDetailHarness();
    const initial = await harness.openRun(runId);
    harness.resolveSummary(initial.summary, runId);
    initial.timeline.resolve([
      { source: 'student', title: 'Valid first event', detail: 'must not render partially', createdAt: '2026-10-03T10:00:00Z' },
      malformed,
    ]);
    await harness.settle();

    assert.equal(harness.get('teacherTimeline').children.length, 0);
    assert.match(harness.text('teacherTimelineState'), /数据格式无效/);
    assert.match(harness.text('teacherRunSummary'), new RegExp(`Experiment ${runId}`));
  }
});

test('teacher run discards stale successful and failed panel responses after switching runs', async () => {
  const harness = createTeacherRunDetailHarness();
  const runA = await harness.openRun(44);
  const runB = await harness.openRun(45);

  harness.resolveSummary(runA.summary, 44, { experimentName: 'STALE SUCCESS SUMMARY' });
  runA.timeline.reject(new Error('STALE FAILURE TIMELINE'));
  harness.resolveSummary(runB.summary, 45);
  harness.resolveTimeline(runB.timeline, 45);
  await harness.settle();

  assert.equal(harness.factory.selectedRunId, 45);
  assert.match(harness.text('teacherRunTitle'), /Student 45/);
  assert.match(harness.text('teacherRunSummary'), /Experiment 45/);
  assert.doesNotMatch(harness.text('teacherRunSummary'), /STALE SUCCESS SUMMARY|44/);
  assert.match(harness.text('teacherTimeline'), /Timeline 45/);
  assert.doesNotMatch(harness.text('teacherTimeline'), /STALE FAILURE TIMELINE|44/);
  assert.equal(harness.text('teacherRunSummaryState'), '');
  assert.equal(harness.text('teacherTimelineState'), '');
});

test('teacher panel retry is a named native button and keyboard activation retries only its panel', async () => {
  const harness = createTeacherRunDetailHarness();
  const initial = await harness.openRun(46);
  harness.resolveSummary(initial.summary, 46);
  initial.timeline.reject(new Error('retry by keyboard'));
  await harness.settle();

  const retry = harness.retryButton('teacherTimelineState');
  assert.ok(retry);
  assert.equal(retry.tagName, 'BUTTON');
  assert.equal(retry.type, 'button');
  assert.equal(retry.textContent, '重试过程记录');
  retry.pressKey('Enter');
  const retriedTimeline = harness.takeRequest('/runs/46/timeline');
  assert.equal(harness.pending.length, 0, 'keyboard retry must not refetch the successful summary');
  harness.resolveTimeline(retriedTimeline, 46, 'Keyboard retry succeeded');
  await harness.settle();

  assert.match(harness.text('teacherTimeline'), /Keyboard retry succeeded/);
  assert.match(harness.text('teacherRunSummary'), /Experiment 46/);
});
