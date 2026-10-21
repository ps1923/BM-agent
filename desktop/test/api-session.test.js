'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { ApiSession } = require('../src/platform/ApiSession');

test('allows only configured HTTPS API base URLs', () => {
  const api = new ApiSession({ session: {}, configuredBaseUrl: 'https://school.example/bm-agent/api/' });
  assert.deepEqual(api.getConfiguration(), { configured: true, origin: 'https://school.example' });
  for (const value of ['http://school.example/api', 'https://u:p@school.example/api', 'https://school.example/api?next=x', 'https://school.example/other']) {
    assert.throws(() => api.setBaseUrl(value));
  }
});

test('proxies cookie-backed requests only within the configured API path', async () => {
  const calls = [];
  const api = new ApiSession({
    configuredBaseUrl: 'https://school.example/bm-agent/api',
    session: { fetch: async (url, options) => { calls.push({ url, options }); return { ok: true, status: 200, json: async () => ({ user: { role: 'student' } }) }; } },
  });
  const result = await api.request({ path: '/auth/login', method: 'POST', body: JSON.stringify({ email: 'x', password: 'y' }) });
  assert.equal(result.payload.user.role, 'student');
  assert.match(calls[0].url, /^https:\/\/school\.example\/bm-agent\/api\/auth\/login$/);
  assert.equal(calls[0].options.credentials, 'include');
  await assert.rejects(api.request({ path: '//attacker.example/x' }));
  await assert.rejects(api.request({ path: '/../../other' }));
  await assert.rejects(api.request({ path: '/auth/login', headers: { Cookie: 'stolen' } }));
});
