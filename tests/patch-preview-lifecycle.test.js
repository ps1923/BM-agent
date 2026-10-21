import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { runInNewContext } from 'node:vm';
import test from 'node:test';

const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');
const closeStart = app.indexOf('function closePatchReview()');
const renderStart = app.indexOf('\nfunction renderPatchFileDiff', closeStart);
const openStart = app.indexOf('\nasync function openPatchReview', renderStart);
const dialogHandlersEnd = app.indexOf("\nclosePatchDialog.addEventListener", openStart);

class TestElement {
  constructor() {
    this.disabled = false;
    this.textContent = '';
    this.children = [];
    this.attributes = new Map();
    this.classes = new Set();
    this.classList = {
      add: (name) => this.classes.add(name),
      remove: (name) => this.classes.delete(name),
      contains: (name) => this.classes.has(name),
    };
  }

  replaceChildren(...children) { this.children = [...children]; }
  append(...children) { this.children.push(...children); }
  setAttribute(name, value) { this.attributes.set(name, value); }
}

function createHarness() {
  const state = {
    activeRun: { runId: 42, workspaceId: 7 },
    patchReviewEpoch: 0,
    patchReviewSourceButton: null,
    patchReviewApplied: false,
    patchApplyInFlight: false,
    pendingPatchReview: null,
    routeResolutionEpoch: 0,
    activeFilePath: null,
    chatMessages: [],
    requests: [],
    patchDiffList: new TestElement(),
    patchDialog: new TestElement(),
    patchDialogMessage: new TestElement(),
    patchDialogDescription: new TestElement(),
    confirmPatchButton: new TestElement(),
  };
  const context = {
    String,
    Array,
    Error,
    Number,
    patchDiffList: state.patchDiffList,
    patchDialog: state.patchDialog,
    patchDialogMessage: state.patchDialogMessage,
    patchDialogDescription: state.patchDialogDescription,
    confirmPatchButton: state.confirmPatchButton,
    workspaceFileUrl: (path) => encodeURIComponent(path),
    verifyWorkspaceFileResponse: (response) => response,
    renderPatchFileDiff: () => new TestElement(),
    appendChatMessage(...message) { state.chatMessages.push(message); },
    isRunContextCurrent(runId, epoch) {
      return String(state.activeRun?.runId) === String(runId) && state.routeResolutionEpoch === epoch;
    },
    setMessage(element, message, isError = false) {
      element.textContent = message;
      element.isError = isError;
    },
    apiRequest(path, options) {
      return new Promise((resolve, reject) => state.requests.push({ path, options, resolve, reject }));
    },
  };
  for (const key of [
    'activeRun', 'patchReviewEpoch', 'patchReviewSourceButton', 'patchReviewApplied',
    'patchApplyInFlight', 'pendingPatchReview', 'routeResolutionEpoch', 'activeFilePath',
  ]) {
    Object.defineProperty(context, key, {
      get: () => state[key],
      set: (value) => { state[key] = value; },
    });
  }
  const functions = runInNewContext(`(() => {
    ${app.slice(closeStart, renderStart)}
    ${app.slice(openStart, dialogHandlersEnd)}
    return { closePatchReview, openPatchReview, confirmPatchReview, isPatchApplyResponseValid, recoverPatchApplyResponse };
  })()`, context);
  return { ...functions, state };
}

function sourceButton(label) {
  return { label, disabled: false, textContent: label };
}

async function finishSuccessfulPreview(harness, previewPromise, detailRequestIndex, detailId = 1) {
  const { state } = harness;
  state.requests[detailRequestIndex].resolve({
    id: detailId,
    files: [{ path: 'src/main.py', content: 'print(1)', expectedHash: 'abc' }],
  });
  await new Promise((resolve) => setImmediate(resolve));
  const fileRequest = state.requests[detailRequestIndex + 1];
  assert.match(fileRequest.path, /^\/workspaces\/7\/files\//);
  fileRequest.resolve({ content: 'print(0)', file: { contentHash: 'abc' } });
  await previewPromise;
}

test('stale failures cannot alter the newer preview and restore only their own button', async () => {
  const harness = createHarness();
  const oldButton = sourceButton('旧补丁');
  const newButton = sourceButton('新补丁');
  const oldPreview = harness.openPatchReview({ id: 1 }, oldButton);
  const newPreview = harness.openPatchReview({ id: 2 }, newButton);

  harness.state.requests[0].reject(new Error('旧请求失败'));
  await oldPreview;
  assert.equal(oldButton.disabled, false);
  assert.equal(newButton.disabled, true);
  assert.equal(harness.state.patchDialogMessage.textContent, '正在读取补丁和当前文件…');

  harness.state.requests[1].reject(new Error('新请求失败'));
  await newPreview;
  assert.equal(newButton.disabled, false);
  assert.equal(harness.state.patchDialogMessage.textContent, '新请求失败');
});

test('a valid stale response cannot replace a successfully rendered newer diff', async () => {
  const harness = createHarness();
  const oldButton = sourceButton('旧补丁');
  const newButton = sourceButton('新补丁');
  const oldPreview = harness.openPatchReview({ id: 1 }, oldButton);
  const newPreview = harness.openPatchReview({ id: 2 }, newButton);

  await finishSuccessfulPreview(harness, newPreview, 1, 2);
  assert.equal(harness.state.pendingPatchReview.detail.id, 2);
  const currentMessage = harness.state.patchDialogMessage.textContent;
  const currentDiff = harness.state.patchDiffList.children[0];

  harness.state.requests[0].resolve({
    id: 1,
    files: [{ path: 'src/old.py', content: 'print(0)', expectedHash: 'old' }],
  });
  await new Promise((resolve) => setImmediate(resolve));
  assert.match(harness.state.requests[3].path, /^\/workspaces\/7\/files\//);
  harness.state.requests[3].resolve({ content: 'print(-1)', file: { contentHash: 'old' } });
  await oldPreview;

  assert.equal(harness.state.pendingPatchReview.detail.id, 2);
  assert.equal(harness.state.patchDialogMessage.textContent, currentMessage);
  assert.equal(harness.state.patchDiffList.children[0], currentDiff);
  assert.equal(oldButton.disabled, false);
  assert.equal(newButton.disabled, true);
});

test('replacing an already rendered preview releases the old button; cancelling restores the new one', async () => {
  const harness = createHarness();
  const oldButton = sourceButton('旧补丁');
  const newButton = sourceButton('新补丁');
  const oldPreview = harness.openPatchReview({ id: 1 }, oldButton);
  await finishSuccessfulPreview(harness, oldPreview, 0);
  assert.equal(oldButton.disabled, true);
  assert.equal(harness.state.pendingPatchReview.detail.id, 1);

  const newPreview = harness.openPatchReview({ id: 2 }, newButton);
  assert.equal(oldButton.disabled, false);
  assert.equal(newButton.disabled, true);
  harness.closePatchReview();
  harness.state.requests[2].reject(new Error('已关闭的预览失败'));
  await newPreview;
  assert.equal(newButton.disabled, false);
  assert.equal(harness.state.patchDialog.attributes.get('aria-hidden'), 'true');
});

test('closing an applied patch does not re-enable its source button', () => {
  const harness = createHarness();
  const appliedButton = sourceButton('已应用');
  appliedButton.disabled = true;
  harness.state.patchReviewSourceButton = appliedButton;
  harness.state.patchReviewApplied = true;

  harness.closePatchReview();

  assert.equal(appliedButton.disabled, true);
  assert.equal(harness.state.patchReviewSourceButton, null);
});

test('another preview is held until patch application settles, then applied state is preserved', async () => {
  const harness = createHarness();
  const appliedButton = sourceButton('查看补丁');
  const nextButton = sourceButton('下一个补丁');
  const preview = harness.openPatchReview({ id: 1 }, appliedButton);
  await finishSuccessfulPreview(harness, preview, 0);

  const applying = harness.confirmPatchReview();
  assert.equal(harness.state.patchApplyInFlight, true);
  assert.equal(harness.state.requests[2].path, '/runs/42/patches/1/apply');
  await harness.openPatchReview({ id: 2 }, nextButton);
  assert.equal(harness.state.requests.length, 3);
  assert.equal(nextButton.disabled, false);
  assert.equal(harness.state.patchDialogMessage.textContent, '补丁正在应用，请等待完成后再查看其他补丁。');

  harness.state.requests[2].resolve({ patchId: 1, status: 'applied', appliedFiles: ['src/main.py'], snapshotId: 10 });
  await applying;
  assert.equal(harness.state.patchApplyInFlight, false);
  assert.equal(appliedButton.disabled, true);
  assert.equal(appliedButton.textContent, '已应用并生成快照');

  const nextPreview = harness.openPatchReview({ id: 2 }, nextButton);
  assert.equal(harness.state.requests.length, 4);
  harness.state.requests[3].reject(new Error('取消测试请求'));
  await nextPreview;
  assert.equal(nextButton.disabled, false);
});

test('failed application releases the apply lock and keeps the unapplied preview recoverable', async () => {
  const harness = createHarness();
  const source = sourceButton('查看补丁');
  const preview = harness.openPatchReview({ id: 1 }, source);
  await finishSuccessfulPreview(harness, preview, 0);

  const applying = harness.confirmPatchReview();
  harness.state.requests[2].reject(new Error('文件哈希已变化'));
  await applying;
  assert.equal(harness.state.patchApplyInFlight, false);
  assert.equal(harness.state.confirmPatchButton.disabled, false);
  assert.equal(source.disabled, true);
  assert.equal(harness.state.patchDialogMessage.textContent, '文件哈希已变化');
});

test('closing the dialog during apply still marks a successful patch as applied', async () => {
  const harness = createHarness();
  const source = sourceButton('查看补丁');
  const preview = harness.openPatchReview({ id: 1 }, source);
  await finishSuccessfulPreview(harness, preview, 0);

  const applying = harness.confirmPatchReview();
  harness.closePatchReview();
  assert.equal(source.disabled, false);
  harness.state.requests[2].resolve({ patchId: 1, status: 'applied', appliedFiles: ['src/main.py'], snapshotId: 11 });
  await applying;

  assert.equal(harness.state.patchApplyInFlight, false);
  assert.equal(source.disabled, true);
  assert.equal(source.textContent, '已应用并生成快照');
});

test('requesting another preview after closing during apply reopens visible feedback and reports completion', async () => {
  const harness = createHarness();
  const source = sourceButton('查看补丁');
  const next = sourceButton('下一个补丁');
  const preview = harness.openPatchReview({ id: 1 }, source);
  await finishSuccessfulPreview(harness, preview, 0);
  const applying = harness.confirmPatchReview();
  harness.closePatchReview();

  await harness.openPatchReview({ id: 2 }, next);
  assert.equal(harness.state.patchDialog.classList.contains('is-open'), true);
  assert.equal(harness.state.patchDialog.attributes.get('aria-hidden'), 'false');
  assert.equal(harness.state.patchDialogMessage.textContent, '补丁正在应用，请等待完成后再查看其他补丁。');
  assert.equal(harness.state.requests.length, 3);

  harness.state.requests[2].resolve({ patchId: 1, status: 'applied', appliedFiles: ['src/main.py'], snapshotId: 12 });
  await applying;
  assert.equal(harness.state.patchDialogMessage.textContent, '补丁已应用，请重新打开要查看的补丁。');
  assert.equal(source.disabled, true);
  assert.equal(source.textContent, '已应用并生成快照');
});

test('successful apply after a route epoch change closes the stale dialog and keeps its button applied', async () => {
  const harness = createHarness();
  const source = sourceButton('查看补丁');
  const next = sourceButton('下一个补丁');
  harness.state.activeFilePath = 'src/main.py';
  const preview = harness.openPatchReview({ id: 1 }, source);
  await finishSuccessfulPreview(harness, preview, 0);
  const applying = harness.confirmPatchReview();

  harness.state.routeResolutionEpoch += 1;
  harness.state.requests[2].resolve({ patchId: 1, status: 'applied', appliedFiles: ['src/main.py'], snapshotId: 13 });
  await applying;

  assert.equal(source.disabled, true);
  assert.equal(source.textContent, '已应用并生成快照');
  assert.equal(harness.state.patchDialog.classList.contains('is-open'), false);
  assert.equal(harness.state.chatMessages.length, 0);
  assert.equal(harness.state.requests.length, 3);
  const nextPreview = harness.openPatchReview({ id: 2 }, next);
  assert.equal(source.disabled, true);
  harness.state.requests[3].reject(new Error('测试结束'));
  await nextPreview;
});

test('failed apply after closing and requesting another preview reports the failure visibly', async () => {
  const harness = createHarness();
  const source = sourceButton('查看补丁');
  const next = sourceButton('下一个补丁');
  const preview = harness.openPatchReview({ id: 1 }, source);
  await finishSuccessfulPreview(harness, preview, 0);
  const applying = harness.confirmPatchReview();
  harness.closePatchReview();

  await harness.openPatchReview({ id: 2 }, next);
  assert.equal(harness.state.patchDialog.classList.contains('is-open'), true);
  harness.state.requests[2].reject(new Error('快照创建失败'));
  await applying;

  assert.equal(harness.state.patchApplyInFlight, false);
  assert.equal(harness.state.patchDialogMessage.textContent, '快照创建失败');
  assert.equal(harness.state.patchDialogMessage.isError, true);
  assert.equal(source.disabled, false);
  assert.equal(next.disabled, false);
});

test('failed apply after a route epoch change closes the stale dialog and restores its source', async () => {
  const harness = createHarness();
  const source = sourceButton('查看补丁');
  const preview = harness.openPatchReview({ id: 1 }, source);
  await finishSuccessfulPreview(harness, preview, 0);
  const applying = harness.confirmPatchReview();
  harness.state.routeResolutionEpoch += 1;

  harness.state.requests[2].reject(new Error('应用请求失败'));
  await applying;

  assert.equal(harness.state.patchApplyInFlight, false);
  assert.equal(harness.state.patchDialog.classList.contains('is-open'), false);
  assert.equal(harness.state.patchDialog.attributes.get('aria-hidden'), 'true');
  assert.equal(source.disabled, false);
  assert.equal(harness.state.confirmPatchButton.disabled, true);
});

test('file refresh failure after successful apply is reported as refresh-only and keeps applied state', async () => {
  const harness = createHarness();
  harness.state.activeFilePath = 'src/main.py';
  const source = sourceButton('查看补丁');
  const next = sourceButton('下一个补丁');
  const preview = harness.openPatchReview({ id: 1 }, source);
  await finishSuccessfulPreview(harness, preview, 0);
  const applying = harness.confirmPatchReview();

  harness.state.requests[2].resolve({ patchId: 1, status: 'applied', appliedFiles: ['src/main.py'], snapshotId: 14 });
  await new Promise((resolve) => setImmediate(resolve));
  assert.match(harness.state.requests[3].path, /^\/workspaces\/7\/files\//);
  await harness.openPatchReview({ id: 2 }, next);
  harness.state.requests[3].reject(new Error('刷新接口暂不可用'));
  await applying;

  const completionMessage = harness.state.patchDialogMessage.textContent;
  assert.equal(harness.state.patchDialog.classList.contains('is-open'), true);
  assert.match(completionMessage, /^补丁已应用，但当前文件自动刷新失败/);
  assert.doesNotMatch(completionMessage, /补丁未应用/);
  assert.equal(harness.state.patchApplyInFlight, false);
  assert.equal(source.disabled, true);
  assert.equal(source.textContent, '已应用并生成快照');
  assert.equal(harness.state.chatMessages.some(([, , message]) => /自动刷新失败/.test(message)), true);
  assert.equal(next.disabled, false);
});

test('malformed apply response is reconciled through patch status before reporting success', async () => {
  const harness = createHarness();
  const source = sourceButton('查看补丁');
  const preview = harness.openPatchReview({ id: 1 }, source);
  await finishSuccessfulPreview(harness, preview, 0);

  const applying = harness.confirmPatchReview();
  harness.state.requests[2].resolve({ status: 'applied' });
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(harness.state.requests[3].path, '/runs/42/patches/1');
  harness.state.requests[3].resolve({
    id: 1,
    status: 'applied',
    snapshotId: 15,
    files: [{ path: 'src/main.py', content: 'print(1)', expectedHash: 'abc' }],
  });
  await applying;

  assert.equal(source.disabled, true);
  assert.equal(source.textContent, '已应用并生成快照');
  assert.equal(harness.state.chatMessages.some(([, , message]) => message.includes('应用前快照编号 15')), true);
  assert.doesNotMatch(harness.state.patchDialogMessage.textContent, /结果无法确认/);
});

test('malformed apply response with unconfirmed status locks the patch and never claims it failed', async () => {
  const harness = createHarness();
  const source = sourceButton('查看补丁');
  const preview = harness.openPatchReview({ id: 1 }, source);
  await finishSuccessfulPreview(harness, preview, 0);

  const applying = harness.confirmPatchReview();
  harness.state.requests[2].resolve({ appliedFiles: ['other.py'], snapshotId: 0 });
  await new Promise((resolve) => setImmediate(resolve));
  harness.state.requests[3].resolve({ id: 1, status: 'pending', snapshotId: null, files: [{ path: 'src/main.py' }] });
  await applying;

  assert.equal(harness.state.patchApplyInFlight, false);
  assert.equal(source.disabled, true);
  assert.equal(source.textContent, '应用结果待核实');
  assert.match(harness.state.chatMessages.at(-1)[2], /补丁应用结果无法确认/);
  assert.doesNotMatch(harness.state.patchDialogMessage.textContent, /补丁未应用/);
  assert.equal(harness.state.patchDialog.classList.contains('is-open'), false);
});

test('patch apply response contract rejects missing, mismatched, unsafe, or incomplete fields', () => {
  const harness = createHarness();
  const valid = { patchId: 1, status: 'applied', snapshotId: 9, appliedFiles: ['src/main.py'] };
  assert.equal(harness.isPatchApplyResponseValid(valid, 1, ['src/main.py']), true);
  assert.equal(harness.isPatchApplyResponseValid({ ...valid, patchId: 2 }, 1, ['src/main.py']), false);
  assert.equal(harness.isPatchApplyResponseValid({ ...valid, snapshotId: Number.MAX_SAFE_INTEGER + 1 }, 1, ['src/main.py']), false);
  assert.equal(harness.isPatchApplyResponseValid({ ...valid, appliedFiles: [] }, 1, ['src/main.py']), false);
  assert.equal(harness.isPatchApplyResponseValid({ ...valid, appliedFiles: ['src/main.py', 'extra.py'] }, 1, ['src/main.py']), false);
});
