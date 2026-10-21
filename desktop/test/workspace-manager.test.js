'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { WorkspaceManager } = require('../src/assistant/WorkspaceManager');

async function fixture(t) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'bmhs-workspace-test-'));
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  return { root, manager: new WorkspaceManager(path.join(root, 'workspaces')) };
}

test('creates an actually empty workspace and stable random association id', async (t) => {
  const { manager } = await fixture(t);
  const created = await manager.create('42');
  assert.equal(created.status, 'running');
  assert.match(created.workspaceId, /^[0-9a-f-]{36}$/i);
  assert.deepEqual(await manager.list('42'), []);
  assert.deepEqual(await manager.create('42'), created);
});

test('accepts server-generated UUID association but rejects mismatched retry', async (t) => {
  const { manager } = await fixture(t);
  const id = '9e7ea9bc-e4f4-4ed7-a0c5-1e5a4e6c025a';
  assert.equal((await manager.create('42', id)).workspaceId, id);
  await assert.rejects(manager.create('42', '11111111-1111-4111-8111-111111111111'), /已绑定/);
});

test('supports bounded file reads/writes and refuses path traversal', async (t) => {
  const { manager } = await fixture(t);
  await manager.create('42');
  const saved = await manager.write('42', 'src/hello.py', 'print("hello")');
  assert.equal(saved.sizeBytes, 14);
  assert.equal((await manager.read('42', 'src/hello.py')).content, 'print("hello")');
  await assert.rejects(manager.write('42', 'src/hello.py', 'changed', '0'.repeat(64)), /已在其他位置发生变化/);
  await manager.write('42', 'src/hello.py', 'updated', saved.contentHash);
  assert.equal((await manager.read('42', 'src/hello.py')).content, 'updated');
  await assert.rejects(manager.read('42', '../outside.txt'), /路径/);
  await assert.rejects(manager.write('42', 'C:/outside.txt', 'no'), /路径/);
  await assert.rejects(manager.write('42', 'src/large.py', 'x'.repeat(2_000_001)), /大小上限/);
});

test('refuses symlink escape outside the run directory', async (t) => {
  const { root, manager } = await fixture(t);
  await manager.create('42');
  const outside = path.join(root, 'outside');
  await fs.mkdir(outside);
  await fs.writeFile(path.join(outside, 'secret.txt'), 'secret');
  await fs.symlink(outside, path.join(await manager.getDirectory('42'), 'escape'), 'junction');
  await assert.rejects(manager.read('42', 'escape/secret.txt'));
  await assert.rejects(manager.write('42', 'escape/new.txt', 'no'));
});
