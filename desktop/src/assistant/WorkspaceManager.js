'use strict';

const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');

const RUN_ID_PATTERN = /^[1-9]\d{0,17}$/;
const MAX_FILE_BYTES = 2_000_000;
const MAX_FILES = 500;

class WorkspaceManager {
  constructor(rootDirectory) {
    this.rootDirectory = path.resolve(rootDirectory);
  }

  async create(runId, requestedWorkspaceId = null) {
    this.#validateRunId(runId);
    if (requestedWorkspaceId !== null && !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(requestedWorkspaceId)) throw new Error('工作区关联标识无效。');
    await fs.mkdir(this.rootDirectory, { recursive: true });
    const workspaceId = requestedWorkspaceId || crypto.randomUUID();
    const directory = this.#directory(runId);
    try {
      await fs.mkdir(directory, { recursive: false });
    } catch (error) {
      if (error.code !== 'EEXIST') throw error;
      const state = await fs.lstat(directory);
      if (!state.isDirectory() || state.isSymbolicLink()) throw new Error('工作区目录状态不安全。');
      const existingId = await this.#readWorkspaceId(runId);
      if (requestedWorkspaceId && existingId !== requestedWorkspaceId) throw new Error('此运行实例已绑定其他本地工作区。');
      return { runId: String(runId), workspaceId: existingId, status: 'running' };
    }
    await fs.writeFile(path.join(directory, '.bmhs-workspace.json'), JSON.stringify({ workspaceId, createdAt: new Date().toISOString() }), { flag: 'wx', mode: 0o600 });
    return { runId: String(runId), workspaceId, status: 'running' };
  }

  async list(runId) {
    const directory = await this.#existingDirectory(runId);
    const result = [];
    const visit = async (current, prefix = '') => {
      const entries = await fs.readdir(current, { withFileTypes: true });
      for (const entry of entries) {
        if (entry.name === '.bmhs-workspace.json' || entry.name === '.git') continue;
        if (entry.isSymbolicLink()) throw new Error('工作区不允许符号链接。');
        const relativePath = prefix ? `${prefix}/${entry.name}` : entry.name;
        if (entry.isDirectory()) await visit(path.join(current, entry.name), relativePath);
        else if (entry.isFile()) {
          const info = await fs.stat(path.join(current, entry.name));
          result.push({ path: relativePath, displayName: entry.name, sizeBytes: info.size, contentHash: await this.#hash(path.join(current, entry.name)) });
          if (result.length > MAX_FILES) throw new Error('工作区文件数量超过上限。');
        }
      }
    };
    await visit(directory);
    return result;
  }

  async read(runId, relativePath) {
    const file = await this.#resolveFile(runId, relativePath);
    const info = await fs.stat(file);
    if (info.size > MAX_FILE_BYTES) throw new Error('文件超过读取大小上限。');
    return { path: relativePath, content: await fs.readFile(file, 'utf8'), contentHash: await this.#hash(file) };
  }

  async createDirectory(runId, relativePath) {
    const root = await this.#existingDirectory(runId);
    if (typeof relativePath !== 'string' || !relativePath.trim() || relativePath.includes('\\') || path.posix.isAbsolute(relativePath)) throw new Error('文件夹路径无效。');
    const segments = relativePath.split('/');
    if (segments.some((segment) => !segment || segment === '.' || segment === '..' || segment.includes(':') || segment.startsWith('.bmhs-'))) throw new Error('文件夹路径越界或保留。');
    const target = path.resolve(root, ...segments);
    if (target === root || !target.startsWith(`${root}${path.sep}`)) throw new Error('文件夹路径越界。');
    await this.#assertNoLinks(path.dirname(target));
    await fs.mkdir(target, { recursive: false });
    await this.#assertNoLinks(target);
    return { path: relativePath };
  }

  async write(runId, relativePath, content, expectedHash = null) {
    if (typeof content !== 'string' || Buffer.byteLength(content, 'utf8') > MAX_FILE_BYTES) throw new Error('文件内容无效或超过大小上限。');
    const file = await this.#resolveFile(runId, relativePath, { allowMissingLeaf: true });
    await fs.mkdir(path.dirname(file), { recursive: true });
    await this.#assertNoLinks(path.dirname(file));
    let currentHash = null;
    try {
      const state = await fs.lstat(file);
      if (state.isSymbolicLink() || !state.isFile()) throw new Error('目标不是普通文件。');
      currentHash = await this.#hash(file);
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
    }
    if (expectedHash !== null && (typeof expectedHash !== 'string' || currentHash !== expectedHash)) {
      throw new Error('文件已在其他位置发生变化，请重新读取后再保存。');
    }
    const temporary = `${file}.${crypto.randomUUID()}.tmp`;
    try {
      await fs.writeFile(temporary, content, { flag: 'wx', mode: 0o600 });
      await fs.rename(temporary, file);
    } finally {
      await fs.rm(temporary, { force: true }).catch(() => {});
    }
    return { path: relativePath, sizeBytes: Buffer.byteLength(content, 'utf8'), contentHash: await this.#hash(file) };
  }

  async getDirectory(runId) {
    return this.#existingDirectory(runId);
  }

  #validateRunId(runId) {
    if (!RUN_ID_PATTERN.test(String(runId))) throw new Error('运行实例标识无效。');
  }

  #directory(runId) {
    this.#validateRunId(runId);
    return path.join(this.rootDirectory, `run-${runId}`);
  }

  async #readWorkspaceId(runId) {
    const data = JSON.parse(await fs.readFile(path.join(this.#directory(runId), '.bmhs-workspace.json'), 'utf8'));
    if (typeof data.workspaceId !== 'string') throw new Error('工作区元数据无效。');
    return data.workspaceId;
  }

  async #existingDirectory(runId) {
    const directory = this.#directory(runId);
    const state = await fs.lstat(directory);
    if (!state.isDirectory() || state.isSymbolicLink()) throw new Error('工作区目录不存在或不安全。');
    await this.#assertNoLinks(directory);
    await this.#readWorkspaceId(runId);
    return directory;
  }

  async #resolveFile(runId, relativePath, { allowMissingLeaf = false } = {}) {
    const directory = await this.#existingDirectory(runId);
    if (typeof relativePath !== 'string' || !relativePath.trim() || relativePath.includes('\\') || path.posix.isAbsolute(relativePath)) throw new Error('文件路径无效。');
    const segments = relativePath.split('/');
    if (segments.some((segment) => !segment || segment === '.' || segment === '..' || segment.includes(':') || segment.startsWith('.bmhs-'))) throw new Error('文件路径越界或保留。');
    const file = path.resolve(directory, ...segments);
    if (file !== directory && !file.startsWith(`${directory}${path.sep}`)) throw new Error('文件路径越界。');
    await this.#assertNoLinks(path.dirname(file));
    try {
      const state = await fs.lstat(file);
      if (state.isSymbolicLink() || !state.isFile()) throw new Error('目标不是普通文件。');
    } catch (error) {
      if (!allowMissingLeaf || error.code !== 'ENOENT') throw error;
    }
    return file;
  }

  async #assertNoLinks(directory) {
    const rootState = await fs.lstat(this.rootDirectory);
    if (!rootState.isDirectory() || rootState.isSymbolicLink()) throw new Error('本地工作区根目录不安全。');
    const relative = path.relative(this.rootDirectory, directory);
    if (relative === '..' || relative.startsWith(`..${path.sep}`) || path.isAbsolute(relative)) {
      throw new Error('工作区路径越界。');
    }
    let current = this.rootDirectory;
    for (const segment of relative.split(path.sep).filter(Boolean)) {
      current = path.join(current, segment);
      try {
        const state = await fs.lstat(current);
        if (state.isSymbolicLink()) throw new Error('工作区不允许符号链接。');
      } catch (error) {
        if (error.code !== 'ENOENT') throw error;
      }
    }
  }

  async #hash(file) {
    return crypto.createHash('sha256').update(await fs.readFile(file)).digest('hex');
  }
}

module.exports = { WorkspaceManager, MAX_FILE_BYTES, MAX_FILES };
