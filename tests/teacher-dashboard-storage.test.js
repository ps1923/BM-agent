import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { runInNewContext } from 'node:vm';
import test from 'node:test';

const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');
const loadStart = app.indexOf('async function loadTeacherCourseDashboard');
const renderStart = app.indexOf('\nasync function renderTeacherDashboard', loadStart);
const renderEnd = app.indexOf('\nfunction hideProtectedViews', renderStart);
const localStorageReaderStart = app.indexOf('function readLocalStorageValue');
const localStorageReaderEnd = app.indexOf('\nfunction writeSessionValue', localStorageReaderStart);

class TestElement {
  constructor() {
    this.children = [];
    this.textContent = '';
    this.value = '';
    this.hidden = false;
    this.className = '';
  }

  replaceChildren(...children) { this.children = [...children]; }
  append(...children) { this.children.push(...children); }
  addEventListener() {}
}

function createHarness({ getItem = () => null } = {}) {
  const state = {
    pending: [],
    user: { id: 'teacher-1', role: 'teacher', name: 'Teacher', email: 't@example.test' },
  };
  const ids = [
    'teacherIdentity', 'teacherCourseCount', 'teacherCourses', 'publishCourse', 'publishExperiment',
    'teacherDrafts', 'teacherDashboardCourse', 'teacherDashboardContent', 'teacherDashboardEmpty',
    'teacherProgressRows', 'teacherDashboardMessage',
  ];
  const context = {
    window: { localStorage: { getItem, setItem() { throw new Error('storage blocked'); } } },
    document: { createElement: () => new TestElement() },
    getCurrentUser: () => state.user,
    setMessage(element, message, isError = false) {
      element.textContent = message;
      element.isError = isError;
    },
    resetTeacherRunDetail() {},
    clearTeacherBugReview() {},
    loadTeacherDshExperiments() {},
    loadTeacherBugCases() {},
    loadTeacherRunDetail() {},
    formatTeacherDate: (value) => value || '尚未开始',
    apiRequest(path) {
      return new Promise((resolve, reject) => state.pending.push({ path, resolve, reject }));
    },
    teacherDashboardEpoch: 0,
    teacherCourseDashboardEpoch: 0,
    STORAGE_KEYS: { teacherCourse: 'bmhs.teacherCourse' },
    String,
    Number,
    Array,
    Error,
    Promise,
  };
  ids.forEach((id) => { context[id] = new TestElement(); });
  const functions = runInNewContext(`(() => {
    ${app.slice(loadStart, renderStart)}
    ${app.slice(renderStart, renderEnd)}
    ${app.slice(localStorageReaderStart, localStorageReaderEnd)}
    return { loadTeacherCourseDashboard, renderTeacherDashboard };
  })()`, context);
  return { ...functions, state, elements: context };
}

const courses = [
  { id: 11, name: '编程实验 A', inviteCode: 'A11', memberCount: 1, experiments: [] },
  { id: 22, name: '编程实验 B', inviteCode: 'B22', memberCount: 2, experiments: [] },
];

async function renderDashboard(harness) {
  const rendering = harness.renderTeacherDashboard();
  harness.state.pending[0].resolve(courses);
  harness.state.pending[1].resolve([]);
  await rendering;
}

function dashboardFor(courseName) {
  return {
    courseName,
    memberCount: 1,
    experimentCount: 1,
    students: [{
      displayName: '学生甲',
      email: 'student-a@example.test',
      experimentName: 'Python 基础实验',
      totalNodes: 4,
      completedNodes: 2,
      totalTasks: 8,
      completedTasks: 3,
      runStatus: 'in_progress',
      lastOpenedAt: '2026-10-03',
      runId: 101,
    }],
  };
}

test('storage read/write denial does not block the teacher course dashboard or switching', async () => {
  const harness = createHarness({ getItem() { throw new Error('storage blocked'); } });
  await renderDashboard(harness);

  assert.equal(harness.elements.teacherCourses.children.length, 2);
  assert.match(harness.elements.teacherCourses.children[0].children[0].children[0].textContent, /编程实验 A/);
  assert.match(harness.elements.teacherCourses.children[1].children[0].children[0].textContent, /编程实验 B/);
  assert.equal(harness.elements.teacherDashboardCourse.value, '11');
  assert.equal(harness.state.pending[2].path, '/teacher/courses/11/dashboard');
  harness.state.pending[2].resolve(dashboardFor('编程实验 A'));
  await new Promise((resolve) => setImmediate(resolve));
  assert.match(harness.elements.teacherDashboardMessage.textContent, /编程实验 A/);
  assert.equal(harness.elements.teacherProgressRows.children.length, 1);
  assert.match(harness.elements.teacherProgressRows.children[0].children[0].children[0].textContent, /学生甲/);
  assert.match(harness.elements.teacherProgressRows.children[0].children[2].textContent, /2\/4 节点 · 3\/8 任务/);

  harness.elements.teacherDashboardCourse.value = '22';
  const switching = harness.loadTeacherCourseDashboard('22');
  assert.equal(harness.state.pending[3].path, '/teacher/courses/22/dashboard');
  harness.state.pending[3].resolve(dashboardFor('编程实验 B'));
  await switching;
  assert.match(harness.elements.teacherDashboardMessage.textContent, /编程实验 B/);
  assert.equal(harness.elements.teacherDashboardContent.hidden, false);
});

test('a saved course ID not owned by the teacher falls back to the first owned course', async () => {
  const harness = createHarness({ getItem: () => '999' });
  await renderDashboard(harness);

  assert.equal(harness.elements.teacherDashboardCourse.value, '11');
  assert.equal(harness.state.pending[2].path, '/teacher/courses/11/dashboard');
});

test('a valid saved owned course remains the initial selection', async () => {
  const harness = createHarness({ getItem: () => '22' });
  await renderDashboard(harness);

  assert.equal(harness.elements.teacherDashboardCourse.value, '22');
  assert.equal(harness.state.pending[2].path, '/teacher/courses/22/dashboard');
});
