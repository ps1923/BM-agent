import { createHash } from 'node:crypto';

const MAX_FILE_BYTES = 128 * 1024;
const MAX_TOTAL_BYTES = 512 * 1024;
const MAX_FILES = 20;
const PROTECTED_NAME = /^(?:\.env(?:\..*)?|\.git|\.ssh|id_(?:rsa|dsa|ecdsa|ed25519)|credentials(?:\.json)?|secrets?(?:\..*)?)$/i;
const PRIVATE_KEY = /-----BEGIN (?:OPENSSH |RSA |EC |DSA )?PRIVATE KEY-----/;
const SECRET_ASSIGNMENT = /(?:api[_-]?key|access[_-]?token|client[_-]?secret|password)\s*[:=]\s*['"]?[^\s'";]{12,}/i;

function safeId(value, label) {
  if (!(typeof value === 'string' || Number.isSafeInteger(value)) || String(value).length > 100 || !String(value).trim()) {
    throw new Error(`${label} is invalid.`);
  }
  return String(value);
}

export function validateWorkspaceRelativePath(value) {
  if (typeof value !== 'string' || value.length === 0 || value.length > 240 || value.includes('\0')) {
    throw new Error('File paths must be non-empty workspace-relative paths.');
  }
  const path = value.replaceAll('\\', '/');
  if (path.startsWith('/') || /^[a-zA-Z]:/.test(path) || path.startsWith('//') || path.split('/').some((part) => part === '..' || part === '')) {
    throw new Error('Absolute paths and workspace traversal are not allowed.');
  }
  if (path.split('/').some((part) => PROTECTED_NAME.test(part)) || /\.(?:pem|key|p12|pfx)$/i.test(path)) {
    throw new Error('Credential and private-key files cannot be submitted.');
  }
  return path;
}

function requireApproval(ctx, exec, toolName, reason) {
  const approval = ctx.get?.('approval');
  if (!approval?.request || !exec?.agent) {
    throw new Error('Student confirmation is unavailable; this action was not submitted.');
  }
  return approval.request({
    agent: exec.agent,
    toolName,
    ...(exec.callId ? { callId: exec.callId } : {}),
    reason,
    signal: exec.signal,
  });
}

async function requireOneShotConfirmation(ctx, exec, toolName, reason) {
  const outcome = await requireApproval(ctx, exec, toolName, reason);
  if (outcome !== 'allowed-once') throw new Error('Student did not approve this action; nothing was submitted.');
}

function outputSchema() {
  return { type: 'object', additionalProperties: true };
}

function renderOutput(_args, value) {
  return [{ type: 'text', text: JSON.stringify(value) }];
}

function tool(defineTool, name, description, parameters, execute) {
  return defineTool({
    name,
    description,
    parameters,
    output: { schema: outputSchema(), render: renderOutput },
    timeoutMs: 20_000,
    async execute(args, exec) {
      return execute(args, exec);
    },
  });
}

function currentAgentId(exec) {
  return safeId(exec?.agent?.id, 'Current DSH session');
}

function encodeId(value) {
  return encodeURIComponent(value);
}

export function createBmhsTools({ ctx, api, workspaceId, defineTool }) {
  if (!ctx?.fs || typeof ctx.fs.resolve !== 'function' || typeof ctx.fs.streamText !== 'function') {
    throw new Error('BM-HS plugin requires the DSH ctx.fs workspace capability.');
  }
  if (!api || typeof api.request !== 'function') throw new Error('BM-HS API client is unavailable.');
  const opaqueWorkspaceId = safeId(workspaceId, 'DSH workspace');
  // The agent session is an immutable DSH identity. Do not accept identity or run IDs
  // from untrusted model arguments; backend ownership is derived from the bearer.
  const activeRuns = new Map();
  const apiRequest = (path, options, exec) => api.request(path, { ...options, signal: exec?.signal });
  const boundRun = (exec) => {
    const binding = activeRuns.get(currentAgentId(exec));
    if (!binding) throw new Error('Attach this DSH session to an experiment before using run tools.');
    return binding;
  };

  const tools = [
    tool(defineTool, 'bmhs_list_experiments', 'List experiments available to the authenticated student. Student identity comes from the short-lived DSH credential.', {}, async (_args, exec) =>
      apiRequest('/api/plugin/experiments', {}, exec)),

    tool(defineTool, 'bmhs_attach_experiment_run', 'Attach the current DSH session and its opaque workspace to an available experiment. Requires one-time student approval; no student identity is accepted.', {
      experimentId: { type: 'string', required: true, description: 'Experiment identifier returned by bmhs_list_experiments.' },
    }, async (args, exec) => {
      const agentId = currentAgentId(exec);
      const experimentId = safeId(args.experimentId, 'Experiment');
      const sessionRef = createHash('sha256').update(agentId).digest('hex');
      await requireOneShotConfirmation(ctx, exec, 'bmhs_attach_experiment_run', `Attach this DSH session to experiment ${experimentId}.`);
      const result = await apiRequest('/api/plugin/runs', {
        method: 'POST',
        body: { experimentId, sessionRef, workspaceRef: opaqueWorkspaceId },
      }, exec);
      const runId = safeId(result?.runId ?? result?.id, 'Experiment run');
      activeRuns.set(agentId, { runId, experimentId });
      return { attached: true, runId, experimentId };
    }),

    tool(defineTool, 'bmhs_get_learning_path', 'Read the attached experiment tree, current progress, and task acceptance criteria.', {}, async (_args, exec) =>
      apiRequest(`/api/plugin/runs/${encodeId(boundRun(exec).runId)}/learning-path`, {}, exec)),

    tool(defineTool, 'bmhs_update_node_progress', 'Record a student-confirmed node progress update. The student must approve this one-time change in DSH.', {
      nodeId: { type: 'string', required: true, description: 'Node identifier from the attached learning path.' },
      status: { type: 'string', required: true, enum: ['pending', 'in_progress', 'completed'], description: 'Student-confirmed progress status.' },
      note: { type: 'string', description: 'Optional short note, not a chat transcript.' },
    }, async (args, exec) => {
      const runId = boundRun(exec).runId;
      const nodeId = safeId(args.nodeId, 'Node');
      await requireOneShotConfirmation(ctx, exec, 'bmhs_update_node_progress', `Record node ${nodeId} as ${args.status}.`);
      return apiRequest(`/api/plugin/runs/${encodeId(runId)}/nodes/${encodeId(nodeId)}`, {
        method: 'PATCH', body: { status: args.status, studentConfirmed: true },
      }, exec);
    }),

    tool(defineTool, 'bmhs_update_task_progress', 'Record a student-confirmed task progress update. The student must approve this one-time change in DSH.', {
      taskId: { type: 'string', required: true, description: 'Task identifier from the attached learning path.' },
      status: { type: 'string', required: true, enum: ['pending', 'in_progress', 'completed', 'failed'], description: 'Student-confirmed progress status.' },
      note: { type: 'string', description: 'Optional short note, not a chat transcript.' },
    }, async (args, exec) => {
      const runId = boundRun(exec).runId;
      const taskId = safeId(args.taskId, 'Task');
      await requireOneShotConfirmation(ctx, exec, 'bmhs_update_task_progress', `Record task ${taskId} as ${args.status}.`);
      return apiRequest(`/api/plugin/runs/${encodeId(runId)}/tasks/${encodeId(taskId)}`, {
        method: 'PATCH', body: { status: args.status, studentConfirmed: true },
      }, exec);
    }),

    tool(defineTool, 'bmhs_search_bug_memory', 'Search only approved Bug memory in the attached experiment pool.', {
      query: { type: 'string', required: true, description: 'Short description of the current error or obstacle.' },
      limit: { type: 'integer', description: 'Maximum matches, from 1 to 10.' },
    }, async (args, exec) => {
      const query = String(args.query || '').trim();
      if (!query || query.length > 2_000) throw new Error('Search query must contain 1-2000 characters.');
      const limit = args.limit ?? 5;
      if (!Number.isInteger(limit) || limit < 1 || limit > 10) throw new Error('limit must be an integer from 1 to 10.');
      const experimentId = boundRun(exec).experimentId;
      const path = `/api/plugin/experiments/${encodeId(experimentId)}/bug-memory/search`;
      return apiRequest(path, { method: 'POST', body: { runId: boundRun(exec).runId, query, limit } }, exec);
    }),

    tool(defineTool, 'bmhs_submit_bug_case', 'Explicitly submit a resolved Bug case to the experiment memory pool for teacher review. Requires one-time student approval; does not automatically publish the case.', {
      title: { type: 'string', required: true, description: 'Short Bug title.' },
      symptom: { type: 'string', required: true, description: 'Observed error or incorrect behavior.' },
      cause: { type: 'string', required: true, description: 'Verified root cause.' },
      resolution: { type: 'string', required: true, description: 'Steps that fixed the issue.' },
      verification: { type: 'string', required: true, description: 'How the fix was verified.' },
    }, async (args, exec) => {
      const runId = boundRun(exec).runId;
      for (const field of ['title', 'symptom', 'cause', 'resolution', 'verification']) {
        if (typeof args[field] !== 'string' || !args[field].trim() || args[field].length > 5_000) {
          throw new Error(`${field} must contain 1-5000 characters.`);
        }
      }
      await requireOneShotConfirmation(ctx, exec, 'bmhs_submit_bug_case', `Submit Bug case “${args.title.slice(0, 120)}” for teacher review.`);
      const result = await apiRequest(`/api/plugin/runs/${encodeId(runId)}/bugs`, {
        method: 'POST', body: {
          title: args.title.trim(),
          problem: `现象：${args.symptom.trim()}\n原因：${args.cause.trim()}\n验证：${args.verification.trim()}`,
          solution: args.resolution.trim(),
        },
      }, exec);
      return { submitted: true, bugId: result?.bugId ?? result?.id ?? null, status: result?.status ?? 'pending_review' };
    }),

    tool(defineTool, 'bmhs_submit_code_snapshot', 'Explicitly submit selected files from the current DSH workspace as an immutable teacher-visible snapshot. Only workspace-relative selected paths and contents are sent. Requires one-time student approval.', {
      paths: { type: 'array', required: true, description: 'The exact workspace-relative files selected by the student.', items: { type: 'string' } },
      nodeId: { type: 'string', description: 'Optional learning-path node this snapshot supports.' },
    }, async (args, exec) => {
      const runId = boundRun(exec).runId;
      if (!Array.isArray(args.paths) || args.paths.length < 1 || args.paths.length > MAX_FILES) {
        throw new Error(`Select between 1 and ${MAX_FILES} files.`);
      }
      const paths = [...new Set(args.paths.map(validateWorkspaceRelativePath))];
      if (paths.length !== args.paths.length) throw new Error('Duplicate file paths are not allowed.');
      const root = await ctx.fs.resolve('.', { signal: exec?.signal });
      const files = [];
      let totalBytes = 0;
      for (const path of paths) {
        const target = await ctx.fs.resolve(path, { signal: exec?.signal });
        if (!ctx.fs.contains(root, target)) throw new Error(`Selected file is outside the current DSH workspace: ${path}`);
        const info = await ctx.fs.stat(target, exec?.signal);
        if (!info || info.type !== 'file' || !Number.isSafeInteger(info.size) || info.size < 0 || info.size > MAX_FILE_BYTES) {
          throw new Error(`Selected file is missing, not a regular file, or exceeds the per-file limit: ${path}`);
        }
        const chunks = [];
        let byteLength = 0;
        const stream = await ctx.fs.streamText(target, exec?.signal);
        for await (const chunk of stream) {
          if (typeof chunk !== 'string') throw new Error(`Selected file is not valid text: ${path}`);
          byteLength += Buffer.byteLength(chunk, 'utf8');
          if (byteLength > MAX_FILE_BYTES) throw new Error(`Selected file exceeds the per-file limit: ${path}`);
          chunks.push(chunk);
        }
        const content = chunks.join('');
        if (PRIVATE_KEY.test(content) || SECRET_ASSIGNMENT.test(content)) throw new Error(`A likely credential was found in ${path}; remove it before submitting.`);
        totalBytes += byteLength;
        if (totalBytes > MAX_TOTAL_BYTES) throw new Error('Selected files exceed the 512 KiB total snapshot limit.');
      files.push({ path, content, sha256: createHash('sha256').update(content, 'utf8').digest('hex') });
      }
      const nodeId = args.nodeId === undefined ? undefined : safeId(args.nodeId, 'Node');
      await requireOneShotConfirmation(ctx, exec, 'bmhs_submit_code_snapshot', `Submit selected workspace files [${paths.join(', ')}] (${totalBytes} bytes) as an immutable teacher-visible snapshot${nodeId ? ` for node ${nodeId}` : ''}.`);
      const result = await apiRequest(`/api/plugin/runs/${encodeId(runId)}/code-submissions`, {
        method: 'POST', body: { ...(nodeId ? { nodeId } : {}), files },
      }, exec);
      // Never project uploaded source back into the model/session result.
      return {
        submitted: true,
        submissionId: result?.submissionId ?? result?.id ?? null,
        fileCount: files.length,
        paths,
        ...(nodeId ? { nodeId } : {}),
      };
    }),
  ];
  return tools;
}
