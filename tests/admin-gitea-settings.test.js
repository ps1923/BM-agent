import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const read = (path) => readFile(new URL(path, import.meta.url), 'utf8');

test('admin Gitea settings migration is additive and stores only ciphertext columns', async () => {
  const [migration, schema] = await Promise.all([
    read('../database/migrations/20261022_add_git_global_settings.sql'),
    read('../database/schema.sql'),
  ]);
  assert.match(migration, /CREATE TABLE IF NOT EXISTS `git_global_settings`/);
  assert.match(migration, /`admin_token_ciphertext` TEXT NULL/);
  assert.match(migration, /`webhook_secret_ciphertext` TEXT NULL/);
  assert.match(migration, /`updated_by` BIGINT UNSIGNED NOT NULL/);
  assert.match(migration, /`updated_at` DATETIME\(6\)/);
  assert.doesNotMatch(migration, /DROP\s+TABLE|TRUNCATE\s+TABLE|DELETE\s+FROM/i);
  assert.match(schema, /CREATE TABLE IF NOT EXISTS `git_global_settings`/);
});

test('admin personal center has a protected settings route and non-echoing secret controls', async () => {
  const [html, js] = await Promise.all([read('../index.html'), read('../app.js')]);
  assert.match(html, /id="adminPersonalCenterView"/);
  assert.match(html, /id="adminGitSettingsForm"/);
  assert.match(html, /id="adminGitClearAdminToken"/);
  assert.match(html, /id="adminGitClearWebhookSecret"/);
  assert.match(js, /admin-personal-center/);
  assert.match(js, /\['admin', 'admin-personal-center'\]\.includes\(route\.type\) \? 'admin'/);
  assert.match(js, /apiRequest\('\/admin\/integrations\/gitea'/);
  assert.match(html, /留空表示保留当前令牌/);
});
