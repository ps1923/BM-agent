'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { GuidanceService, sanitizeContext } = require('../src/assistant/GuidanceService');

test('removes credential files and redacts secret-like values from allowed context', () => {
  const context = sanitizeContext([
    { path: '.env', content: 'DEEPSEEK_API_KEY=secret' },
    { path: 'id_rsa', content: 'private key' },
    { path: 'settings.py', content: 'password = hunter2\nkey = sk-1234567890123456789012345' },
  ], 'Authorization: Bearer abc');
  assert.deepEqual(context.files.map((file) => file.path), ['settings.py']);
  assert.doesNotMatch(context.files[0].content, /hunter2|sk-123456/);
  assert.match(context.files[0].content, /\[已脱敏\]/);
  assert.doesNotMatch(context.terminalOutput, /abc/);
});

test('requires explicit context confirmation and keeps model key in service request only', async () => {
  const calls = [];
  const service = new GuidanceService({
    secretStore: { getDeepSeekKey: async () => 'sk-test-key-123456789012345678901234' },
    fetchImpl: async (url, options) => { calls.push({ url, options }); return { ok: true, json: async () => ({ choices: [{ message: { content: '先检查路由配置。' } }] }) }; },
  });
  await assert.rejects(service.ask({ confirmed: false, question: '为什么启动失败' }), /确认/);
  const result = await service.ask({ confirmed: true, question: '为什么启动失败', files: [{ path: 'manage.py', content: 'print(1)' }] });
  assert.equal(result.answer, '先检查路由配置。');
  assert.match(calls[0].options.headers.Authorization, /^Bearer sk-/);
  assert.doesNotMatch(JSON.stringify(result), /sk-test-key/);
});
