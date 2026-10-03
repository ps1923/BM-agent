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
const verifyWorkspaceFileResponse = runInNewContext(
  `(${app.slice(validatorStart, validatorEnd).trim()})`,
  { window: { crypto: webcrypto, TextEncoder }, Uint8Array, Array, Error },
);

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
  assert.match(terminalUi, /terminalCommandInFlight = true/);
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

test('teacher dashboards and task updates discard stale request results', () => {
  assert.match(app, /requestEpoch !== teacherCourseDashboardEpoch \|\| teacherDashboardCourse\.value !== String\(courseId\)/);
  assert.match(app, /requestEpoch !== teacherDashboardEpoch \|\| getCurrentUser\(\)\?\.id !== user\.id/);
  assert.match(app, /requestEpoch !== taskProgressRequestEpoch/);
  assert.match(app, /taskProgressMutationInFlight = false;\s*if \(epoch === routeResolutionEpoch/);
  assert.match(app, /finally \{[\s\S]*?renderSelectedNodeTasks\(\);\s*updateCompleteNodeControl\(\);/);
  assert.match(app, /clearTeacherBugReview\(\);\s*try \{\s*window\.localStorage\.setItem\(STORAGE_KEYS\.teacherCourse/);
  assert.match(app, /function isRunContextCurrent\(runId, epoch\)/);
  assert.match(app, /if \(!isRunContextCurrent\(runId, epoch\)\) return;/);
  assert.match(app, /const allTasksComplete = Boolean\(definition\) && tasks\.every/);
  assert.match(app, /本节点没有细分任务。进度正常时，可直接手动完成节点。/);
  assert.match(app, /本节点没有细分任务。进度正常时，可直接手动完成节点。/);
});
