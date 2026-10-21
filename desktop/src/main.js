'use strict';

const { app, BrowserWindow, ipcMain, protocol, session, safeStorage } = require('electron');
const fs = require('node:fs/promises');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { WorkspaceManager } = require('./assistant/WorkspaceManager');
const { ProcessManager } = require('./assistant/ProcessManager');
const { ApiSession } = require('./platform/ApiSession');
const { SecretStore } = require('./assistant/SecretStore');
const { GuidanceService } = require('./assistant/GuidanceService');

const APP_ORIGIN = 'bmhs://student';
protocol.registerSchemesAsPrivileged([{ scheme: 'bmhs', privileges: { standard: true, secure: true, supportFetchAPI: true, stream: true } }]);
let mainWindow;
let apiSession;
let workspaceManager;
let processManager;
let secretStore;
let guidanceService;
let packagedPagePath;

function requireTrustedFrame(event) {
  const frame = event.senderFrame;
  if (!frame || !mainWindow || event.sender !== mainWindow.webContents || frame.url !== APP_ORIGIN + '/') {
    throw new Error('已拒绝非应用页面的本机能力调用。');
  }
}

function requireRunId(value) {
  const runId = String(value ?? '');
  if (!/^[1-9]\d{0,17}$/.test(runId)) throw new Error('运行实例标识无效。');
  return runId;
}

function register(channel, handler) {
  ipcMain.handle(channel, (event, ...args) => {
    requireTrustedFrame(event);
    return handler(...args);
  });
}

async function createWindow() {
  const isPackaged = app.isPackaged;
  packagedPagePath = isPackaged
    ? path.join(process.resourcesPath, 'app', 'index.html')
    : path.resolve(__dirname, '../../index.html');
  const preloadPath = path.join(__dirname, 'preload.js');
  const userData = app.getPath('userData');
  const settingsPath = path.join(userData, 'desktop-settings.json');
  let settings = {};
  try { settings = JSON.parse(await fs.readFile(settingsPath, 'utf8')); } catch (error) { if (error.code !== 'ENOENT') throw error; }

  await protocol.handle('bmhs', (request) => {
    const url = new URL(request.url);
    if (url.host !== 'student') return new Response('Not found', { status: 404 });
    const allowed = new Map([
      ['/', packagedPagePath],
      ['/index.html', packagedPagePath],
      ['/app.js', path.join(path.dirname(packagedPagePath), 'app.js')],
      ['/api-config.js', path.join(path.dirname(packagedPagePath), 'api-config.js')],
      ['/styles.css', path.join(path.dirname(packagedPagePath), 'styles.css')],
    ]);
    const filePath = allowed.get(decodeURIComponent(url.pathname));
    if (!filePath) return new Response('Not found', { status: 404 });
    return fetch(pathToFileURL(filePath).href);
  });

  apiSession = new ApiSession({ session: session.defaultSession, configuredBaseUrl: settings.apiBaseUrl || process.env.BM_HS_API_BASE_URL || '' });
  workspaceManager = new WorkspaceManager(path.join(userData, 'workspaces'));
  const runtimeDirectory = isPackaged ? path.join(process.resourcesPath, 'runtime') : path.resolve(__dirname, '../runtime');
  processManager = new ProcessManager({ workspaceManager, runtimeDirectory });
  secretStore = new SecretStore({ safeStorage, filePath: path.join(userData, 'deepseek-key.bin') });
  guidanceService = new GuidanceService({ secretStore });

  mainWindow = new BrowserWindow({
    width: 1440,
    height: 940,
    minWidth: 1024,
    minHeight: 720,
    webPreferences: { preload: preloadPath, contextIsolation: true, sandbox: true, nodeIntegration: false, webSecurity: true, allowRunningInsecureContent: false },
  });
  mainWindow.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  mainWindow.webContents.on('will-navigate', (event, url) => { if (url !== `${APP_ORIGIN}/`) event.preventDefault(); });
  mainWindow.webContents.session.webRequest.onBeforeRequest((details, callback) => {
    if (details.webContentsId === mainWindow.webContents.id && !details.url.startsWith('bmhs://student/')) return callback({ cancel: true });
    callback({});
  });
  await mainWindow.loadURL(`${APP_ORIGIN}/`);
}

function registerIpc() {
  register('platform:get-configuration', () => apiSession.getConfiguration());
  register('platform:set-api-base-url', async (url) => {
    const result = apiSession.setBaseUrl(url);
    const file = path.join(app.getPath('userData'), 'desktop-settings.json');
    await fs.mkdir(path.dirname(file), { recursive: true });
    await fs.writeFile(file, JSON.stringify({ apiBaseUrl: apiSession.baseUrl }), { mode: 0o600 });
    return result;
  });
  register('platform:request', (request) => apiSession.request(request));
  register('workspace:create', ({ runId, workspaceId }) => workspaceManager.create(requireRunId(runId), workspaceId || null));
  register('workspace:list', ({ runId }) => workspaceManager.list(requireRunId(runId)));
  register('workspace:create-directory', ({ runId, relativePath }) => workspaceManager.createDirectory(requireRunId(runId), relativePath));
  register('workspace:read', ({ runId, relativePath }) => workspaceManager.read(requireRunId(runId), relativePath));
  register('workspace:write', ({ runId, relativePath, content, expectedHash }) => workspaceManager.write(requireRunId(runId), relativePath, content, expectedHash ?? null));
  register('process:run-command', ({ runId, command }) => processManager.runCommand(requireRunId(runId), command));
  register('process:start', ({ runId, command }) => processManager.startProcess(requireRunId(runId), command));
  register('process:get', ({ processId }) => processManager.getProcess(processId));
  register('process:stop', ({ processId }) => processManager.stopProcess(processId));
  register('secret:set-deepseek-key', (key) => secretStore.setDeepSeekKey(key));
  register('secret:has-deepseek-key', () => secretStore.hasDeepSeekKey());
  register('guidance:request', (request) => guidanceService.ask(request));
}

app.whenReady().then(async () => {
  registerIpc();
  await createWindow();
  app.on('activate', () => { if (BrowserWindow.getAllWindows().length === 0) void createWindow(); });
});

app.on('before-quit', () => { void processManager?.stopAll(); });
app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });

module.exports = { requireTrustedFrame, requireRunId };
