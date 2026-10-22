const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { resolveApiBaseUrl } = require('../api-config.js');
const app = fs.readFileSync(require.resolve('../app.js'), 'utf8');

function loadApiRequest(fetchImpl) {
  const start = app.indexOf('async function apiRequest(');
  const end = app.indexOf('\nfunction beginCreateRequest', start);
  assert.ok(start >= 0 && end > start, 'apiRequest must remain a standalone helper');
  const context = {
    AbortController,
    API_BASE_URL: 'http://127.0.0.1:8080/api',
    API_BASE_URL_ERROR: '',
    fetch: fetchImpl,
    window: { setTimeout, clearTimeout },
  };
  vm.runInNewContext(`${app.slice(start, end)}\nglobalThis.request = apiRequest;`, context);
  return context.request;
}

test('local loopback development connects to the local Spring API', () => {
  assert.equal(resolveApiBaseUrl(new URL('http://127.0.0.1:8000/')), 'http://127.0.0.1:8080/api');
  assert.equal(resolveApiBaseUrl(new URL('http://localhost:8000/')), 'http://localhost:8080/api');
});

test('remote origins use same-origin /api rather than the browser loopback', () => {
  assert.equal(resolveApiBaseUrl(new URL('https://student.example.edu/')), 'https://student.example.edu/api');
  assert.equal(resolveApiBaseUrl(new URL('http://192.0.2.18:8000/')), 'http://192.0.2.18:8000/api');
});

test('explicit safe API base is honored and trailing slashes are normalized', () => {
  assert.equal(resolveApiBaseUrl(new URL('http://127.0.0.1:8000/'), 'http://localhost:8080/api/'),
    'http://localhost:8080/api');
  assert.equal(resolveApiBaseUrl(new URL('https://student.example.edu/'), '/api/'),
    'https://student.example.edu/api');
});

test('empty overrides use the environment-specific default', () => {
  assert.equal(resolveApiBaseUrl(new URL('https://student.example.edu/'), '  '), 'https://student.example.edu/api');
});

test('BM Agent reverse-proxy mount uses its reserved API prefix', () => {
  assert.equal(
    resolveApiBaseUrl(new URL('http://47.109.176.135/bm-agent')),
    'http://47.109.176.135/bm-agent/api',
  );
  assert.equal(
    resolveApiBaseUrl(new URL('http://47.109.176.135/bm-agent/')),
    'http://47.109.176.135/bm-agent/api',
  );
  assert.equal(
    resolveApiBaseUrl(new URL('http://47.109.176.135/bm-agentish/')),
    'http://47.109.176.135/api',
  );
});

test('HTTPS rejects insecure API overrides', () => {
  assert.throws(
    () => resolveApiBaseUrl(new URL('https://student.example.edu/'), 'http://api.example.edu/api'),
    /HTTPS 页面不能连接明文 HTTP API/,
  );
});

test('API overrides reject unsupported schemes and embedded credentials', () => {
  assert.throws(
    () => resolveApiBaseUrl(new URL('http://localhost:8000/'), 'javascript:alert(1)'),
    /有效的 HTTP\(S\) 地址/,
  );
  assert.throws(
    () => resolveApiBaseUrl(new URL('http://localhost:8000/'), 'http://user:secret@localhost:8080/api'),
    /不能包含凭据/,
  );
  assert.throws(
    () => resolveApiBaseUrl(new URL('http://localhost:8000/'), 'http://localhost:8080'),
    /必须以 \/api 结尾/,
  );
  assert.throws(
    () => resolveApiBaseUrl(new URL('http://localhost:8000/'), 'http://localhost:8080/api?token=secret'),
    /不能包含凭据、查询参数或片段/,
  );
  assert.throws(
    () => resolveApiBaseUrl(new URL('http://localhost:8000/'), 'http://localhost:8080/api#debug'),
    /不能包含凭据、查询参数或片段/,
  );
});

test('browser script exposes the API config before app startup', () => {
  const source = fs.readFileSync(require.resolve('../api-config.js'), 'utf8');
  const browser = { window: {}, URL };
  vm.runInNewContext(source, browser);

  assert.equal(typeof browser.window.BMApiConfig.resolveApiBaseUrl, 'function');
  assert.equal(browser.window.BMApiConfig.resolveApiBaseUrl(new URL('https://student.example.edu/')),
    'https://student.example.edu/api');
});

test('apiRequest propagates caller cancellation without misreporting a timeout', async () => {
  const request = loadApiRequest((_url, options) => new Promise((_resolve, reject) => {
    const abort = () => {
      const error = new Error('aborted');
      error.name = 'AbortError';
      reject(error);
    };
    options.signal.addEventListener('abort', abort, { once: true });
    if (options.signal.aborted) abort();
  }));
  const controller = new AbortController();
  const pending = request('/experiment-creation/sessions/session-1/plan', {
    signal: controller.signal,
    timeoutMs: 5000,
  });

  controller.abort();

  await assert.rejects(pending, (error) => error.name === 'AbortError' && /已取消/.test(error.message));
});

test('apiRequest keeps its independent timeout when no caller cancellation occurs', async () => {
  const request = loadApiRequest((_url, options) => new Promise((_resolve, reject) => {
    options.signal.addEventListener('abort', () => {
      const error = new Error('aborted');
      error.name = 'AbortError';
      reject(error);
    }, { once: true });
  }));

  await assert.rejects(request('/slow', { timeoutMs: 5 }), /请求超时/);
});

test('creation dialog close only hides the panel while request and materialization safeguards remain', () => {
  const lifecycleStart = app.indexOf('function beginCreateRequest()');
  const lifecycleEnd = app.indexOf('\nfunction createApiRequest', lifecycleStart);
  assert.ok(lifecycleStart >= 0 && lifecycleEnd > lifecycleStart, 'creation request lifecycle helpers must exist');
  const context = { AbortController };
  vm.runInNewContext(
    `let activeCreateRequestController = null;${app.slice(lifecycleStart, lifecycleEnd)}\n` +
      'globalThis.lifecycle = { beginCreateRequest, finishCreateRequest, cancelCreateRequest, current: () => activeCreateRequestController };',
    context,
  );
  const first = context.lifecycle.beginCreateRequest();
  const replacement = context.lifecycle.beginCreateRequest();

  assert.equal(first.signal.aborted, true);
  assert.equal(context.lifecycle.current(), replacement);
  context.lifecycle.cancelCreateRequest();
  assert.equal(replacement.signal.aborted, true);
  const newer = context.lifecycle.beginCreateRequest();
  context.lifecycle.finishCreateRequest(replacement);
  assert.equal(context.lifecycle.current(), newer);

  const closeStart = app.indexOf('function closeCreateDialogPanel()');
  const closeEnd = app.indexOf('\nfunction updateCreateStep', closeStart);
  const closeSource = app.slice(closeStart, closeEnd);
  assert.match(closeSource, /createDialog\.classList\.remove\('is-open'\)/);
  assert.doesNotMatch(closeSource, /cancelCreateRequest\(\)|createDialogEpoch\s*\+\+/);
  assert.match(app, /createMaterializationInFlight = true;[\s\S]*await apiRequest\(`\/experiment-creation\/sessions\/\$\{creationSessionId\}\/confirm`/);
  assert.match(app, /const requestController = createPlanReady \? null : beginCreateRequest\(\)/);
});
