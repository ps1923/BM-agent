'use strict';

const fs = require('node:fs/promises');
const path = require('node:path');

class SecretStore {
  constructor({ safeStorage, filePath }) {
    this.safeStorage = safeStorage;
    this.filePath = filePath;
  }

  async setDeepSeekKey(key) {
    if (typeof key !== 'string' || !/^sk-[A-Za-z0-9_-]{20,}$/.test(key)) throw new Error('DeepSeek API 密钥格式无效。');
    if (!this.safeStorage.isEncryptionAvailable()) throw new Error('此设备无法启用系统安全存储，未保存密钥。');
    const encrypted = this.safeStorage.encryptString(key);
    await fs.mkdir(path.dirname(this.filePath), { recursive: true });
    const temporary = `${this.filePath}.tmp`;
    await fs.writeFile(temporary, encrypted, { mode: 0o600 });
    await fs.rename(temporary, this.filePath);
    return { saved: true };
  }

  async getDeepSeekKey() {
    if (!this.safeStorage.isEncryptionAvailable()) throw new Error('系统安全存储不可用。');
    try { return this.safeStorage.decryptString(await fs.readFile(this.filePath)); }
    catch (error) { if (error.code === 'ENOENT') return null; throw error; }
  }

  async hasDeepSeekKey() {
    try { await fs.access(this.filePath); return true; }
    catch (error) { if (error.code === 'ENOENT') return false; throw error; }
  }
}

module.exports = { SecretStore };
