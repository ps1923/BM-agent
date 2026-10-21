'use strict';

const FORBIDDEN_HEADERS = new Set(['authorization', 'cookie', 'host', 'origin', 'referer', 'set-cookie']);

class ApiSession {
  constructor({ session, configuredBaseUrl = '' }) {
    this.session = session;
    this.baseUrl = '';
    if (configuredBaseUrl) this.setBaseUrl(configuredBaseUrl);
  }

  setBaseUrl(value) {
    let url;
    try { url = new URL(value); } catch { throw new Error('平台地址无效。'); }
    if (url.protocol !== 'https:' || url.username || url.password || url.search || url.hash || !url.pathname.replace(/\/$/, '').endsWith('/api')) {
      throw new Error('平台地址必须是 HTTPS，并以 /api 结尾。');
    }
    this.baseUrl = url.href.replace(/\/+$/, '');
    return { configured: true, origin: url.origin };
  }

  getConfiguration() {
    if (!this.baseUrl) return { configured: false };
    return { configured: true, origin: new URL(this.baseUrl).origin };
  }

  async request(request) {
    if (!this.baseUrl) throw new Error('请先设置平台 HTTPS API 地址。');
    if (!request || typeof request.path !== 'string' || !request.path.startsWith('/') || request.path.startsWith('//') || request.path.includes('\\')) throw new Error('平台 API 路径无效。');
    const url = new URL(`${this.baseUrl}${request.path}`);
    if (url.origin !== new URL(this.baseUrl).origin || !url.pathname.startsWith(`${new URL(this.baseUrl).pathname}/`)) throw new Error('平台请求超出已配置 API 范围。');
    const method = String(request.method || 'GET').toUpperCase();
    if (!['GET', 'POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) throw new Error('不支持的平台请求方法。');
    const headers = { Accept: 'application/json' };
    for (const [name, value] of Object.entries(request.headers || {})) {
      if (FORBIDDEN_HEADERS.has(name.toLowerCase())) throw new Error('禁止页面覆盖认证及来源请求头。');
      if (name.toLowerCase() === 'content-type' && typeof value === 'string' && value.length < 100) headers['Content-Type'] = value;
    }
    let body;
    if (request.body !== undefined && request.body !== null) {
      body = typeof request.body === 'string' ? request.body : JSON.stringify(request.body);
      if (Buffer.byteLength(body, 'utf8') > 2_000_000) throw new Error('平台请求内容超过限制。');
    }
    const timeoutMs = Math.max(1000, Math.min(Number(request.timeoutMs) || 30000, 60000));
    const response = await this.session.fetch(url.href, { method, headers, body, credentials: 'include', redirect: 'error', signal: AbortSignal.timeout(timeoutMs) });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) return { ok: false, status: response.status, payload };
    return { ok: true, status: response.status, payload };
  }
}

module.exports = { ApiSession };
