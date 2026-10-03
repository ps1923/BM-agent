import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { runInNewContext } from 'node:vm';
import test from 'node:test';

const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');
const numberingStart = app.indexOf('function compareTreeSiblings');
const numberingEnd = app.indexOf('\nfunction renderCanvas', numberingStart);
assert.ok(numberingStart >= 0 && numberingEnd > numberingStart, 'tree numbering helpers must exist');
const treeHelpers = runInNewContext(
  `(() => { ${app.slice(numberingStart, numberingEnd).trim()}\n return { buildTreeNodeNumbers, getCanvasAnchorNode }; })()`,
);
const { buildTreeNodeNumbers, getCanvasAnchorNode } = treeHelpers;

test('tree labels reflect sibling branches and recursive levels, including 3.1.1', () => {
  const nodes = [
    { key: 'stage-start', parentKey: null, canvasX: 320 },
    { key: 'stage-one', parentKey: 'stage-start', canvasX: 0 },
    { key: 'stage-two', parentKey: 'stage-start', canvasX: 320 },
    { key: 'stage-three', parentKey: 'stage-start', canvasX: 640 },
    { key: 'stage-missing-x', parentKey: 'stage-start' },
    { key: 'stage-null-x', parentKey: 'stage-start', canvasX: null },
    { key: 'request-validation', parentKey: 'stage-three', canvasX: 480 },
    { key: 'required-fields', parentKey: 'request-validation', canvasX: 480 },
    { key: 'field-format', parentKey: 'request-validation', canvasX: 800 },
  ];

  const numbers = buildTreeNodeNumbers(nodes);

  assert.deepEqual(Object.fromEntries(numbers), {
    'stage-start': 'start',
    'stage-one': '1',
    'stage-two': '2',
    'stage-three': '3',
    'stage-missing-x': '4',
    'stage-null-x': '5',
    'request-validation': '3.1',
    'required-fields': '3.1.1',
    'field-format': '3.1.2',
  });
});

test('tree numbering is independent of API node response order and is used in canvas and preview', () => {
  const nodes = [
    { key: 'root', parentKey: null },
    { key: 'z-branch', parentKey: 'root' },
    { key: 'a-branch', parentKey: 'root' },
    { key: 'leaf', parentKey: 'z-branch' },
  ];
  const first = Object.fromEntries(buildTreeNodeNumbers(nodes));
  const reversed = Object.fromEntries(buildTreeNodeNumbers([...nodes].reverse()));

  assert.deepEqual(first, reversed);
  assert.match(app, /const nodeNumbers = buildTreeNodeNumbers\(activeTreeNodes\)/);
  assert.match(app, /nodeNumbers\.get\(node\.key\) \|\| node\.key/);
  assert.match(app, /buildTreeNodeNumbers\(activeTreeNodes\)\.get\(selectedTreeNode\.key\)/);
  const canvas = app.slice(app.indexOf('function renderCanvas'), app.indexOf('\nfunction selectBubble'));
  assert.match(canvas, /getCanvasAnchorNode\(activeTreeNodes, bubble\.dataset\.treeNodeKey\)/);
  const preview = app.slice(app.indexOf('function renderPlanPreview'), app.indexOf('\nasync function openCreateDialog'));
  assert.match(preview, /buildTreeNodeNumbers\(plan\.nodes\)/);
  assert.match(preview, /nodeNumbers\.get\(planNode\.key\) \|\| planNode\.key/);
  assert.match(preview, /items\.sort\(compareTreeSiblings\)/);
});

test('legacy trees with multiple roots receive unique hierarchical labels', () => {
  const nodes = [
    { key: 'legacy-root-b', parentKey: null, canvasX: 320 },
    { key: 'legacy-child-b', parentKey: 'legacy-root-b', canvasX: 320 },
    { key: 'legacy-root-a', parentKey: null, canvasX: 0 },
    { key: 'legacy-child-a', parentKey: 'legacy-root-a', canvasX: 0 },
  ];

  const numbers = buildTreeNodeNumbers(nodes);

  assert.deepEqual(Object.fromEntries(numbers), {
    'legacy-root-a': '1',
    'legacy-child-a': '1.1',
    'legacy-root-b': '2',
    'legacy-child-b': '2.1',
  });
  assert.equal(getCanvasAnchorNode(nodes, 'legacy-root-a').key, 'legacy-root-a');
});
