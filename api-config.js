(function attachApiConfig(root, createApiConfig) {
  const apiConfig = createApiConfig();
  if (typeof module === 'object' && module.exports) {
    module.exports = apiConfig;
  } else {
    root.BMApiConfig = apiConfig;
  }
})(typeof window === 'undefined' ? globalThis : window, function createApiConfig() {
  const LOOPBACK_HOSTS = new Set(['localhost', '127.0.0.1']);

  function resolveApiBaseUrl(location, configuredBaseUrl) {
    if (!location || !location.origin || !location.protocol || !location.hostname) {
      throw new TypeError('无法识别当前页面地址。');
    }

    const host = location.hostname.toLowerCase();
    const localDevelopment = location.protocol === 'http:'
      && location.port === '8000'
      && LOOPBACK_HOSTS.has(host);
    const localApiHost = host === 'localhost' ? 'localhost' : location.hostname;
    const deployedApiPath = location.pathname.startsWith('/bm-agent/') ? '/bm-agent/api' : '/api';
    const defaultBaseUrl = localDevelopment
      ? `${location.protocol}//${localApiHost}:8080/api`
      : new URL(deployedApiPath, location.origin).href;
    const configured = typeof configuredBaseUrl === 'string' ? configuredBaseUrl.trim() : '';
    const candidate = configured || defaultBaseUrl;

    let apiUrl;
    try {
      apiUrl = new URL(candidate, location.origin);
    } catch {
      throw new TypeError('API 地址配置无效，请检查部署配置。');
    }

    if (!['http:', 'https:'].includes(apiUrl.protocol)
      || apiUrl.username || apiUrl.password || apiUrl.search || apiUrl.hash) {
      throw new TypeError('API 地址必须是有效的 HTTP(S) 地址，且不能包含凭据、查询参数或片段。');
    }
    if (!apiUrl.pathname.replace(/\/+$/, '').endsWith('/api')) {
      throw new TypeError('API 地址路径必须以 /api 结尾。');
    }
    if (location.protocol === 'https:' && apiUrl.protocol !== 'https:') {
      throw new TypeError('HTTPS 页面不能连接明文 HTTP API，请改用 HTTPS 或同源 /api。');
    }

    return apiUrl.href.replace(/\/+$/, '');
  }

  return { resolveApiBaseUrl };
});
