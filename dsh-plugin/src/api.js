const DEFAULT_TIMEOUT_MS = 15_000;

function normalizeBaseUrl(value, allowInsecureHttp = false) {
  if (!value) throw new Error('BMHS_API_BASE_URL is required.');
  let url;
  try {
    url = new URL(value);
  } catch {
    throw new Error('BMHS_API_BASE_URL must be a valid absolute URL.');
  }
  if (url.username || url.password || url.search || url.hash) {
    throw new Error('BMHS_API_BASE_URL must not contain credentials, query, or fragment.');
  }
  const loopbackHttp = url.protocol === 'http:' && ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname);
  if (url.protocol !== 'https:' && !loopbackHttp && !(allowInsecureHttp && url.protocol === 'http:')) {
    throw new Error('BMHS_API_BASE_URL must use HTTPS except for loopback development.');
  }
  return url.toString().replace(/\/$/, '');
}

export class BmhsApiClient {
  constructor({ baseUrl, credential, refreshCredential, allowInsecureHttp = false, fetchImpl = globalThis.fetch, timeoutMs = DEFAULT_TIMEOUT_MS }) {
    this.baseUrl = normalizeBaseUrl(baseUrl, allowInsecureHttp);
    if (typeof credential !== 'string' || credential.length < 16 || /[\r\n]/.test(credential)) {
      throw new Error('BMHS_DSH_CREDENTIAL must be a valid short-lived bearer credential.');
    }
    if (typeof fetchImpl !== 'function') throw new Error('Fetch API is unavailable in this DSH runtime.');
    this.credential = credential;
    this.refreshCredential = refreshCredential;
    this.fetchImpl = fetchImpl;
    this.timeoutMs = timeoutMs;
    this.refreshInFlight = null;
  }

  async request(path, { method = 'GET', body, signal } = {}) {
    if (typeof path !== 'string' || !path.startsWith('/api/plugin/') || path.startsWith('//')) {
      throw new Error('Refusing a request outside the BM-HS plugin API.');
    }
    const timeoutSignal = AbortSignal.timeout(this.timeoutMs);
    const requestSignal = signal ? AbortSignal.any([signal, timeoutSignal]) : timeoutSignal;
    let response = await this.send(path, { method, body, signal });
    if (response.status === 401 && this.refreshCredential && path !== '/api/plugin/auth/refresh') {
      await this.refresh(signal);
      response = await this.send(path, { method, body, signal });
    }
    if (!response.ok) {
      // Do not echo server response bodies: they may contain private data or tokens.
      throw new Error(`BM-HS plugin API request failed (${response.status}).`);
    }
    if (response.status === 204) return {};
    const contentType = response.headers?.get?.('content-type') || '';
    if (!contentType.toLowerCase().includes('application/json')) {
      throw new Error('BM-HS plugin API returned an unexpected response type.');
    }
    return response.json();
  }

  async send(path, { method, body, signal, credential = this.credential } = {}) {
    const timeoutSignal = AbortSignal.timeout(this.timeoutMs);
    const requestSignal = signal ? AbortSignal.any([signal, timeoutSignal]) : timeoutSignal;
    return this.fetchImpl(`${this.baseUrl}${path}`, {
      method,
      signal: requestSignal,
      headers: {
        Accept: 'application/json',
        ...(credential ? { Authorization: `Bearer ${credential}` } : {}),
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
  }

  async refresh(signal) {
    if (!this.refreshInFlight) {
      this.refreshInFlight = (async () => {
        const response = await this.send('/api/plugin/auth/refresh', {
          method: 'POST', body: { refreshCredential: this.refreshCredential }, signal, credential: null,
        });
        if (!response.ok || !response.headers?.get?.('content-type')?.toLowerCase().includes('application/json')) {
          throw new Error(`BM-HS plugin credential renewal failed (${response.status}).`);
        }
        const next = await response.json();
        if (typeof next.credential !== 'string' || next.credential.length < 40
            || typeof next.refreshCredential !== 'string' || next.refreshCredential.length < 40) {
          throw new Error('BM-HS plugin credential renewal returned an invalid response.');
        }
        this.credential = next.credential;
        this.refreshCredential = next.refreshCredential;
      })().finally(() => { this.refreshInFlight = null; });
    }
    return this.refreshInFlight;
  }
}

export function createApiFromEnvironment(env = process.env, fetchImpl = globalThis.fetch) {
  return new BmhsApiClient({
    baseUrl: env.BMHS_API_BASE_URL,
    credential: env.BMHS_DSH_CREDENTIAL,
    refreshCredential: env.BMHS_DSH_REFRESH_CREDENTIAL,
    allowInsecureHttp: env.BMHS_DSH_ALLOW_INSECURE_HTTP === 'true',
    fetchImpl,
  });
}
