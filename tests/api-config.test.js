const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const { resolveApiBaseUrl } = require('../api-config.js');

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
