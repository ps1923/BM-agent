import { defineTool } from '@deepseek-ai/dsh-tools';
import { createApiFromEnvironment } from './api.js';
import { createBmhsTools } from './tools.js';

export const name = 'bm-hs-dsh-plugin';
export const inject = ['tools', 'fs'];

export function apply(ctx) {
  const api = createApiFromEnvironment();
  // Keep short-lived credentials inside the plugin's closure, not inherited by DSH subprocess tools.
  delete process.env.BMHS_DSH_CREDENTIAL;
  delete process.env.BMHS_DSH_REFRESH_CREDENTIAL;
  const workspaceId = process.env.BMHS_DSH_WORKSPACE_ID;
  const tools = createBmhsTools({ ctx, api, workspaceId, defineTool });
  for (const definition of tools) ctx.tools.register(definition);
}
