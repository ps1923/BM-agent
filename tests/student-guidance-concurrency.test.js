import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { runInNewContext } from 'node:vm';
import test from 'node:test';

const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');
const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const guidanceStart = app.indexOf('async function requestGuidance(');
const guidanceEnd = app.indexOf('\nfunction appendDiagnosisPatch', guidanceStart);
const submitStart = app.indexOf('function sendChatMessage()');
const submitEnd = app.indexOf('\nsendButton.addEventListener', submitStart);
const controlsStart = app.indexOf('function syncGuidanceControls()');
const controlsEnd = app.indexOf('\nfunction setCurrentUser', controlsStart);
const contextGuardStart = app.indexOf('function isRunContextCurrent(runId, epoch)');
const contextGuardEnd = app.indexOf('\nfunction syncGuidanceControls', contextGuardStart);

test('student guidance input explains automatic context and matches the server request limit', () => {
  const composer = html.match(/<textarea id="chatInput"[^>]*>/)?.[0] || '';
  assert.match(composer, /maxlength="6000"/);
  assert.match(composer, /aria-describedby="chatComposerHint"/);
  assert.match(html, /id="chatComposerHint"[^>]*>最多 6000 字；无需粘贴整个项目/);
  assert.match(html, /系统会自动读取当前节点、相关代码和终端信息/);
});

function createHarness(initialRunId = 'run-a') {
  const state = {
    activeRun: { runId: initialRunId },
    routeResolutionEpoch: 0,
    guidanceRequestsInFlight: new Set(),
    requests: [],
    messages: [],
    chatInput: { value: '' },
    sendButton: { disabled: false },
    softenButton: { disabled: false },
  };
  const context = {
    String,
    Array,
    Error,
    apiRequest(path, options) {
      return new Promise((resolve, reject) => state.requests.push({ path, options, resolve, reject }));
    },
    appendChatMessage(...message) { state.messages.push(message); },
  };
  Object.defineProperties(context, {
    activeRun: { get: () => state.activeRun },
    routeResolutionEpoch: { get: () => state.routeResolutionEpoch },
    guidanceRequestsInFlight: { get: () => state.guidanceRequestsInFlight },
    chatInput: { get: () => state.chatInput },
    sendButton: { get: () => state.sendButton },
    softenButton: { get: () => state.softenButton },
  });
  const contextGuard = runInNewContext(`(${app.slice(contextGuardStart, contextGuardEnd).trim()})`, context);
  context.isRunContextCurrent = contextGuard;
  const syncControls = runInNewContext(`(${app.slice(controlsStart, controlsEnd).trim()})`, context);
  context.syncGuidanceControls = syncControls;
  const requestGuidance = runInNewContext(`(${app.slice(guidanceStart, guidanceEnd).trim()})`, context);
  context.requestGuidance = requestGuidance;
  const sendChatMessage = runInNewContext(`(${app.slice(submitStart, submitEnd).trim()})`, context);
  return { state, contextGuard, syncControls, requestGuidance, sendChatMessage };
}

function guidanceResult(content) {
  return { content, ragAvailable: false, ragReferences: [] };
}

test('repeated keyboard submission keeps the draft and creates only one user message/request', async () => {
  const harness = createHarness();
  const { state } = harness;
  const first = harness.requestGuidance('第一个问题');
  assert.equal(state.requests.length, 1);
  assert.equal(state.sendButton.disabled, true);
  assert.equal(state.softenButton.disabled, true);

  state.chatInput.value = '第二个问题';
  harness.sendChatMessage();
  assert.equal(state.chatInput.value, '第二个问题');
  assert.equal(state.requests.length, 1);
  assert.deepEqual(state.messages.filter(([kind]) => kind === 'user').map(([, , text]) => text), ['第一个问题']);

  state.requests[0].resolve(guidanceResult('第一个答复'));
  await first;
  assert.equal(state.sendButton.disabled, false);
  assert.equal(state.softenButton.disabled, false);

  harness.sendChatMessage();
  assert.equal(state.chatInput.value, '');
  assert.equal(state.requests.length, 2);
  assert.equal(state.messages.filter(([kind]) => kind === 'user').length, 2);
});

test('pending requests are isolated by run and stale completion cannot unlock another run', async () => {
  const harness = createHarness('run-a');
  const { state } = harness;
  const runA = harness.requestGuidance('A 的问题');

  state.activeRun = { runId: 'run-b' };
  state.routeResolutionEpoch += 1;
  harness.syncControls();
  assert.equal(state.sendButton.disabled, false);
  const runB = harness.requestGuidance('B 的问题');
  assert.equal(state.sendButton.disabled, true);
  assert.equal(state.requests.length, 2);

  state.requests[0].resolve(guidanceResult('A 的答复'));
  await runA;
  assert.equal(state.sendButton.disabled, true);
  assert.equal(state.softenButton.disabled, true);
  assert.equal(state.messages.some(([, label, text]) => label === '导师' && text === 'A 的答复'), false);

  state.requests[1].resolve(guidanceResult('B 的答复'));
  await runB;
  assert.equal(state.sendButton.disabled, false);
  assert.equal(state.messages.some(([, label, text]) => label === '导师' && text === 'B 的答复'), true);
});

test('failed request releases only its run gate and the predefined hint cannot bypass it', async () => {
  const harness = createHarness();
  const { state } = harness;
  const failed = harness.requestGuidance('会失败的问题');
  const duplicateHint = harness.requestGuidance('预设的最小提示');
  assert.equal(state.requests.length, 1);
  await duplicateHint;

  state.requests[0].reject(new Error('模型暂不可用'));
  await failed;
  assert.equal(state.sendButton.disabled, false);
  assert.equal(state.softenButton.disabled, false);
  assert.equal(state.messages.some(([, label, text]) => label === '系统' && text === '模型暂不可用'), true);

  const retry = harness.requestGuidance('重试问题');
  assert.equal(state.requests.length, 2);
  state.requests[1].resolve(guidanceResult('重试答复'));
  await retry;
});

test('failed student guidance restores the submitted draft without overwriting newer input', async () => {
  const harness = createHarness();
  const { state } = harness;
  state.chatInput.value = '  请帮我定位这个报错  ';
  harness.sendChatMessage();
  assert.equal(state.chatInput.value, '');
  assert.equal(state.requests.length, 1);

  state.requests[0].reject(new Error('网络暂不可用'));
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(state.chatInput.value, '  请帮我定位这个报错  ');

  state.chatInput.value = '另一个新问题';
  harness.sendChatMessage();
  state.chatInput.value = '请求期间输入的新内容';
  state.requests[1].reject(new Error('模型暂不可用'));
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(state.chatInput.value, '请求期间输入的新内容');
});

test('stale guidance failure does not restore a draft into another run', async () => {
  const harness = createHarness('run-a');
  const { state } = harness;
  state.chatInput.value = '旧实验的问题';
  harness.sendChatMessage();
  state.activeRun = { runId: 'run-b' };
  state.routeResolutionEpoch += 1;
  state.chatInput.value = '新实验的问题';

  state.requests[0].reject(new Error('旧实验请求失败'));
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(state.chatInput.value, '新实验的问题');
  assert.equal(state.messages.some(([, , text]) => text === '旧实验请求失败'), false);
});

test('Ctrl/Cmd+Enter remains wired through the guarded submit function', () => {
  const listener = app.slice(app.indexOf("chatInput.addEventListener('keydown'"), app.indexOf("softenButton.addEventListener", submitEnd));
  assert.match(listener, /event\.key === 'Enter'/);
  assert.match(listener, /event\.ctrlKey \|\| event\.metaKey/);
  assert.match(listener, /sendChatMessage\(\)/);
  assert.match(app.slice(submitStart, submitEnd), /guidanceRequestsInFlight\.has\(String\(activeRun\.runId\)\)/);
});

test('clearing the active run on logout resynchronizes the guidance controls', () => {
  const logoutStart = app.indexOf('async function logout()');
  const logoutEnd = app.indexOf('\ndocument.querySelectorAll', logoutStart);
  const logout = app.slice(logoutStart, logoutEnd);
  assert.match(logout, /activeRun = null;\s*syncGuidanceControls\(\);/);
});
