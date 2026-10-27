import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { test } from 'node:test';

const baseUrl = (process.env.BMHS_E2E_BASE_URL ?? '').replace(/\/+$/, '');
const experimentId = Number(process.env.BMHS_E2E_EXPERIMENT_ID);
const studentEmail = process.env.BMHS_E2E_STUDENT_EMAIL ?? '';
const studentPassword = process.env.BMHS_E2E_STUDENT_PASSWORD ?? '';
const teacherEmail = process.env.BMHS_E2E_TEACHER_EMAIL ?? '';
const teacherPassword = process.env.BMHS_E2E_TEACHER_PASSWORD ?? '';

function requiredEnvironment() {
  return baseUrl && Number.isSafeInteger(experimentId) && experimentId > 0
    && studentEmail && studentPassword && teacherEmail && teacherPassword;
}

async function login(email, password) {
  const response = await fetch(`${baseUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  assert.equal(response.status, 200, `login failed (HTTP ${response.status})`);
  const cookieHeaders = response.headers.getSetCookie?.() ?? [response.headers.get('set-cookie') ?? ''];
  const cookie = cookieHeaders.map((value) => value.split(';', 1)[0]).filter(Boolean).join('; ');
  assert.ok(cookie, 'login response did not set a session cookie');
  return cookie;
}

async function api(cookie, method, route, body) {
  const response = await fetch(`${baseUrl}${route}`, {
    method,
    headers: {
      cookie,
      ...(body === undefined ? {} : { 'content-type': 'application/json' }),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  const text = await response.text();
  let payload;
  try { payload = text ? JSON.parse(text) : null; } catch { payload = null; }
  assert.ok(response.ok, `${method} ${route} failed (HTTP ${response.status}, code ${payload?.code ?? 'unknown'})`);
  return payload;
}

function runGit(args, cwd, env) {
  return new Promise((resolve, reject) => {
    const child = spawn('git', args, { cwd, env, windowsHide: true, stdio: ['ignore', 'ignore', 'ignore'] });
    child.once('error', reject);
    child.once('close', (code) => code === 0
      ? resolve()
      : reject(new Error(`git ${args[0]} failed (exit ${code})`)));
  });
}

test('live BM-HS → Gitea push → AI node acceptance → teacher evidence', {
  skip: !requiredEnvironment() && 'set BMHS_E2E_* variables to target the isolated live E2E experiment',
  timeout: 300_000,
}, async () => {
  assert.ok(requiredEnvironment(), 'missing BMHS_E2E_* configuration');
  const studentCookie = await login(studentEmail, studentPassword);
  const teacherCookie = await login(teacherEmail, teacherPassword);
  const experiment = await api(studentCookie, 'GET', `/api/experiments/${experimentId}`);
  const nodes = experiment.nodes ?? [];
  assert.ok(nodes.length >= 2, 'the isolated experiment must include implementation and error-handling nodes');
  const selectedNodes = nodes.slice(-2);
  const run = await api(studentCookie, 'POST', `/api/student/experiments/${experimentId}/runs`);
  assert.equal(run.experimentId, experimentId);
  assert.equal(run.executionMode, 'git');

  const repository = await api(studentCookie, 'GET', `/api/student/runs/${run.runId}/repository`);
  assert.match(repository.cloneUrl, /^http:\/\//i, 'E2E currently targets the configured HTTP demo endpoint');
  const credentials = await api(studentCookie, 'POST', `/api/student/runs/${run.runId}/repository-credentials`);
  assert.ok(credentials.username && credentials.password, 'Gitea credentials are missing');

  const persistentWorkspace = process.env.BMHS_E2E_WORKSPACE;
  let directory;
  if (persistentWorkspace) {
    const workspaceRoot = path.resolve(persistentWorkspace);
    await mkdir(workspaceRoot, { recursive: true });
    directory = await mkdtemp(path.join(workspaceRoot, 'bmhs-git-acceptance-e2e-'));
  } else {
    directory = await mkdtemp(path.join(os.tmpdir(), 'bmhs-git-acceptance-e2e-'));
  }
  const askpassDirectory = await mkdtemp(path.join(os.tmpdir(), 'bmhs-git-askpass-'));
  try {
    if (persistentWorkspace) await mkdir(directory, { recursive: true });
    await writeFile(path.join(directory, 'main.py'), [
      'import sys',
      '',
      'try:',
      '    first = int(input("First integer: "))',
      '    second = int(input("Second integer: "))',
      '    print(first + second)',
      'except ValueError:',
      '    print("Error: enter valid integers.", file=sys.stderr)',
      '    sys.exit(1)',
      '',
      `# BM-HS E2E verification: ${new Date().toISOString()}`,
      '',
    ].join('\n'), 'utf8');
    await writeFile(path.join(directory, 'README.md'), [
      '# BM-HS 简易 Python 实验',
      '',
      '从标准输入读取两个整数并输出它们的和。非数字输入会打印错误提示并以非零状态退出。',
      '',
      `端到端验收提交：${new Date().toISOString()}`,
      '',
      '运行：`python main.py`',
      '',
    ].join('\n'), 'utf8');
    await runGit(['init', '--initial-branch=main'], directory, process.env);
    await runGit(['config', 'user.name', 'BM-HS E2E'], directory, process.env);
    await runGit(['config', 'user.email', 'bmhs-e2e@example.invalid'], directory, process.env);
    await runGit(['add', 'main.py', 'README.md'], directory, process.env);
    await runGit(['commit', '-m', `E2E acceptance ${new Date().toISOString()}`], directory, process.env);
    try {
      await runGit(['remote', 'set-url', 'origin', repository.cloneUrl], directory, process.env);
    } catch {
      await runGit(['remote', 'add', 'origin', repository.cloneUrl], directory, process.env);
    }

    const askpassPath = path.join(askpassDirectory, 'git-askpass.cmd');
    await writeFile(askpassPath,
      '@echo off\r\necho %* | findstr /I "Username" >nul\r\nif not errorlevel 1 (echo %BMHS_GIT_USERNAME%) else (echo %BMHS_GIT_PASSWORD%)\r\n',
      'utf8');
    const env = {
      ...process.env,
      GIT_ASKPASS: askpassPath,
      GIT_ASKPASS_REQUIRE: 'force',
      GIT_TERMINAL_PROMPT: '0',
      BMHS_GIT_USERNAME: credentials.username,
      BMHS_GIT_PASSWORD: credentials.password,
    };
    await runGit(['-c', 'credential.helper=', 'push', '--set-upstream', 'origin', 'main'], directory, env);
    const commitSha = await new Promise((resolve, reject) => {
      const child = spawn('git', ['rev-parse', 'HEAD'], { cwd: directory, windowsHide: true, stdio: ['ignore', 'pipe', 'ignore'] });
      let output = '';
      child.stdout.setEncoding('utf8').on('data', (chunk) => { output += chunk; });
      child.once('error', reject);
      child.once('close', (code) => code === 0 ? resolve(output.trim()) : reject(new Error('could not read commit SHA')));
    });
    assert.match(commitSha, /^[0-9a-f]{40}$/i);

    const submitted = await api(studentCookie, 'POST', `/api/student/runs/${run.runId}/submissions`, {
      commitSha,
      nodeIds: selectedNodes.map((node) => node.nodeId),
      description: 'Isolated live E2E: HTTP push, DeepSeek node check and teacher evidence',
    });
    const deadline = Date.now() + 180_000;
    let report;
    while (Date.now() < deadline) {
      report = await api(studentCookie, 'GET', `/api/student/submissions/${submitted.submissionId}`);
      if (['completed', 'failed'].includes(report.status)) break;
      await new Promise((resolve) => setTimeout(resolve, 2_000));
    }
    assert.equal(report?.status, 'completed', `AI acceptance did not complete (status ${report?.status ?? 'unknown'})`);
    assert.equal(report.results?.length, selectedNodes.length, 'AI report does not contain every selected node');
    assert.ok(report.results.every((item) => ['meets', 'partially_meets', 'does_not_meet', 'unable_to_verify'].includes(item.decision)));

    const teacherList = await api(teacherCookie, 'GET', `/api/teacher/runs/${run.runId}/submissions`);
    assert.ok(teacherList.some((item) => item.submissionId === submitted.submissionId), 'teacher cannot see the submitted commit');
    const teacherDetail = await api(teacherCookie, 'GET', `/api/teacher/submissions/${submitted.submissionId}`);
    assert.equal(teacherDetail.commitSha, commitSha);
    assert.ok(teacherDetail.diff?.some((file) => file.path === 'main.py'), 'teacher view lacks the changed file diff');
    assert.equal(teacherDetail.results?.length, selectedNodes.length);
  } finally {
    await rm(askpassDirectory, { recursive: true, force: true });
    if (!persistentWorkspace) await rm(directory, { recursive: true, force: true });
  }
});
