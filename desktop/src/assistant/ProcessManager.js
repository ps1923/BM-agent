'use strict';

const { spawn } = require('node:child_process');
const path = require('node:path');
const crypto = require('node:crypto');

const MAX_OUTPUT_BYTES = 256_000;
const MAX_COMMAND_SECONDS = 120;
const MAX_CONCURRENT_PROCESSES = 4;
const ALLOWED_COMMANDS = new Set(['python', 'python.exe', 'python3', 'git', 'git.exe']);

function parseCommandLine(input) {
  if (typeof input !== 'string' || !input.trim() || input.length > 4000) throw new Error('命令为空或超过长度限制。');
  if (/[;&|<>\r\n`]/.test(input)) throw new Error('不支持 Shell 管道、重定向或多命令语法，请逐条运行命令。');
  const args = [];
  let token = '';
  let quote = null;
  let escaped = false;
  for (const char of input.trim()) {
    if (escaped) { token += char; escaped = false; continue; }
    if (char === '\\' && quote === '"') { escaped = true; continue; }
    if (quote) { if (char === quote) quote = null; else token += char; continue; }
    if (char === '"' || char === "'") { quote = char; continue; }
    if (/\s/.test(char)) { if (token) { args.push(token); token = ''; } continue; }
    token += char;
  }
  if (quote || escaped) throw new Error('命令引号不匹配。');
  if (token) args.push(token);
  if (!args.length || args.length > 64) throw new Error('命令参数数量无效。');
  return args;
}

class ProcessManager {
  constructor({ workspaceManager, runtimeDirectory, platform = process.platform }) {
    this.workspaceManager = workspaceManager;
    this.runtimeDirectory = runtimeDirectory;
    this.platform = platform;
    this.processes = new Map();
  }

  async runCommand(runId, command) {
    const parsed = parseCommandLine(command);
    const child = await this.#spawn(runId, parsed);
    return new Promise((resolve, reject) => {
      let output = '';
      let outputBytes = 0;
      let timedOut = false;
      const append = (chunk) => {
        const text = chunk.toString('utf8');
        const remaining = MAX_OUTPUT_BYTES - outputBytes;
        if (remaining <= 0) return;
        const buffer = Buffer.from(text, 'utf8').subarray(0, remaining);
        output += buffer.toString('utf8');
        outputBytes += buffer.length;
      };
      const timeout = setTimeout(() => { timedOut = true; void this.#terminate(child); }, MAX_COMMAND_SECONDS * 1000);
      child.stdout.on('data', append);
      child.stderr.on('data', append);
      child.once('error', (error) => { clearTimeout(timeout); reject(error); });
      child.once('close', (code) => {
        clearTimeout(timeout);
        resolve({ exitCode: code, timedOut, output, truncated: outputBytes >= MAX_OUTPUT_BYTES });
      });
    });
  }

  async startProcess(runId, command) {
    const parsed = parseCommandLine(command);
    const child = await this.#spawn(runId, parsed);
    const id = crypto.randomUUID();
    const processInfo = { id, runId: String(runId), child, output: '', outputBytes: 0, startedAt: new Date().toISOString(), exitCode: null, timedOut: false };
    const append = (chunk) => {
      const remaining = MAX_OUTPUT_BYTES - processInfo.outputBytes;
      if (remaining <= 0) return;
      const buffer = Buffer.from(chunk).subarray(0, remaining);
      processInfo.output += buffer.toString('utf8');
      processInfo.outputBytes += buffer.length;
    };
    child.stdout.on('data', append);
    child.stderr.on('data', append);
    child.once('error', (error) => { processInfo.error = error.message; });
    child.once('close', (code) => { processInfo.exitCode = code; processInfo.endedAt = new Date().toISOString(); });
    processInfo.timeout = setTimeout(() => { processInfo.timedOut = true; void this.stopProcess(id); }, MAX_COMMAND_SECONDS * 60 * 1000);
    this.processes.set(id, processInfo);
    return { processId: id, pid: child.pid, status: 'running' };
  }

  getProcess(id) {
    const item = this.processes.get(id);
    if (!item) throw new Error('进程不存在或已过期。');
    return { processId: item.id, runId: item.runId, pid: item.child.pid, status: item.exitCode === null ? 'running' : 'stopped', exitCode: item.exitCode, timedOut: item.timedOut, output: item.output, truncated: item.outputBytes >= MAX_OUTPUT_BYTES, startedAt: item.startedAt, endedAt: item.endedAt || null, error: item.error || null };
  }

  async stopProcess(id) {
    const item = this.processes.get(id);
    if (!item) throw new Error('进程不存在或已过期。');
    clearTimeout(item.timeout);
    if (item.exitCode === null) await this.#terminate(item.child);
    return this.getProcess(id);
  }

  async stopAll() {
    await Promise.all([...this.processes.keys()].map((id) => this.stopProcess(id).catch(() => {})));
  }

  async #spawn(runId, parsed) {
    const workspace = await this.workspaceManager.getDirectory(runId);
    const executableName = path.basename(parsed[0]).toLowerCase();
    if (!ALLOWED_COMMANDS.has(executableName)) throw new Error('首版仅允许使用随应用提供的 Python 与 Git 命令。');
    if (this.processes.size >= MAX_CONCURRENT_PROCESSES) throw new Error('同时运行的命令/进程已达上限。');
    const exe = executableName.startsWith('git')
      ? path.join(this.runtimeDirectory, 'git', 'cmd', 'git.exe')
      : path.join(this.runtimeDirectory, 'python', 'python.exe');
    const child = spawn(exe, parsed.slice(1), {
      cwd: workspace,
      env: { SYSTEMROOT: process.env.SYSTEMROOT || 'C:\\Windows', WINDIR: process.env.WINDIR || 'C:\\Windows', TEMP: process.env.TEMP || 'C:\\Windows\\Temp', TMP: process.env.TMP || 'C:\\Windows\\Temp', PATH: path.dirname(exe), PYTHONNOUSERSITE: '1', PYTHONDONTWRITEBYTECODE: '1' },
      windowsHide: true,
      windowsVerbatimArguments: false,
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    return child;
  }

  async #terminate(child) {
    if (!child.pid || child.exitCode !== null) return;
    if (this.platform === 'win32') {
      const killer = spawn(path.join(process.env.SYSTEMROOT || 'C:\\Windows', 'System32', 'taskkill.exe'), ['/PID', String(child.pid), '/T', '/F'], { windowsHide: true, stdio: 'ignore' });
      await new Promise((resolve) => { killer.once('error', resolve); killer.once('close', resolve); });
    } else {
      try { process.kill(-child.pid, 'SIGTERM'); } catch { try { child.kill('SIGTERM'); } catch {} }
    }
  }
}

module.exports = { ProcessManager, parseCommandLine, MAX_OUTPUT_BYTES, MAX_COMMAND_SECONDS, MAX_CONCURRENT_PROCESSES };
