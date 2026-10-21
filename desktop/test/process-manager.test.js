'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { parseCommandLine, MAX_OUTPUT_BYTES, MAX_COMMAND_SECONDS, MAX_CONCURRENT_PROCESSES } = require('../src/assistant/ProcessManager');

test('parses quoted arguments without invoking a shell', () => {
  assert.deepEqual(parseCommandLine('python -m django startproject "my app"'), ['python', '-m', 'django', 'startproject', 'my app']);
});

test('rejects shell composition and malformed input', () => {
  for (const command of ['', 'python a.py && git status', 'python a.py | more', 'python a.py > out', 'python "unfinished']) {
    assert.throws(() => parseCommandLine(command));
  }
});

test('declares conservative command, output, timeout and concurrency bounds', () => {
  assert.equal(MAX_OUTPUT_BYTES, 256_000);
  assert.equal(MAX_COMMAND_SECONDS, 120);
  assert.equal(MAX_CONCURRENT_PROCESSES, 4);
});
