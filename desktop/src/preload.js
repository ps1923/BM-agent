'use strict';

const { contextBridge, ipcRenderer } = require('electron');

const invoke = (channel, payload) => ipcRenderer.invoke(channel, payload);

contextBridge.exposeInMainWorld('BMDesktop', Object.freeze({
  isDesktop: true,
  apiRequest: (request) => invoke('platform:request', request),
  getApiConfiguration: () => invoke('platform:get-configuration'),
  setApiBaseUrl: (url) => invoke('platform:set-api-base-url', url),
  workspace: Object.freeze({
    create: (runId, workspaceId) => invoke('workspace:create', { runId, workspaceId }),
    list: (runId) => invoke('workspace:list', { runId }),
    createDirectory: (runId, relativePath) => invoke('workspace:create-directory', { runId, relativePath }),
    read: (runId, relativePath) => invoke('workspace:read', { runId, relativePath }),
    write: (runId, relativePath, content, expectedHash) => invoke('workspace:write', { runId, relativePath, content, expectedHash }),
    runCommand: (runId, command) => invoke('process:run-command', { runId, command }),
    startProcess: (runId, command) => invoke('process:start', { runId, command }),
    getProcess: (processId) => invoke('process:get', { processId }),
    stopProcess: (processId) => invoke('process:stop', { processId }),
  }),
  secrets: Object.freeze({ setDeepSeekKey: (key) => invoke('secret:set-deepseek-key', key), hasDeepSeekKey: () => invoke('secret:has-deepseek-key') }),
  guidance: (request) => invoke('guidance:request', request),
}));
