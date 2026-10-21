import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { runInNewContext } from 'node:vm';
import test from 'node:test';

const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');
const recoveryStart = app.indexOf('function isValidDatabaseId');
const recoveryEnd = app.indexOf('\nfunction clearCachedTerminalSessions', recoveryStart);
const sessionUtilitiesStart = app.indexOf('function terminalSessionStorageKey(');
const handlerStart = app.indexOf("terminalInput.addEventListener('keydown', async (event) => {");
const handlerArrow = app.indexOf('async (event) => {', handlerStart);
const handlerBodyStart = app.indexOf('{', handlerArrow) + 1;
const handlerEnd = app.indexOf('\n});\n\ndocument.querySelectorAll', handlerBodyStart);
for (const [name, index] of Object.entries({
  recoveryStart,
  recoveryEnd,
  sessionUtilitiesStart,
  handlerStart,
  handlerArrow,
  handlerEnd,
})) {
  assert.notEqual(index, -1, `production source boundary ${name} must be present`);
}

function createHarness({ replacement, defer = false } = {}) {
  const state = {
    runId: '42',
    workspaceId: '73',
    epoch: 8,
    activeRun: { runId: '42', workspaceId: '73' },
    activeSession: { id: '100', workspaceId: '73', status: 'open' },
    persisted: [],
    requests: [],
    removedKeys: [],
    cache: new Map(),
  };
  const apiRequest = (path, options) => {
    state.requests.push({ path, options });
    if (defer) return new Promise((resolve, reject) => { state.resolve = resolve; state.reject = reject; });
    return Promise.resolve(replacement);
  };
  const functions = runInNewContext(`(() => {
    ${app.slice(recoveryStart, recoveryEnd)}
    return {
      isValidDatabaseId,
      readTerminalSession,
      persistTerminalSession,
      terminalSessionRequestStorageKey,
      terminalSessionRequestId,
      createTerminalSessionRequest,
      acceptTerminalSessionRequest,
      terminalCommandLockKey,
      recoverClosedTerminalSession,
      isTerminalSessionClosedError,
      invalidateClosedTerminalSession,
      createTerminalReconnectButton,
      getActiveTerminalSession: () => activeTerminalSession,
      setActiveTerminalSession: (session) => { activeTerminalSession = session; },
    };
  })()`, {
    activeRun: state.activeRun,
    activeTerminalSession: state.activeSession,
    STORAGE_KEYS: {
      terminalSessionPrefix: 'bmhs.terminalSession.',
      terminalSessionRequestPrefix: 'bmhs.terminalSession.request.',
    },
    readSessionValue: (key) => state.cache.get(key) || null,
    writeSessionValue: (key, value) => { state.cache.set(key, value); state.persisted.push({ key, value }); },
    isRunContextCurrent: (runId, epoch) => String(runId) === state.runId && epoch === state.epoch,
    apiRequest,
    removeSessionValue: (key) => { state.cache.delete(key); state.removedKeys.push(key); },
    terminalSessionStorageKey: (runId) => `bmhs.terminalSession.${runId}`,
    getCurrentUser: () => ({ id: 'student-7' }),
    window: { crypto: { getRandomValues(bytes) { bytes.forEach((_, index) => { bytes[index] = index; }); return bytes; } } },
    Uint8Array,
    String,
  });
  return { ...functions, state };
}

test('terminal session creation persists one request identity and recovers after a lost response', async () => {
  const harness = createHarness({ defer: true });
  const storageKey = harness.terminalSessionRequestStorageKey(harness.state.activeRun);
  const firstAttempt = harness.createTerminalSessionRequest(harness.state.activeRun);
  const requestId = harness.state.cache.get(storageKey);

  assert.match(requestId, /^[0-9a-f-]{36}$/);
  assert.equal(harness.state.requests[0].options.headers['Idempotency-Key'], requestId);
  harness.state.reject(new Error('network response lost'));
  await assert.rejects(firstAttempt, /network response lost/);
  assert.equal(harness.state.cache.get(storageKey), requestId);

  const retry = harness.createTerminalSessionRequest(harness.state.activeRun);
  assert.equal(harness.state.requests[1].options.headers['Idempotency-Key'], requestId);
  harness.state.resolve({ id: '101', workspaceId: '73', status: 'open' });
  const session = await retry;
  assert.equal(session.id, '101');
  harness.acceptTerminalSessionRequest(harness.state.activeRun, session);
  assert.equal(harness.state.cache.get('bmhs.terminalSession.42'), JSON.stringify({
    id: '101', workspaceId: '73', status: 'open',
  }));
  assert.equal(harness.state.cache.has(storageKey), false);
});

test('an invalid session response retains its request identity for safe reconciliation', async () => {
  const harness = createHarness({ replacement: { id: 'not-an-id', workspaceId: '73', status: 'open' } });
  const storageKey = harness.terminalSessionRequestStorageKey(harness.state.activeRun);

  await assert.rejects(harness.createTerminalSessionRequest(harness.state.activeRun), /会话信息无效/);

  assert.match(harness.state.cache.get(storageKey), /^[0-9a-f-]{36}$/);
  assert.equal(harness.state.cache.has('bmhs.terminalSession.42'), false);
});

function createCommandHarness({
  createSession,
  commandRequest,
  statusResponses = [],
  refreshFiles = async () => {},
  activeSession = { id: '100', workspaceId: '73', status: 'open' },
} = {}) {
  const state = {
    runId: '42',
    workspaceId: '73',
    epoch: 8,
    requests: [],
    removedKeys: [],
    persisted: [],
    chat: [],
    createSession,
    cache: new Map(),
  };
  const terminalInput = { value: 'python main.py', disabled: false };
  const terminalOutput = {
    children: [],
    scrollTop: 0,
    scrollHeight: 12,
    appendChild(child) { this.children.push(child); },
    append(child) { this.children.push(child); },
  };
  const element = () => ({
    children: [],
    listeners: {},
    removed: false,
    append(...children) { this.children.push(...children); },
    appendChild(child) { this.children.push(child); },
    replaceChildren(...children) { this.children = [...children]; },
    replaceWith(child) { this.replacement = child; },
    addEventListener(type, listener) { this.listeners[type] = listener; },
    remove() { this.removed = true; },
    click() { return this.listeners.click?.(); },
  });
  const context = {
    activeRun: { runId: '42', workspaceId: '73' },
    activeRunProgress: null,
    activeTerminalSession: activeSession,
    activeTerminalRunId: '42',
    activeFilePath: null,
    activeFileSavedContent: '',
    codeEditor: { value: '', disabled: false },
    saveFileButton: { disabled: false },
    terminalInput,
    terminalOutput,
    terminalCommandsInFlight: new Set(),
    routeResolutionEpoch: 8,
    STORAGE_KEYS: {
      terminalSessionPrefix: 'bmhs.terminalSession.',
      terminalSessionRequestPrefix: 'bmhs.terminalSession.request.',
      terminalCommandPrefix: 'bmhs.terminalCommand.',
    },
    isRunContextCurrent: (runId, epoch) => String(runId) === state.runId && epoch === state.epoch,
    getCurrentUser: () => ({ id: 'student-1', role: 'student' }),
    isTerminalSessionClosedError: (error) => error?.status === 409 && error.code === 'TERMINAL_SESSION_CLOSED',
    removeSessionValue: (key) => { state.cache.delete(key); state.removedKeys.push(key); },
    readSessionValue: (key) => state.cache.get(key) || null,
    writeSessionValue: (key, value) => { state.cache.set(key, value); state.persisted.push({ key, value }); },
    window: { crypto: {
      randomUUID: () => 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
      getRandomValues(bytes) { bytes.forEach((_, index) => { bytes[index] = index; }); return bytes; },
    }, setTimeout(callback) { callback(); return 1; } },
    Uint8Array,
    terminalSessionStorageKey: (runId) => `bmhs.terminalSession.${runId}`,
    apiRequest(path, options) {
      state.requests.push({ path, options });
      if (path.endsWith('/commands')) {
        if (commandRequest) return commandRequest(path, options);
        const error = new Error('终端会话已关闭');
        error.status = 409;
        error.code = 'TERMINAL_SESSION_CLOSED';
        return Promise.reject(error);
      }
      if (path.includes('/commands/')) {
        const response = statusResponses.shift();
        return response instanceof Error ? Promise.reject(response)
          : Promise.resolve(response || { status: 'queued', output: '' });
      }
      if (state.createSession) return state.createSession();
      return Promise.resolve({ id: 101, workspaceId: 73, status: 'open' });
    },
    refreshWorkspaceFilesAfterTerminal: refreshFiles,
    renderWorkspaceFileTabs() {},
    appendChatMessage(role, name, message) { state.chat.push({ role, name, message }); },
    document: { createElement: element },
    encodeURIComponent,
    String,
    Number,
    Promise,
  };
  const handlers = runInNewContext(`(() => {
    ${app.slice(sessionUtilitiesStart, recoveryEnd)}
    async function runCommandHandler(event) {${app.slice(handlerBodyStart, handlerEnd)}}
    return {
      runCommandHandler,
      readPendingTerminalCommand,
      inspectPendingTerminalCommand,
      terminalCommandStorageKey,
      persistPendingTerminalCommand,
      reconcileTerminalCommand,
      presentTerminalCommandReconciliation,
      createTerminalReconnectButton,
      getActiveTerminalSession: () => activeTerminalSession,
    isCommandInFlight: (runId = '42') => terminalCommandsInFlight.has('student-1:' + runId),
    };
  })()`, context);
  return { ...handlers, context, state, terminalInput, terminalOutput };
}

function createFileTabHarness() {
  const buttons = {
    children: [],
    replaceChildren(...children) { this.children = [...children]; },
    append(button) { this.children.push(button); },
  };
  const codeLocation = { textContent: '' };
  const context = {
    activeTerminalRunId: '42',
    activeWorkspaceFiles: [
      { path: '/src/main.py', displayName: 'main.py' },
      { path: '/tests/main.py', displayName: 'main.py' },
    ],
    activeFilePath: '/src/main.py',
    activeFileSavedContent: 'saved',
    codeEditor: { value: 'unsaved edit' },
    codeFileTabs: buttons,
    codeLocation,
    terminalCommandsInFlight: new Set(['student-1:42']),
    getCurrentUser: () => ({ id: 'student-1' }),
    document: {
      createElement() {
        return {
          attributes: {},
          setAttribute(name, value) { this.attributes[name] = value; },
          addEventListener() {},
        };
      },
    },
    String,
    Map,
  };
  const lockStart = app.indexOf('function terminalCommandLockKey');
  const lockEnd = app.indexOf('\nfunction readTerminalSession', lockStart);
  const tabsStart = app.indexOf('function renderWorkspaceFileTabs');
  const tabsEnd = app.indexOf('\nfunction renderCodeLineNumbers', tabsStart);
  assert.notEqual(lockStart, -1, 'terminal command lock helper must be present');
  assert.notEqual(lockEnd, -1, 'terminal command lock boundary must be present');
  assert.notEqual(tabsStart, -1, 'workspace tabs renderer must be present');
  assert.notEqual(tabsEnd, -1, 'workspace tabs renderer boundary must be present');
  const render = runInNewContext(`(() => {
    ${app.slice(lockStart, lockEnd)}
    ${app.slice(tabsStart, tabsEnd)}
    return renderWorkspaceFileTabs;
  })()`, context);
  return { buttons, codeLocation, context, render };
}

test('only the explicit closed-session conflict is eligible for recovery', () => {
  const { isTerminalSessionClosedError } = createHarness();
  assert.equal(isTerminalSessionClosedError({ status: 409, code: 'TERMINAL_SESSION_CLOSED' }), true);
  assert.equal(isTerminalSessionClosedError({ status: 409, code: 'WORKSPACE_NOT_READY' }), false);
  assert.equal(isTerminalSessionClosedError({ status: 404, code: 'TERMINAL_SESSION_NOT_FOUND' }), false);
  assert.equal(isTerminalSessionClosedError(new Error('network failure')), false);
});

test('a valid replacement is created for the same workspace and persisted', async () => {
  const replacement = { id: 101, workspaceId: 73, status: 'open' };
  const harness = createHarness({ replacement });
  assert.equal(await harness.recoverClosedTerminalSession('42', '73', '100', 8), true);
  assert.equal(harness.state.requests.length, 1);
  assert.equal(harness.state.requests[0].path, '/workspaces/73/terminal-sessions');
  assert.equal(harness.state.requests[0].options.method, 'POST');
  assert.equal(harness.state.requests[0].options.timeoutMs, 15000);
  assert.equal(harness.state.persisted.filter((entry) => entry.key === 'bmhs.terminalSession.42').length, 1);
  assert.equal(harness.state.cache.has(harness.terminalSessionRequestStorageKey(harness.state.activeRun)), false);
  assert.equal(JSON.parse(harness.state.cache.get('bmhs.terminalSession.42')).id, 101);
  assert.equal(harness.getActiveTerminalSession().id, 101);
});

test('an invalid replacement is rejected without persistence', async () => {
  for (const replacement of [
    { id: 0, workspaceId: 73, status: 'open' },
    { id: '0101', workspaceId: 73, status: 'open' },
    { id: '9223372036854775808', workspaceId: 73, status: 'open' },
    { id: Number.MAX_SAFE_INTEGER + 1, workspaceId: 73, status: 'open' },
    { id: 100, workspaceId: 73, status: 'open' },
    { id: 101, workspaceId: 74, status: 'open' },
    { id: 101, workspaceId: 73, status: 'closed' },
  ]) {
    const harness = createHarness({ replacement });
    await assert.rejects(
      harness.recoverClosedTerminalSession('42', '73', '100', 8),
      /终端会话信息无效/,
    );
    assert.equal(harness.state.cache.has('bmhs.terminalSession.42'), false);
    assert.equal(harness.getActiveTerminalSession().id, '100');
  }
});

test('failed replacement invalidates only the matching closed session and cached ID', async () => {
  const harness = createHarness({ defer: true });
  const recovery = harness.recoverClosedTerminalSession('42', '73', '100', 8);
  harness.state.reject(new Error('server unavailable'));
  await assert.rejects(recovery, /server unavailable/);
  assert.equal(harness.invalidateClosedTerminalSession('42', '73', '100', 8), true);
  assert.equal(harness.getActiveTerminalSession(), null);
  assert.deepEqual(harness.state.removedKeys, ['bmhs.terminalSession.42']);
});

test('a late replacement cannot overwrite a changed session while its route remains current', async () => {
  const harness = createHarness({ defer: true });
  const recovery = harness.recoverClosedTerminalSession('42', '73', '100', 8);
  harness.setActiveTerminalSession({ id: '200', workspaceId: '73', status: 'open' });
  harness.state.resolve({ id: 101, workspaceId: 73, status: 'open' });
  assert.equal(await recovery, false);
  assert.equal(harness.state.cache.has('bmhs.terminalSession.42'), false);
  assert.equal(harness.getActiveTerminalSession().id, '200');
});

test('a late replacement cannot overwrite a newer route', async () => {
  const harness = createHarness({ defer: true });
  const recovery = harness.recoverClosedTerminalSession('42', '73', '100', 8);
  harness.state.epoch = 9;
  harness.state.resolve({ id: 101, workspaceId: 73, status: 'open' });
  assert.equal(await recovery, false);
  assert.equal(harness.state.cache.has('bmhs.terminalSession.42'), false);
  assert.equal(harness.getActiveTerminalSession().id, '100');
});

test('a late replacement cannot be adopted after the active workspace changes', async () => {
  const harness = createHarness({ defer: true });
  const recovery = harness.recoverClosedTerminalSession('42', '73', '100', 8);
  harness.state.activeRun.workspaceId = '74';
  harness.state.resolve({ id: 101, workspaceId: 73, status: 'open' });
  assert.equal(await recovery, false);
  assert.equal(harness.state.cache.has('bmhs.terminalSession.42'), false);
  assert.equal(harness.getActiveTerminalSession().id, '100');
});

test('the command handler replaces a closed session once and tells the student not to replay', async () => {
  const harness = createCommandHarness();
  await harness.runCommandHandler({ key: 'Enter' });
  assert.equal(harness.state.requests.length, 2);
  assert.match(harness.state.requests[0].path, /terminal-sessions\/100\/commands$/);
  assert.equal(harness.state.requests[1].path, '/workspaces/73/terminal-sessions');
  assert.equal(harness.terminalOutput.children.at(-1).textContent, '旧终端会话已关闭，已重新连接。刚才的命令未执行；确认代码状态后请手动重新运行。');
  assert.equal(harness.terminalInput.disabled, false);
  assert.equal(harness.getActiveTerminalSession().id, 101);
  assert.equal(harness.isCommandInFlight(), false);
});

test('an unrelated command error does not create another terminal session', async () => {
  const harness = createCommandHarness();
  const originalApiRequest = harness.context.apiRequest;
  harness.context.apiRequest = (path, options) => {
    if (path.endsWith('/commands')) {
      harness.state.requests.push({ path, options });
      return Promise.reject(new Error('ordinary command failure'));
    }
    return originalApiRequest(path, options);
  };
  await harness.runCommandHandler({ key: 'Enter' });
  assert.equal(harness.state.requests.filter(({ path }) => path.endsWith('/commands')).length, 1);
  assert.equal(harness.state.requests.filter(({ path, options }) => path.includes('/commands/') && options.method === 'GET').length, 4);
  assert.match(harness.terminalOutput.children.at(-1).children[0].textContent, /不要重新运行/);
  assert.equal(harness.terminalInput.disabled, true);
});

test('timeout issues only status GETs and retains the command lock and key', async () => {
  const harness = createCommandHarness({ commandRequest: () => Promise.reject(new Error('请求超时')) });
  await harness.runCommandHandler({ key: 'Enter' });
  const posts = harness.state.requests.filter(({ path, options }) => path.endsWith('/commands') && options.method === 'POST');
  const gets = harness.state.requests.filter(({ path, options }) => path.includes('/commands/') && options.method === 'GET');
  assert.equal(posts.length, 1);
  assert.equal(gets.length, 4);
  assert.equal(posts[0].options.headers['Idempotency-Key'], 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa');
  assert.equal(harness.isCommandInFlight(), true);
  assert.equal(harness.terminalInput.disabled, true);
  assert.equal(harness.state.cache.has('bmhs.terminalCommand.student-1:42'), true);
  const saved = JSON.parse(harness.state.cache.get('bmhs.terminalCommand.student-1:42'));
  assert.equal(saved.clientCommandId, 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa');
  assert.equal(Object.hasOwn(saved, 'commandText'), false);
  assert.equal(Object.hasOwn(saved, 'workingDirectory'), false);
});

test('HTTP 408 reconciles command status instead of unlocking or generating a new identity', async () => {
  const timeout = new Error('gateway timeout');
  timeout.status = 408;
  const harness = createCommandHarness({ commandRequest: () => Promise.reject(timeout) });
  await harness.runCommandHandler({ key: 'Enter' });
  const posts = harness.state.requests.filter(({ path, options }) => path.endsWith('/commands') && options.method === 'POST');
  const gets = harness.state.requests.filter(({ path, options }) => path.includes('/commands/') && options.method === 'GET');
  assert.equal(posts.length, 1);
  assert.equal(gets.length, 4);
  assert.equal(harness.isCommandInFlight(), true);
  assert.equal(harness.terminalInput.disabled, true);
  assert.equal(harness.state.cache.has('bmhs.terminalCommand.student-1:42'), true);
});

test('a reload restores its saved key and reconciles through GET without another POST', async () => {
  const harness = createCommandHarness({ statusResponses: [{ status: 'succeeded', output: 'recovered output' }] });
  const pending = {
    runId: '42', workspaceId: '73', userId: 'student-1', sessionId: '100',
    clientCommandId: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
  };
  harness.state.cache.set('bmhs.terminalCommand.student-1:42', JSON.stringify(pending));
  const restored = harness.readPendingTerminalCommand('42');
  assert.deepEqual(JSON.parse(JSON.stringify(restored)), pending);
  harness.context.activeRun = { runId: '42', workspaceId: '73' };
  harness.context.activeTerminalSession = { id: '101', workspaceId: '73', status: 'open' };
  const resolved = await harness.presentTerminalCommandReconciliation(pending, '42', '73', 8);
  assert.equal(resolved, true);
  assert.equal(harness.state.requests.length, 1);
  assert.equal(harness.state.requests[0].options.method, 'GET');
  assert.equal(harness.isCommandInFlight(), false);
  assert.equal(harness.state.cache.has('bmhs.terminalCommand.student-1:42'), false);
  assert.match(harness.terminalOutput.children.at(-1).replacement.textContent, /recovered output/);
});

test('unreadable or cross-workspace pending identity is surfaced as blocked, never treated as absent', () => {
  const harness = createCommandHarness();
  const key = 'bmhs.terminalCommand.student-1:42';
  assert.equal(harness.inspectPendingTerminalCommand('42', '73').status, 'none');
  harness.state.cache.set(key, '{broken-json');
  assert.equal(harness.inspectPendingTerminalCommand('42', '73').status, 'invalid');
  harness.state.cache.set(key, JSON.stringify({ runId: '42', workspaceId: '73', userId: 'student-1',
    sessionId: '100', clientCommandId: 'cccccccc-cccc-4ccc-8ccc-cccccccccccc' }));
  assert.equal(harness.inspectPendingTerminalCommand('42', '74').status, 'invalid');
  assert.equal(harness.inspectPendingTerminalCommand('42', '73').status, 'pending');
});

test('command submission suppresses default Enter behavior and preserves exact shell text', async () => {
  const harness = createCommandHarness({
    commandRequest: () => Promise.resolve({ status: 'succeeded', output: 'done' }),
  });
  const commandText = '  printf "kept whitespace"\\ ';
  harness.terminalInput.value = commandText;
  const event = { key: 'Enter', prevented: false, preventDefault() { this.prevented = true; } };
  await harness.runCommandHandler(event);
  assert.equal(event.prevented, true);
  assert.equal(JSON.parse(harness.state.requests[0].options.body).commandText, commandText);
});

test('a malformed command response is reconciled with GET and remains locked when unresolved', async () => {
  const harness = createCommandHarness({ commandRequest: () => Promise.resolve({ status: 'mystery', output: [] }) });
  await harness.runCommandHandler({ key: 'Enter' });
  assert.equal(harness.state.requests.filter(({ path }) => path.endsWith('/commands')).length, 1);
  assert.equal(harness.state.requests.filter(({ path }) => path.includes('/commands/') && path.endsWith('/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa')).length, 4);
  assert.equal(harness.terminalInput.disabled, true);
});

test('a failed recovery clears the stale session and leaves terminal commands disabled', async () => {
  const harness = createCommandHarness({ createSession: () => Promise.reject(new Error('服务器暂不可用')) });
  await harness.runCommandHandler({ key: 'Enter' });
  assert.equal(harness.state.requests.length, 2);
  assert.deepEqual(harness.state.removedKeys, [
    'bmhs.terminalCommand.student-1:42', 'bmhs.terminalSession.42',
  ]);
  assert.equal(harness.getActiveTerminalSession(), null);
  assert.equal(harness.terminalInput.disabled, true);
  assert.match(harness.terminalOutput.children.at(-1).textContent, /自动重连失败：服务器暂不可用/);
  assert.equal(harness.isCommandInFlight(), false);
});

test('the recovery error offers a retry that reconnects and enables the terminal', async () => {
  const harness = createCommandHarness({ createSession: () => Promise.reject(new Error('服务器暂不可用')) });
  await harness.runCommandHandler({ key: 'Enter' });
  const failure = harness.terminalOutput.children.at(-1);
  const reconnect = failure.children[0];
  assert.equal(reconnect.textContent, '重新连接终端');
  assert.equal(harness.terminalInput.disabled, true);

  await reconnect.click();
  assert.equal(reconnect.textContent, '重试连接终端');
  assert.equal(reconnect.disabled, false);
  assert.match(failure.textContent, /终端重连失败：服务器暂不可用/);

  harness.state.createSession = () => Promise.resolve({ id: 102, workspaceId: 73, status: 'open' });
  await reconnect.click();
  assert.equal(reconnect.removed, true);
  assert.equal(harness.getActiveTerminalSession().id, 102);
  assert.equal(harness.terminalInput.disabled, false);
  assert.match(failure.textContent, /终端已重新连接/);
  assert.match(failure.textContent, /命令未执行/);
});

test('a concurrent session connection settles the retry button after either POST outcome', async () => {
  for (const outcome of ['success', 'failure']) {
    let resolveCreate;
    let rejectCreate;
    const harness = createCommandHarness({
      createSession: () => new Promise((resolve, reject) => {
        resolveCreate = resolve;
        rejectCreate = reject;
      }),
    });
    harness.context.activeTerminalSession = null;
    harness.terminalInput.disabled = true;
    const message = { textContent: '重连中' };
    const button = harness.createTerminalReconnectButton('42', '73', '100', 8, message);
    const request = button.click();
    assert.equal(button.disabled, true);

    harness.context.activeTerminalSession = { id: 103, workspaceId: 73, status: 'open' };
    if (outcome === 'success') resolveCreate({ id: 102, workspaceId: 73, status: 'open' });
    else rejectCreate(new Error('重连请求失败'));
    await request;

    assert.equal(button.removed, true, `${outcome} response should settle the obsolete retry control`);
    assert.equal(harness.getActiveTerminalSession().id, 103);
    assert.equal(harness.terminalInput.disabled, false);
    assert.match(message.textContent, /终端已重新连接/);
  }
});

test('a malformed replacement is not adopted by the terminal command handler', async () => {
  const harness = createCommandHarness({ createSession: () => Promise.resolve({ id: 0, workspaceId: 73, status: 'open' }) });
  await harness.runCommandHandler({ key: 'Enter' });
  assert.equal(harness.state.requests.length, 2);
  assert.deepEqual(harness.state.removedKeys, [
    'bmhs.terminalCommand.student-1:42', 'bmhs.terminalSession.42',
  ]);
  assert.equal(harness.getActiveTerminalSession(), null);
  assert.equal(harness.terminalInput.disabled, true);
  assert.match(harness.terminalOutput.children.at(-1).textContent, /终端会话信息无效/);
});

test('cached and newly persisted session IDs share positive canonical validation', () => {
  const harness = createHarness();
  const run = { runId: '42', workspaceId: '73' };
  for (const id of [0, '0', '0101', '9223372036854775808', Number.MAX_SAFE_INTEGER + 1]) {
    harness.state.cache.set('bmhs.terminalSession.42', JSON.stringify({ id, workspaceId: 73, status: 'open' }));
    assert.equal(harness.readTerminalSession(run), null);
    assert.equal(harness.state.cache.has('bmhs.terminalSession.42'), false);
    harness.persistTerminalSession(run, { id, workspaceId: 73, status: 'open' });
    assert.equal(harness.state.cache.has('bmhs.terminalSession.42'), false);
  }
  harness.persistTerminalSession(run, { id: 101, workspaceId: 73, status: 'open' });
  assert.equal(JSON.parse(harness.state.cache.get('bmhs.terminalSession.42')).id, 101);
});

test('malformed, closed, and wrong-workspace cached sessions are discarded', () => {
  const harness = createHarness();
  const run = { runId: '42', workspaceId: '73' };
  for (const serialized of [
    '{invalid json',
    JSON.stringify({ id: 101, workspaceId: 73, status: 'closed' }),
    JSON.stringify({ id: 101, workspaceId: 74, status: 'open' }),
  ]) {
    harness.state.cache.set('bmhs.terminalSession.42', serialized);
    assert.equal(harness.readTerminalSession(run), null);
    assert.equal(harness.state.cache.has('bmhs.terminalSession.42'), false);
  }
});

test('the command handler rejects an invalid or cross-workspace active session before sending', async () => {
  for (const activeSession of [
    { id: 0, workspaceId: 73, status: 'open' },
    { id: 101, workspaceId: 74, status: 'open' },
    { id: 101, workspaceId: 73, status: 'closed' },
  ]) {
    const harness = createCommandHarness({ activeSession });
    await harness.runCommandHandler({ key: 'Enter' });
    assert.equal(harness.state.requests.length, 0);
    assert.equal(harness.getActiveTerminalSession(), null);
    assert.equal(harness.terminalInput.disabled, true);
    assert.match(harness.state.chat.at(-1).message, /会话信息与当前工作区不匹配/);
  }
});

test('workspace file tabs honor the active run lock and preserve selection/dirty labels', () => {
  const harness = createFileTabHarness();
  harness.render();
  assert.equal(harness.buttons.children.length, 2);
  assert.equal(harness.buttons.children[0].disabled, true);
  assert.equal(harness.buttons.children[1].disabled, true);
  assert.equal(harness.buttons.children[0].attributes['aria-selected'], 'true');
  assert.equal(harness.buttons.children[1].attributes['aria-selected'], 'false');
  assert.match(harness.buttons.children[0].textContent, /main\.py — \/src\/main\.py •/);

  harness.context.terminalCommandsInFlight.delete('student-1:42');
  harness.render();
  assert.equal(harness.buttons.children[0].disabled, false);
  assert.equal(harness.buttons.children[1].disabled, false);
});

test('an old command cannot unlock or alter controls for a newer run command', async () => {
  const pendingCommands = [];
  const harness = createCommandHarness({
    commandRequest: () => new Promise((resolve, reject) => pendingCommands.push({ resolve, reject })),
  });
  const oldCommand = harness.runCommandHandler({ key: 'Enter' });
  assert.equal(harness.isCommandInFlight('42'), true);

  harness.state.runId = '43';
  harness.state.workspaceId = '74';
  harness.state.epoch = 9;
  harness.context.routeResolutionEpoch = 9;
  harness.context.activeRun = { runId: '43', workspaceId: '74' };
  harness.context.activeTerminalRunId = '43';
  harness.context.activeTerminalSession = { id: 200, workspaceId: 74, status: 'open' };
  harness.terminalInput.disabled = false;
  harness.terminalInput.value = 'python newer.py';
  const newCommand = harness.runCommandHandler({ key: 'Enter' });
  assert.equal(harness.isCommandInFlight('43'), true);

  pendingCommands[0].resolve({ status: 'succeeded', output: 'old run finished' });
  await oldCommand;
  assert.equal(harness.isCommandInFlight('42'), false);
  assert.equal(harness.isCommandInFlight('43'), true);
  assert.equal(harness.terminalInput.disabled, true);
  harness.terminalInput.value = 'python should-wait.py';
  await harness.runCommandHandler({ key: 'Enter' });
  assert.equal(harness.state.requests.filter(({ path }) => path.endsWith('/commands')).length, 2);
  assert.equal(harness.terminalInput.value, 'python should-wait.py');

  pendingCommands[1].resolve({ status: 'succeeded', output: 'new run finished' });
  await newCommand;
  assert.equal(harness.isCommandInFlight('43'), false);
  assert.equal(harness.terminalInput.disabled, false);
});

test('file refresh rejection still releases the command lock and restores current controls', async () => {
  const harness = createCommandHarness({
    commandRequest: () => Promise.resolve({ status: 'succeeded', output: 'done' }),
    refreshFiles: async () => { throw new Error('refresh rejected'); },
  });
  await harness.runCommandHandler({ key: 'Enter' });
  assert.equal(harness.isCommandInFlight('42'), false);
  assert.equal(harness.terminalInput.disabled, false);
  assert.match(harness.state.chat.at(-1).message, /refresh rejected/);
});

test('route changes during post-command file refresh leave the new route controls untouched', async () => {
  let completeRefresh;
  const harness = createCommandHarness({
    commandRequest: () => Promise.resolve({ status: 'succeeded', output: 'done' }),
    refreshFiles: () => new Promise((resolve) => { completeRefresh = resolve; }),
  });
  const command = harness.runCommandHandler({ key: 'Enter' });
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(typeof completeRefresh, 'function');

  harness.state.runId = '43';
  harness.state.workspaceId = '74';
  harness.state.epoch = 9;
  harness.context.routeResolutionEpoch = 9;
  harness.context.activeRun = { runId: '43', workspaceId: '74' };
  harness.context.activeTerminalRunId = '43';
  harness.context.activeTerminalSession = { id: 200, workspaceId: 74, status: 'open' };
  harness.terminalInput.disabled = true;
  harness.context.codeEditor.disabled = true;
  harness.context.saveFileButton.disabled = true;

  completeRefresh();
  await command;
  assert.equal(harness.isCommandInFlight('42'), false);
  assert.equal(harness.terminalInput.disabled, true);
  assert.equal(harness.context.codeEditor.disabled, true);
  assert.equal(harness.context.saveFileButton.disabled, true);
});
