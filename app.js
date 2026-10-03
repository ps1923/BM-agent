const libraryView = document.querySelector('#libraryView');
const canvasView = document.querySelector('#canvasView');
const bubble = document.querySelector('#bubble');
const inspector = document.querySelector('#inspector');
const editorView = document.querySelector('#editorView');
const openExperimentButtons = document.querySelectorAll('.experiment-link');
const backToLibrary = document.querySelector('#backToLibrary');
const startNodeButton = document.querySelector('#startNodeButton');
const backToCanvas = document.querySelector('#backToCanvas');
const chatMessages = document.querySelector('#chatMessages');
const chatInput = document.querySelector('#chatInput');
const sendButton = document.querySelector('#sendButton');
const softenButton = document.querySelector('#softenButton');
const terminalOutput = document.querySelector('#terminalOutput');
const terminalInput = document.querySelector('#terminalInput');
const codeEditor = document.querySelector('#codeEditor');
const codeLineNumbers = document.querySelector('#codeLineNumbers');
const codeFileTabs = document.querySelector('#codeFileTabs');
const codeLocation = document.querySelector('#codeLocation');
const saveFileButton = document.querySelector('#saveFileButton');
const createSnapshotButton = document.querySelector('#createSnapshotButton');
const completeNodeButton = document.querySelector('#completeNodeButton');
const recordBugButton = document.querySelector('#recordBugButton');
const deepDiagnosisButton = document.querySelector('#deepDiagnosisButton');
const bugDialog = document.querySelector('#bugDialog');
const closeBugDialog = document.querySelector('#closeBugDialog');
const cancelBugButton = document.querySelector('#cancelBugButton');
const bugCaseForm = document.querySelector('#bugCaseForm');
const bugTitle = document.querySelector('#bugTitle');
const bugTechnologyStack = document.querySelector('#bugTechnologyStack');
const bugProblem = document.querySelector('#bugProblem');
const bugSolution = document.querySelector('#bugSolution');
const bugMessage = document.querySelector('#bugMessage');
const saveBugButton = document.querySelector('#saveBugButton');
const workspaceStatus = document.querySelector('#workspaceStatus');
const patchDialog = document.querySelector('#patchDialog');
const closePatchDialog = document.querySelector('#closePatchDialog');
const cancelPatchButton = document.querySelector('#cancelPatchButton');
const confirmPatchButton = document.querySelector('#confirmPatchButton');
const patchDialogMessage = document.querySelector('#patchDialogMessage');
const patchDialogDescription = document.querySelector('#patchDialogDescription');
const patchDiffList = document.querySelector('#patchDiffList');
const loginView = document.querySelector('#loginView');
const loginForm = document.querySelector('#loginForm');
const loginEmail = document.querySelector('#loginEmail');
const loginPassword = document.querySelector('#loginPassword');
const loginError = document.querySelector('#loginError');
const routeStatus = document.querySelector('#routeStatus');
const studentView = document.querySelector('#studentView');
const teacherView = document.querySelector('#teacherView');
const studentIdentity = document.querySelector('#studentIdentity');
const teacherIdentity = document.querySelector('#teacherIdentity');
const studentLogoutButton = document.querySelector('#studentLogoutButton');
const teacherLogoutButton = document.querySelector('#teacherLogoutButton');
const studentLibraryButton = document.querySelector('#studentLibraryButton');
const teacherLibraryButton = document.querySelector('#teacherLibraryButton');
const joinCourseForm = document.querySelector('#joinCourseForm');
const joinCode = document.querySelector('#joinCode');
const studentMessage = document.querySelector('#studentMessage');
const studentCourses = document.querySelector('#studentCourses');
const studentCourseCount = document.querySelector('#studentCourseCount');
const createCourseForm = document.querySelector('#createCourseForm');
const courseName = document.querySelector('#courseName');
const teacherMessage = document.querySelector('#teacherMessage');
const teacherCourses = document.querySelector('#teacherCourses');
const teacherCourseCount = document.querySelector('#teacherCourseCount');
const publishForm = document.querySelector('#publishForm');
const publishCourse = document.querySelector('#publishCourse');
const publishExperiment = document.querySelector('#publishExperiment');
const teacherDrafts = document.querySelector('#teacherDrafts');
const teacherDashboardCourse = document.querySelector('#teacherDashboardCourse');
const teacherDashboardMessage = document.querySelector('#teacherDashboardMessage');
const teacherDashboardEmpty = document.querySelector('#teacherDashboardEmpty');
const teacherDashboardContent = document.querySelector('#teacherDashboardContent');
const teacherProgressRows = document.querySelector('#teacherProgressRows');
const teacherRunDetail = document.querySelector('#teacherRunDetail');
const teacherRunTitle = document.querySelector('#teacherRunTitle');
const teacherRunStatus = document.querySelector('#teacherRunStatus');
const teacherRunSummary = document.querySelector('#teacherRunSummary');
const teacherAiSummary = document.querySelector('#teacherAiSummary');
const teacherRunMetrics = document.querySelector('#teacherRunMetrics');
const teacherTimeline = document.querySelector('#teacherTimeline');
const teacherEvidenceMessage = document.querySelector('#teacherEvidenceMessage');
const teacherStageEvidence = document.querySelector('#teacherStageEvidence');
const teacherEvidenceFiles = document.querySelector('#teacherEvidenceFiles');
const teacherEvidenceCode = document.querySelector('#teacherEvidenceCode');
const teacherReviewForm = document.querySelector('#teacherReviewForm');
const teacherRating = document.querySelector('#teacherRating');
const teacherFeedback = document.querySelector('#teacherFeedback');
const teacherReviewMessage = document.querySelector('#teacherReviewMessage');
const teacherBugMessage = document.querySelector('#teacherBugMessage');
const teacherBugList = document.querySelector('#teacherBugList');
const libraryDashboardButton = document.querySelector('#libraryDashboardButton');
const libraryLogoutButton = document.querySelector('#libraryLogoutButton');
const libraryAccountLabel = document.querySelector('#libraryAccountLabel');
const createExperimentButton = document.querySelector('#createExperimentButton');
const createDialog = document.querySelector('#createDialog');
const closeCreateDialog = document.querySelector('#closeCreateDialog');
const libraryMainColumn = document.querySelector('#libraryMainColumn');
const libraryOverviewButton = document.querySelector('#libraryOverviewButton');
const sidebarExperimentCount = document.querySelector('#sidebarExperimentCount');
const createStepButton = document.querySelector('#createStepButton');
const createStepLabel = document.querySelector('#createStepLabel');
const createLoading = document.querySelector('#createLoading');
const createLoadingText = document.querySelector('#createLoadingText');
const learningGoal = document.querySelector('#learningGoal');
const directionTitle = document.querySelector('#directionTitle');
const directionDetail = document.querySelector('#directionDetail');
const durationHoursInput = document.querySelector('#durationHoursInput');
const durationPreview = document.querySelector('#durationPreview');
const createPlan = document.querySelector('#createPlan');
const planDirection = document.querySelector('#planDirection');
const planDuration = document.querySelector('#planDuration');
const planDifficulty = document.querySelector('#planDifficulty');
const planPurpose = document.querySelector('#planPurpose');
const planNodes = document.querySelector('#planNodes');
const creationConversation = document.querySelector('#creationConversation');
const directionReview = document.querySelector('#directionReview');
const directionTitleReview = document.querySelector('#directionTitleReview');
const directionDetailReview = document.querySelector('#directionDetailReview');
const recentExperiments = document.querySelector('#recentExperiments');
const personalExperimentList = document.querySelector('#personalExperimentList');
const defaultLearningGoalPlaceholder = learningGoal.placeholder;
let API_BASE_URL = '';
let API_BASE_URL_ERROR = '';
try {
  if (typeof window.BMApiConfig?.resolveApiBaseUrl !== 'function') {
    throw new Error('API 配置模块未加载。');
  }
  API_BASE_URL = window.BMApiConfig.resolveApiBaseUrl(window.location, window.BM_API_BASE_URL);
} catch (error) {
  API_BASE_URL_ERROR = error.message || 'API 地址配置无效。';
}
const STORAGE_KEYS = {
  pendingRoute: 'bmhs.demo.pendingRoute',
  teacherCourse: 'bmhs.teacherCourse',
  activeExperiment: 'bmhs.activeExperiment',
  terminalSessionPrefix: 'bmhs.terminalSession.',
};
let currentUser = null;
let sessionReady = false;
let pendingProtectedRoute = null;
let routeResolutionEpoch = 0;
let libraryLoadEpoch = 0;
let routeStatusTimer = 0;

let bubbleX = window.innerWidth / 2;
let bubbleY = window.innerHeight / 2;
let gridX = 0;
let gridY = 0;
let panState = null;
let bubbleDragState = null;
let createStep = 1;
let selectedDifficulty = '入门';
let selectedDuration = '1 天 0 小时';
let durationTotalHours = 24;
let createPlanReady = false;
let creationSessionId = null;
let directionReady = false;
let pendingPlan = null;
let createBusy = false;
let materializationKey = null;
let createDialogEpoch = 0;
let activeTreeNodes = [];
let selectedTreeNode = null;
let treeEdgeLayer = null;
let activeExperimentId = null;
let activeRun = null;
let activeFilePath = null;
let activeFileHash = null;
let activeFileSavedContent = '';
let activeWorkspaceFiles = [];
let workspaceFileSelectionEpoch = 0;
let terminalCommandInFlight = false;
let teacherDashboardEpoch = 0;
let teacherCourseDashboardEpoch = 0;
let taskProgressRequestEpoch = 0;
let taskProgressMutationInFlight = false;
const taskProgressButtonEpochs = new Map();
let activeTerminalSession = null;
let activeTerminalRunId = null;
let pendingPatchReview = null;
let patchReviewEpoch = 0;
let patchReviewSourceButton = null;
let patchReviewApplied = false;
let activeNodeId = null;
let selectedTeacherRunId = null;
let activeRunProgress = null;
let runProgressLoadFailed = false;

const experimentData = {};
const recentlyOpenedExperimentIds = [];

function getCurrentUser() {
  return currentUser;
}

function isRunContextCurrent(runId, epoch) {
  return epoch === routeResolutionEpoch && String(activeRun?.runId) === String(runId);
}

function setCurrentUser(user) {
  const nextUser = user && ['student', 'teacher'].includes(user.role)
    ? { ...user, name: user.name || user.displayName || user.email }
    : null;
  if (currentUser?.id !== nextUser?.id) {
    Object.keys(experimentData).forEach((id) => delete experimentData[id]);
    recentlyOpenedExperimentIds.length = 0;
    libraryLoadEpoch += 1;
    renderRecentExperiments();
  }
  currentUser = nextUser;
  routeResolutionEpoch += 1;
}

function setMessage(element, text, isError = false) {
  element.textContent = text;
  element.classList.toggle('is-error', isError);
}

function renderPublishedExperiment(item) {
  const row = document.createElement('div');
  row.className = 'published-item';
  const title = document.createElement('strong');
  title.textContent = item.name;
  const actions = document.createElement('div');
  actions.className = 'published-actions';
  const status = document.createElement('span');
  status.className = 'status-badge';
  status.textContent = '已发布';
  const openButton = document.createElement('button');
  openButton.className = 'text-action open-published-experiment';
  openButton.type = 'button';
  openButton.dataset.experimentId = item.experimentId;
  openButton.textContent = '打开';
  actions.append(status, openButton);
  row.append(title, actions);
  return row;
}

async function renderStudentDashboard() {
  const user = getCurrentUser();
  if (!user || user.role !== 'student') return;
  studentIdentity.textContent = `${user.name} · ${user.email}`;
  studentCourseCount.textContent = '加载中…';
  studentCourses.replaceChildren();
  try {
    const enrolled = await apiRequest('/courses', { method: 'GET' });
    if (!Array.isArray(enrolled)) throw new Error('课程数据格式无效，请刷新后重试。');
    studentCourseCount.textContent = `${enrolled.length} 门课程`;
    if (!enrolled.length) {
      const empty = document.createElement('p');
      empty.className = 'course-empty';
      empty.textContent = '还没有加入课程，请使用上方邀请码加入。';
      studentCourses.append(empty);
      return;
    }
    enrolled.forEach((course) => {
      const card = document.createElement('article');
      card.className = 'course-card';
      const header = document.createElement('div');
      header.className = 'course-card-header';
      const copy = document.createElement('div');
      const title = document.createElement('h3');
      title.textContent = course.name;
      const description = document.createElement('p');
      description.textContent = '教师已发布的实验会显示在这里。';
      copy.append(title, description);
      const code = document.createElement('span');
      code.className = 'invite-code';
      code.textContent = course.inviteCode;
      header.append(copy, code);
      const publishedList = document.createElement('div');
      publishedList.className = 'published-list';
      const published = Array.isArray(course.experiments) ? course.experiments : [];
      if (!published.length) {
        const empty = document.createElement('p');
        empty.className = 'course-card-footer';
        empty.textContent = '等待教师发布实验';
        publishedList.append(empty);
      } else {
        published.forEach((item) => publishedList.append(renderPublishedExperiment({
          ...item, experimentId: item.experimentId,
        })));
      }
      card.append(header, publishedList);
      studentCourses.append(card);
    });
  } catch (error) {
    studentCourseCount.textContent = '加载失败';
    const message = document.createElement('p');
    message.className = 'course-empty';
    message.textContent = error.message || '课程加载失败，请刷新后重试。';
    studentCourses.append(message);
  }
}

function formatTeacherDate(value) {
  if (!value) return '尚未开始';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '时间未知' : date.toLocaleString('zh-CN', { hour12: false });
}

function resetTeacherRunDetail() {
  teacherEvidenceRequestEpoch += 1;
  teacherEvidenceSelectionEpoch += 1;
  teacherRunDetailRequestEpoch += 1;
  selectedTeacherRunId = null;
  teacherRunDetail.hidden = true;
  teacherRunTitle.textContent = '学生运行详情';
  teacherRunStatus.textContent = '';
  teacherRunSummary.textContent = '';
  teacherAiSummary.textContent = '';
  teacherAiSummary.hidden = true;
  teacherRunMetrics.replaceChildren();
  teacherTimeline.replaceChildren();
  teacherStageEvidence.replaceChildren();
  teacherEvidenceFiles.replaceChildren();
  teacherEvidenceCode.textContent = '选择一个阶段快照文件查看。';
  setMessage(teacherEvidenceMessage, '');
  teacherRating.value = '';
  teacherFeedback.value = '';
  setMessage(teacherReviewMessage, '');
}

async function openTeacherSnapshot(runId, snapshotId, requestEpoch) {
  const selectionEpoch = ++teacherEvidenceSelectionEpoch;
  teacherEvidenceFiles.replaceChildren();
  teacherEvidenceCode.textContent = '正在加载快照文件…';
  setMessage(teacherEvidenceMessage, '正在读取阶段代码证据…');
  try {
    const files = await apiRequest(`/teacher/runs/${runId}/snapshots/${snapshotId}/files`, { method: 'GET' });
    if (requestEpoch !== teacherEvidenceRequestEpoch || selectionEpoch !== teacherEvidenceSelectionEpoch
        || selectedTeacherRunId !== Number(runId)) return;
    setMessage(teacherEvidenceMessage, '快照只读；此处不会修改学生工作区。');
    if (!Array.isArray(files) || !files.length) {
      teacherEvidenceCode.textContent = '该阶段快照没有文件。';
      return;
    }
    files.forEach((file) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'teacher-evidence-file';
      button.textContent = file.text ? `${file.path} · ${file.sizeBytes} B` : `${file.path} · 不支持的文本格式`;
      button.disabled = !file.text;
      button.addEventListener('click', async () => {
        const fileSelectionEpoch = ++teacherEvidenceSelectionEpoch;
        teacherEvidenceCode.textContent = '正在读取文件…';
        const encodedPath = String(file.path).split('/').map(encodeURIComponent).join('/');
        try {
          const result = await apiRequest(`/teacher/runs/${runId}/snapshots/${snapshotId}/files/${encodedPath}`, { method: 'GET' });
          if (requestEpoch !== teacherEvidenceRequestEpoch || fileSelectionEpoch !== teacherEvidenceSelectionEpoch
              || selectedTeacherRunId !== Number(runId)) return;
          teacherEvidenceCode.textContent = result.content ?? '';
          setMessage(teacherEvidenceMessage, `只读查看：${file.path}`);
        } catch (error) {
          if (requestEpoch === teacherEvidenceRequestEpoch && fileSelectionEpoch === teacherEvidenceSelectionEpoch) {
            teacherEvidenceCode.textContent = '文件读取失败。';
            setMessage(teacherEvidenceMessage, error.message || '文件读取失败，请重试。', true);
          }
        }
      });
      teacherEvidenceFiles.append(button);
    });
    teacherEvidenceCode.textContent = '选择左侧文件查看快照内容。';
  } catch (error) {
    if (requestEpoch !== teacherEvidenceRequestEpoch || selectionEpoch !== teacherEvidenceSelectionEpoch) return;
    teacherEvidenceCode.textContent = '阶段代码证据读取失败。';
    setMessage(teacherEvidenceMessage, error.message || '快照文件加载失败，请重试。', true);
  }
}

async function loadTeacherStageEvidence(runId) {
  const requestEpoch = ++teacherEvidenceRequestEpoch;
  teacherStageEvidence.replaceChildren();
  teacherEvidenceFiles.replaceChildren();
  teacherEvidenceCode.textContent = '选择一个阶段快照文件查看。';
  setMessage(teacherEvidenceMessage, '正在加载阶段完成记录…');
  try {
    const stages = await apiRequest(`/teacher/runs/${runId}/stages`, { method: 'GET' });
    if (requestEpoch !== teacherEvidenceRequestEpoch || selectedTeacherRunId !== Number(runId)) return;
    if (!Array.isArray(stages) || !stages.length) {
      setMessage(teacherEvidenceMessage, '暂无阶段节点记录。');
      return;
    }
    stages.forEach((stage) => {
      const row = document.createElement('article');
      row.className = 'teacher-stage-evidence-item';
      const title = document.createElement('strong');
      title.textContent = `${stage.stageName || '未分组阶段'} · ${stage.nodeName || '未命名节点'}`;
      const status = document.createElement('span');
      status.textContent = stage.status === 'completed' ? `已完成 · ${formatTeacherDate(stage.completedAt)}` : stage.status || '未开始';
      row.append(title, status);
      if (stage.snapshotId) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'secondary-action';
        button.textContent = '查看完成时代码';
        button.addEventListener('click', () => void openTeacherSnapshot(runId, stage.snapshotId, requestEpoch));
        row.append(button);
      } else {
        const missing = document.createElement('small');
        missing.textContent = stage.status === 'completed' ? '该历史节点没有留存代码快照。' : '节点完成后会生成只读代码快照。';
        row.append(missing);
      }
      teacherStageEvidence.append(row);
    });
    setMessage(teacherEvidenceMessage, '快照只读；历史节点缺少快照时不会用当前代码代替。');
  } catch (error) {
    if (requestEpoch !== teacherEvidenceRequestEpoch) return;
    setMessage(teacherEvidenceMessage, error.message || '阶段记录加载失败，请重试。', true);
  }
}

function clearTeacherBugReview() {
  teacherBugRequestEpoch += 1;
  teacherBugList.replaceChildren();
  setMessage(teacherBugMessage, '');
}

let teacherBugRequestEpoch = 0;
let teacherEvidenceRequestEpoch = 0;
let teacherEvidenceSelectionEpoch = 0;
let teacherRunDetailRequestEpoch = 0;

async function loadTeacherBugCases(courseId) {
  if (!/^\d+$/.test(String(courseId))) {
    clearTeacherBugReview();
    return;
  }
  const requestEpoch = ++teacherBugRequestEpoch;
  teacherBugList.replaceChildren();
  setMessage(teacherBugMessage, '正在加载 Bug 案例…');
  try {
    const bugs = await apiRequest(`/bugs?courseId=${encodeURIComponent(courseId)}`, { method: 'GET' });
    if (requestEpoch !== teacherBugRequestEpoch || teacherDashboardCourse.value !== String(courseId)) return;
    setMessage(teacherBugMessage, '');
    if (!Array.isArray(bugs) || !bugs.length) {
      const empty = document.createElement('p');
      empty.className = 'monitor-empty';
      empty.textContent = '当前课程还没有 Bug 案例。';
      teacherBugList.append(empty);
      return;
    }
    bugs.forEach((bug) => {
      const card = document.createElement('article');
      card.className = 'teacher-bug-item';
      const heading = document.createElement('div');
      heading.className = 'teacher-bug-item-heading';
      const title = document.createElement('strong');
      title.textContent = bug.title || '未命名案例';
      const status = document.createElement('span');
      status.className = `bug-status bug-status-${bug.status || 'pending'}`;
      status.textContent = bug.status === 'approved' ? '已通过' : bug.status === 'rejected' ? '已拒绝' : '待审核';
      heading.append(title, status);
      const problem = document.createElement('p');
      problem.textContent = bug.problem || '未填写问题现象。';
      card.append(heading, problem);
      if (bug.solution) {
        const solution = document.createElement('p');
        solution.className = 'teacher-bug-solution';
        solution.textContent = `解决方法：${bug.solution}`;
        card.append(solution);
      }
      if (bug.status === 'pending') {
        const actions = document.createElement('div');
        actions.className = 'teacher-bug-actions';
        const feedback = document.createElement('input');
        feedback.type = 'text';
        feedback.maxLength = 1000;
        feedback.placeholder = '审核意见（可选）';
        const approve = document.createElement('button');
        approve.className = 'primary-action';
        approve.type = 'button';
        approve.textContent = '通过并入库';
        const reject = document.createElement('button');
        reject.className = 'secondary-action';
        reject.type = 'button';
        reject.textContent = '拒绝';
        const review = async (decision) => {
          approve.disabled = true;
          reject.disabled = true;
          setMessage(teacherBugMessage, decision === 'approved' ? '正在审核并建立向量索引…' : '正在保存拒绝结果…');
          try {
            await apiRequest(`/bugs/${bug.id}/approve`, {
              method: 'POST', body: JSON.stringify({ decision, feedback: feedback.value.trim() }), timeoutMs: 30000,
            });
            if (teacherDashboardCourse.value === String(courseId)) await loadTeacherBugCases(courseId);
          } catch (error) {
            if (teacherDashboardCourse.value !== String(courseId)) return;
            approve.disabled = false;
            reject.disabled = false;
            setMessage(teacherBugMessage, error.message || 'Bug 审核失败，请稍后重试。', true);
          }
        };
        approve.addEventListener('click', () => void review('approved'));
        reject.addEventListener('click', () => void review('rejected'));
        actions.append(feedback, approve, reject);
        card.append(actions);
      } else {
        const vector = document.createElement('small');
        vector.className = 'teacher-bug-vector';
        vector.textContent = `RAG 索引：${bug.vectorStatus || '未建立'}`;
        card.append(vector);
      }
      teacherBugList.append(card);
    });
  } catch (error) {
    if (requestEpoch !== teacherBugRequestEpoch) return;
    setMessage(teacherBugMessage, error.message || 'Bug 案例加载失败，请稍后重试。', true);
  }
}

async function loadTeacherRunDetail(runId, studentName) {
  if (!/^\d+$/.test(String(runId)) || Number(runId) <= 0) {
    resetTeacherRunDetail();
    setMessage(teacherDashboardMessage, '该学生尚未创建实验运行实例。');
    return;
  }
  resetTeacherRunDetail();
  const requestEpoch = ++teacherRunDetailRequestEpoch;
  teacherRunDetail.hidden = false;
  selectedTeacherRunId = Number(runId);
  teacherRunTitle.textContent = `${studentName || '学生'} 的学习详情`;
  teacherRunStatus.textContent = '加载中…';
  try {
    const [summary, timeline] = await Promise.all([
      apiRequest(`/teacher/runs/${runId}/summary`, { method: 'GET' }),
      apiRequest(`/teacher/runs/${runId}/timeline`, { method: 'GET' }),
    ]);
    if (requestEpoch !== teacherRunDetailRequestEpoch || selectedTeacherRunId !== Number(runId)) return;
    teacherRunStatus.textContent = summary.runStatus || '未知状态';
    teacherRunSummary.textContent = `${summary.experimentName || '实验'} · 节点完成 ${summary.completedNodes}/${summary.totalNodes} · 任务完成 ${summary.completedTasks || 0}/${summary.totalTasks || 0}`;
    if (summary.aiSummaryAvailable && summary.aiSummary) {
      teacherAiSummary.textContent = `AI 过程摘要：${summary.aiSummary}`;
      teacherAiSummary.hidden = false;
    }
    [[summary.completedNodes, '已完成节点'], [summary.completedTasks, '已完成任务'], [summary.commandCount, '终端命令'], [summary.messageCount, 'AI 对话'], [summary.bugCount, 'Bug 记录']].forEach(([value, label]) => {
      const metric = document.createElement('div');
      metric.className = 'run-metric';
      const number = document.createElement('strong');
      number.textContent = String(value ?? 0);
      const caption = document.createElement('span');
      caption.textContent = label;
      metric.append(number, caption);
      teacherRunMetrics.append(metric);
    });
    if (summary.review) {
      teacherRating.value = summary.review.rating ?? '';
      teacherFeedback.value = summary.review.feedback || '';
    }
    void loadTeacherStageEvidence(runId);
    if (!Array.isArray(timeline) || !timeline.length) {
      const empty = document.createElement('p');
      empty.className = 'monitor-empty';
      empty.textContent = '暂时没有过程记录。';
      teacherTimeline.append(empty);
    } else {
      timeline.slice(-30).forEach((event) => {
        const item = document.createElement('div');
        item.className = 'timeline-item';
        const meta = document.createElement('span');
        meta.textContent = `${event.source || '记录'} · ${formatTeacherDate(event.createdAt)}`;
        const title = document.createElement('strong');
        title.textContent = event.title || '未命名记录';
        const detail = document.createElement('p');
        detail.textContent = event.detail || '';
        item.append(meta, title, detail);
        teacherTimeline.append(item);
      });
    }
  } catch (error) {
    if (requestEpoch !== teacherRunDetailRequestEpoch || selectedTeacherRunId !== Number(runId)) return;
    resetTeacherRunDetail();
    setMessage(teacherDashboardMessage, error.message || '学生详情加载失败，请稍后重试。', true);
  }
}

async function loadTeacherCourseDashboard(courseId) {
  const requestEpoch = ++teacherCourseDashboardEpoch;
  if (!/^\d+$/.test(String(courseId))) {
    teacherDashboardContent.hidden = true;
    teacherDashboardEmpty.hidden = false;
    resetTeacherRunDetail();
    clearTeacherBugReview();
    return;
  }
  clearTeacherBugReview();
  try {
    window.localStorage.setItem(STORAGE_KEYS.teacherCourse, String(courseId));
  } catch {
    // Course selection remains usable when storage is blocked by browser policy.
  }
  teacherDashboardEmpty.hidden = true;
  teacherDashboardContent.hidden = false;
  teacherProgressRows.replaceChildren();
  resetTeacherRunDetail();
  setMessage(teacherDashboardMessage, '正在加载学生进度…');
  try {
    const dashboard = await apiRequest(`/teacher/courses/${courseId}/dashboard`, { method: 'GET' });
    if (requestEpoch !== teacherCourseDashboardEpoch || teacherDashboardCourse.value !== String(courseId)) return;
    setMessage(teacherDashboardMessage, `${dashboard.courseName || '课程'} · ${dashboard.memberCount || 0} 名学生 · ${dashboard.experimentCount || 0} 个实验`);
    void loadTeacherBugCases(courseId);
    const students = Array.isArray(dashboard.students) ? dashboard.students : [];
    if (!students.length) {
      const row = document.createElement('tr');
      const cell = document.createElement('td');
      cell.colSpan = 6;
      cell.className = 'table-empty';
      cell.textContent = '课程中还没有学生运行记录。';
      row.append(cell);
      teacherProgressRows.append(row);
      return;
    }
    students.forEach((student) => {
      const row = document.createElement('tr');
      const identity = document.createElement('td');
      const name = document.createElement('strong');
      name.textContent = student.displayName || student.email || '未命名学生';
      const email = document.createElement('small');
      email.textContent = student.email || '';
      identity.append(name, email);
      const experiment = document.createElement('td');
      experiment.textContent = student.experimentName || '尚未分配';
      const progress = document.createElement('td');
      const total = Number(student.totalNodes || 0);
      const completed = Number(student.completedNodes || 0);
      const totalTasks = Number(student.totalTasks || 0);
      const completedTasks = Number(student.completedTasks || 0);
      progress.textContent = total
        ? `${completed}/${total} 节点 · ${completedTasks}/${totalTasks} 任务`
        : '未开始';
      const status = document.createElement('td');
      status.textContent = student.runStatus || 'not_started';
      const lastOpened = document.createElement('td');
      lastOpened.textContent = formatTeacherDate(student.lastOpenedAt);
      const action = document.createElement('td');
      const button = document.createElement('button');
      button.className = 'text-action';
      button.type = 'button';
      button.textContent = student.runId ? '查看详情' : '未开始';
      button.disabled = !student.runId;
      button.addEventListener('click', () => void loadTeacherRunDetail(student.runId, student.displayName));
      action.append(button);
      row.append(identity, experiment, progress, status, lastOpened, action);
      teacherProgressRows.append(row);
    });
  } catch (error) {
    if (requestEpoch !== teacherCourseDashboardEpoch || teacherDashboardCourse.value !== String(courseId)) return;
    teacherDashboardContent.hidden = true;
    teacherDashboardEmpty.hidden = false;
    setMessage(teacherDashboardMessage, error.message || '课程进度加载失败，请稍后重试。', true);
    clearTeacherBugReview();
  }
}

async function renderTeacherDashboard() {
  const user = getCurrentUser();
  if (!user || user.role !== 'teacher') return;
  const requestEpoch = ++teacherDashboardEpoch;
  teacherIdentity.textContent = `${user.name} · ${user.email}`;
  teacherCourseCount.textContent = '加载中…';
  teacherCourses.replaceChildren();
  publishCourse.replaceChildren();
  publishExperiment.replaceChildren();
  teacherDrafts.replaceChildren();
  teacherDashboardCourse.replaceChildren();
  const dashboardPlaceholder = document.createElement('option');
  dashboardPlaceholder.value = '';
  dashboardPlaceholder.textContent = '请选择课程';
  teacherDashboardCourse.append(dashboardPlaceholder);
  teacherDashboardContent.hidden = true;
  teacherDashboardEmpty.hidden = false;
  resetTeacherRunDetail();
  clearTeacherBugReview();
  try {
    const [owned, drafts] = await Promise.all([
      apiRequest('/courses', { method: 'GET' }),
      apiRequest('/experiments/drafts', { method: 'GET' }),
    ]);
    if (requestEpoch !== teacherDashboardEpoch || getCurrentUser()?.id !== user.id) return;
    teacherCourseCount.textContent = `${owned.length} 门课程`;
    const coursePlaceholder = document.createElement('option');
    coursePlaceholder.value = '';
    coursePlaceholder.textContent = owned.length ? '请选择课程' : '请先创建课程';
    publishCourse.append(coursePlaceholder);
    owned.forEach((course) => {
      const option = document.createElement('option');
      option.value = course.id;
      option.textContent = course.name;
      publishCourse.append(option);
      const card = document.createElement('article');
      card.className = 'course-card';
      const header = document.createElement('div');
      header.className = 'course-card-header';
      const title = document.createElement('h3');
      title.textContent = course.name;
      const code = document.createElement('span');
      code.className = 'invite-code';
      code.textContent = course.inviteCode;
      header.append(title, code);
      const footer = document.createElement('div');
      footer.className = 'course-card-footer';
      footer.textContent = `${course.memberCount || 0} 名学生 · ${(course.experiments || []).length} 个已发布实验`;
      card.append(header, footer);
      teacherCourses.append(card);
      const dashboardOption = document.createElement('option');
      dashboardOption.value = course.id;
      dashboardOption.textContent = course.name;
      teacherDashboardCourse.append(dashboardOption);
    });
    if (!owned.length) {
      const empty = document.createElement('p');
      empty.className = 'course-empty';
      empty.textContent = '还没有课程，请先创建一个课程。';
      teacherCourses.append(empty);
    } else {
      const savedCourseId = window.localStorage.getItem(STORAGE_KEYS.teacherCourse);
      const selectedCourseId = owned.some((course) => String(course.id) === savedCourseId)
        ? savedCourseId : String(owned[0].id);
      teacherDashboardCourse.value = selectedCourseId;
      void loadTeacherCourseDashboard(selectedCourseId);
    }
    const draftPlaceholder = document.createElement('option');
    draftPlaceholder.value = '';
    draftPlaceholder.textContent = drafts.length ? '请选择实验' : '暂无可发布实验';
    publishExperiment.append(draftPlaceholder);
    drafts.forEach((draft) => {
      const option = document.createElement('option');
      option.value = draft.id;
      option.textContent = draft.name;
      publishExperiment.append(option);
      const row = document.createElement('div');
      row.className = 'draft-item';
      const title = document.createElement('strong');
      title.textContent = draft.name;
      const status = document.createElement('span');
      status.className = 'section-count';
      status.textContent = draft.status === 'published' ? '已发布' : '草稿';
      row.append(title, status);
      teacherDrafts.append(row);
    });
  } catch (error) {
    if (requestEpoch !== teacherDashboardEpoch || getCurrentUser()?.id !== user.id) return;
    teacherCourseCount.textContent = '加载失败';
    const message = document.createElement('p');
    message.className = 'course-empty';
    message.textContent = error.message || '课程加载失败，请刷新后重试。';
    teacherCourses.append(message);
  }
}

function hideProtectedViews() {
  [libraryView, canvasView, editorView].forEach((view) => view.classList.add('auth-hidden'));
  canvasView.classList.add('is-hidden');
  editorView.classList.remove('is-visible');
}

function setRouteStatus(message = '', isError = false) {
  if (!routeStatus) return;
  window.clearTimeout(routeStatusTimer);
  routeStatus.textContent = message;
  routeStatus.hidden = !message;
  routeStatus.classList.toggle('is-error', isError);
  if (message) {
    routeStatusTimer = window.setTimeout(() => {
      routeStatus.hidden = true;
      routeStatus.textContent = '';
    }, 7000);
  }
}

function parseHashRoute(hash = window.location.hash) {
  const path = hash.replace(/^#\/?/, '').replace(/\/$/, '');
  if (!path) return { type: 'default' };
  if (['login', 'student', 'teacher', 'library'].includes(path)) return { type: path };
  const canvasMatch = path.match(/^experiments\/([^/]+)\/canvas$/);
  if (canvasMatch && /^[a-zA-Z0-9_-]+$/.test(canvasMatch[1])) {
    return { type: 'canvas', experimentId: decodeURIComponent(canvasMatch[1]) };
  }
  const editorMatch = path.match(/^runs\/(\d+)\/editor$/);
  if (editorMatch && Number(editorMatch[1]) > 0) return { type: 'editor', runId: editorMatch[1] };
  return { type: 'invalid' };
}

function routeHash(route) {
  if (route.type === 'canvas') return `#/experiments/${encodeURIComponent(route.experimentId)}/canvas`;
  if (route.type === 'editor') return `#/runs/${encodeURIComponent(route.runId)}/editor`;
  return `#/${route.type}`;
}

function readSessionValue(key) {
  try {
    return window.sessionStorage.getItem(key);
  } catch (error) {
    return null;
  }
}

function writeSessionValue(key, value) {
  try {
    window.sessionStorage.setItem(key, value);
  } catch (error) {
    // Route recovery can still use the in-memory pendingProtectedRoute.
  }
}

function removeSessionValue(key) {
  try {
    window.sessionStorage.removeItem(key);
  } catch (error) {
    // Storage may be unavailable in restricted browser contexts.
  }
}

function terminalSessionStorageKey(runId) {
  return `${STORAGE_KEYS.terminalSessionPrefix}${runId}`;
}

function readTerminalSession(run) {
  const key = terminalSessionStorageKey(run.runId);
  const serialized = readSessionValue(key);
  if (!serialized) return null;
  try {
    const session = JSON.parse(serialized);
    if (String(session.workspaceId) === String(run.workspaceId)
      && /^\d+$/.test(String(session.id))
      && session.status === 'open') {
      return session;
    }
  } catch (error) {
    // Ignore stale or malformed session state and request a fresh session.
  }
  removeSessionValue(key);
  return null;
}

function persistTerminalSession(run, session) {
  if (session?.status !== 'open' || !/^\d+$/.test(String(session.id))
    || String(session.workspaceId) !== String(run.workspaceId)) return;
  writeSessionValue(terminalSessionStorageKey(run.runId), JSON.stringify(session));
}

function clearCachedTerminalSessions() {
  try {
    const keys = [];
    for (let index = 0; index < window.sessionStorage.length; index += 1) {
      const key = window.sessionStorage.key(index);
      if (key?.startsWith(STORAGE_KEYS.terminalSessionPrefix)) keys.push(key);
    }
    keys.forEach(removeSessionValue);
  } catch (error) {
    // Cached terminal IDs are scoped to this tab and are not authentication credentials.
  }
}

function persistPendingRoute(route) {
  if (!['student', 'teacher', 'library', 'canvas', 'editor'].includes(route?.type)) return;
  const serialized = routeHash(route);
  const parsed = parseHashRoute(serialized);
  if (!parsed || parsed.type !== route.type) return;
  writeSessionValue(STORAGE_KEYS.pendingRoute, serialized);
}

function readPendingRoute() {
  const serialized = readSessionValue(STORAGE_KEYS.pendingRoute);
  if (!serialized) return null;
  const route = parseHashRoute(serialized);
  if (!['student', 'teacher', 'library', 'canvas', 'editor'].includes(route.type)) {
    removeSessionValue(STORAGE_KEYS.pendingRoute);
    return null;
  }
  return route;
}

function navigateTo(route, { replace = false } = {}) {
  const nextHash = routeHash(route);
  if (window.location.hash === nextHash) {
    if (sessionReady) void resolveCurrentRoute();
    return;
  }
  if (replace) {
    window.location.replace(nextHash);
    return;
  }
  window.location.hash = nextHash;
}

function showRoute(route) {
  navigateTo({ type: route });
}

function renderSimpleRoute(route) {
  const user = getCurrentUser();
  loginView.classList.remove('is-visible');
  studentView.classList.remove('is-visible');
  teacherView.classList.remove('is-visible');
  hideProtectedViews();
  if (route === 'login') {
    loginView.classList.add('is-visible');
    return;
  }
  if (route === 'student' && user.role === 'student') {
    studentView.classList.add('is-visible');
    renderStudentDashboard();
    return;
  }
  if (route === 'teacher' && user.role === 'teacher') {
    teacherView.classList.add('is-visible');
    renderTeacherDashboard();
    return;
  }
  if (route === 'library') {
    libraryView.classList.remove('auth-hidden');
    libraryView.style.display = 'block';
    libraryAccountLabel.textContent = `${user.name} · 实验库`;
    return;
  }
}

function redirectToRoleHome(message = '') {
  const role = getCurrentUser()?.role;
  const fallback = role === 'teacher' ? 'teacher' : role === 'student' ? 'student' : 'login';
  if (message) setRouteStatus(message, true);
  navigateTo({ type: fallback }, { replace: true });
}

async function resolveCurrentRoute() {
  if (!sessionReady) return;
  const epoch = ++routeResolutionEpoch;
  const route = parseHashRoute();
  const user = getCurrentUser();
  if (route.type === 'invalid') {
    redirectToRoleHome('页面地址无效，已返回登录页或本角色工作台。');
    return;
  }
  if (route.type === 'default') {
    navigateTo({ type: user?.role || 'login' }, { replace: true });
    return;
  }
  if (route.type === 'login') {
    renderSimpleRoute('login');
    return;
  }
  if (!user) {
    pendingProtectedRoute = route;
    persistPendingRoute(route);
    setRouteStatus('请先登录，登录成功后会返回你打开的页面。');
    renderSimpleRoute('login');
    navigateTo({ type: 'login' }, { replace: true });
    return;
  }
  const requiredRole = route.type === 'teacher' ? 'teacher'
    : ['student', 'canvas', 'editor'].includes(route.type) ? 'student' : null;
  if (requiredRole && user.role !== requiredRole) {
    redirectToRoleHome('当前账号无权访问此页面，已返回本角色工作台。');
    return;
  }

  if (route.type === 'library') {
    renderSimpleRoute('library');
    await loadPersonalExperiments(epoch);
    if (epoch === routeResolutionEpoch) setRouteStatus('');
    return;
  }

  if (route.type === 'student' || route.type === 'teacher') {
    renderSimpleRoute(route.type);
    return;
  }

  setRouteStatus(route.type === 'canvas' ? '正在恢复实验画布…' : '正在恢复已有工作区…');
  try {
    if (route.type === 'canvas') {
      let experiment = experimentData[route.experimentId];
      if (/^\d+$/.test(route.experimentId)) {
        experiment = await apiRequest(`/experiments/${encodeURIComponent(route.experimentId)}`, { method: 'GET', timeoutMs: 15000 });
      }
      if (epoch !== routeResolutionEpoch) return;
      if (!experiment || !experiment.name) throw new Error('实验不存在或当前账号无权访问。');
      experimentData[route.experimentId] = experiment;
      renderExperimentCanvas(route.experimentId);
      setRouteStatus('');
      return;
    }
    if (route.type === 'editor') {
      await restoreExistingRun(route.runId, epoch);
      if (epoch === routeResolutionEpoch) setRouteStatus('');
      return;
    }
    redirectToRoleHome();
  } catch (error) {
    if (epoch !== routeResolutionEpoch) return;
    if (error.status === 401) {
      setCurrentUser(null);
      pendingProtectedRoute = route;
      persistPendingRoute(route);
      renderSimpleRoute('login');
      setRouteStatus('登录状态已失效，请重新登录后继续。', true);
      navigateTo({ type: 'login' }, { replace: true });
      return;
    }
    const message = error.message || '页面恢复失败，请从工作台重新打开。';
    redirectToRoleHome(message);
  }
}

function compareTreeSiblings(left, right) {
  const leftX = left.canvasX;
  const rightX = right.canvasX;
  const leftHasX = typeof leftX === 'number' && Number.isFinite(leftX);
  const rightHasX = typeof rightX === 'number' && Number.isFinite(rightX);
  if (leftHasX !== rightHasX) return leftHasX ? -1 : 1;
  if (leftHasX && rightHasX && leftX !== rightX) return leftX - rightX;
  return String(left.key) < String(right.key) ? -1 : String(left.key) > String(right.key) ? 1 : 0;
}

function buildTreeNodeNumbers(nodes) {
  const childrenByParent = new Map();
  const roots = [];
  nodes.forEach((node) => {
    if (node.parentKey == null || node.parentKey === '') {
      roots.push(node);
      return;
    }
    if (!childrenByParent.has(node.parentKey)) childrenByParent.set(node.parentKey, []);
    childrenByParent.get(node.parentKey).push(node);
  });
  childrenByParent.forEach((children) => children.sort(compareTreeSiblings));
  roots.sort(compareTreeSiblings);

  const numbers = new Map();
  const visited = new Set();
  const visitChildren = (parent, parentNumber) => {
    const children = childrenByParent.get(parent.key) || [];
    children.forEach((child, index) => {
      if (visited.has(child.key)) return;
      const number = parentNumber ? `${parentNumber}.${index + 1}` : String(index + 1);
      numbers.set(child.key, number);
      visited.add(child.key);
      visitChildren(child, number);
    });
  };
  const hasSingleRoot = roots.length === 1;
  roots.forEach((root, index) => {
    if (visited.has(root.key)) return;
    const rootNumber = hasSingleRoot ? '' : String(index + 1);
    numbers.set(root.key, hasSingleRoot ? 'start' : rootNumber);
    visited.add(root.key);
    visitChildren(root, rootNumber);
  });
  return numbers;
}

function getCanvasAnchorNode(nodes, renderedRootKey) {
  return nodes.find((node) => node.key === renderedRootKey)
    || nodes.find((node) => node.parentKey == null || node.parentKey === '')
    || null;
}

function renderCanvas() {
  bubble.style.left = `${bubbleX}px`;
  bubble.style.top = `${bubbleY}px`;
  const root = getCanvasAnchorNode(activeTreeNodes, bubble.dataset.treeNodeKey);
  document.querySelectorAll('.bubble.tree-node[data-tree-node-key]').forEach((element) => {
    const node = activeTreeNodes.find((item) => item.key === element.dataset.treeNodeKey);
    if (!node || !root || element === bubble) return;
    element.style.left = `${bubbleX + Number(node.canvasX) - Number(root.canvasX)}px`;
    element.style.top = `${bubbleY + Number(node.canvasY) - Number(root.canvasY)}px`;
  });
  canvasView.style.backgroundPosition = `${gridX}px ${gridY}px`;
  renderTreeEdges();
}

function selectBubble() {
  selectTreeNode(bubble);
}

function closeInspector() {
  document.querySelectorAll('.bubble.is-selected').forEach((node) => node.classList.remove('is-selected'));
  inspector.classList.remove('is-open');
}

function selectTreeNode(element) {
  document.querySelectorAll('.bubble.is-selected').forEach((node) => node.classList.remove('is-selected'));
  element.classList.add('is-selected');
  selectedTreeNode = activeTreeNodes.find((node) => node.key === element.dataset.treeNodeKey) || null;
  activeNodeId = selectedTreeNode?.nodeId || null;
  completeNodeButton.disabled = true;
  deepDiagnosisButton.disabled = true;
  recordBugButton.disabled = true;
  if (selectedTreeNode) {
    inspector.querySelector('h2').textContent = selectedTreeNode.title;
    inspector.querySelector('.inspector-intro').textContent = selectedTreeNode.description;
    inspector.querySelector('.inspector-sequence span').textContent =
      buildTreeNodeNumbers(activeTreeNodes).get(selectedTreeNode.key) || selectedTreeNode.key;
    inspector.querySelector('.node-fact:nth-child(1) dd').textContent = `约 ${selectedTreeNode.estimatedMinutes} 分钟`;
    inspector.querySelector('.node-fact:nth-child(3) dd').textContent = '启动工作区后查看实际文件路径';
    inspector.querySelector('.node-fact:nth-child(4) dd').textContent = selectedTreeNode.title;
    const taskList = editorView.querySelector('.task-list');
    taskList.replaceChildren();
    renderSelectedNodeTasks();
  }
  updateCompleteNodeControl();
  inspector.classList.add('is-open');
}

function renderSelectedNodeTasks() {
  const taskList = editorView.querySelector('.task-list');
  const taskCount = editorView.querySelector('.panel-count');
  const tasks = Array.isArray(selectedTreeNode?.tasks) ? selectedTreeNode.tasks : [];
  const progressRows = Array.isArray(activeRunProgress?.tasks) ? activeRunProgress.tasks : [];
  const nodeProgress = activeRunProgress?.nodes?.find((item) => Number(item.nodeId) === Number(selectedTreeNode?.nodeId));
  const canUpdate = Boolean(activeRun && nodeProgress && !['locked', 'completed'].includes(nodeProgress.status));
  let completedCount = 0;
  taskList.replaceChildren();
  if (tasks.length === 0) {
    const emptyState = document.createElement('p');
    emptyState.className = 'task-empty-state';
    emptyState.textContent = '本节点没有细分任务。进度正常时，可直接手动完成节点。';
    taskList.append(emptyState);
  }
  tasks.forEach((task) => {
    const taskId = Number(task.taskId);
    const progress = progressRows.find((item) => Number(item.taskId) === taskId
      && Number(item.nodeId) === Number(selectedTreeNode.nodeId));
    const completed = progress?.status === 'completed';
    if (completed) completedCount += 1;
    const button = document.createElement('button');
    button.className = `task-item${completed ? ' is-completed' : ''}`;
    button.type = 'button';
    button.setAttribute('aria-pressed', String(completed));
    const stateLabel = !activeRunProgress ? '进度未加载' : progress ? (completed ? '已完成' : '待完成') : '进度缺失';
    button.setAttribute('aria-label', `${task.title}，${stateLabel}`);
    button.disabled = taskProgressMutationInFlight || !canUpdate || !activeRunProgress
      || !Number.isSafeInteger(taskId) || taskId <= 0 || !progress;
    const check = document.createElement('span');
    check.className = 'task-check';
    check.setAttribute('aria-hidden', 'true');
    const title = document.createElement('span');
    title.className = 'task-title';
    title.textContent = task.title;
    const state = document.createElement('span');
    state.className = 'task-state';
    state.textContent = stateLabel;
    button.append(check, title, state);
    button.addEventListener('click', () => void updateTaskCompletion(taskId, !completed, button));
    taskList.append(button);
  });
  taskCount.textContent = tasks.length === 0 ? '0 项'
    : activeRunProgress ? `${completedCount}/${tasks.length} 项已完成` : `${tasks.length} 项 · 进度未加载`;
  if (runProgressLoadFailed && activeRun) {
    taskCount.textContent = '读取失败';
    const retryButton = document.createElement('button');
    retryButton.className = 'task-progress-retry';
    retryButton.type = 'button';
    retryButton.textContent = '重试读取进度';
    retryButton.addEventListener('click', () => {
      retryButton.disabled = true;
      retryButton.textContent = '正在重试…';
      void refreshRunProgress(String(activeRun.runId), routeResolutionEpoch);
    });
    taskList.append(retryButton);
  }
  updateCompleteNodeControl();
}

function updateCompleteNodeControl() {
  const nodes = Array.isArray(activeRunProgress?.nodes) ? activeRunProgress.nodes : [];
  const current = nodes.find((item) => Number(item.nodeId) === Number(activeNodeId));
  const definition = activeTreeNodes.find((item) => Number(item.nodeId) === Number(activeNodeId));
  const tasks = Array.isArray(definition?.tasks) ? definition.tasks : [];
  const progressTasks = Array.isArray(activeRunProgress?.tasks) ? activeRunProgress.tasks : [];
  const allTasksComplete = Boolean(definition) && tasks.every((task) => {
    const taskId = Number(task.taskId);
    return Number.isSafeInteger(taskId) && taskId > 0 && progressTasks.some((item) =>
      Number(item.taskId) === taskId && Number(item.nodeId) === Number(activeNodeId) && item.status === 'completed');
  });
  completeNodeButton.disabled = !activeRun || !current
    || ['locked', 'completed'].includes(current.status) || !allTasksComplete;
}

async function updateTaskCompletion(taskId, completed, button) {
  if (!activeRun || taskProgressMutationInFlight) return;
  taskProgressMutationInFlight = true;
  renderSelectedNodeTasks();
  const runId = String(activeRun.runId);
  const epoch = routeResolutionEpoch;
  const requestEpoch = ++taskProgressRequestEpoch;
  const taskButtonEpoch = (taskProgressButtonEpochs.get(String(taskId)) || 0) + 1;
  taskProgressButtonEpochs.set(String(taskId), taskButtonEpoch);
  button.disabled = true;
  button.setAttribute('aria-busy', 'true');
  try {
    const progress = await apiRequest(`/runs/${encodeURIComponent(runId)}/tasks/${encodeURIComponent(taskId)}`, {
      method: 'PATCH',
      body: JSON.stringify({ status: completed ? 'completed' : 'pending' }),
    });
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== runId
      || requestEpoch !== taskProgressRequestEpoch) return;
    activeRunProgress = progress;
    renderSelectedNodeTasks();
    updateCompleteNodeControl();
    const current = progress.nodes?.find((item) => Number(item.nodeId) === Number(activeNodeId));
    if (current) workspaceStatus.textContent = `工作区已就绪 · 节点 ${current.progressPercent}%`;
  } catch (error) {
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== runId
      || requestEpoch !== taskProgressRequestEpoch) return;
    appendChatMessage('agent', '系统', error.message || '任务进度保存失败，请稍后重试。');
    await refreshRunProgress(runId, epoch);
  } finally {
    taskProgressMutationInFlight = false;
    if (epoch === routeResolutionEpoch && String(activeRun?.runId) === runId
      && taskProgressButtonEpochs.get(String(taskId)) === taskButtonEpoch) {
      button.removeAttribute('aria-busy');
    }
    renderSelectedNodeTasks();
    updateCompleteNodeControl();
  }
}

function renderTreeEdges() {
  if (!treeEdgeLayer || !activeTreeNodes.length) return;
  treeEdgeLayer.replaceChildren();
  activeTreeNodes.filter((node) => node.parentKey).forEach((node) => {
    const child = canvasView.querySelector(`[data-tree-node-key="${CSS.escape(node.key)}"]`);
    const parent = canvasView.querySelector(`[data-tree-node-key="${CSS.escape(node.parentKey)}"]`);
    if (!child || !parent) return;
    const line = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    const startX = Number.parseFloat(parent.style.left);
    const startY = Number.parseFloat(parent.style.top) + 65;
    const endX = Number.parseFloat(child.style.left);
    const endY = Number.parseFloat(child.style.top) - 65;
    const middleY = (startY + endY) / 2;
    line.setAttribute('d', `M ${startX} ${startY} C ${startX} ${middleY}, ${endX} ${middleY}, ${endX} ${endY}`);
    line.setAttribute('class', 'tree-edge');
    treeEdgeLayer.append(line);
  });
}

function renderExperimentTree(nodes) {
  document.querySelectorAll('.bubble.tree-node:not(#bubble)').forEach((node) => node.remove());
  treeEdgeLayer?.remove();
  activeTreeNodes = nodes || [];
  if (!activeTreeNodes.length) {
    bubble.classList.remove('tree-node');
    bubble.removeAttribute('data-tree-node-key');
    bubbleX = window.innerWidth / 2;
    bubbleY = window.innerHeight / 2;
    return;
  }
  treeEdgeLayer = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  treeEdgeLayer.classList.add('tree-edge-layer');
  canvasView.prepend(treeEdgeLayer);
  const nodeNumbers = buildTreeNodeNumbers(activeTreeNodes);
  const ordered = [...activeTreeNodes].sort((a, b) => a.depth - b.depth || a.key.localeCompare(b.key));
  ordered.forEach((node, index) => {
    const element = index === 0 ? bubble : bubble.cloneNode(true);
    element.id = index === 0 ? 'bubble' : '';
    element.classList.add('tree-node');
    element.dataset.treeNodeKey = node.key;
    element.querySelector('.bubble-sequence').textContent = nodeNumbers.get(node.key) || node.key;
    element.querySelector('h1').textContent = node.title;
    element.querySelector('p').textContent = node.description;
    if (index > 0) {
      bindTreeNode(element);
      canvasView.append(element);
    }
  });
  bubbleX = window.innerWidth / 2;
  bubbleY = 150;
  renderCanvas();
  const initialNode = activeTreeNodes.find((node) => !node.parentKey) || activeTreeNodes[0];
  const initialElement = canvasView.querySelector(`.bubble.tree-node[data-tree-node-key="${CSS.escape(initialNode.key)}"]`);
  if (initialElement) selectTreeNode(initialElement);
}

function bindTreeNode(element) {
  let drag = null;
  element.addEventListener('pointerdown', (event) => {
    if (event.button !== 0) return;
    event.stopPropagation();
    selectTreeNode(element);
    drag = { pointerId: event.pointerId, x: event.clientX, y: event.clientY };
    element.setPointerCapture(event.pointerId);
  });
  element.addEventListener('pointermove', (event) => {
    if (!drag || drag.pointerId !== event.pointerId) return;
    const node = activeTreeNodes.find((item) => item.key === element.dataset.treeNodeKey);
    node.canvasX += event.clientX - drag.x;
    node.canvasY += event.clientY - drag.y;
    drag = { pointerId: event.pointerId, x: event.clientX, y: event.clientY };
    renderCanvas();
  });
  element.addEventListener('pointerup', () => { drag = null; });
  element.addEventListener('click', () => selectTreeNode(element));
}

function openExperiment(experimentId) {
  navigateTo({ type: 'canvas', experimentId: String(experimentId) });
}

function renderExperimentCanvas(experimentId) {
  loginView.classList.remove('is-visible');
  studentView.classList.remove('is-visible');
  teacherView.classList.remove('is-visible');
  [canvasView, editorView].forEach((view) => view.classList.remove('auth-hidden'));
  closePatchReview();
  activeExperimentId = String(experimentId);
  writeSessionValue(STORAGE_KEYS.activeExperiment, activeExperimentId);
  activeRun = null;
  activeRunProgress = null;
  runProgressLoadFailed = false;
  activeFilePath = null;
  activeFileHash = null;
  activeFileSavedContent = '';
  activeWorkspaceFiles = [];
  workspaceFileSelectionEpoch += 1;
  terminalCommandInFlight = false;
  activeTerminalSession = null;
  activeNodeId = null;
  workspaceStatus.textContent = '未启动工作区';
  saveFileButton.disabled = true;
  createSnapshotButton.disabled = true;
  completeNodeButton.disabled = true;
  terminalInput.disabled = true;
  terminalInput.value = '';
  terminalOutput.replaceChildren();
  const terminalState = document.createElement('div');
  terminalState.className = 'terminal-muted';
  terminalState.textContent = '工作区启动后才能执行命令。';
  terminalOutput.append(terminalState);
  const data = experimentData[experimentId] || { name: '新的学习实验', description: '根据学习目标拆分的实验节点。' };
  touchRecentExperiment(experimentId);
  libraryView.style.display = 'none';
  editorView.classList.remove('is-visible');
  canvasView.classList.remove('is-hidden');
  closeInspector();
  document.querySelector('#currentExperimentName').textContent = data.name;
  document.querySelector('.canvas-nav small').textContent = data.nodes?.find((node) => !node.parentKey)?.stage || '个人实验';
  document.querySelector('#bubble h1').textContent = data.name;
  document.querySelector('#bubble p').textContent = data.description;
  bubble.setAttribute('aria-label', `${data.name}节点`);
  document.querySelector('#inspector h2').textContent = data.name;
  document.querySelector('#inspector .inspector-intro').textContent = data.description;
  renderExperimentTree(data.nodes);
  renderCanvas();
}

async function startWorkspaceRun() {
  if (!activeExperimentId || !/^\d+$/.test(activeExperimentId)) {
    appendChatMessage('agent', '系统', '当前实验还没有连接到服务端运行实例，请从课程中打开已发布实验。');
    return false;
  }
  startNodeButton.disabled = true;
  workspaceStatus.textContent = '正在启动工作区…';
  const epoch = routeResolutionEpoch;
  try {
    const run = await apiRequest(`/experiments/${activeExperimentId}/runs`, { method: 'POST', timeoutMs: 45000 });
    if (epoch !== routeResolutionEpoch) return false;
    const connected = await connectWorkspace(run, { createTerminalSession: true, epoch });
    if (!connected || epoch !== routeResolutionEpoch) return false;
    appendChatMessage('agent', '系统', '独立工作区已就绪，可以开始修改代码。');
    return true;
  } catch (error) {
    workspaceStatus.textContent = '工作区启动失败';
    appendChatMessage('agent', '系统', error.message || '工作区启动失败，请稍后重试。');
    return false;
  } finally {
    if (!activeTerminalSession) terminalInput.disabled = true;
    startNodeButton.disabled = false;
  }
}

async function connectWorkspace(run, { createTerminalSession = false, epoch = null } = {}) {
  if (!run || !/^\d+$/.test(String(run.runId)) || !/^\d+$/.test(String(run.workspaceId))) {
    throw new Error('服务端返回的运行实例信息无效。');
  }
  const isCurrentRoute = () => epoch === null || epoch === routeResolutionEpoch;
  if (!isCurrentRoute()) return false;
  workspaceStatus.textContent = run.workspaceStatus === 'running' ? '正在读取工作区…' : `工作区：${run.workspaceStatus}`;
  if (run.workspaceStatus !== 'running') {
    if (['provisioning', 'starting'].includes(run.workspaceStatus)) {
      throw new Error('工作区正在启动，请稍等片刻后再次进入。');
    }
    throw new Error('该运行实例的工作区当前不可用。');
  }
  const files = await apiRequest(`/workspaces/${run.workspaceId}/files`, { method: 'GET' });
  if (!isCurrentRoute()) return false;
  if (!Array.isArray(files)) throw new Error('工作区文件列表格式无效。');
  let nextFilePath = null;
  let nextFileHash = null;
  let nextFileContent = '';
  if (files.length) {
    nextFilePath = files[0].path;
    const response = await apiRequest(`/workspaces/${run.workspaceId}/files/${workspaceFileUrl(nextFilePath)}`, { method: 'GET' });
    if (!isCurrentRoute()) return false;
    const file = await verifyWorkspaceFileResponse(response, run.workspaceId, files[0]);
    if (!isCurrentRoute()) return false;
    nextFileContent = file.content;
    nextFileHash = file.file.contentHash || null;
  }
  let nextTerminalSession = activeTerminalSession;
  const needsTerminalSession = createTerminalSession
    || activeTerminalRunId !== String(run.runId)
    || !activeTerminalSession;
  if (createTerminalSession) nextTerminalSession = null;
  if (needsTerminalSession && !createTerminalSession) {
    nextTerminalSession = readTerminalSession(run);
  }
  if (needsTerminalSession && !nextTerminalSession) {
    nextTerminalSession = await apiRequest(`/workspaces/${run.workspaceId}/terminal-sessions`, {
      method: 'POST',
      timeoutMs: 15000,
    });
    if (!isCurrentRoute()) return false;
    persistTerminalSession(run, nextTerminalSession);
  }
  activeRun = run;
  activeRunProgress = null;
  runProgressLoadFailed = false;
  activeExperimentId = String(run.experimentId);
  activeNodeId = run.currentNodeId || null;
  activeFilePath = nextFilePath;
  workspaceFileSelectionEpoch += 1;
  activeFileHash = nextFileHash;
  activeFileSavedContent = nextFileContent;
  activeWorkspaceFiles = files;
  codeEditor.value = nextFileContent;
  renderCodeLineNumbers();
  codeEditor.disabled = !nextFilePath;
  saveFileButton.disabled = !nextFilePath;
  renderWorkspaceFileTabs();
  activeTerminalSession = nextTerminalSession;
  activeTerminalRunId = String(run.runId);
  terminalInput.disabled = false;
  terminalInput.placeholder = '输入受限终端命令…';
  terminalOutput.replaceChildren();
  const terminalReady = document.createElement('div');
  terminalReady.className = 'terminal-muted';
  terminalReady.textContent = '终端已连接。命令会在隔离工作区内执行。';
  terminalOutput.append(terminalReady);
  createSnapshotButton.disabled = false;
  recordBugButton.disabled = false;
  deepDiagnosisButton.disabled = false;
  const currentNode = activeTreeNodes.find((node) => Number(node.nodeId) === Number(run.currentNodeId));
  if (currentNode) {
    const nodeElement = canvasView.querySelector(`[data-tree-node-key="${CSS.escape(currentNode.key)}"]`);
    if (nodeElement) selectTreeNode(nodeElement);
  } else {
    activeNodeId = run.currentNodeId || null;
  }
  inspector.classList.remove('is-open');
  await refreshRunProgress(String(run.runId), epoch ?? routeResolutionEpoch);
  if (!isCurrentRoute()) return false;
  workspaceStatus.textContent = '工作区已就绪';
  return true;
}

function renderWorkspaceFileTabs() {
  if (!codeFileTabs) return;
  codeFileTabs.replaceChildren();
  const filenameCounts = new Map();
  activeWorkspaceFiles.forEach((file) => {
    const filename = file.path.split('/').pop();
    filenameCounts.set(filename, (filenameCounts.get(filename) || 0) + 1);
  });
  activeWorkspaceFiles.forEach((file) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `code-tab code-tab-button${file.path === activeFilePath ? ' is-active' : ''}`;
    button.setAttribute('role', 'tab');
    button.setAttribute('aria-selected', String(file.path === activeFilePath));
    const filename = file.displayName || file.path.split('/').pop();
    const label = filenameCounts.get(file.path.split('/').pop()) > 1 ? `${filename} — ${file.path}` : filename;
    const dirty = file.path === activeFilePath && codeEditor.value !== activeFileSavedContent;
    button.textContent = dirty ? `${label} •` : label;
    if (dirty) button.setAttribute('aria-label', `${label}，有未保存修改`);
    button.title = file.path;
    button.disabled = terminalCommandInFlight;
    button.addEventListener('click', () => void selectWorkspaceFile(file.path));
    codeFileTabs.append(button);
  });
  if (codeLocation) codeLocation.textContent = activeFilePath || '工作区暂无文件';
}

function renderCodeLineNumbers() {
  if (!codeLineNumbers) return;
  let lineCount = 1;
  for (const character of codeEditor.value) {
    if (character === '\n') lineCount += 1;
  }
  const numbers = [];
  for (let line = 1; line <= lineCount; line += 1) {
    numbers.push(String(line));
  }
  codeLineNumbers.textContent = numbers.join('\n');
  codeLineNumbers.style.transform = `translateY(-${codeEditor.scrollTop}px)`;
}

async function selectWorkspaceFile(path) {
  if (!activeRun || !activeWorkspaceFiles.some((file) => file.path === path)) return;
  if (activeFilePath && codeEditor.value !== activeFileSavedContent) {
    appendChatMessage('agent', '系统', '当前文件有未保存修改。请先保存，再切换文件，以免丢失代码。');
    return;
  }
  const runId = String(activeRun.runId);
  const workspaceId = String(activeRun.workspaceId);
  const epoch = routeResolutionEpoch;
  const currentPath = activeFilePath;
  const savedContentAtRequest = activeFileSavedContent;
  const expectedFile = activeWorkspaceFiles.find((file) => file.path === path);
  const selectionEpoch = ++workspaceFileSelectionEpoch;
  try {
    const response = await apiRequest(`/workspaces/${encodeURIComponent(workspaceId)}/files/${workspaceFileUrl(path)}`, { method: 'GET' });
    const file = await verifyWorkspaceFileResponse(response, workspaceId, expectedFile);
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== runId
      || selectionEpoch !== workspaceFileSelectionEpoch || activeFilePath !== currentPath
      || codeEditor.value !== savedContentAtRequest) {
      if (epoch === routeResolutionEpoch && String(activeRun?.runId) === runId
        && codeEditor.value !== savedContentAtRequest) {
        appendChatMessage('agent', '系统', '切换文件期间检测到新输入，已保留当前内容；请先保存后重试切换。');
      }
      return;
    }
    activeFilePath = path;
    activeFileHash = file.file?.contentHash || null;
    codeEditor.value = file.content;
    activeFileSavedContent = file.content;
    renderCodeLineNumbers();
    renderWorkspaceFileTabs();
  } catch (error) {
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== runId
      || selectionEpoch !== workspaceFileSelectionEpoch) return;
    appendChatMessage('agent', '系统', error.message || '文件读取失败，请重试。');
  }
}

async function refreshWorkspaceFilesAfterTerminal(runId, workspaceId, epoch) {
  try {
    const files = await apiRequest(`/workspaces/${encodeURIComponent(workspaceId)}/files`, { method: 'GET' });
    if (!isRunContextCurrent(runId, epoch) || String(activeRun?.workspaceId) !== workspaceId) return;
    if (!Array.isArray(files)) throw new Error('工作区文件列表格式无效。');
    const selected = files.find((file) => file.path === activeFilePath) || files[0] || null;
    activeWorkspaceFiles = files;
    if (!selected) {
      activeFilePath = null;
      activeFileHash = null;
      activeFileSavedContent = '';
      codeEditor.value = '';
      codeEditor.disabled = true;
      saveFileButton.disabled = true;
      renderCodeLineNumbers();
      renderWorkspaceFileTabs();
      appendChatMessage('agent', '系统', '终端命令后工作区没有可显示的源码文件。');
      return;
    }
    const response = await apiRequest(`/workspaces/${encodeURIComponent(workspaceId)}/files/${workspaceFileUrl(selected.path)}`, { method: 'GET' });
    const file = await verifyWorkspaceFileResponse(response, workspaceId, selected);
    if (!isRunContextCurrent(runId, epoch) || String(activeRun?.workspaceId) !== workspaceId) return;
    const changedSelection = selected.path !== activeFilePath;
    const changedContent = !changedSelection && file.content !== activeFileSavedContent;
    activeFilePath = selected.path;
    activeFileHash = file.file.contentHash || null;
    activeFileSavedContent = file.content;
    codeEditor.value = file.content;
    codeEditor.disabled = false;
    saveFileButton.disabled = false;
    renderCodeLineNumbers();
    renderWorkspaceFileTabs();
    if (changedSelection || changedContent) {
      appendChatMessage('agent', '系统', '终端运行后已重新同步工作区文件列表和当前文件内容。');
    }
  } catch (error) {
    if (!isRunContextCurrent(runId, epoch)) return;
    appendChatMessage('agent', '系统', error.message || '终端运行后同步工作区文件失败，请重新打开文件列表。');
  }
}

async function restoreExistingRun(runId, epoch) {
  const run = await apiRequest(`/runs/${encodeURIComponent(runId)}`, { method: 'GET', timeoutMs: 15000 });
  if (epoch !== routeResolutionEpoch) return;
  if (String(run.runId) !== String(runId) || !/^\d+$/.test(String(run.experimentId))) {
    throw new Error('运行实例不存在或当前账号无权访问。');
  }
  const experiment = await apiRequest(`/experiments/${encodeURIComponent(run.experimentId)}`, { method: 'GET', timeoutMs: 15000 });
  if (epoch !== routeResolutionEpoch) return;
  if (String(experiment.experimentId) !== String(run.experimentId)) throw new Error('运行实例关联的实验不存在或无权访问。');
  experimentData[String(run.experimentId)] = experiment;
  renderExperimentCanvas(String(run.experimentId));
  const connected = await connectWorkspace(run, { epoch });
  if (!connected || epoch !== routeResolutionEpoch) return;
  canvasView.classList.add('is-hidden');
  editorView.classList.add('is-visible');
}

async function refreshRunProgress(runId = activeRun?.runId, epoch = routeResolutionEpoch) {
  if (!activeRun || String(activeRun.runId) !== String(runId)) return;
  runProgressLoadFailed = false;
  try {
    const progress = await apiRequest(`/runs/${runId}/progress`, { method: 'GET' });
    if (!Array.isArray(progress?.nodes) || !Array.isArray(progress?.tasks)) {
      throw new Error('服务端进度数据缺少任务记录，请升级后端或稍后重试。');
    }
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== String(runId)) return;
    activeRunProgress = progress;
    runProgressLoadFailed = false;
    const current = activeNodeId
      ? progress.nodes.find((node) => Number(node.nodeId) === Number(activeNodeId))
      : null;
    if (selectedTreeNode) renderSelectedNodeTasks();
    updateCompleteNodeControl();
    if (current) workspaceStatus.textContent = `工作区已就绪 · 节点 ${current.progressPercent}%`;
  } catch (error) {
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== String(runId)) return;
    activeRunProgress = null;
    runProgressLoadFailed = true;
    if (selectedTreeNode) renderSelectedNodeTasks();
    updateCompleteNodeControl();
    appendChatMessage('agent', '系统', error.message || '节点进度加载失败，请稍后重试。');
  }
}

async function saveWorkspaceFile() {
  if (!activeRun || !activeFilePath) return;
  const runId = String(activeRun.runId);
  const workspaceId = String(activeRun.workspaceId);
  const path = activeFilePath;
  const expectedHash = activeFileHash;
  const content = codeEditor.value;
  const epoch = routeResolutionEpoch;
  if (!expectedHash) {
    appendChatMessage('agent', '系统', '无法确认当前文件版本，已阻止无校验保存；请重新打开文件后重试。');
    return;
  }
  saveFileButton.disabled = true;
  try {
    const saved = await apiRequest(`/workspaces/${workspaceId}/files/${workspaceFileUrl(path)}`, {
      method: 'PUT',
      body: JSON.stringify({ content, ...(expectedHash ? { expectedHash } : {}) }),
    });
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== runId || activeFilePath !== path) return;
    activeFileHash = saved.contentHash || saved.file?.contentHash || null;
    activeFileSavedContent = content;
    activeWorkspaceFiles = activeWorkspaceFiles.map((file) => file.path === path
      ? { ...file, contentHash: activeFileHash } : file);
    renderWorkspaceFileTabs();
    appendChatMessage('agent', '系统', '文件已保存到独立工作区。');
  } catch (error) {
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== runId || activeFilePath !== path) return;
    appendChatMessage('agent', '系统', error.message || '文件保存失败，请刷新后重试。');
  } finally {
    if (epoch === routeResolutionEpoch && String(activeRun?.runId) === runId && activeFilePath === path) {
      saveFileButton.disabled = false;
    }
  }
}

async function createWorkspaceSnapshot() {
  if (!activeRun) return;
  const runId = String(activeRun.runId);
  const epoch = routeResolutionEpoch;
  createSnapshotButton.disabled = true;
  try {
    await apiRequest(`/workspaces/${activeRun.workspaceId}/snapshots`, {
      method: 'POST',
      body: JSON.stringify({ description: '学生手动创建的工作区快照' }),
    });
    if (!isRunContextCurrent(runId, epoch)) return;
    appendChatMessage('agent', '系统', '工作区快照已保存。');
  } catch (error) {
    if (!isRunContextCurrent(runId, epoch)) return;
    appendChatMessage('agent', '系统', error.message || '快照创建失败，请稍后重试。');
  } finally {
    if (isRunContextCurrent(runId, epoch)) createSnapshotButton.disabled = !activeRun;
  }
}

async function completeCurrentNode() {
  if (!activeRun || !activeNodeId) return;
  const runId = String(activeRun.runId);
  const nodeId = Number(activeNodeId);
  const epoch = routeResolutionEpoch;
  completeNodeButton.disabled = true;
  try {
    const progress = await apiRequest(`/runs/${encodeURIComponent(runId)}/nodes/${encodeURIComponent(nodeId)}`, {
      method: 'PATCH',
      body: JSON.stringify({ status: 'completed', progressPercent: 100 }),
    });
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== runId) return;
    activeRunProgress = progress;
    if (selectedTreeNode) renderSelectedNodeTasks();
    workspaceStatus.textContent = '当前节点已完成';
    appendChatMessage('agent', '系统', '当前节点已完成，进度已保存。');
  } catch (error) {
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== runId) return;
    appendChatMessage('agent', '系统', error.message || '节点完成状态保存失败，请稍后重试。');
    await refreshRunProgress(runId, epoch);
  }
}

function closeBugCaseDialog() {
  bugDialog.classList.remove('is-open');
  bugDialog.setAttribute('aria-hidden', 'true');
  bugCaseForm.reset();
  setMessage(bugMessage, '');
}

function openBugCaseDialog() {
  if (!activeRun) {
    appendChatMessage('agent', '系统', '请先启动工作区，再记录已解决 Bug。');
    return;
  }
  bugDialog.classList.add('is-open');
  bugDialog.setAttribute('aria-hidden', 'false');
  bugTitle.focus();
}

async function saveBugCase(event) {
  event.preventDefault();
  if (!activeRun) return;
  const title = bugTitle.value.trim();
  const problem = bugProblem.value.trim();
  if (!title || !problem) {
    setMessage(bugMessage, '请填写案例标题和问题现象。', true);
    return;
  }
  saveBugButton.disabled = true;
  setMessage(bugMessage, '正在保存个人案例…');
  try {
    await apiRequest(`/runs/${activeRun.runId}/bugs`, {
      method: 'POST',
      body: JSON.stringify({ title, problem, solution: bugSolution.value.trim() || null,
        technologyStack: bugTechnologyStack.value || null, nodeId: activeNodeId }),
      timeoutMs: 15000,
    });
    closeBugCaseDialog();
    appendChatMessage('agent', '系统', 'Bug 案例已保存为个人记录，等待教师审核。');
  } catch (error) {
    setMessage(bugMessage, error.message || '案例保存失败，请稍后重试。', true);
  } finally {
    saveBugButton.disabled = false;
  }
}

function workspaceFileUrl(path) {
  return path.split('/').map((part) => encodeURIComponent(part)).join('/');
}

async function verifyWorkspaceFileResponse(response, workspaceId, expectedFile) {
  const metadata = response?.file;
  if (!expectedFile?.path || typeof response?.content !== 'string' || !metadata
    || String(metadata.workspaceId) !== String(workspaceId)
    || metadata.path !== expectedFile.path) {
    throw new Error('服务器返回的代码文件与当前选择不一致，已阻止显示和编辑。请刷新后重试。');
  }
  if (expectedFile.workspaceId != null && String(expectedFile.workspaceId) !== String(workspaceId)) {
    throw new Error('当前文件列表与工作区不匹配，请刷新后重试。');
  }
  const listedHash = typeof expectedFile.contentHash === 'string' && expectedFile.contentHash
    ? expectedFile.contentHash.toLowerCase() : null;
  const returnedHash = typeof metadata.contentHash === 'string' && metadata.contentHash
    ? metadata.contentHash.toLowerCase() : null;
  if (listedHash && returnedHash && listedHash !== returnedHash) {
    throw new Error('文件列表与文件内容的版本不一致，已停止加载。请刷新后重试。');
  }
  const expectedHash = returnedHash || listedHash;
  if (expectedHash) {
    if (!/^[a-f0-9]{64}$/.test(expectedHash) || !window.crypto?.subtle || !window.TextEncoder) {
      throw new Error('当前浏览器无法安全校验文件内容，请使用受支持的安全浏览器重试。');
    }
    const digest = await window.crypto.subtle.digest('SHA-256', new window.TextEncoder().encode(response.content));
    const actualHash = Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, '0')).join('');
    if (actualHash !== expectedHash) {
      throw new Error('文件内容校验失败，已阻止显示和编辑。请刷新后重试。');
    }
  }
  return { ...response, file: { ...metadata, contentHash: expectedHash } };
}

function setLibraryState(message, { isError = false, canRetry = false, routeEpoch = routeResolutionEpoch } = {}) {
  const state = document.createElement('p');
  state.className = `library-state${isError ? ' is-error' : ''}`;
  state.textContent = message;
  personalExperimentList.replaceChildren(state);
  if (canRetry) {
    const retry = document.createElement('button');
    retry.className = 'library-retry-button';
    retry.type = 'button';
    retry.textContent = '重试加载';
    retry.addEventListener('click', () => void loadPersonalExperiments(routeEpoch));
    personalExperimentList.append(retry);
  }
}

function renderPersonalExperiment(item) {
  const experimentId = String(item.id ?? item.experimentId);
  const isStudent = getCurrentUser()?.role === 'student';
  const row = document.createElement('div');
  row.className = 'experiment-row';

  const button = document.createElement('button');
  button.className = 'experiment-item experiment-link';
  button.type = 'button';
  button.dataset.experimentId = experimentId;

  const icon = document.createElement('span');
  icon.className = 'experiment-icon';
  icon.textContent = '实验';
  const copy = document.createElement('span');
  copy.className = 'experiment-copy';
  const title = document.createElement('strong');
  title.textContent = item.name;
  const status = document.createElement('small');
  status.textContent = isStudent
    ? (item.status === 'published' ? '已发布 · 个人实验' : '草稿 · 个人实验')
    : `${item.status === 'published' ? '已发布' : '草稿'} · 请到课程管理发布`;
  copy.append(title, status);
  const arrow = document.createElement('span');
  arrow.className = 'experiment-arrow';
  arrow.textContent = isStudent ? '›' : '';
  button.append(icon, copy, arrow);
  button.disabled = !isStudent;
  if (isStudent) button.addEventListener('click', () => openExperiment(experimentId));
  row.append(button);
  return row;
}

function renderRecentExperiments() {
  recentExperiments.replaceChildren();
  const items = recentlyOpenedExperimentIds
    .map((id) => ({ id, ...experimentData[id] }))
    .filter((item) => item.name);
  if (!items.length) {
    const empty = document.createElement('p');
    empty.className = 'library-state recent-empty';
    empty.textContent = '打开实验后，这里会显示本次访问中最近查看的内容。';
    recentExperiments.append(empty);
    return;
  }
  items.forEach((item) => {
    const button = document.createElement('button');
    button.className = 'recent-item experiment-link';
    button.type = 'button';
    button.dataset.experimentId = item.id;
    const marker = document.createElement('span');
    marker.className = 'recent-sequence';
    marker.textContent = '实验';
    const copy = document.createElement('span');
    copy.className = 'recent-copy';
    const title = document.createElement('strong');
    title.textContent = item.name;
    const meta = document.createElement('small');
    meta.textContent = '本次访问中打开';
    copy.append(title, meta);
    const action = document.createElement('span');
    action.className = 'recent-continue';
    action.textContent = '继续 →';
    button.append(marker, copy, action);
    button.addEventListener('click', () => openExperiment(item.id));
    recentExperiments.append(button);
  });
}

async function loadPersonalExperiments(routeEpoch = routeResolutionEpoch) {
  const requestEpoch = ++libraryLoadEpoch;
  setLibraryState('正在读取你的实验…', { routeEpoch });
  try {
    const response = await apiRequest('/experiments/mine', { method: 'GET', timeoutMs: 15000 });
    if (!Array.isArray(response)) throw new Error('实验列表数据格式无效，请重试。');
    if (requestEpoch !== libraryLoadEpoch || routeEpoch !== routeResolutionEpoch) return;

    const items = response.filter((item) => item && (item.id != null || item.experimentId != null)
      && typeof item.name === 'string' && item.name.trim());
    Object.keys(experimentData).forEach((id) => delete experimentData[id]);
    items.forEach((item) => {
      const id = String(item.id ?? item.experimentId);
      experimentData[id] = { ...experimentData[id], ...item, experimentId: id };
    });
    const ownedIds = new Set(items.map((item) => String(item.id ?? item.experimentId)));
    for (let index = recentlyOpenedExperimentIds.length - 1; index >= 0; index -= 1) {
      if (!ownedIds.has(recentlyOpenedExperimentIds[index])) recentlyOpenedExperimentIds.splice(index, 1);
    }
    personalExperimentList.replaceChildren();
    if (!items.length) {
      setLibraryState('还没有个人实验。点击“创建实验”，完成确认后就会保存在这里。', { routeEpoch });
    } else {
      items.forEach((item) => personalExperimentList.append(renderPersonalExperiment(item)));
    }
    renderRecentExperiments();
    updateLibraryCount();
  } catch (error) {
    if (requestEpoch !== libraryLoadEpoch || routeEpoch !== routeResolutionEpoch) return;
    if (error.status === 401) {
      setCurrentUser(null);
      pendingProtectedRoute = { type: 'library' };
      persistPendingRoute(pendingProtectedRoute);
      renderSimpleRoute('login');
      setRouteStatus('登录状态已失效，请重新登录后查看个人实验。', true);
      navigateTo({ type: 'login' }, { replace: true });
      return;
    }
    setLibraryState(error.message || '实验加载失败，请检查服务连接后重试。',
      { isError: true, canRetry: true, routeEpoch });
    const libraryCount = document.querySelector('.library-count');
    if (libraryCount) libraryCount.textContent = '暂不可用';
    if (sidebarExperimentCount) sidebarExperimentCount.textContent = '—';
  }
}

function touchRecentExperiment(experimentId) {
  if (!experimentData[experimentId]) return;
  const index = recentlyOpenedExperimentIds.indexOf(experimentId);
  if (index >= 0) recentlyOpenedExperimentIds.splice(index, 1);
  recentlyOpenedExperimentIds.unshift(experimentId);
  recentlyOpenedExperimentIds.length = Math.min(recentlyOpenedExperimentIds.length, 4);
  renderRecentExperiments();
}

function updateLibraryCount() {
  const totalExperiments = document.querySelectorAll('.folder-card .experiment-link').length;
  const libraryCount = document.querySelector('.library-count');
  if (libraryCount) libraryCount.textContent = `${totalExperiments} 个实验`;
  if (sidebarExperimentCount) sidebarExperimentCount.textContent = totalExperiments;
  const countLabel = document.querySelector('.folder-card .folder-count');
  if (countLabel) countLabel.textContent = `${totalExperiments} 个实验`;
}

libraryOverviewButton.addEventListener('click', () => {
  libraryMainColumn.scrollTo({ top: 0, behavior: 'smooth' });
});

openExperimentButtons.forEach((button) => {
  button.addEventListener('click', () => openExperiment(button.dataset.experimentId));
});

backToLibrary.addEventListener('click', () => {
  closeInspector();
  showRoute('library');
});

canvasView.addEventListener('pointerdown', (event) => {
  if (event.target !== canvasView || event.button !== 0) return;
  closeInspector();
  panState = { pointerId: event.pointerId, x: event.clientX, y: event.clientY };
  canvasView.classList.add('is-panning');
  canvasView.setPointerCapture(event.pointerId);
});

canvasView.addEventListener('pointermove', (event) => {
  if (!panState || panState.pointerId !== event.pointerId) return;
  const dx = event.clientX - panState.x;
  const dy = event.clientY - panState.y;
  panState.x = event.clientX;
  panState.y = event.clientY;
  gridX += dx;
  gridY += dy;
  bubbleX += dx;
  bubbleY += dy;
  renderCanvas();
});

canvasView.addEventListener('pointerup', () => {
  panState = null;
  canvasView.classList.remove('is-panning');
});

bubble.addEventListener('pointerdown', (event) => {
  if (event.button !== 0) return;
  event.stopPropagation();
  selectBubble();
  bubbleDragState = { pointerId: event.pointerId, x: event.clientX, y: event.clientY };
  bubble.classList.add('is-dragging');
  bubble.setPointerCapture(event.pointerId);
});

bubble.addEventListener('pointermove', (event) => {
  if (!bubbleDragState || bubbleDragState.pointerId !== event.pointerId) return;
  bubbleX += event.clientX - bubbleDragState.x;
  bubbleY += event.clientY - bubbleDragState.y;
  bubbleDragState.x = event.clientX;
  bubbleDragState.y = event.clientY;
  renderCanvas();
});

bubble.addEventListener('pointerup', () => {
  bubbleDragState = null;
  bubble.classList.remove('is-dragging');
});

bubble.addEventListener('click', selectBubble);

startNodeButton.addEventListener('click', async () => {
  inspector.classList.remove('is-open');
  const started = await startWorkspaceRun();
  if (started && activeRun) navigateTo({ type: 'editor', runId: String(activeRun.runId) });
});

saveFileButton.addEventListener('click', saveWorkspaceFile);
createSnapshotButton.addEventListener('click', createWorkspaceSnapshot);
completeNodeButton.addEventListener('click', completeCurrentNode);

backToCanvas.addEventListener('click', () => {
  if (activeExperimentId) navigateTo({ type: 'canvas', experimentId: activeExperimentId });
  else redirectToRoleHome('没有可返回的实验画布。');
});

function appendChatMessage(kind, label, text) {
  const message = document.createElement('div');
  message.className = `chat-message ${kind === 'user' ? 'user-message' : kind === 'soften' ? 'soften-message' : 'agent-message'}`;
  const messageLabel = document.createElement('div');
  messageLabel.className = 'message-label';
  messageLabel.textContent = label;
  const messageText = document.createElement('p');
  messageText.textContent = text;
  message.append(messageLabel, messageText);
  chatMessages.appendChild(message);
  chatMessages.scrollTop = chatMessages.scrollHeight;
}

async function requestGuidance(question) {
  if (!activeRun) {
    appendChatMessage('agent', '系统', '请先启动工作区，再请求代码指导。');
    return;
  }
  const runId = String(activeRun.runId);
  const epoch = routeResolutionEpoch;
  appendChatMessage('user', '你', question);
  sendButton.disabled = true;
  softenButton.disabled = true;
  try {
    const result = await apiRequest(`/runs/${runId}/guidance`, {
      method: 'POST',
      body: JSON.stringify({ question }),
      timeoutMs: 60000,
    });
    if (!isRunContextCurrent(runId, epoch)) return;
    appendChatMessage('agent', '导师', result.content || '模型未返回有效指导。');
    if (Array.isArray(result.ragReferences) && result.ragReferences.length) {
      const references = result.ragReferences.map((reference) => {
        const title = reference.title || `案例 ${reference.bugCaseId || ''}`;
        const problem = reference.problem || '未提供问题描述';
        const solution = reference.solution || '未提供解决方法';
        return `案例：${title}\n问题：${problem}\n解决：${solution}`;
      }).join('\n\n');
      appendChatMessage('agent', 'RAG 历史案例', references);
    }
    appendChatMessage('agent', '系统', result.ragAvailable
      ? '本次指导已参考课程历史 Bug 案例。'
      : '本次指导未使用 RAG，历史案例服务暂不可用。');
  } catch (error) {
    if (!isRunContextCurrent(runId, epoch)) return;
    appendChatMessage('agent', '系统', error.message || '代码指导暂时不可用，请稍后重试。');
  } finally {
    if (isRunContextCurrent(runId, epoch)) {
      sendButton.disabled = false;
      softenButton.disabled = false;
    }
  }
}

function appendDiagnosisPatch(patch) {
  const message = document.createElement('div');
  message.className = 'chat-message agent-message diagnosis-message';
  const label = document.createElement('div');
  label.className = 'message-label';
  label.textContent = '诊断补丁';
  const text = document.createElement('p');
  text.textContent = `${patch.description || '建议修改'}（${(patch.files || []).join('、')}）`;
  const button = document.createElement('button');
  button.className = 'secondary-action diagnosis-apply-button';
  button.type = 'button';
  button.textContent = '查看补丁差异';
  button.addEventListener('click', () => void openPatchReview(patch, button));
  message.append(label, text, button);
  chatMessages.appendChild(message);
  chatMessages.scrollTop = chatMessages.scrollHeight;
}

function closePatchReview() {
  patchReviewEpoch += 1;
  if (patchReviewSourceButton && !patchReviewApplied) patchReviewSourceButton.disabled = false;
  patchReviewSourceButton = null;
  patchReviewApplied = false;
  pendingPatchReview = null;
  patchDiffList.replaceChildren();
  setMessage(patchDialogMessage, '');
  confirmPatchButton.disabled = true;
  patchDialogDescription.textContent = '确认前请检查每个文件的当前内容和建议内容。系统应用时仍会再次校验文件哈希。';
  patchDialog.classList.remove('is-open');
  patchDialog.setAttribute('aria-hidden', 'true');
}

function renderPatchFileDiff(file, current) {
  const item = document.createElement('article');
  item.className = 'patch-diff-item';
  const heading = document.createElement('div');
  heading.className = 'patch-diff-heading';
  const path = document.createElement('strong');
  path.textContent = file.path;
  const state = document.createElement('span');
  const currentHash = current.file && current.file.contentHash;
  const isStale = currentHash && file.expectedHash && currentHash.toLowerCase() !== file.expectedHash.toLowerCase();
  state.className = `patch-file-state ${isStale ? 'is-stale' : 'is-current'}`;
  state.textContent = isStale ? '文件已变化，应用将被拒绝' : '哈希匹配';
  heading.append(path, state);
  const columns = document.createElement('div');
  columns.className = 'patch-diff-columns';
  [['当前内容', current.content || ''], ['建议内容', file.content || '']].forEach(([labelText, content]) => {
    const column = document.createElement('section');
    const label = document.createElement('span');
    label.className = 'patch-diff-label';
    label.textContent = labelText;
    const code = document.createElement('pre');
    code.textContent = content;
    column.append(label, code);
    columns.append(column);
  });
  item.append(heading, columns);
  return item;
}

async function openPatchReview(patch, sourceButton) {
  if (!activeRun || !patch || !patch.id) return;
  const requestEpoch = ++patchReviewEpoch;
  const runId = activeRun.runId;
  const workspaceId = activeRun.workspaceId;
  patchReviewSourceButton = sourceButton;
  patchReviewApplied = false;
  pendingPatchReview = null;
  patchDiffList.replaceChildren();
  setMessage(patchDialogMessage, '正在读取补丁和当前文件…');
  patchDialogDescription.textContent = patch.description || '请检查补丁内容后再决定是否应用。';
  confirmPatchButton.disabled = true;
  patchDialog.classList.add('is-open');
  patchDialog.setAttribute('aria-hidden', 'false');
  sourceButton.disabled = true;
  try {
    const detail = await apiRequest(`/runs/${runId}/patches/${patch.id}`, { method: 'GET', timeoutMs: 30000 });
    const files = Array.isArray(detail.files) ? detail.files : [];
    if (!files.length) throw new Error('补丁没有可预览的文件。');
    const currentFiles = await Promise.all(files.map(async (file) => {
      const response = await apiRequest(
        `/workspaces/${workspaceId}/files/${workspaceFileUrl(file.path)}`, { method: 'GET', timeoutMs: 30000 },
      );
      return verifyWorkspaceFileResponse(response, workspaceId, { path: file.path });
    }));
    if (requestEpoch !== patchReviewEpoch || !activeRun || activeRun.runId !== runId) return;
    pendingPatchReview = { detail, sourceButton };
    files.forEach((file, index) => patchDiffList.append(renderPatchFileDiff(file, currentFiles[index])));
    setMessage(patchDialogMessage, '请确认文件内容和哈希状态；确认后才会创建应用前快照并修改工作区。');
    confirmPatchButton.disabled = false;
  } catch (error) {
    if (requestEpoch !== patchReviewEpoch) return;
    setMessage(patchDialogMessage, error.message || '补丁预览失败，请稍后重试。', true);
    sourceButton.disabled = false;
    patchReviewSourceButton = null;
  }
}

async function confirmPatchReview() {
  if (!pendingPatchReview || !activeRun) return;
  const { detail, sourceButton } = pendingPatchReview;
  const runId = String(activeRun.runId);
  const workspaceId = String(activeRun.workspaceId);
  const epoch = routeResolutionEpoch;
  const requestedPatchReviewEpoch = patchReviewEpoch;
  confirmPatchButton.disabled = true;
  setMessage(patchDialogMessage, '正在再次校验并应用补丁…');
  try {
    const result = await apiRequest(`/runs/${runId}/patches/${detail.id}/apply`, { method: 'POST', timeoutMs: 30000 });
    if (!isRunContextCurrent(runId, epoch) || requestedPatchReviewEpoch !== patchReviewEpoch) return;
    sourceButton.disabled = true;
    sourceButton.textContent = '已应用并生成快照';
    patchReviewApplied = true;
    closePatchReview();
    appendChatMessage('agent', '系统', `补丁已应用：${(result.appliedFiles || []).join('、')}。应用前快照编号 ${result.snapshotId}，请重新运行验证。`);
    if (activeFilePath) {
      const currentPath = activeFilePath;
      const response = await apiRequest(`/workspaces/${workspaceId}/files/${workspaceFileUrl(currentPath)}`, { method: 'GET' });
      const file = await verifyWorkspaceFileResponse(response, workspaceId, { path: currentPath });
      if (!isRunContextCurrent(runId, epoch) || activeFilePath !== currentPath) return;
      codeEditor.value = file.content;
      activeFileSavedContent = file.content;
      renderCodeLineNumbers();
      activeFileHash = file.file.contentHash || null;
      activeWorkspaceFiles = activeWorkspaceFiles.map((item) => item.path === activeFilePath
        ? { ...item, contentHash: activeFileHash } : item);
    }
  } catch (error) {
    if (!isRunContextCurrent(runId, epoch) || requestedPatchReviewEpoch !== patchReviewEpoch) return;
    confirmPatchButton.disabled = false;
    setMessage(patchDialogMessage, error.message || '补丁未应用，文件可能已经发生变化。', true);
  }
}

closePatchDialog.addEventListener('click', closePatchReview);
cancelPatchButton.addEventListener('click', closePatchReview);
confirmPatchButton.addEventListener('click', () => void confirmPatchReview());
codeEditor.addEventListener('input', () => {
  renderCodeLineNumbers();
  renderWorkspaceFileTabs();
});
codeEditor.addEventListener('scroll', () => {
  if (codeLineNumbers) codeLineNumbers.style.transform = `translateY(-${codeEditor.scrollTop}px)`;
});

async function requestDeepDiagnosis() {
  if (!activeRun) {
    appendChatMessage('agent', '系统', '请先启动工作区，再发起深度诊断。');
    return;
  }
  const runId = String(activeRun.runId);
  const epoch = routeResolutionEpoch;
  deepDiagnosisButton.disabled = true;
  appendChatMessage('agent', '系统', '已提交深度诊断：系统将遍历可识别源码、配置和测试文件，完成后再展示补丁。');
  try {
    const task = await apiRequest(`/runs/${runId}/deep-diagnosis`, {
      method: 'POST', body: JSON.stringify({ question: '请分析当前项目并指出最值得优先验证的问题。' }), timeoutMs: 15000,
    });
    const deadline = Date.now() + 120000;
    let result = task;
    while (result.status === 'queued' || result.status === 'running') {
      if (!isRunContextCurrent(runId, epoch)) return;
      if (Date.now() >= deadline) throw new Error('深度诊断耗时较长，请稍后刷新诊断状态。');
      await new Promise((resolve) => window.setTimeout(resolve, 2000));
      if (!isRunContextCurrent(runId, epoch)) return;
      result = await apiRequest(`/diagnoses/${task.id}`, { method: 'GET', timeoutMs: 15000 });
    }
    if (!isRunContextCurrent(runId, epoch)) return;
    if (result.status !== 'completed') throw new Error(result.error || '深度诊断失败，请稍后重试。');
    appendChatMessage('agent', '深度诊断', result.summary || '诊断已完成，请按发现项逐条验证。');
    (result.findings || []).forEach((finding) => {
      appendChatMessage('agent', `发现 · ${finding.severity || '提示'}`, `${finding.title || '问题'}：${finding.detail || ''} 验证：${(finding.verificationSteps || []).join('；')}`);
    });
    if (!(result.patches || []).length) appendChatMessage('agent', '系统', '本次诊断没有生成可安全确认的补丁，请先按验证步骤定位问题。');
    (result.patches || []).forEach(appendDiagnosisPatch);
  } catch (error) {
    if (!isRunContextCurrent(runId, epoch)) return;
    appendChatMessage('agent', '系统', error.message || '深度诊断暂时不可用。');
  } finally {
    if (isRunContextCurrent(runId, epoch)) deepDiagnosisButton.disabled = false;
  }
}

function sendChatMessage() {
  const text = chatInput.value.trim();
  if (!text) return;
  chatInput.value = '';
  void requestGuidance(text);
}

sendButton.addEventListener('click', sendChatMessage);
chatInput.addEventListener('keydown', (event) => {
  if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
    event.preventDefault();
    sendChatMessage();
  }
});

softenButton.addEventListener('click', () => {
  void requestGuidance('请不要直接给出完整答案，先给我一个最小提示，并说明我应该如何验证当前代码。');
});

recordBugButton.addEventListener('click', openBugCaseDialog);
deepDiagnosisButton.addEventListener('click', requestDeepDiagnosis);
closeBugDialog.addEventListener('click', closeBugCaseDialog);
cancelBugButton.addEventListener('click', closeBugCaseDialog);
bugCaseForm.addEventListener('submit', saveBugCase);

terminalInput.addEventListener('keydown', async (event) => {
  if (event.key !== 'Enter') return;
  if (terminalCommandInFlight) return;
  const command = terminalInput.value.trim();
  if (!command) return;
  if (!activeTerminalSession) {
    appendChatMessage('agent', '系统', '终端尚未连接，请先启动工作区。');
    return;
  }
  if (activeFilePath && codeEditor.value !== activeFileSavedContent) {
    appendChatMessage('agent', '系统', '请先保存当前代码再运行终端命令，避免终端执行旧版本文件。');
    return;
  }
  if (!/^\d+$/.test(String(activeRun?.workspaceId || ''))) {
    appendChatMessage('agent', '系统', '工作区信息无效，已阻止执行命令。');
    return;
  }
  terminalInput.value = '';
  const terminalSessionId = String(activeTerminalSession.id);
  const runId = String(activeTerminalRunId);
  const workspaceId = String(activeRun.workspaceId);
  const epoch = routeResolutionEpoch;
  terminalCommandInFlight = true;
  terminalInput.disabled = true;
  codeEditor.disabled = true;
  saveFileButton.disabled = true;
  renderWorkspaceFileTabs();
  const line = document.createElement('div');
  const prompt = document.createElement('span');
  prompt.className = 'terminal-prompt';
  prompt.textContent = '$';
  const commandText = document.createElement('span');
  commandText.textContent = ` ${command}`;
  line.append(prompt, commandText);
  terminalOutput.appendChild(line);
  try {
    const result = await apiRequest(`/terminal-sessions/${terminalSessionId}/commands`, {
      method: 'POST',
      body: JSON.stringify({ commandText: command, workingDirectory: '.' }),
      timeoutMs: 45000,
    });
    if (!isRunContextCurrent(runId, epoch) || String(activeTerminalSession?.id) !== terminalSessionId) return;
    const output = document.createElement('div');
    output.className = result.status === 'succeeded' ? 'terminal-result' : 'terminal-error';
    output.textContent = result.output || `命令结束，状态：${result.status}`;
    terminalOutput.appendChild(output);
  } catch (error) {
    if (!isRunContextCurrent(runId, epoch) || String(activeTerminalSession?.id) !== terminalSessionId) return;
    const failure = document.createElement('div');
    failure.className = 'terminal-error';
    failure.textContent = error.message || '命令执行失败。';
    terminalOutput.appendChild(failure);
  } finally {
    if (isRunContextCurrent(runId, epoch) && String(activeTerminalSession?.id) === terminalSessionId) {
      await refreshWorkspaceFilesAfterTerminal(runId, workspaceId, epoch);
      terminalCommandInFlight = false;
      codeEditor.disabled = !activeFilePath;
      saveFileButton.disabled = !activeFilePath;
      renderWorkspaceFileTabs();
      terminalInput.disabled = false;
      terminalOutput.scrollTop = terminalOutput.scrollHeight;
    } else {
      terminalCommandInFlight = false;
    }
  }
});

document.querySelectorAll('.task-item').forEach((task) => {
  task.addEventListener('click', () => {
    document.querySelectorAll('.task-item').forEach((item) => item.classList.remove('is-active'));
    task.classList.add('is-active');
  });
});

function updateDurationPreview() {
  const rawHours = durationHoursInput.value.trim();
  const numericHours = Number(rawHours);
  const parsedHours = Number.isFinite(numericHours) ? Math.trunc(numericHours) : 0;
  durationTotalHours = rawHours ? Math.min(87600, Math.max(0, Number.isNaN(parsedHours) ? 0 : parsedHours)) : 0;
  if (rawHours && (Number(rawHours) !== parsedHours || parsedHours !== durationTotalHours)) durationHoursInput.value = durationTotalHours;
  const days = Math.floor(durationTotalHours / 24);
  const hours = durationTotalHours % 24;
  selectedDuration = `${days} 天 ${hours} 小时`;
  durationPreview.classList.toggle('is-invalid', durationTotalHours <= 0);
  durationPreview.textContent = durationTotalHours > 0
    ? `当前周期：${selectedDuration} · 共 ${durationTotalHours} 小时`
    : '请输入大于 0 的整数小时数。';
}

async function apiRequest(path, options = {}) {
  if (API_BASE_URL_ERROR) throw new Error(`API 地址配置错误：${API_BASE_URL_ERROR}`);
  const { timeoutMs: requestedTimeoutMs = 60000, ...fetchOptions } = options;
  const timeoutMs = Number.isFinite(requestedTimeoutMs) && requestedTimeoutMs > 0
    ? requestedTimeoutMs
    : 60000;
  const timeoutController = new AbortController();
  const timeout = window.setTimeout(() => timeoutController.abort(), timeoutMs);
  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...fetchOptions,
      credentials: 'include',
      signal: timeoutController.signal,
      headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      const requestError = new Error(payload.message || `请求失败（${response.status}）`);
      requestError.status = response.status;
      throw requestError;
    }
    return payload;
  } catch (error) {
    if (error.name === 'AbortError') throw new Error('请求超时，请检查后端和 Harness 状态后重试。');
    if (error instanceof TypeError && error.message === 'Failed to fetch') {
      throw new Error('无法连接服务器，请检查后端服务是否已启动。');
    }
    throw error;
  } finally {
    window.clearTimeout(timeout);
  }
}

function difficultyCode(label) {
  return { 入门: 'BEGINNER', 简单: 'EASY', 普通: 'NORMAL', 困难: 'HARD', 挑战: 'CHALLENGE' }[label];
}

function appendCreationMessage(role, text) {
  const message = document.createElement('div');
  message.className = `direction-message ${role === 'user' ? 'is-user' : role === 'error' ? 'is-error' : 'is-agent'}`;
  const avatar = document.createElement('span');
  avatar.className = 'direction-message-avatar';
  avatar.textContent = role === 'user' ? '我' : role === 'error' ? '!' : 'H';
  const content = document.createElement('div');
  content.className = 'direction-message-content';
  const label = document.createElement('strong');
  label.textContent = role === 'user' ? '你' : role === 'error' ? '连接提示' : 'Harness';
  const body = document.createElement('p');
  body.textContent = text;
  content.append(label, body);
  message.append(avatar, content);
  creationConversation.append(message);
  creationConversation.scrollTop = creationConversation.scrollHeight;
}

function showCreationMessage(text, isError = false) {
  appendCreationMessage(isError ? 'error' : 'assistant', text);
}

function setCreateLoading(active, text = 'Harness 正在处理中…') {
  createLoading.hidden = !active;
  createLoading.setAttribute('aria-hidden', String(!active));
  createDialog.setAttribute('aria-busy', String(active));
  if (active) createLoadingText.textContent = text;
}

function renderPlanPreview(plan) {
  const goal = directionTitle.textContent || '新的学习实验';
  pendingPlan = plan;
  updateDurationPreview();
  planDirection.textContent = goal;
  planDuration.textContent = selectedDuration;
  planDifficulty.textContent = selectedDifficulty;
  planPurpose.textContent = plan.purpose.join('；');
  planNodes.replaceChildren();
  const nodeNumbers = buildTreeNodeNumbers(plan.nodes);
  const children = new Map();
  plan.nodes.forEach((planNode) => {
    const parent = planNode.parentKey || '__root__';
    if (!children.has(parent)) children.set(parent, []);
    children.get(parent).push(planNode);
  });
  children.forEach((items) => items.sort(compareTreeSiblings));
  const buildBranch = (planNode) => {
    const branch = document.createElement('div');
    branch.className = 'plan-tree-branch';
    const node = document.createElement('div');
    node.className = 'plan-node';
    node.innerHTML = '<span class="plan-node-sequence"></span><div><strong></strong><p></p></div>';
    node.querySelector('.plan-node-sequence').textContent = nodeNumbers.get(planNode.key) || planNode.key;
    node.querySelector('strong').textContent = `${planNode.stage} · ${planNode.title}`;
    node.querySelector('p').textContent = planNode.tasks.map((task, taskIndex) => `${taskIndex + 1}. ${task.title}`).join('　');
    branch.append(node);
    const descendants = children.get(planNode.key) || [];
    if (descendants.length) {
      const childRow = document.createElement('div');
      childRow.className = 'plan-tree-children';
      descendants.forEach((child) => childRow.append(buildBranch(child)));
      branch.append(childRow);
    }
    return branch;
  };
  (children.get('__root__') || []).forEach((root) => planNodes.append(buildBranch(root)));
}

async function openCreateDialog() {
  const epoch = ++createDialogEpoch;
  createStep = 1;
  createPlanReady = false;
  directionReady = false;
  creationSessionId = null;
  pendingPlan = null;
  materializationKey = null;
  createPlan.classList.remove('is-active');
  durationHoursInput.value = 24;
  learningGoal.value = '';
  selectedDifficulty = '入门';
  document.querySelectorAll('[data-choice-group="difficulty"]').forEach((choice) => {
    choice.classList.toggle('is-selected', choice.dataset.value === selectedDifficulty);
  });
  directionReview.hidden = true;
  creationConversation.replaceChildren();
  appendCreationMessage('assistant', '告诉我你想学习什么，或想完成什么成果。我会先帮你把方向说清楚；如果关键信息不足，只会一次追问一个问题。');
  updateDurationPreview();
  directionTitle.textContent = '等待第一段对话';
  directionDetail.textContent = '完成方向描述后，这里会显示 Harness 记录的事实。';
  updateCreateStep();
  createDialog.classList.add('is-open');
  learningGoal.placeholder = defaultLearningGoalPlaceholder;
  createBusy = true;
  updateCreateStep();
  setCreateLoading(true, '正在建立独立的 Harness 会话…');
  try {
    const session = await apiRequest('/experiment-creation/sessions', { method: 'POST', body: '{}' });
    if (epoch !== createDialogEpoch || !createDialog.classList.contains('is-open')) return;
    creationSessionId = session.sessionId;
    learningGoal.focus();
  } catch (error) {
    showCreationMessage(error.message, true);
  } finally {
    if (epoch === createDialogEpoch && createDialog.classList.contains('is-open')) {
      createBusy = false;
      setCreateLoading(false);
      updateCreateStep();
    }
  }
}

function closeCreateDialogPanel() {
  createDialogEpoch += 1;
  createDialog.classList.remove('is-open');
  createBusy = false;
  setCreateLoading(false);
  updateCreateStep();
}

function updateCreateStep() {
  createStepLabel.textContent = createPlanReady ? '方案 / 确认' : `0${createStep} / 03`;
  document.querySelectorAll('.create-step').forEach((step) => step.classList.toggle('is-active', !createPlanReady && Number(step.dataset.step) === createStep));
  document.querySelectorAll('.step-progress span').forEach((bar, index) => bar.classList.toggle('is-active', createPlanReady || index < createStep));
  document.querySelectorAll('.create-dialog-step').forEach((step) => {
    const stage = Number(step.dataset.createStage);
    const isCurrent = !createPlanReady && stage === createStep;
    const isComplete = createPlanReady || stage < createStep;
    step.classList.toggle('is-current', isCurrent);
    step.classList.toggle('is-complete', isComplete);
    step.setAttribute('aria-label', `${stage}. ${step.querySelector('strong').textContent}，${isComplete ? '已完成' : isCurrent ? '进行中' : '未开始'}`);
    if (isCurrent) step.setAttribute('aria-current', 'step');
    else step.removeAttribute('aria-current');
  });
  const hasDirectionCorrection = learningGoal.value.trim().length > 0;
  const label = createPlanReady
    ? '确认并创建实验'
    : createStep === 1
      ? directionReady && !hasDirectionCorrection ? '确认方向并继续' : directionReady ? '补充方向' : '发送给 Harness'
      : createStep === 2 ? '保存周期并继续' : '生成实验方案';
  createStepButton.innerHTML = `${label} <span>→</span>`;
  createStepButton.disabled = createBusy;
  learningGoal.disabled = createBusy;
  durationHoursInput.disabled = createBusy;
  document.querySelectorAll('.choice-item').forEach((choice) => { choice.disabled = createBusy; });
}

learningGoal.addEventListener('input', updateCreateStep);
learningGoal.addEventListener('keydown', (event) => {
  if (event.key !== 'Enter' || event.shiftKey || event.isComposing || createBusy) return;
  event.preventDefault();
  createStepButton.click();
});

durationHoursInput.addEventListener('input', updateDurationPreview);

document.querySelectorAll('.choice-item').forEach((choice) => {
  choice.addEventListener('click', () => {
    if (createBusy) return;
    const group = choice.dataset.choiceGroup;
    document.querySelectorAll(`[data-choice-group="${group}"]`).forEach((item) => item.classList.remove('is-selected'));
    choice.classList.add('is-selected');
    if (group === 'difficulty') selectedDifficulty = choice.dataset.value;
    if (group === 'duration') selectedDuration = choice.dataset.value;
  });
});

createStepButton.addEventListener('click', async () => {
  if (createBusy) return;
  if (!creationSessionId) {
    showCreationMessage('需求会话尚未建立，请关闭后重试。', true);
    return;
  }
  createBusy = true;
  const requestEpoch = createDialogEpoch;
  updateCreateStep();
  setCreateLoading(true);
  try {
    if (createPlanReady) {
      setCreateLoading(true, '正在创建实验节点和连线…');
      materializationKey ||= crypto.randomUUID ? crypto.randomUUID() : `create-${creationSessionId}-${Date.now()}`;
      const created = await apiRequest(`/experiment-creation/sessions/${creationSessionId}/confirm`, {
        method: 'POST', headers: { 'Idempotency-Key': materializationKey }, body: '{}',
      });
      const id = String(created.experimentId);
      experimentData[id] = { ...created, description: created.description, nodes: created.nodes };
      await loadPersonalExperiments(routeResolutionEpoch);
      createBusy = false;
      closeCreateDialogPanel();
      learningGoal.value = '';
      createPlanReady = false;
      if (getCurrentUser()?.role === 'student') {
        openExperiment(id);
      } else {
        navigateTo({ type: 'teacher' });
      }
      return;
    }

    if (createStep === 1 && (!directionReady || learningGoal.value.trim())) {
      setCreateLoading(true, 'Harness 正在整理实验方向…');
      const message = learningGoal.value.trim();
      if (!message) {
        learningGoal.focus();
        return;
      }
      const envelope = await apiRequest(`/experiment-creation/sessions/${creationSessionId}/messages`, {
        method: 'POST', body: JSON.stringify({ message }),
      });
      appendCreationMessage('user', message);
      showCreationMessage(envelope.assistantMessage);
      learningGoal.value = '';
      directionTitle.textContent = envelope.intent.observable_outcome || envelope.intent.raw_request;
      directionDetail.textContent = envelope.intent.target_artifact || envelope.assistantMessage;
      directionReady = envelope.status === 'ready_for_confirmation';
      directionTitleReview.textContent = directionTitle.textContent;
      directionDetailReview.textContent = directionDetail.textContent;
      directionReview.hidden = !directionReady;
      learningGoal.placeholder = directionReady
        ? '如果想修正或补充方向，可以继续告诉 Harness…'
        : envelope.nextQuestion?.text || '请继续补充…';
      return;
    }

    if (createStep === 1 && directionReady) {
      setCreateLoading(true, '正在确认实验方向…');
      await apiRequest(`/experiment-creation/sessions/${creationSessionId}/direction/confirm`, { method: 'POST', body: '{}' });
      createStep = 2;
      updateCreateStep();
      return;
    }

    if (createStep === 2) {
      setCreateLoading(true, '正在保存学习周期…');
      updateDurationPreview();
      if (durationTotalHours <= 0) {
        durationHoursInput.focus();
        return;
      }
      createStep = 3;
      updateCreateStep();
      return;
    }

    setCreateLoading(true, 'Harness 正在生成树状实验方案，请稍候…');
    await apiRequest(`/experiment-creation/sessions/${creationSessionId}/configuration`, {
      method: 'PATCH',
      body: JSON.stringify({
        durationDays: Math.floor(durationTotalHours / 24),
        durationHours: durationTotalHours % 24,
        difficulty: difficultyCode(selectedDifficulty),
      }),
    });
    const plan = await apiRequest(`/experiment-creation/sessions/${creationSessionId}/plan`, {
      method: 'POST',
      body: '{}',
      timeoutMs: 150000,
    });
    renderPlanPreview(plan);
    createPlanReady = true;
    document.querySelectorAll('.create-step').forEach((step) => step.classList.remove('is-active'));
    createPlan.classList.add('is-active');
  } catch (error) {
    showCreationMessage(error.message, true);
  } finally {
    if (requestEpoch === createDialogEpoch && createDialog.classList.contains('is-open')) {
      createBusy = false;
      setCreateLoading(false);
      updateCreateStep();
    }
  }
});

createExperimentButton.addEventListener('click', openCreateDialog);
closeCreateDialog.addEventListener('click', closeCreateDialogPanel);
createDialog.addEventListener('click', (event) => {
  if (event.target === createDialog) closeCreateDialogPanel();
});

window.addEventListener('resize', () => {
  if (!panState && !bubbleDragState) renderCanvas();
});

loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const email = loginEmail.value.trim().toLowerCase();
  const password = loginPassword.value;
  if (!email || !password) {
    setMessage(loginError, '请输入邮箱和密码。', true);
    return;
  }
  const submitButton = loginForm.querySelector('button[type="submit"]');
  submitButton.disabled = true;
  setMessage(loginError, '正在验证账号…');
  try {
    const session = await apiRequest('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
      timeoutMs: 15000,
    });
    setCurrentUser(session.user);
    loginPassword.value = '';
    setMessage(loginError, '');
    const destination = pendingProtectedRoute || readPendingRoute() || { type: getCurrentUser().role };
    pendingProtectedRoute = null;
    removeSessionValue(STORAGE_KEYS.pendingRoute);
    navigateTo(destination);
  } catch (error) {
    setMessage(loginError, error.message || '登录失败，请稍后重试。', true);
  } finally {
    submitButton.disabled = false;
  }
});
loginEmail.addEventListener('input', () => setMessage(loginError, ''));
loginPassword.addEventListener('input', () => setMessage(loginError, ''));

joinCourseForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const normalizedCode = joinCode.value.trim().toUpperCase();
  if (!normalizedCode) {
    setMessage(studentMessage, '请输入课程邀请码。', true);
    joinCode.focus();
    return;
  }
  const submitButton = joinCourseForm.querySelector('button[type="submit"]');
  submitButton.disabled = true;
  setMessage(studentMessage, '正在加入课程…');
  try {
    const course = await apiRequest('/courses/join', {
      method: 'POST',
      body: JSON.stringify({ inviteCode: normalizedCode }),
    });
    joinCode.value = '';
    setMessage(studentMessage, `已加入「${course.name}」，现在可以查看教师发布的实验。`);
    await renderStudentDashboard();
  } catch (error) {
    setMessage(studentMessage, error.message || '加入课程失败，请稍后重试。', true);
  } finally {
    submitButton.disabled = false;
  }
});

createCourseForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const name = courseName.value.trim();
  if (!name) {
    setMessage(teacherMessage, '请输入课程名称。', true);
    courseName.focus();
    return;
  }
  const submitButton = createCourseForm.querySelector('button[type="submit"]');
  submitButton.disabled = true;
  setMessage(teacherMessage, '正在创建课程…');
  try {
    const course = await apiRequest('/courses', {
      method: 'POST',
      body: JSON.stringify({ name }),
    });
    courseName.value = '';
    setMessage(teacherMessage, `课程已创建，邀请码是 ${course.inviteCode}。`);
    await renderTeacherDashboard();
    publishCourse.value = String(course.id);
  } catch (error) {
    setMessage(teacherMessage, error.message || '课程创建失败，请稍后重试。', true);
  } finally {
    submitButton.disabled = false;
  }
});

publishForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const courseId = publishCourse.value;
  const experimentId = publishExperiment.value;
  if (!courseId) {
    setMessage(teacherMessage, '请选择要发布到的课程。', true);
    publishCourse.focus();
    return;
  }
  if (!experimentId) {
    setMessage(teacherMessage, '请选择要发布的实验草稿。', true);
    publishExperiment.focus();
    return;
  }
  const submitButton = publishForm.querySelector('button[type="submit"]');
  submitButton.disabled = true;
  setMessage(teacherMessage, '正在发布实验…');
  try {
    await apiRequest(`/experiments/${encodeURIComponent(experimentId)}/publish`, {
      method: 'POST',
      body: JSON.stringify({ courseId: Number(courseId) }),
    });
    setMessage(teacherMessage, '实验已发布，学生端将可以查看。');
    await renderTeacherDashboard();
    publishCourse.value = courseId;
    publishExperiment.value = experimentId;
  } catch (error) {
    setMessage(teacherMessage, error.message || '实验发布失败，请稍后重试。', true);
  } finally {
    submitButton.disabled = false;
  }
});

studentCourses.addEventListener('click', async (event) => {
  const button = event.target.closest('.open-published-experiment');
  if (!button) return;
  const routeEpoch = routeResolutionEpoch;
  const userId = getCurrentUser()?.id;
  const experimentIdValue = String(button.dataset.experimentId);
  button.disabled = true;
  setMessage(studentMessage, '正在读取实验节点…');
  try {
    const experimentId = encodeURIComponent(experimentIdValue);
    const experiment = await apiRequest(`/experiments/${experimentId}`, { method: 'GET' });
    if (routeEpoch !== routeResolutionEpoch || getCurrentUser()?.id !== userId) return;
    experimentData[experimentIdValue] = experiment;
    setMessage(studentMessage, '');
    openExperiment(experimentIdValue);
  } catch (error) {
    if (routeEpoch !== routeResolutionEpoch || getCurrentUser()?.id !== userId) return;
    setMessage(studentMessage, error.message || '实验加载失败，请稍后重试。', true);
  } finally {
    if (routeEpoch === routeResolutionEpoch && getCurrentUser()?.id === userId) button.disabled = false;
  }
});

document.querySelector('#studentLibraryButton').addEventListener('click', () => showRoute('library'));
document.querySelector('#teacherLibraryButton').addEventListener('click', () => showRoute('library'));
teacherDashboardCourse.addEventListener('change', () => void loadTeacherCourseDashboard(teacherDashboardCourse.value));
teacherReviewForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!selectedTeacherRunId) {
    setMessage(teacherReviewMessage, '请先选择一个已开始的学生运行实例。', true);
    return;
  }
  const rating = teacherRating.value.trim();
  if (rating === '' || !/^\d{1,3}$/.test(rating) || Number(rating) < 0 || Number(rating) > 100) {
    setMessage(teacherReviewMessage, '请输入 0–100 的整数评分。', true);
    return;
  }
  const submitButton = teacherReviewForm.querySelector('button[type="submit"]');
  const runId = String(selectedTeacherRunId);
  const requestEpoch = teacherRunDetailRequestEpoch;
  submitButton.disabled = true;
  setMessage(teacherReviewMessage, '正在保存评价…');
  try {
    await apiRequest(`/teacher/runs/${runId}/reviews`, {
      method: 'POST', body: JSON.stringify({ rating: Number(rating), feedback: teacherFeedback.value.trim() }), timeoutMs: 15000,
    });
    if (requestEpoch !== teacherRunDetailRequestEpoch || selectedTeacherRunId !== Number(runId)) return;
    setMessage(teacherReviewMessage, '评价已保存。');
  } catch (error) {
    if (requestEpoch !== teacherRunDetailRequestEpoch || selectedTeacherRunId !== Number(runId)) return;
    setMessage(teacherReviewMessage, error.message || '评价保存失败，请稍后重试。', true);
  } finally {
    if (requestEpoch === teacherRunDetailRequestEpoch && selectedTeacherRunId === Number(runId)) {
      submitButton.disabled = false;
    }
  }
});
document.querySelector('#libraryDashboardButton').addEventListener('click', () => showRoute(getCurrentUser()?.role || 'login'));

async function logout() {
  try {
    await apiRequest('/auth/logout', { method: 'POST', timeoutMs: 10000 });
  } catch (error) {
    const messageTarget = getCurrentUser()?.role === 'teacher' ? teacherMessage : studentMessage;
    if (messageTarget) setMessage(messageTarget, '退出请求失败，会话仍保持有效，请重试。', true);
    else setMessage(loginError, '退出请求失败，请重试。', true);
    return;
  }
  setCurrentUser(null);
  pendingProtectedRoute = null;
  removeSessionValue(STORAGE_KEYS.pendingRoute);
  removeSessionValue(STORAGE_KEYS.activeExperiment);
  clearCachedTerminalSessions();
  activeRun = null;
  activeRunProgress = null;
  runProgressLoadFailed = false;
  activeTerminalSession = null;
  activeTerminalRunId = null;
  activeExperimentId = null;
  closePatchReview();
  createDialog.classList.remove('is-open');
  closeInspector();
  showRoute('login');
}

document.querySelectorAll('#studentLogoutButton, #teacherLogoutButton, #libraryLogoutButton').forEach((button) => button.addEventListener('click', logout));

async function restoreSession() {
  renderCanvas();
  updateLibraryCount();
  let sessionError = null;
  try {
    const sessionUser = await apiRequest('/me', { method: 'GET', timeoutMs: 10000 });
    setCurrentUser(sessionUser);
  } catch (error) {
    sessionError = error;
    setCurrentUser(null);
  }
  if (sessionError && sessionError.status !== 401) {
    const currentRoute = parseHashRoute();
    if (['student', 'teacher', 'library', 'canvas', 'editor'].includes(currentRoute.type)) {
      pendingProtectedRoute = currentRoute;
      persistPendingRoute(currentRoute);
    }
    sessionReady = true;
    renderSimpleRoute('login');
    setRouteStatus(API_BASE_URL_ERROR
      ? `API 地址配置错误：${API_BASE_URL_ERROR}`
      : '暂时无法验证登录状态，请检查服务连接后重试。', true);
    return;
  }
  if (!getCurrentUser()) pendingProtectedRoute = readPendingRoute();
  sessionReady = true;
  await resolveCurrentRoute();
}

window.addEventListener('hashchange', () => void resolveCurrentRoute());
restoreSession();
