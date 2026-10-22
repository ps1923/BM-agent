import test from 'node:test';
import { readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
import { defineTool } from '@deepseek-ai/dsh-tools';
import { BmhsApiClient } from '../src/api.js';
import { apply } from '../src/index.js';
import { createBmhsTools, validateWorkspaceRelativePath } from '../src/tools.js';

function harness({ approved = true, outside = false } = {}) {
  const calls = [];
  const toolDefs = [];
  let approvalGranted = approved;
  const ctx = {
    get: (name) => name === 'approval' ? {
      request: async (request) => {
        calls.push({ type: 'approval', request });
        return approvalGranted ? 'allowed-once' : 'rejected';
      },
    } : undefined,
    fs: {
      resolve: async (path) => ({ displayPath: path, key: path === '.' ? 'workspace' : `workspace/${path}` }),
      contains: (root, target) => !outside && (target.key === root.key || target.key.startsWith(`${root.key}/`)),
      stat: async (target) => target.key === 'workspace' ? { type: 'directory' } : { type: 'file', size: Buffer.byteLength(files[target.key.slice('workspace/'.length)] ?? '') },
      streamText: async function* (target) { yield files[target.key.slice('workspace/'.length)] ?? ''; },
    },
    tools: { register: (definition) => toolDefs.push(definition) },
  };
  const api = { request: async (path, options) => { calls.push({ type: 'api', path, options }); return path === '/api/plugin/runs' ? { runId: 'run-1' } : { ok: true }; } };
  const definitions = createBmhsTools({ ctx, api, workspaceId: 'opaque-ws-123456789', defineTool: (definition) => definition });
  const agent = { id: 'session-a' };
  const exec = { agent, callId: 'call-1', signal: new AbortController().signal };
  const invoke = async (name, args = {}, execution = exec) => definitions.find((item) => item.name === name).execute(args, execution);
  return { calls, definitions, invoke, exec, setApproval: (value) => { approvalGranted = value; } };
}

const files = {
  'src/hello.py': 'print("hello")\n',
  '.env': 'not-a-real-secret\n',
};

test('DSH package is declared as an installable Cordis bundle with its patch included', async () => {
  const manifest = JSON.parse(await readFile(new URL('../package.json', import.meta.url), 'utf8'));
  assert.equal(manifest.dsh?.bundle?.patch, './cordis.patch.yml');
  assert.ok(manifest.files.includes('cordis.patch.yml'));
});

test('registers the expected tools without student identity arguments', () => {
  const { definitions } = harness();
  assert.deepEqual(definitions.map(({ name }) => name), [
    'bmhs_list_experiments', 'bmhs_attach_experiment_run', 'bmhs_get_learning_path',
    'bmhs_update_node_progress', 'bmhs_update_task_progress', 'bmhs_search_bug_memory',
    'bmhs_submit_bug_case', 'bmhs_submit_code_snapshot',
  ]);
  for (const definition of definitions) {
    assert.equal(Object.hasOwn(definition.parameters, 'studentId'), false);
    assert.equal(Object.hasOwn(definition.parameters, 'userId'), false);
  }
});

test('every tool definition is accepted by the pinned DSH defineTool implementation', () => {
  const { definitions } = harness();
  const nativeDefinitions = definitions.map((definition) => defineTool(definition));
  assert.equal(nativeDefinitions.length, 8);
  assert.deepEqual(nativeDefinitions.map(({ name }) => name), definitions.map(({ name }) => name));
});

test('Cordis plugin entry registers all model tools with the pinned DSH runtime API', () => {
  const prior = {
    baseUrl: process.env.BMHS_API_BASE_URL,
    credential: process.env.BMHS_DSH_CREDENTIAL,
    workspace: process.env.BMHS_DSH_WORKSPACE_ID,
  };
  process.env.BMHS_API_BASE_URL = 'https://bmhs.example.test';
  process.env.BMHS_DSH_CREDENTIAL = 'short-lived-credential-value';
  process.env.BMHS_DSH_WORKSPACE_ID = 'opaque-ws-123456789';
  const registrations = [];
  const ctx = {
    fs: { resolve() {}, streamText() {} },
    tools: { register: (definition) => registrations.push(definition) },
  };
  try {
    apply(ctx);
    assert.equal(registrations.length, 8);
    assert.ok(registrations.every((definition) => definition.output && definition.execute));
  } finally {
    if (prior.baseUrl === undefined) delete process.env.BMHS_API_BASE_URL;
    else process.env.BMHS_API_BASE_URL = prior.baseUrl;
    if (prior.credential === undefined) delete process.env.BMHS_DSH_CREDENTIAL;
    else process.env.BMHS_DSH_CREDENTIAL = prior.credential;
    if (prior.workspace === undefined) delete process.env.BMHS_DSH_WORKSPACE_ID;
    else process.env.BMHS_DSH_WORKSPACE_ID = prior.workspace;
  }
});

test('attaches current DSH session using opaque workspace binding and no student id', async () => {
  const { calls, invoke } = harness();
  await invoke('bmhs_attach_experiment_run', { experimentId: 'exp-3' });
  const request = calls.find((call) => call.type === 'api');
  assert.equal(request.path, '/api/plugin/runs');
  assert.deepEqual(request.options.body, {
    experimentId: 'exp-3', sessionRef: 'fa57a52dbf08190218529730a3e99db6946c6c29220fb6e0551e21598b0b05db',
    workspaceRef: 'opaque-ws-123456789',
  });
  assert.equal('studentId' in request.options.body, false);
});

test('binds runs per DSH agent session', async () => {
  const { invoke } = harness();
  await assert.rejects(invoke('bmhs_get_learning_path', {}, { agent: { id: 'session-b' } }), /Attach this DSH session/);
});

test('all mutating actions fail closed when student rejects approval', async () => {
  const { calls, invoke } = harness({ approved: false });
  await assert.rejects(invoke('bmhs_attach_experiment_run', { experimentId: 'exp-3' }), /did not approve/);
  assert.equal(calls.some((call) => call.type === 'api'), false);
});

test('snapshot submission is not sent if the student rejects its one-shot approval', async () => {
  const { calls, invoke, setApproval } = harness();
  await invoke('bmhs_attach_experiment_run', { experimentId: 'exp-3' });
  calls.length = 0;
  setApproval(false);
  await assert.rejects(invoke('bmhs_submit_code_snapshot', { paths: ['src/hello.py'] }), /did not approve/);
  assert.equal(calls.some((call) => call.type === 'api'), false);
});

test('progress mutation requires a one-shot student approval', async () => {
  const { calls, invoke } = harness();
  await invoke('bmhs_attach_experiment_run', { experimentId: 'exp-3' });
  calls.length = 0;
  await invoke('bmhs_update_node_progress', { nodeId: 'node-2', status: 'completed', note: 'checked' });
  assert.equal(calls[0].type, 'approval');
  assert.equal(calls[1].path, '/api/plugin/runs/run-1/nodes/node-2');
});

test('code submission reads only selected workspace-relative files and hashes them', async () => {
  const { calls, invoke } = harness();
  await invoke('bmhs_attach_experiment_run', { experimentId: 'exp-3' });
  calls.length = 0;
  const result = await invoke('bmhs_submit_code_snapshot', { paths: ['src/hello.py'], nodeId: 'node-2' });
  const submission = calls.find((call) => call.type === 'api');
  const approval = calls.find((call) => call.type === 'approval');
  assert.equal(submission.path, '/api/plugin/runs/run-1/code-submissions');
  assert.deepEqual(submission.options.body.files.map(({ path }) => path), ['src/hello.py']);
  assert.match(submission.options.body.files[0].sha256, /^[a-f0-9]{64}$/);
  assert.match(approval.request.reason, /src\/hello\.py/);
  assert.equal(JSON.stringify(result).includes('print("hello")'), false);
  assert.equal('studentId' in submission.options.body, false);
});

test('Bug search uses the attached experiment scope rather than course or student scope', async () => {
  const { calls, invoke } = harness();
  await invoke('bmhs_attach_experiment_run', { experimentId: 'exp-3' });
  calls.length = 0;
  await invoke('bmhs_search_bug_memory', { query: 'Django import error' });
  assert.equal(calls[0].path, '/api/plugin/experiments/exp-3/bug-memory/search');
  assert.deepEqual(calls[0].options.body, { runId: 'run-1', query: 'Django import error', limit: 5 });
});

test('code submission rejects workspace escape and secret files before network call', async () => {
  const { calls, invoke } = harness();
  await invoke('bmhs_attach_experiment_run', { experimentId: 'exp-3' });
  calls.length = 0;
  await assert.rejects(invoke('bmhs_submit_code_snapshot', { paths: ['../outside.py'] }), /traversal/);
  await assert.rejects(invoke('bmhs_submit_code_snapshot', { paths: ['.env'] }), /Credential/);
  assert.equal(calls.some((call) => call.type === 'api'), false);
});

test('code submission rejects resolved targets outside DSH workspace', async () => {
  const { calls, invoke } = harness({ outside: true });
  await invoke('bmhs_attach_experiment_run', { experimentId: 'exp-3' });
  calls.length = 0;
  await assert.rejects(invoke('bmhs_submit_code_snapshot', { paths: ['src/hello.py'] }), /outside the current DSH workspace/);
  assert.equal(calls.some((call) => call.type === 'api'), false);
});

test('path validator rejects absolute, traversal, credential and private-key paths', () => {
  for (const path of ['/tmp/a.py', 'C:\\work\\a.py', '../a.py', 'src/../a.py', '.env.local', 'keys/private.pem']) {
    assert.throws(() => validateWorkspaceRelativePath(path));
  }
  assert.equal(validateWorkspaceRelativePath('src/app.py'), 'src/app.py');
});

test('API client accepts HTTPS and loopback only and sends bearer without identity fields', async () => {
  let received;
  const client = new BmhsApiClient({
    baseUrl: 'https://bmhs.example.test', credential: 'short-lived-credential-value',
    fetchImpl: async (url, options) => {
      received = { url, options };
      return { ok: true, status: 200, headers: { get: () => 'application/json' }, json: async () => ({ ok: true }) };
    },
  });
  await client.request('/api/plugin/experiments');
  assert.equal(received.options.headers.Authorization, 'Bearer short-lived-credential-value');
  assert.equal(received.options.body, undefined);
  assert.throws(() => new BmhsApiClient({ baseUrl: 'http://bmhs.example.test', credential: 'short-lived-credential-value' }), /HTTPS/);
  assert.equal(new BmhsApiClient({ baseUrl: 'http://bmhs.example.test', credential: 'short-lived-credential-value', allowInsecureHttp: true }).baseUrl,
    'http://bmhs.example.test');
});

test('API error never echoes response body', async () => {
  const client = new BmhsApiClient({
    baseUrl: 'http://127.0.0.1:8080', credential: 'short-lived-credential-value',
    fetchImpl: async () => ({ ok: false, status: 401, text: async () => 'bearer-secret-value' }),
  });
  await assert.rejects(client.request('/api/plugin/experiments'), (error) => {
    assert.match(error.message, /401/);
    assert.doesNotMatch(error.message, /bearer-secret/);
    return true;
  });
});

test('API client rotates short-lived credentials once after expiry and retries the request', async () => {
  const calls = [];
  const client = new BmhsApiClient({
    baseUrl: 'https://bmhs.example.test',
    credential: 'a'.repeat(43),
    refreshCredential: 'b'.repeat(43),
    fetchImpl: async (url, options) => {
      calls.push({ url, options });
      if (url.endsWith('/api/plugin/auth/refresh')) {
        return {
          ok: true, status: 200, headers: { get: () => 'application/json' },
          json: async () => ({ credential: 'c'.repeat(43), refreshCredential: 'd'.repeat(43) }),
        };
      }
      if (options.headers.Authorization === `Bearer ${'a'.repeat(43)}`) {
        return { ok: false, status: 401, headers: { get: () => 'application/json' } };
      }
      return {
        ok: true, status: 200, headers: { get: () => 'application/json' }, json: async () => ({ ok: true }),
      };
    },
  });

  assert.deepEqual(await client.request('/api/plugin/experiments'), { ok: true });
  assert.equal(calls.length, 3);
  assert.equal(calls[1].options.headers.Authorization, undefined);
  assert.match(calls[1].options.body, /"refreshCredential":"b{43}"/);
  assert.equal(calls[2].options.headers.Authorization, `Bearer ${'c'.repeat(43)}`);
  assert.equal(client.refreshCredential, 'd'.repeat(43));
});

test('concurrent expired requests share one rotating refresh credential exchange', async () => {
  let refreshCalls = 0;
  const client = new BmhsApiClient({
    baseUrl: 'https://bmhs.example.test', credential: 'a'.repeat(43), refreshCredential: 'b'.repeat(43),
    fetchImpl: async (url, options) => {
      if (url.endsWith('/api/plugin/auth/refresh')) {
        refreshCalls += 1;
        await new Promise((resolve) => setTimeout(resolve, 5));
        return { ok: true, status: 200, headers: { get: () => 'application/json' },
          json: async () => ({ credential: 'c'.repeat(43), refreshCredential: 'd'.repeat(43) }) };
      }
      if (options.headers.Authorization === `Bearer ${'a'.repeat(43)}`) {
        await new Promise((resolve) => setTimeout(resolve, 1));
        return { ok: false, status: 401, headers: { get: () => 'application/json' } };
      }
      return { ok: true, status: 200, headers: { get: () => 'application/json' }, json: async () => ({ ok: true }) };
    },
  });

  await Promise.all([client.request('/api/plugin/experiments'), client.request('/api/plugin/experiments')]);
  assert.equal(refreshCalls, 1);
});
