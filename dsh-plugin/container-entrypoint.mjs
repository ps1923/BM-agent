import { spawn } from 'node:child_process';
import { readFile, unlink, writeFile } from 'node:fs/promises';
import { createInterface } from 'node:readline';

const credentialPath = '/run/bmhs-dsh/credentials';
const launchUrlPath = '/run/bmhs-dsh/launch-url';
const credentialKeys = new Set([
  'BMHS_API_BASE_URL',
  'BMHS_DSH_CREDENTIAL',
  'BMHS_DSH_REFRESH_CREDENTIAL',
  'BMHS_DSH_WORKSPACE_ID',
]);

async function waitForCredentials() {
  const deadline = Date.now() + 90_000;
  while (Date.now() < deadline) {
    try {
      return await readFile(credentialPath, 'utf8');
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
      await new Promise((resolve) => setTimeout(resolve, 100));
    }
  }
  throw new Error('Timed out waiting for the DSH instance credentials.');
}

function loadCredentials(contents) {
  for (const line of contents.replaceAll('\r\n', '\n').split('\n')) {
    if (!line) continue;
    const separator = line.indexOf('=');
    if (separator < 1) throw new Error('Invalid DSH instance credential record.');
    const key = line.slice(0, separator);
    const value = line.slice(separator + 1);
    if (!credentialKeys.has(key) || /[\r\n]/.test(value)) {
      throw new Error('Invalid DSH instance credential record.');
    }
    process.env[key] = value;
  }
  for (const key of credentialKeys) {
    if (!process.env[key]) throw new Error('A required DSH instance setting is missing.');
  }
  if (!process.env.DSH_PUBLIC_URL || !process.env.DSH_TRUSTED_HOST) {
    throw new Error('A required DSH public routing setting is missing.');
  }
}

function createLaunchUrlMatcher() {
  const expected = new URL(process.env.DSH_PUBLIC_URL);
  return async (line) => {
    const matches = line.match(/https?:\/\/[^\s"'<>]+/gi) || [];
    for (const match of matches) {
      const candidate = match.replace(/[),.;]+$/, '');
      try {
        const url = new URL(candidate);
        if (url.origin === expected.origin && url.pathname === expected.pathname
            && (url.search || url.hash)) {
          await writeFile(launchUrlPath, candidate, { encoding: 'utf8', mode: 0o600 });
          break;
        }
      } catch { /* Ignore unrelated or malformed URLs in startup messages. */ }
    }
    return line.replace(/https?:\/\/[^\s"'<>]+/gi, '[DSH URL withheld]');
  };
}

async function pipeRedacted(input, output, redact) {
  const lines = createInterface({ input, crlfDelay: Infinity });
  for await (const line of lines) output.write(`${await redact(line)}\n`);
}

async function main() {
  process.umask(0o077);
  const credentials = await waitForCredentials();
  loadCredentials(credentials);
  await unlink(credentialPath);

  const redact = createLaunchUrlMatcher();
  const child = spawn('dsh', process.argv.slice(2), { stdio: ['inherit', 'pipe', 'pipe'] });
  void pipeRedacted(child.stdout, process.stdout, redact);
  void pipeRedacted(child.stderr, process.stderr, redact);
  for (const signal of ['SIGINT', 'SIGTERM']) {
    process.on(signal, () => child.kill(signal));
  }
  child.on('error', (error) => {
    console.error(`Unable to start DSH: ${error.message}`);
    process.exitCode = 1;
  });
  child.on('exit', (code, signal) => {
    process.exitCode = signal ? 1 : (code ?? 1);
  });
}

main().catch((error) => {
  console.error(`DSH startup failed: ${error.message}`);
  process.exitCode = 1;
});
