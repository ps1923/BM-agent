const libraryView = document.querySelector('#libraryView');
const canvasView = document.querySelector('#canvasView');
const bubble = document.querySelector('#bubble');
const inspector = document.querySelector('#inspector');
const editorView = document.querySelector('#editorView');
const openExperimentButtons = document.querySelectorAll('.experiment-link');
const backToLibrary = document.querySelector('#backToLibrary');
const startNodeButton = document.querySelector('#startNodeButton');
const startNodeStatus = document.querySelector('#startNodeStatus');
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
const desktopNewFolderButton = document.querySelector('#desktopNewFolderButton');
const desktopNewFileButton = document.querySelector('#desktopNewFileButton');
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
const gitSubmissionPanel = document.querySelector('#gitSubmissionPanel');
const gitSubmissionExperimentName = document.querySelector('#gitSubmissionExperimentName');
const gitRepositoryState = document.querySelector('#gitRepositoryState');
const gitCloneCommand = document.querySelector('#gitCloneCommand');
const gitRepositoryHelp = document.querySelector('#gitRepositoryHelp');
const gitCopyCloneCommand = document.querySelector('#gitCopyCloneCommand');
const showGitCredentials = document.querySelector('#showGitCredentials');
const gitCredentials = document.querySelector('#gitCredentials');
const gitCredentialNotice = document.querySelector('#gitCredentialNotice');
const gitSubmissionForm = document.querySelector('#gitSubmissionForm');
const gitReviewNodeList = document.querySelector('#gitReviewNodeList');
const selectAllReviewNodes = document.querySelector('#selectAllReviewNodes');
const gitCommitSha = document.querySelector('#gitCommitSha');
const gitSubmissionDescription = document.querySelector('#gitSubmissionDescription');
const gitSubmissionMessage = document.querySelector('#gitSubmissionMessage');
const submitGitCommit = document.querySelector('#submitGitCommit');
const gitSubmissionHistory = document.querySelector('#gitSubmissionHistory');
const refreshGitSubmissions = document.querySelector('#refreshGitSubmissions');
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
const adminView = document.querySelector('#adminView');
const adminIdentity = document.querySelector('#adminIdentity');
const adminExperiment = document.querySelector('#adminExperiment');
const adminReviewer = document.querySelector('#adminReviewer');
const adminAssignReviewer = document.querySelector('#adminAssignReviewer');
const adminReviewerMessage = document.querySelector('#adminReviewerMessage');
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
const studentLocalCodingState = document.querySelector('#studentLocalCodingState');
const studentLocalCodingButton = document.querySelector('#studentLocalCodingButton');
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
const teacherRunSummaryState = document.querySelector('#teacherRunSummaryState');
const teacherRunSummary = document.querySelector('#teacherRunSummary');
const teacherAiSummary = document.querySelector('#teacherAiSummary');
const teacherRunMetrics = document.querySelector('#teacherRunMetrics');
const teacherTimeline = document.querySelector('#teacherTimeline');
const teacherTimelineState = document.querySelector('#teacherTimelineState');
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
const teacherDshExperiment = document.querySelector('#teacherDshExperiment');
const teacherDshMessage = document.querySelector('#teacherDshMessage');
const teacherDshRuns = document.querySelector('#teacherDshRuns');
const teacherDshSubmissions = document.querySelector('#teacherDshSubmissions');
const teacherDshCode = document.querySelector('#teacherDshCode');
const teacherDshBugMessage = document.querySelector('#teacherDshBugMessage');
const teacherDshBugs = document.querySelector('#teacherDshBugs');
const teacherDshReviewForm = document.querySelector('#teacherDshReviewForm');
const teacherDshRating = document.querySelector('#teacherDshRating');
const teacherDshFeedback = document.querySelector('#teacherDshFeedback');
const teacherDshReviewSave = document.querySelector('#teacherDshReviewSave');
const teacherDshReviewMessage = document.querySelector('#teacherDshReviewMessage');
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
const creationDraftList = document.querySelector('#creationDraftList');
const personalExperimentList = document.querySelector('#personalExperimentList');
const defaultLearningGoalPlaceholder = learningGoal.placeholder;
let API_BASE_URL = '';
let API_BASE_URL_ERROR = '';
try {
  if (window.BMDesktop?.isDesktop) {
    API_BASE_URL = 'desktop://main-process-proxy';
  } else {
    if (typeof window.BMApiConfig?.resolveApiBaseUrl !== 'function') {
      throw new Error('API 配置模块未加载。');
    }
    API_BASE_URL = window.BMApiConfig.resolveApiBaseUrl(window.location, window.BM_API_BASE_URL);
  }
} catch (error) {
  API_BASE_URL_ERROR = error.message || 'API 地址配置无效。';
}
const STORAGE_KEYS = {
  pendingRoute: 'bmhs.demo.pendingRoute',
  teacherCourse: 'bmhs.teacherCourse',
  activeExperiment: 'bmhs.activeExperiment',
  terminalSessionPrefix: 'bmhs.terminalSession.',
  terminalSessionRequestPrefix: 'bmhs.terminalSession.request.',
  terminalCommandPrefix: 'bmhs.terminalCommand.',
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
let creationSessionOwnerId = null;
let creationPendingMessage = null;
let directionReady = false;
let pendingPlan = null;
let createBusy = false;
let materializationKey = null;
let createDialogEpoch = 0;
let activeCreateRequestController = null;
let createMaterializationInFlight = false;
let activeTreeNodes = [];
let selectedTreeNode = null;
let treeEdgeLayer = null;
let activeExperimentId = null;
let activeRun = null;
let currentGitBugDraftContext = null;
let activeGitRepository = null;
let gitSubmissionRequestEpoch = 0;
let activeFilePath = null;
let activeFileHash = null;
let activeFileSavedContent = '';
let activeWorkspaceFiles = [];
let lastDesktopTerminalOutput = '';
let workspaceFileSelectionEpoch = 0;
const terminalCommandsInFlight = new Set();
let teacherDashboardEpoch = 0;
let teacherCourseDashboardEpoch = 0;
let studentDashboardEpoch = 0;
let taskProgressRequestEpoch = 0;
let taskProgressMutationInFlight = false;
const taskProgressButtonEpochs = new Map();
let activeTerminalSession = null;
let activeTerminalRunId = null;
let pendingPatchReview = null;
let patchReviewEpoch = 0;
let patchReviewSourceButton = null;
let patchReviewApplied = false;
let patchApplyInFlight = false;
let activeNodeId = null;
let selectedTeacherRunId = null;
let activeRunProgress = null;
let runProgressLoadFailed = false;
const guidanceRequestsInFlight = new Set();

const experimentData = {};
const recentlyOpenedExperimentIds = [];

function getCurrentUser() {
  return currentUser;
}

function isRunContextCurrent(runId, epoch) {
  return epoch === routeResolutionEpoch && String(activeRun?.runId) === String(runId);
}

function syncGuidanceControls() {
  const activeRunId = activeRun ? String(activeRun.runId) : null;
  const requestInFlight = activeRunId !== null && guidanceRequestsInFlight.has(activeRunId);
  sendButton.disabled = requestInFlight;
  softenButton.disabled = requestInFlight;
}

function setCurrentUser(user) {
  const nextUser = user && ['student', 'teacher', 'admin'].includes(user.role)
    ? { ...user, name: user.name || user.displayName || user.email }
    : null;
  if (currentUser?.id !== nextUser?.id) {
    Object.keys(experimentData).forEach((id) => delete experimentData[id]);
    recentlyOpenedExperimentIds.length = 0;
    libraryLoadEpoch += 1;
    renderRecentExperiments();
    creationSessionId = null;
    creationSessionOwnerId = null;
    creationPendingMessage = null;
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
  openButton.textContent = '查看学习路径';
  actions.append(status, openButton);
  row.append(title, actions);
  return row;
}

async function renderStudentDashboard() {
  const user = getCurrentUser();
  if (!user || user.role !== 'student') {
    clearStudentDashboard();
    return;
  }
  const requestEpoch = ++studentDashboardEpoch;
  const userId = String(user.id);
  const isCurrentRequest = () => requestEpoch === studentDashboardEpoch
    && String(getCurrentUser()?.id) === userId
    && getCurrentUser()?.role === 'student';
  studentIdentity.textContent = `${user.name} · ${user.email}`;
  if (studentLocalCodingState) studentLocalCodingState.textContent = '在本地 IDE 编码；push 到实验仓库后可选择节点进行 AI 验收。';
  studentCourseCount.textContent = '加载中…';
  studentCourses.replaceChildren();
  try {
    const enrolled = await apiRequest('/courses', { method: 'GET' });
    if (!isCurrentRequest()) return;
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
    if (!isCurrentRequest()) return;
    studentCourseCount.textContent = '加载失败';
    const message = document.createElement('p');
    message.className = 'course-empty';
    message.textContent = error.message || '课程加载失败，请刷新后重试。';
    studentCourses.append(message);
  }
}

async function renderAdminDashboard() {
  const user = getCurrentUser();
  if (!user || user.role !== 'admin') return;
  adminIdentity.textContent = `${user.name} · ${user.email}`;
  adminExperiment.replaceChildren();
  adminReviewer.replaceChildren();
  adminAssignReviewer.disabled = true;
  setMessage(adminReviewerMessage, '正在加载个人实验和教师列表…');
  try {
    const [experiments, teachers] = await Promise.all([
      apiRequest('/admin/reviewable-experiments', { method: 'GET' }),
      apiRequest('/admin/teachers', { method: 'GET' }),
    ]);
    if (getCurrentUser()?.id !== user.id || getCurrentUser()?.role !== 'admin') return;
    const experimentPlaceholder = document.createElement('option');
    experimentPlaceholder.value = '';
    experimentPlaceholder.textContent = experiments.length ? '请选择个人实验' : '暂无可绑定的个人实验';
    adminExperiment.append(experimentPlaceholder);
    experiments.forEach((experiment) => {
      const option = document.createElement('option');
      option.value = String(experiment.experimentId);
      option.dataset.teacherId = experiment.teacherId == null ? '' : String(experiment.teacherId);
      option.textContent = `${experiment.name} · ${experiment.teacherName || '未绑定'}`;
      adminExperiment.append(option);
    });
    const teacherPlaceholder = document.createElement('option');
    teacherPlaceholder.value = '';
    teacherPlaceholder.textContent = teachers.length ? '请选择审核教师' : '暂无可用教师';
    adminReviewer.append(teacherPlaceholder);
    teachers.forEach((teacher) => {
      const option = document.createElement('option');
      option.value = String(teacher.teacherId);
      option.textContent = `${teacher.displayName} · ${teacher.email}`;
      adminReviewer.append(option);
    });
    adminAssignReviewer.disabled = !experiments.length || !teachers.length;
    setMessage(adminReviewerMessage, '选择实验后可查看或更换其审核教师；课程已发布实验由课程教师负责审核。');
  } catch (error) {
    setMessage(adminReviewerMessage, error.message || '管理数据加载失败。', true);
  }
}

function syncAdminReviewerSelection() {
  const option = adminExperiment.selectedOptions?.[0];
  if (option?.dataset?.teacherId) adminReviewer.value = option.dataset.teacherId;
  else adminReviewer.value = '';
}

function clearStudentDashboard() {
  studentDashboardEpoch += 1;
  studentCourses.replaceChildren();
  studentIdentity.textContent = '';
  studentCourseCount.textContent = '';
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
  teacherRunSummaryState.replaceChildren();
  teacherRunSummaryState.classList.remove('is-error');
  teacherRunSummary.textContent = '';
  teacherAiSummary.textContent = '';
  teacherAiSummary.hidden = true;
  teacherRunMetrics.replaceChildren();
  teacherTimeline.replaceChildren();
  teacherTimelineState.replaceChildren();
  teacherTimelineState.classList.remove('is-error');
  teacherStageEvidence.replaceChildren();
  teacherEvidenceFiles.replaceChildren();
  teacherEvidenceCode.textContent = '选择一个阶段快照文件查看。';
  setMessage(teacherEvidenceMessage, '');
  teacherRating.value = '';
  teacherRating.disabled = true;
  teacherFeedback.value = '';
  teacherFeedback.disabled = true;
  teacherReviewForm.querySelector('button[type="submit"]').disabled = true;
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
let teacherRunSummaryRequestEpoch = 0;
let teacherRunTimelineRequestEpoch = 0;
let teacherDshRequestEpoch = 0;
let teacherDshRunsRequestEpoch = 0;
let teacherDshSubmissionsRequestEpoch = 0;
let selectedTeacherDshRunId = null;

async function loadTeacherDshExperiments() {
  const requestEpoch = ++teacherDshRequestEpoch;
  ++teacherDshBugRequestEpoch;
  teacherDshExperiment.replaceChildren();
  teacherDshRuns.replaceChildren();
  teacherDshSubmissions.replaceChildren();
  teacherDshBugs.replaceChildren();
  selectedTeacherDshRunId = null;
  teacherDshRating.disabled = true;
  teacherDshFeedback.disabled = true;
  teacherDshReviewSave.disabled = true;
  teacherDshRating.value = '';
  teacherDshFeedback.value = '';
  setMessage(teacherDshReviewMessage, '选择学生查看或填写阶段评价。');
  teacherDshCode.textContent = '选择一名学生查看已提交 Git commit。';
  setMessage(teacherDshMessage, '正在加载授权实验…');
  try {
    const experiments = await apiRequest('/teacher/experiments', { method: 'GET' });
    if (requestEpoch !== teacherDshRequestEpoch || getCurrentUser()?.role !== 'teacher') return;
    const placeholder = document.createElement('option');
    placeholder.value = '';
    placeholder.textContent = experiments.length ? '请选择实验' : '暂无 DSH 实验权限';
    teacherDshExperiment.append(placeholder);
    experiments.forEach((experiment) => {
      const option = document.createElement('option');
      option.value = String(experiment.experimentId ?? experiment.id);
      option.textContent = `${experiment.name}${experiment.courseName ? ` · ${experiment.courseName}` : ' · 个人实验'}`;
      teacherDshExperiment.append(option);
    });
    setMessage(teacherDshMessage, experiments.length
      ? '学生只主动提交的快照对教师可见。'
      : '目前没有已授权的 DSH 实验。');
    if (experiments.length) {
      teacherDshExperiment.value = String(experiments[0].experimentId ?? experiments[0].id);
      void loadTeacherDshRuns(teacherDshExperiment.value);
    }
  } catch (error) {
    if (requestEpoch !== teacherDshRequestEpoch) return;
    const placeholder = document.createElement('option');
    placeholder.value = '';
    placeholder.textContent = 'DSH 记录暂不可用';
    teacherDshExperiment.append(placeholder);
    setMessage(teacherDshMessage, error.message || 'DSH 学习记录加载失败。', true);
  }
}

async function loadTeacherDshRuns(experimentId) {
  const requestEpoch = ++teacherDshRunsRequestEpoch;
  ++teacherDshSubmissionsRequestEpoch;
  teacherDshRuns.replaceChildren();
  teacherDshSubmissions.replaceChildren();
  selectedTeacherDshRunId = null;
  teacherDshRating.disabled = true;
  teacherDshFeedback.disabled = true;
  teacherDshReviewSave.disabled = true;
  teacherDshRating.value = '';
  teacherDshFeedback.value = '';
  teacherDshCode.textContent = '选择一条学生记录查看已提交快照。';
  if (!/^\d+$/.test(String(experimentId))) {
    ++teacherDshBugRequestEpoch;
    teacherDshBugs.replaceChildren();
    setMessage(teacherDshBugMessage, '请选择实验查看 Bug 案例。');
    return;
  }
  void loadTeacherDshBugs(experimentId);
  setMessage(teacherDshMessage, '正在加载学生学习进度…');
  try {
    const runs = await apiRequest(`/teacher/experiments/${encodeURIComponent(experimentId)}/runs`, { method: 'GET' });
    if (requestEpoch !== teacherDshRunsRequestEpoch || teacherDshExperiment.value !== String(experimentId)) return;
    if (!Array.isArray(runs) || !runs.length) {
      setMessage(teacherDshMessage, '该实验还没有学生运行记录。');
      return;
    }
    setMessage(teacherDshMessage, `${runs.length} 条学生运行记录；只展示已 push 并主动登记的 commit。`);
    runs.forEach((run) => {
      const card = document.createElement('article');
      card.className = 'teacher-dsh-run';
      const identity = document.createElement('strong');
      identity.textContent = run.studentName || run.studentEmail || `学生 ${run.studentId}`;
      const progress = document.createElement('small');
      progress.textContent = `${run.completedNodes}/${run.totalNodes} 节点 · ${run.completedTasks}/${run.totalTasks} 任务 · ${run.status}`;
      const button = document.createElement('button');
      button.className = 'secondary-action';
      button.type = 'button';
      button.textContent = '查看 Git 提交';
      button.addEventListener('click', () => void loadTeacherDshSubmissions(run.runId));
      card.append(identity, progress, button);
      teacherDshRuns.append(card);
    });
  } catch (error) {
    if (requestEpoch !== teacherDshRunsRequestEpoch) return;
    setMessage(teacherDshMessage, error.message || '学生学习进度加载失败。', true);
  }
}

let teacherDshBugRequestEpoch = 0;
async function loadTeacherDshBugs(experimentId) {
  const requestEpoch = ++teacherDshBugRequestEpoch;
  teacherDshBugs.replaceChildren();
  setMessage(teacherDshBugMessage, '正在加载实验 Bug 案例…');
  try {
    const bugs = await apiRequest(`/teacher/experiments/${encodeURIComponent(experimentId)}/bugs`, { method: 'GET' });
    if (requestEpoch !== teacherDshBugRequestEpoch || teacherDshExperiment.value !== String(experimentId)) return;
    if (!Array.isArray(bugs) || !bugs.length) {
      setMessage(teacherDshBugMessage, '当前实验还没有学生手动提交的 Bug 案例。');
      return;
    }
    setMessage(teacherDshBugMessage, '审核通过的案例只会进入当前实验的记忆池。');
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
        for (const decision of ['approved', 'rejected']) {
          const button = document.createElement('button');
          button.className = decision === 'approved' ? 'primary-action' : 'secondary-action';
          button.type = 'button';
          button.textContent = decision === 'approved' ? '通过并入记忆池' : '拒绝';
          button.addEventListener('click', async () => {
            button.disabled = true;
            try {
              await apiRequest(`/bugs/${bug.id}/approve`, {
                method: 'POST', body: JSON.stringify({ decision, feedback: feedback.value.trim() }),
              });
              if (teacherDshExperiment.value === String(experimentId)) await loadTeacherDshBugs(experimentId);
            } catch (error) {
              button.disabled = false;
              if (teacherDshExperiment.value === String(experimentId)) {
                setMessage(teacherDshBugMessage, error.message || 'Bug 审核失败，请重试。', true);
              }
            }
          });
          actions.append(button);
        }
        actions.prepend(feedback);
        card.append(actions);
      }
      teacherDshBugs.append(card);
    });
  } catch (error) {
    if (requestEpoch !== teacherDshBugRequestEpoch) return;
    setMessage(teacherDshBugMessage, error.message || '实验 Bug 案例加载失败。', true);
  }
}

async function loadTeacherDshSubmissions(runId) {
  const requestEpoch = ++teacherDshSubmissionsRequestEpoch;
  selectedTeacherDshRunId = Number(runId);
  teacherDshRating.disabled = true;
  teacherDshFeedback.disabled = true;
  teacherDshReviewSave.disabled = true;
  teacherDshRating.value = '';
  teacherDshFeedback.value = '';
  setMessage(teacherDshReviewMessage, '正在读取已有评价…');
  void loadTeacherDshReview(runId, requestEpoch);
  teacherDshSubmissions.replaceChildren();
  teacherDshCode.textContent = '正在加载已登记的 Git commit…';
  try {
    const submissions = await apiRequest(`/teacher/runs/${encodeURIComponent(runId)}/submissions`, { method: 'GET' });
    if (requestEpoch !== teacherDshSubmissionsRequestEpoch) return;
    if (!Array.isArray(submissions) || !submissions.length) {
      teacherDshCode.textContent = '该学生尚未登记 Git commit。';
      return;
    }
    submissions.forEach((submission) => {
      const button = document.createElement('button');
      button.className = 'teacher-dsh-submission';
      button.type = 'button';
      button.textContent = `${String(submission.commitSha || '').slice(0, 12)} · ${gitDecisionLabel(submission.status)} · ${formatTeacherDate(submission.createdAt)}`;
      button.addEventListener('click', () => void loadTeacherDshSubmission(runId, submission.submissionId, requestEpoch));
      teacherDshSubmissions.append(button);
    });
    teacherDshCode.textContent = '选择一个 commit 查看代码差异与逐节点 AI 验收证据。';
  } catch (error) {
    if (requestEpoch !== teacherDshSubmissionsRequestEpoch) return;
    teacherDshCode.textContent = error.message || '代码提交列表读取失败。';
  }
}

async function loadTeacherDshReview(runId, requestEpoch) {
  try {
    const review = await apiRequest(`/teacher/runs/${encodeURIComponent(runId)}/reviews`, { method: 'GET' });
    if (requestEpoch !== teacherDshSubmissionsRequestEpoch || selectedTeacherDshRunId !== Number(runId)) return;
    if (review && typeof review === 'object') {
      teacherDshRating.value = review.rating == null ? '' : String(review.rating);
      teacherDshFeedback.value = review.feedback || '';
    }
    teacherDshRating.disabled = false;
    teacherDshFeedback.disabled = false;
    teacherDshReviewSave.disabled = false;
    setMessage(teacherDshReviewMessage, review ? '可修改已有阶段评价。' : '尚无阶段评价，可填写后保存。');
  } catch (error) {
    if (requestEpoch !== teacherDshSubmissionsRequestEpoch || selectedTeacherDshRunId !== Number(runId)) return;
    setMessage(teacherDshReviewMessage, error.message || '阶段评价读取失败。', true);
  }
}

async function loadTeacherDshSubmission(runId, submissionId, requestEpoch) {
  try {
    const detail = await apiRequest(`/teacher/submissions/${encodeURIComponent(submissionId)}`, { method: 'GET' });
    if (requestEpoch !== teacherDshSubmissionsRequestEpoch) return;
    const sections = [];
    const commit = detail.commit || {};
    sections.push(`COMMIT ${detail.commitSha || commit.commitSha || ''}\n${commit.message || ''}\n作者：${commit.author || '未知'} · ${formatTeacherDate(commit.committedAt || detail.createdAt)}`);
    (Array.isArray(detail.diff) ? detail.diff : []).forEach((file) => {
      const counts = Number(file.additions) < 0 || Number(file.deletions) < 0
        ? '变更统计未计算' : `+${Number(file.additions) || 0}/-${Number(file.deletions) || 0}`;
      sections.push(`--- ${file.path} (${file.status || 'modified'}, ${counts}) ---\n${file.patch || '没有可显示的文本差异。'}`);
    });
    (Array.isArray(detail.results) ? detail.results : []).forEach((result) => {
      sections.push(`节点 ${result.nodeId} · ${gitDecisionLabel(result.decision)}\n${result.summary || ''}`);
      (Array.isArray(result.evidence) ? result.evidence : []).forEach((evidence) => {
        sections.push(`证据：${evidence.path || ''}${evidence.lineStart ? `:${evidence.lineStart}` : ''}\n${evidence.excerpt || ''}`);
      });
      if (Array.isArray(result.suggestions) && result.suggestions.length) sections.push(`建议：${result.suggestions.join('；')}`);
    });
    teacherDshCode.textContent = sections.join('\n\n');
  } catch (error) {
    if (requestEpoch !== teacherDshSubmissionsRequestEpoch) return;
    teacherDshCode.textContent = error.message || '代码快照读取失败。';
  }
}

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

function isCurrentTeacherRunDetail(runId, requestEpoch) {
  return requestEpoch === teacherRunDetailRequestEpoch && selectedTeacherRunId === Number(runId);
}

function renderTeacherRunPanelError(container, error, retryLabel, onRetry) {
  container.classList.add('is-error');
  const message = document.createElement('span');
  message.textContent = error || '加载失败，请重试。';
  const retry = document.createElement('button');
  retry.className = 'text-action';
  retry.type = 'button';
  retry.textContent = retryLabel;
  retry.addEventListener('click', onRetry);
  container.replaceChildren(message, retry);
}

async function loadTeacherRunSummary(runId, studentName, requestEpoch) {
  const panelEpoch = ++teacherRunSummaryRequestEpoch;
  teacherRunSummaryState.classList.remove('is-error');
  teacherRunSummaryState.textContent = '正在加载学习概况…';
  setMessage(teacherReviewMessage, '正在读取已保存评价…');
  try {
    const summary = await apiRequest(`/teacher/runs/${runId}/summary`, { method: 'GET' });
    if (!isCurrentTeacherRunDetail(runId, requestEpoch) || panelEpoch !== teacherRunSummaryRequestEpoch) return;
    const metrics = ['completedNodes', 'totalNodes', 'completedTasks', 'totalTasks', 'commandCount', 'messageCount', 'bugCount'];
    if (!summary || typeof summary !== 'object' || Number(summary.runId) !== Number(runId)
        || metrics.some((key) => !Number.isSafeInteger(summary[key]) || summary[key] < 0)
        || summary.completedNodes > summary.totalNodes || summary.completedTasks > summary.totalTasks
        || typeof summary.runStatus !== 'string' || !summary.runStatus.trim()
        || typeof summary.experimentName !== 'string' || !summary.experimentName.trim()
        || typeof summary.aiSummaryAvailable !== 'boolean'
        || (summary.aiSummaryAvailable && typeof summary.aiSummary !== 'string')
        || (summary.review != null && (typeof summary.review !== 'object' || Array.isArray(summary.review)
          || (summary.review.rating != null && (!Number.isInteger(summary.review.rating)
            || summary.review.rating < 0 || summary.review.rating > 100))
          || (summary.review.feedback != null && typeof summary.review.feedback !== 'string')))) {
      throw new Error('学习概况数据格式无效，请重试。');
    }
    teacherRunStatus.textContent = summary.runStatus || '未知状态';
    teacherRunSummary.textContent = `${summary.experimentName || '实验'} · 节点完成 ${summary.completedNodes}/${summary.totalNodes} · 任务完成 ${summary.completedTasks}/${summary.totalTasks}`;
    teacherRunMetrics.replaceChildren();
    if (summary.aiSummaryAvailable && summary.aiSummary) {
      teacherAiSummary.textContent = `AI 过程摘要：${summary.aiSummary}`;
      teacherAiSummary.hidden = false;
    } else {
      teacherAiSummary.textContent = '';
      teacherAiSummary.hidden = true;
    }
    [[summary.completedNodes, '已完成节点'], [summary.completedTasks, '已完成任务'], [summary.commandCount, '终端命令'], [summary.messageCount, 'AI 对话'], [summary.bugCount, 'Bug 记录']].forEach(([value, label]) => {
      const metric = document.createElement('div');
      metric.className = 'run-metric';
      const number = document.createElement('strong');
      number.textContent = String(value);
      const caption = document.createElement('span');
      caption.textContent = label;
      metric.append(number, caption);
      teacherRunMetrics.append(metric);
    });
    if (summary.review) {
      teacherRating.value = summary.review.rating ?? '';
      teacherFeedback.value = summary.review.feedback || '';
    }
    teacherRating.disabled = false;
    teacherFeedback.disabled = false;
    teacherReviewForm.querySelector('button[type="submit"]').disabled = false;
    setMessage(teacherReviewMessage, '');
    teacherRunSummaryState.replaceChildren();
    teacherRunSummaryState.classList.remove('is-error');
  } catch (error) {
    if (!isCurrentTeacherRunDetail(runId, requestEpoch) || panelEpoch !== teacherRunSummaryRequestEpoch) return;
    teacherRunStatus.textContent = '概况不可用';
    setMessage(teacherReviewMessage,
      '未能读取已保存评价。为避免覆盖，评价暂不可编辑；请重试加载学习概况。', true);
    renderTeacherRunPanelError(teacherRunSummaryState, error.message, '重试概况',
      () => void loadTeacherRunSummary(runId, studentName, requestEpoch));
  }
}

async function loadTeacherRunTimeline(runId, requestEpoch) {
  const panelEpoch = ++teacherRunTimelineRequestEpoch;
  teacherTimelineState.classList.remove('is-error');
  teacherTimelineState.textContent = '正在加载过程记录…';
  try {
    const timeline = await apiRequest(`/teacher/runs/${runId}/timeline`, { method: 'GET' });
    if (!isCurrentTeacherRunDetail(runId, requestEpoch) || panelEpoch !== teacherRunTimelineRequestEpoch) return;
    if (!Array.isArray(timeline) || timeline.some((event) => !event || typeof event !== 'object' || Array.isArray(event)
      || ['source', 'title', 'detail'].some((key) => event[key] != null && typeof event[key] !== 'string')
      || (event.createdAt != null && (typeof event.createdAt !== 'string' || !Number.isFinite(Date.parse(event.createdAt)))))) {
      throw new Error('过程记录数据格式无效，请重试。');
    }
    teacherTimeline.replaceChildren();
    if (!timeline.length) {
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
    teacherTimelineState.replaceChildren();
    teacherTimelineState.classList.remove('is-error');
  } catch (error) {
    if (!isCurrentTeacherRunDetail(runId, requestEpoch) || panelEpoch !== teacherRunTimelineRequestEpoch) return;
    renderTeacherRunPanelError(teacherTimelineState, error.message, '重试过程记录',
      () => void loadTeacherRunTimeline(runId, requestEpoch));
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
  void loadTeacherRunSummary(runId, studentName, requestEpoch);
  void loadTeacherRunTimeline(runId, requestEpoch);
  void loadTeacherStageEvidence(runId);
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
  void loadTeacherDshExperiments();
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
      const savedCourseId = readLocalStorageValue(STORAGE_KEYS.teacherCourse);
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
  if (gitCredentials) {
    gitCredentials.textContent = '凭据只在你点击获取后显示；不要把它提交到代码或截图中。';
    showGitCredentials.textContent = '获取本实验 Git 凭据';
  }
  [libraryView, canvasView, editorView].forEach((view) => view.classList.add('auth-hidden'));
  canvasView.classList.add('is-hidden');
  editorView.classList.remove('is-visible');
  editorView.classList.remove('git-submission-mode');
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
  if (['login', 'student', 'teacher', 'admin', 'library'].includes(path)) return { type: path };
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

function readLocalStorageValue(key) {
  try {
    return window.localStorage.getItem(key);
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

function terminalCommandStorageKey(runId, userId = getCurrentUser()?.id) {
  return `${STORAGE_KEYS.terminalCommandPrefix}${String(userId || '')}:${String(runId)}`;
}

function isValidPendingTerminalCommand(pending, runId, userId = getCurrentUser()?.id) {
  return pending && typeof pending === 'object'
    && String(pending.runId) === String(runId)
    && String(pending.userId) === String(userId)
    && isValidDatabaseId(pending.workspaceId)
    && isValidDatabaseId(pending.sessionId)
    && typeof pending.clientCommandId === 'string'
    && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(pending.clientCommandId);
}

function readPendingTerminalCommand(runId, userId = getCurrentUser()?.id) {
  const serialized = readSessionValue(terminalCommandStorageKey(runId, userId));
  if (!serialized) return null;
  try {
    const pending = JSON.parse(serialized);
    return isValidPendingTerminalCommand(pending, runId, userId) ? pending : null;
  } catch (error) {
    return null;
  }
}

function inspectPendingTerminalCommand(runId, workspaceId, userId = getCurrentUser()?.id) {
  const serialized = readSessionValue(terminalCommandStorageKey(runId, userId));
  if (!serialized) return { status: 'none', command: null };
  try {
    const pending = JSON.parse(serialized);
    if (!isValidPendingTerminalCommand(pending, runId, userId)
        || String(pending.workspaceId) !== String(workspaceId)) {
      return { status: 'invalid', command: null };
    }
    return { status: 'pending', command: pending };
  } catch (error) {
    return { status: 'invalid', command: null };
  }
}

function persistPendingTerminalCommand(pending) {
  const key = terminalCommandStorageKey(pending.runId, pending.userId);
  const serialized = JSON.stringify(pending);
  writeSessionValue(key, serialized);
  if (readSessionValue(key) !== serialized) {
    throw new Error('浏览器无法保留待确认的终端命令，已阻止提交。');
  }
}

async function reconcileTerminalCommand(pending, { runId, epoch, attempts = 4 } = {}) {
  let result = null;
  for (let attempt = 0; attempt < attempts; attempt += 1) {
    if (!isRunContextCurrent(runId, epoch)) return { stale: true, result: null };
    try {
      const candidate = await apiRequest(
        `/terminal-sessions/${encodeURIComponent(pending.sessionId)}/commands/${encodeURIComponent(pending.clientCommandId)}`,
        { method: 'GET', timeoutMs: 10000 },
      );
      if (candidate && ['succeeded', 'failed', 'cancelled', 'queued', 'running'].includes(candidate.status)
          && typeof candidate.output === 'string') result = candidate;
    } catch (error) {
      // A missing row or failed status read never authorizes another command POST.
    }
    if (result && ['succeeded', 'failed', 'cancelled'].includes(result.status)) {
      return { stale: false, result };
    }
    if (attempt + 1 < attempts) {
      await new Promise((resolve) => window.setTimeout(resolve, [750, 1500, 3000][attempt] || 3000));
    }
  }
  return { stale: false, result };
}

async function presentTerminalCommandReconciliation(pending, runId, workspaceId, epoch, container = null) {
  const lockKey = terminalCommandLockKey(runId);
  terminalCommandsInFlight.add(lockKey);
  terminalInput.disabled = true;
  codeEditor.disabled = true;
  saveFileButton.disabled = true;
  renderWorkspaceFileTabs();
  const status = container || document.createElement('div');
  status.className = 'terminal-error';
  status.textContent = '正在查询原命令状态；不会重新提交命令。';
  if (!container) terminalOutput.appendChild(status);
  const reconciliation = await reconcileTerminalCommand(pending, { runId, epoch });
  if (reconciliation.stale || !isRunContextCurrent(runId, epoch)
      || String(activeRun?.workspaceId) !== String(workspaceId)) return null;
  const result = reconciliation.result;
  if (result && ['succeeded', 'failed', 'cancelled'].includes(result.status)) {
    removeSessionValue(terminalCommandStorageKey(runId, pending.userId));
    terminalCommandsInFlight.delete(lockKey);
    const output = document.createElement('div');
    output.className = result.status === 'succeeded' ? 'terminal-result' : 'terminal-error';
    output.textContent = result.output || `原命令已结束，状态：${result.status}`;
    status.replaceWith(output);
    try {
      await refreshWorkspaceFilesAfterTerminal(runId, String(workspaceId), epoch);
    } catch (error) {
      if (isRunContextCurrent(runId, epoch)) {
        appendChatMessage('agent', '系统', error.message || '同步终端结果后的工作区文件失败。');
      }
    }
    if (isRunContextCurrent(runId, epoch)) {
      codeEditor.disabled = !activeFilePath;
      saveFileButton.disabled = !activeFilePath;
      renderWorkspaceFileTabs();
      terminalInput.disabled = !activeTerminalSession;
      terminalOutput.scrollTop = terminalOutput.scrollHeight;
    }
    return true;
  }
  status.replaceChildren();
  const explanation = document.createElement('span');
  explanation.textContent = '执行结果仍无法确认。请检查工作区状态；在确认前不要重新运行这条命令。';
  const retry = document.createElement('button');
  retry.type = 'button';
  retry.className = 'text-action terminal-reconnect-button';
  retry.textContent = '只查询命令状态';
  retry.addEventListener('click', async () => {
    retry.disabled = true;
    await presentTerminalCommandReconciliation(pending, runId, workspaceId, epoch, status);
  });
  status.append(explanation, retry);
  terminalInput.disabled = true;
  codeEditor.disabled = true;
  saveFileButton.disabled = true;
  renderWorkspaceFileTabs();
  return false;
}

function isValidDatabaseId(value) {
  if (typeof value === 'number' && !Number.isSafeInteger(value)) return false;
  const id = String(value);
  return /^[1-9]\d{0,18}$/.test(id)
    && (id.length < 19 || id <= '9223372036854775807');
}

function isValidOpenTerminalSession(session, workspaceId, previousSessionId = null) {
  return session?.status === 'open'
    && isValidDatabaseId(session.id)
    && String(session.workspaceId) === String(workspaceId)
    && (previousSessionId === null || String(session.id) !== String(previousSessionId));
}

function terminalCommandLockKey(runId, userId = getCurrentUser()?.id) {
  return `${String(userId || '')}:${String(runId)}`;
}

function readTerminalSession(run) {
  const key = terminalSessionStorageKey(run.runId);
  const serialized = readSessionValue(key);
  if (!serialized) return null;
  try {
    const session = JSON.parse(serialized);
    if (isValidOpenTerminalSession(session, run.workspaceId)) {
      return session;
    }
  } catch (error) {
    // Ignore stale or malformed session state and request a fresh session.
  }
  removeSessionValue(key);
  return null;
}

function persistTerminalSession(run, session) {
  if (!isValidOpenTerminalSession(session, run.workspaceId)) return;
  writeSessionValue(terminalSessionStorageKey(run.runId), JSON.stringify(session));
}

function terminalSessionRequestStorageKey(run) {
  return `${STORAGE_KEYS.terminalSessionRequestPrefix}${String(getCurrentUser()?.id || '')}:${run.runId}:${run.workspaceId}`;
}

function createTerminalSessionRequestId() {
  if (!window.crypto?.getRandomValues) throw new Error('浏览器无法安全生成终端连接标识，请升级浏览器后重试。');
  const bytes = new Uint8Array(16);
  window.crypto.getRandomValues(bytes);
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = [...bytes].map((value) => value.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

function terminalSessionRequestId(run, storageKey = terminalSessionRequestStorageKey(run)) {
  const saved = readSessionValue(storageKey);
  if (saved && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(saved)) {
    return saved.toLowerCase();
  }
  const requestId = createTerminalSessionRequestId();
  writeSessionValue(storageKey, requestId);
  if (readSessionValue(storageKey) !== requestId) {
    throw new Error('浏览器无法保存终端重连状态，为避免创建重复会话，本次未发送请求。');
  }
  return requestId;
}

async function createTerminalSessionRequest(run, { timeoutMs = 15000 } = {}) {
  const storageKey = terminalSessionRequestStorageKey(run);
  const requestId = terminalSessionRequestId(run, storageKey);
  try {
    const session = await apiRequest(`/workspaces/${run.workspaceId}/terminal-sessions`, {
      method: 'POST', timeoutMs, headers: { 'Idempotency-Key': requestId },
    });
    if (!isValidOpenTerminalSession(session, run.workspaceId)) {
      throw new Error('服务器返回的终端会话信息无效，重试时将继续使用同一请求标识。');
    }
    return session;
  } catch (error) {
    if (isTerminalSessionClosedError(error)) removeSessionValue(storageKey);
    throw error;
  }
}

function acceptTerminalSessionRequest(run, session) {
  persistTerminalSession(run, session);
  if (String(readTerminalSession(run)?.id) !== String(session.id)) {
    throw new Error('终端会话已创建，但浏览器未能保存恢复信息；请重试以安全恢复。');
  }
  removeSessionValue(terminalSessionRequestStorageKey(run));
}

async function recoverClosedTerminalSession(runId, workspaceId, terminalSessionId, epoch) {
  if (!isRunContextCurrent(runId, epoch)
    || String(activeRun?.workspaceId) !== String(workspaceId)
    || String(activeTerminalSession?.id) !== String(terminalSessionId)) return false;
  const replacement = await createTerminalSessionRequest({ runId, workspaceId });
  if (!isRunContextCurrent(runId, epoch)
    || String(activeRun?.workspaceId) !== String(workspaceId)
    || String(activeTerminalSession?.id) !== String(terminalSessionId)) return false;
  if (!isValidOpenTerminalSession(replacement, workspaceId, terminalSessionId)) {
    throw new Error('服务器返回的终端会话信息无效，未替换当前会话。');
  }
  activeTerminalSession = replacement;
  acceptTerminalSessionRequest(activeRun, replacement);
  return true;
}

function isTerminalSessionClosedError(error) {
  return error?.status === 409 && error.code === 'TERMINAL_SESSION_CLOSED';
}

function invalidateClosedTerminalSession(runId, workspaceId, terminalSessionId, epoch) {
  if (!isRunContextCurrent(runId, epoch)
    || String(activeRun?.workspaceId) !== String(workspaceId)
    || String(activeTerminalSession?.id) !== String(terminalSessionId)) return false;
  removeSessionValue(terminalSessionStorageKey(runId));
  activeTerminalSession = null;
  return true;
}

function createTerminalReconnectButton(runId, workspaceId, closedSessionId, epoch, messageElement) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'text-action terminal-reconnect-button';
  button.textContent = '重新连接终端';
  button.addEventListener('click', async () => {
    if (!isRunContextCurrent(runId, epoch) || String(activeRun?.workspaceId) !== String(workspaceId)) return;
    const finishIfConnected = () => {
      if (!isValidOpenTerminalSession(activeTerminalSession, workspaceId)) return false;
      terminalInput.disabled = false;
      terminalInput.placeholder = '输入受限终端命令…';
      messageElement.textContent = '终端已重新连接。刚才的命令未执行，请核对后手动重试。';
      button.remove();
      return true;
    };
    if (finishIfConnected()) return;
    if (activeTerminalSession) {
      invalidateClosedTerminalSession(runId, workspaceId, String(activeTerminalSession.id), epoch);
    }
    button.disabled = true;
    button.textContent = '正在重新连接…';
    try {
      const replacement = await createTerminalSessionRequest(activeRun);
      if (!isRunContextCurrent(runId, epoch) || String(activeRun?.workspaceId) !== String(workspaceId)) return;
      if (finishIfConnected()) return;
      if (activeTerminalSession) {
        invalidateClosedTerminalSession(runId, workspaceId, String(activeTerminalSession.id), epoch);
      }
      if (!isValidOpenTerminalSession(replacement, workspaceId, closedSessionId)) {
        throw new Error('服务器返回的终端会话信息无效。');
      }
      activeTerminalSession = replacement;
      acceptTerminalSessionRequest(activeRun, replacement);
      terminalInput.disabled = false;
      terminalInput.placeholder = '输入受限终端命令…';
      messageElement.textContent = '终端已重新连接。刚才的命令未执行，请核对后手动重试。';
      button.remove();
    } catch (error) {
      if (!isRunContextCurrent(runId, epoch) || String(activeRun?.workspaceId) !== String(workspaceId)) return;
      if (finishIfConnected()) return;
      if (activeTerminalSession) {
        invalidateClosedTerminalSession(runId, workspaceId, String(activeTerminalSession.id), epoch);
      }
      button.disabled = false;
      button.textContent = '重试连接终端';
      messageElement.textContent = `终端重连失败：${error.message || '请稍后重试。'}`;
    }
  });
  return button;
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
  if (route !== 'student' || user?.role !== 'student') clearStudentDashboard();
  loginView.classList.remove('is-visible');
  studentView.classList.remove('is-visible');
  teacherView.classList.remove('is-visible');
  adminView.classList.remove('is-visible');
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
  if (route === 'admin' && user.role === 'admin') {
    adminView.classList.add('is-visible');
    renderAdminDashboard();
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
  const fallback = role === 'teacher' ? 'teacher' : role === 'admin' ? 'admin' : role === 'student' ? 'student' : 'login';
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
    : route.type === 'admin' ? 'admin'
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
    inspector.querySelector('.node-fact:nth-child(3) dd').textContent = '在本地 IDE 中创建和编辑项目';
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
  const canUpdate = Boolean(activeRun && nodeProgress && nodeProgress.status !== 'completed');
  let completedCount = 0;
  taskList.replaceChildren();
  if (tasks.length === 0) {
    const emptyState = document.createElement('p');
    emptyState.className = 'task-empty-state';
    emptyState.textContent = '本节点没有细分任务。节点顺序仅供参考，不会限制学习。';
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
  completeNodeButton.disabled = !activeRun || !current
    || current.status === 'completed';
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
  startNodeButton.disabled = false;
  setStartNodeLabel('查看节点并提交代码');
  setStartNodeStatus('');
  writeSessionValue(STORAGE_KEYS.activeExperiment, activeExperimentId);
  activeRun = null;
  activeGitRepository = null;
  syncGuidanceControls();
  activeRunProgress = null;
  runProgressLoadFailed = false;
  activeFilePath = null;
  activeFileHash = null;
  activeFileSavedContent = '';
  activeWorkspaceFiles = [];
  workspaceFileSelectionEpoch += 1;
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
  editorView.classList.remove('git-submission-mode');
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

function setStartNodeLabel(label) {
  const labelElement = startNodeButton.querySelector('span:last-child');
  labelElement.textContent = label;
  startNodeButton.setAttribute('aria-label', label);
}

function setStartNodeStatus(message, state = 'error') {
  startNodeStatus.textContent = message;
  startNodeStatus.hidden = !message;
  startNodeStatus.classList.toggle('is-error', state === 'error' && Boolean(message));
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
  if (run.workspaceType === 'desktop') {
    const local = await window.BMDesktop.workspace.create(String(run.runId), run.localWorkspaceId);
    if (String(local.workspaceId) !== String(run.localWorkspaceId)) throw new Error('本地工作区绑定不匹配。');
  }
  const files = run.workspaceType === 'desktop'
    ? await listDesktopWorkspaceFiles(run)
    : await apiRequest(`/workspaces/${run.workspaceId}/files`, { method: 'GET' });
  if (!isCurrentRoute()) return false;
  if (!Array.isArray(files)) throw new Error('工作区文件列表格式无效。');
  let nextFilePath = null;
  let nextFileHash = null;
  let nextFileContent = '';
  if (files.length) {
    nextFilePath = files[0].path;
    const response = run.workspaceType === 'desktop'
      ? await readDesktopWorkspaceFile(run, nextFilePath)
      : await apiRequest(`/workspaces/${run.workspaceId}/files/${workspaceFileUrl(nextFilePath)}`, { method: 'GET' });
    if (!isCurrentRoute()) return false;
    const file = await verifyWorkspaceFileResponse(response, run.workspaceId, files[0]);
    if (!isCurrentRoute()) return false;
    nextFileContent = file.content;
    nextFileHash = file.file.contentHash || null;
  }
  let nextTerminalSession = run.workspaceType === 'desktop' ? null : activeTerminalSession;
  const needsTerminalSession = run.workspaceType !== 'desktop' && (createTerminalSession
    || activeTerminalRunId !== String(run.runId)
    || !activeTerminalSession);
  if (createTerminalSession) nextTerminalSession = null;
  if (needsTerminalSession && !createTerminalSession) {
    nextTerminalSession = readTerminalSession(run);
  }
  if (needsTerminalSession && !nextTerminalSession) {
    nextTerminalSession = await createTerminalSessionRequest(run);
    if (!isCurrentRoute()) return false;
    acceptTerminalSessionRequest(run, nextTerminalSession);
  }
  activeRun = run;
  syncGuidanceControls();
  activeRunProgress = null;
  runProgressLoadFailed = false;
  activeExperimentId = String(run.experimentId);
  activeNodeId = run.currentNodeId || selectedTreeNode?.nodeId || null;
  activeFilePath = nextFilePath;
  workspaceFileSelectionEpoch += 1;
  activeFileHash = nextFileHash;
  activeFileSavedContent = nextFileContent;
  activeWorkspaceFiles = files;
  codeEditor.value = nextFileContent;
  renderCodeLineNumbers();
  activeTerminalSession = nextTerminalSession;
  activeTerminalRunId = String(run.runId);
  const pendingCommandState = run.workspaceType === 'desktop'
    ? { status: 'none', command: null }
    : inspectPendingTerminalCommand(run.runId, run.workspaceId);
  const pendingTerminalCommand = pendingCommandState.command;
  const terminalCommandBlocked = pendingCommandState.status === 'invalid';
  if (pendingTerminalCommand || terminalCommandBlocked) {
    terminalCommandsInFlight.add(terminalCommandLockKey(run.runId));
  }
  codeEditor.disabled = !nextFilePath || Boolean(pendingTerminalCommand) || terminalCommandBlocked;
  saveFileButton.disabled = !nextFilePath || Boolean(pendingTerminalCommand) || terminalCommandBlocked;
  terminalInput.disabled = run.workspaceType !== 'desktop' && (Boolean(pendingTerminalCommand) || terminalCommandBlocked);
  renderWorkspaceFileTabs();
  terminalInput.placeholder = '输入受限终端命令…';
  terminalOutput.replaceChildren();
  const terminalReady = document.createElement('div');
  terminalReady.className = 'terminal-muted';
  terminalReady.textContent = run.workspaceType === 'desktop'
    ? '本地终端已连接。命令会在此实验目录中运行。'
    : '终端已连接。命令会在隔离工作区内执行。';
  terminalOutput.append(terminalReady);
  if (pendingTerminalCommand) {
    void presentTerminalCommandReconciliation(pendingTerminalCommand, String(run.runId),
      String(run.workspaceId), routeResolutionEpoch);
  } else if (terminalCommandBlocked) {
    const warning = document.createElement('div');
    warning.className = 'terminal-error';
    warning.textContent = '待确认命令标识无法读取。为防止重复执行，终端和文件编辑已锁定；请先核对工作区状态并联系教师处理。';
    terminalOutput.appendChild(warning);
  }
  createSnapshotButton.disabled = run.workspaceType === 'desktop';
  desktopNewFolderButton.hidden = run.workspaceType !== 'desktop';
  desktopNewFileButton.hidden = run.workspaceType !== 'desktop';
  recordBugButton.disabled = false;
  deepDiagnosisButton.disabled = run.workspaceType === 'desktop';
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
    button.disabled = terminalCommandsInFlight.has(terminalCommandLockKey(activeTerminalRunId));
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
    const isDesktop = activeRun?.workspaceType === 'desktop';
    const files = isDesktop
      ? await listDesktopWorkspaceFiles(activeRun)
      : await apiRequest(`/workspaces/${encodeURIComponent(workspaceId)}/files`, { method: 'GET' });
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
    const response = isDesktop
      ? await readDesktopWorkspaceFile(activeRun, selected.path)
      : await apiRequest(`/workspaces/${encodeURIComponent(workspaceId)}/files/${workspaceFileUrl(selected.path)}`, { method: 'GET' });
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
  if (!['git', 'git_submission'].includes(String(run.executionMode || run.mode || '').toLowerCase())) {
    throw new Error('这是旧工作区记录，已停止在线编辑；请从实验画布启动新的本地 Git 提交流程。');
  }
  const experiment = await apiRequest(`/experiments/${encodeURIComponent(run.experimentId)}`, { method: 'GET', timeoutMs: 15000 });
  if (epoch !== routeResolutionEpoch) return;
  if (String(experiment.experimentId) !== String(run.experimentId)) throw new Error('运行实例关联的实验不存在或无权访问。');
  experimentData[String(run.experimentId)] = experiment;
  renderExperimentCanvas(String(run.experimentId));
  activeRun = run;
  activeExperimentId = String(run.experimentId);
  activeNodeId = run.currentNodeId || null;
  workspaceStatus.textContent = '本地 Git 提交模式';
  const nodeProgress = await apiRequest(`/runs/${encodeURIComponent(runId)}/progress`, { method: 'GET' });
  if (epoch !== routeResolutionEpoch) return;
  activeRunProgress = nodeProgress;
  runProgressLoadFailed = false;
  if (activeNodeId) {
    const selected = activeTreeNodes.find((node) => Number(node.nodeId) === Number(activeNodeId));
    const element = selected && canvasView.querySelector(`[data-tree-node-key="${CSS.escape(selected.key)}"]`);
    if (element) selectTreeNode(element);
  }
  await loadGitRepository(String(runId), epoch);
  await loadGitSubmissions(String(runId), epoch);
  if (epoch !== routeResolutionEpoch) return;
  canvasView.classList.add('is-hidden');
  gitSubmissionExperimentName.textContent = experiment.name;
  editorView.classList.add('git-submission-mode');
  editorView.classList.add('is-visible');
}

async function startGitExperimentRun() {
  if (getCurrentUser()?.role !== 'student') {
    setStartNodeStatus('请以学生账号登录后开始实验。', 'error');
    return;
  }
  if (!/^\d+$/.test(String(activeExperimentId || ''))) {
    setStartNodeStatus('当前实验编号无效，无法准备代码仓库。', 'error');
    return;
  }
  const experimentId = String(activeExperimentId);
  const epoch = routeResolutionEpoch;
  startNodeButton.disabled = true;
  setStartNodeStatus('正在准备你的独立 Git 仓库…', 'loading');
  try {
    const run = await apiRequest(`/student/experiments/${encodeURIComponent(experimentId)}/runs`, {
      method: 'POST', timeoutMs: 30000,
      body: JSON.stringify({ currentNodeId: selectedTreeNode?.nodeId || null }),
    });
    const runId = run.runId ?? run.id;
    if (!/^\d+$/.test(String(runId || ''))) throw new Error('服务端未返回有效的实验运行编号。');
    if (epoch !== routeResolutionEpoch || activeExperimentId !== experimentId) return;
    navigateTo({ type: 'editor', runId: String(runId) });
  } catch (error) {
    if (epoch === routeResolutionEpoch && activeExperimentId === experimentId) {
      setStartNodeStatus(error.message || 'Git 仓库准备失败，请稍后重试。', 'error');
    }
  } finally {
    if (epoch === routeResolutionEpoch && activeExperimentId === experimentId) startNodeButton.disabled = false;
  }
}

function renderGitReviewNodes() {
  if (!gitReviewNodeList) return;
  gitReviewNodeList.replaceChildren();
  const sequenceNumbers = buildTreeNodeNumbers(activeTreeNodes);
  activeTreeNodes.filter((node) => Number.isSafeInteger(Number(node.nodeId)) && Number(node.nodeId) > 0)
    .forEach((node) => {
      const label = document.createElement('label');
      label.className = 'git-review-node';
      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.name = 'nodeIds';
      checkbox.value = String(node.nodeId);
      checkbox.checked = Number(node.nodeId) === Number(activeNodeId);
      const text = document.createElement('span');
      const title = document.createElement('strong');
      title.textContent = `${sequenceNumbers.get(node.key) || node.key} · ${node.title}`;
      const description = document.createElement('small');
      const tasks = Array.isArray(node.tasks) ? node.tasks : [];
      description.textContent = tasks.map((task) => task.title).filter(Boolean).slice(0, 3).join('；')
        || node.description || '按实验目标提交代码，由 AI 检查该节点。';
      text.append(title, description);
      label.append(checkbox, text);
      checkbox.addEventListener('change', updateGitSubmitButton);
      const row = document.createElement('div');
      row.className = 'git-review-node-row';
      row.append(label);
      const progress = activeRunProgress?.nodes?.find((item) => Number(item.nodeId) === Number(node.nodeId));
      const completeButton = document.createElement('button');
      completeButton.className = 'git-node-complete';
      completeButton.type = 'button';
      completeButton.textContent = progress?.status === 'completed' ? '已完成' : '我已完成';
      completeButton.disabled = !activeRun || progress?.status === 'completed';
      completeButton.setAttribute('aria-label', `${title.textContent}：${completeButton.textContent}`);
      completeButton.addEventListener('click', () => void markGitNodeCompleted(node, completeButton));
      row.append(completeButton);
      gitReviewNodeList.append(row);
    });
  updateGitSubmitButton();
}

function gitDecisionLabel(decision) {
  return ({ meets: '符合', partially_meets: '部分符合', does_not_meet: '不符合', unable_to_verify: '无法判断' })[decision]
    || ({ queued: '排队中', running: '检查中', completed: '已完成', failed: '检查失败' })[decision]
    || '处理中';
}

async function markGitNodeCompleted(node, button) {
  if (!activeRun || !node?.nodeId) return;
  const runId = String(activeRun.runId);
  const epoch = routeResolutionEpoch;
  button.disabled = true;
  try {
    const progress = await apiRequest(`/runs/${encodeURIComponent(runId)}/nodes/${encodeURIComponent(node.nodeId)}`, {
      method: 'PATCH',
      body: JSON.stringify({ status: 'completed', progressPercent: 100 }),
    });
    if (!isRunContextCurrent(runId, epoch)) return;
    activeRunProgress = progress;
    button.textContent = '已完成';
    button.setAttribute('aria-label', `${node.title || '节点'}：已完成`);
    setMessage(gitSubmissionMessage, '已记录你的自我确认；这不代表 AI 验收或教师评分。');
    if (selectedTreeNode) renderSelectedNodeTasks();
  } catch (error) {
    if (!isRunContextCurrent(runId, epoch)) return;
    button.disabled = false;
    setMessage(gitSubmissionMessage, error.message || '节点进度保存失败，请稍后重试。', true);
  }
}

function updateGitSubmitButton() {
  const selectedCount = gitReviewNodeList?.querySelectorAll('input[name="nodeIds"]:checked').length || 0;
  submitGitCommit.disabled = !activeRun || selectedCount === 0 || !gitCommitSha.value.trim();
}

async function loadGitRepository(runId, epoch = routeResolutionEpoch) {
  gitRepositoryState.textContent = '正在读取仓库信息…';
  gitCloneCommand.textContent = '正在加载仓库地址…';
  gitCopyCloneCommand.disabled = true;
  showGitCredentials.disabled = true;
  gitCredentials.textContent = '凭据只在你点击获取后显示；不要把它提交到代码或截图中。';
  try {
    const repository = await apiRequest(`/student/runs/${encodeURIComponent(runId)}/repository`, { method: 'GET' });
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== String(runId)) return;
    const cloneUrl = String(repository.cloneUrl || repository.repoUrl || '').trim();
    if (!cloneUrl) throw new Error('仓库服务尚未返回可用的克隆地址。');
    let parsed;
    try { parsed = new URL(cloneUrl); } catch { throw new Error('仓库克隆地址格式无效。'); }
    if (parsed.protocol !== 'https:' || parsed.username || parsed.password) {
      throw new Error('仓库地址包含不安全协议或嵌入式凭据，已阻止显示。');
    }
    activeGitRepository = { ...repository, cloneUrl };
    gitCloneCommand.textContent = `git clone ${cloneUrl}`;
    gitRepositoryState.textContent = '仓库已准备';
    gitRepositoryHelp.textContent = repository.setupInstructions || repository.instructions || '克隆仓库到本地，在 VS Code 中完成实验，然后手动 push。';
    gitCredentialNotice.textContent = repository.credentialNotice || '需要认证时，点击“获取本实验 Git 凭据”；不要把凭据写入代码仓库。';
    gitCopyCloneCommand.disabled = false;
    showGitCredentials.disabled = false;
    renderGitReviewNodes();
  } catch (error) {
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== String(runId)) return;
    activeGitRepository = null;
    gitRepositoryState.textContent = '仓库暂不可用';
    gitCloneCommand.textContent = error.message || '仓库地址读取失败。';
    gitCredentialNotice.textContent = '可以重试读取；不要通过不受信任的外部链接上传源码。';
    setMessage(gitSubmissionMessage, error.message || 'Git 仓库加载失败。', true);
  }
}

async function revealGitCredentials() {
  if (!activeRun || !activeGitRepository) return;
  showGitCredentials.disabled = true;
  gitCredentials.textContent = '正在获取本实验专属凭据…';
  try {
    const credentials = await apiRequest(`/student/runs/${encodeURIComponent(activeRun.runId)}/repository-credentials`, {
      method: 'POST', timeoutMs: 15000,
    });
    if (!credentials?.username || !credentials?.password) throw new Error('服务端未返回有效 Git 凭据。');
    gitCredentials.textContent = `用户名：${credentials.username}\nGit 凭据：${credentials.password}\n只用于本实验仓库 push。建议通过 Git Credential Manager 保存；离开页面前清除剪贴板。`;
    showGitCredentials.textContent = '重新获取凭据';
  } catch (error) {
    gitCredentials.textContent = error.message || '获取凭据失败，请重试。';
  } finally {
    showGitCredentials.disabled = false;
  }
}

function renderGitSubmissionList(submissions) {
  gitSubmissionHistory.replaceChildren();
  if (!Array.isArray(submissions) || submissions.length === 0) {
    const empty = document.createElement('p');
    empty.className = 'library-state';
    empty.textContent = '还没有提交记录。完成本地 commit 并 push 后，在上方填写 commit SHA 开始检查。';
    gitSubmissionHistory.append(empty);
    return;
  }
  submissions.forEach((submission) => {
    const card = document.createElement('article');
    card.className = 'git-submission-item';
    const heading = document.createElement('header');
    const sha = document.createElement('code');
    sha.textContent = `${String(submission.commitSha || '').slice(0, 12)} · ${gitDecisionLabel(submission.status)}`;
    const date = document.createElement('small');
    date.textContent = submission.createdAt ? formatTeacherDate(submission.createdAt) : '';
    heading.append(sha, date);
    card.append(heading);
    if (submission.description) {
      const description = document.createElement('p');
      description.textContent = submission.description;
      card.append(description);
    }
    if (submission.status === 'failed') {
      const retry = document.createElement('button');
      retry.type = 'button';
      retry.className = 'secondary-action';
      retry.textContent = '重新尝试验收';
      retry.addEventListener('click', () => {
        gitCommitSha.value = String(submission.commitSha || '');
        const selected = new Set((Array.isArray(submission.nodeIds) ? submission.nodeIds : []).map(String));
        gitReviewNodeList.querySelectorAll('input[name="nodeIds"]').forEach((input) => {
          input.checked = selected.has(input.value);
        });
        updateGitSubmitButton();
        gitCommitSha.scrollIntoView({ behavior: 'smooth', block: 'center' });
        setMessage(gitSubmissionMessage, '已恢复失败提交的 SHA 和节点选择；确认后可再次提交验收。');
      });
      card.append(retry);
    }
    (Array.isArray(submission.results) ? submission.results : []).forEach((result) => {
      const report = document.createElement('section');
      report.className = 'git-review-result';
      report.dataset.decision = result.decision || result.outcome || 'unknown';
      const title = document.createElement('h4');
      const node = activeTreeNodes.find((item) => Number(item.nodeId) === Number(result.nodeId));
      const decision = result.decision || result.outcome;
      title.textContent = `${node?.title || `节点 ${result.nodeId}`} · ${gitDecisionLabel(decision)}`;
      const summary = document.createElement('p');
      summary.textContent = result.summary || result.feedback || '验收正在进行或暂无说明。';
      report.append(title, summary);
      (Array.isArray(result.evidence) ? result.evidence : []).forEach((evidence) => {
        const block = document.createElement('pre');
        block.className = 'git-review-evidence';
        block.textContent = `${evidence.path || '代码证据'}${evidence.lineStart ? `:${evidence.lineStart}` : ''}\n${evidence.excerpt || evidence.code || ''}`;
        report.append(block);
      });
      if (Array.isArray(result.suggestions) && result.suggestions.length) {
        const suggestions = document.createElement('p');
        suggestions.textContent = `建议：${result.suggestions.join('；')}`;
        report.append(suggestions);
      }
      if (result.bugDraft && typeof result.bugDraft === 'object') {
        const bugButton = document.createElement('button');
        bugButton.type = 'button';
        bugButton.className = 'secondary-action';
        bugButton.textContent = '检查并提交为 Bug 案例';
      bugButton.addEventListener('click', () => {
          activeNodeId = Number(result.nodeId) || activeNodeId;
        currentGitBugDraftContext = { submissionId: Number(submission.submissionId), nodeId: Number(result.nodeId) };
          bugTitle.value = result.bugDraft.title || '';
          bugProblem.value = result.bugDraft.problem || result.summary || '';
          bugSolution.value = result.bugDraft.solution || '';
          if (result.bugDraft.technologyStack) bugTechnologyStack.value = result.bugDraft.technologyStack;
          openBugCaseDialog();
        });
        report.append(bugButton);
      }
      card.append(report);
    });
    gitSubmissionHistory.append(card);
  });
}

async function loadGitSubmissions(runId = activeRun?.runId, epoch = routeResolutionEpoch) {
  if (!runId) return;
  try {
    const submissions = await apiRequest(`/student/runs/${encodeURIComponent(runId)}/submissions`, { method: 'GET' });
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== String(runId)) return;
    renderGitSubmissionList(submissions);
  } catch (error) {
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== String(runId)) return;
    gitSubmissionHistory.replaceChildren();
    const message = document.createElement('p');
    message.className = 'library-state is-error';
    message.textContent = error.message || '提交历史加载失败。';
    gitSubmissionHistory.append(message);
  }
}

async function submitGitCommitForReview(event) {
  event.preventDefault();
  if (!activeRun || !activeGitRepository) return;
  const runId = String(activeRun.runId);
  const epoch = routeResolutionEpoch;
  const nodeIds = Array.from(gitReviewNodeList.querySelectorAll('input[name="nodeIds"]:checked'))
    .map((input) => Number(input.value)).filter((id) => Number.isSafeInteger(id) && id > 0);
  const commitSha = gitCommitSha.value.trim();
  if (!nodeIds.length) {
    setMessage(gitSubmissionMessage, '至少选择一个要检查的节点。', true);
    return;
  }
  if (!/^[0-9a-f]{40,64}$/i.test(commitSha)) {
    setMessage(gitSubmissionMessage, '请输入完整的 40–64 位 Git commit SHA。', true);
    return;
  }
  submitGitCommit.disabled = true;
  setMessage(gitSubmissionMessage, '正在核对远程 commit 并创建节点验收任务…');
  try {
    await apiRequest(`/student/runs/${encodeURIComponent(runId)}/submissions`, {
      method: 'POST', timeoutMs: 30000,
      body: JSON.stringify({ commitSha, nodeIds, description: gitSubmissionDescription.value.trim() || null }),
    });
    if (epoch !== routeResolutionEpoch || String(activeRun?.runId) !== runId) return;
    gitCommitSha.value = '';
    gitSubmissionDescription.value = '';
    setMessage(gitSubmissionMessage, '提交已登记。AI 正在按所选节点检查这个 commit；你可以继续编码或提交新版本。');
    await loadGitSubmissions(runId, epoch);
  } catch (error) {
    if (epoch === routeResolutionEpoch && String(activeRun?.runId) === runId) {
      setMessage(gitSubmissionMessage, error.message || '提交登记失败，请确认已 push 该 commit 后重试。', true);
    }
  } finally {
    if (epoch === routeResolutionEpoch && String(activeRun?.runId) === runId) updateGitSubmitButton();
  }
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
  currentGitBugDraftContext = null;
  setMessage(bugMessage, '');
}

function openBugCaseDialog() {
  if (!activeRun) {
    setMessage(gitSubmissionMessage, '请先打开实验，再记录 Bug 案例。', true);
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
  setMessage(bugMessage, '正在提交个人 Bug 案例，等待教师审核…');
  try {
    const endpoint = currentGitBugDraftContext
      ? `/student/submissions/${encodeURIComponent(currentGitBugDraftContext.submissionId)}/bug-draft/${encodeURIComponent(currentGitBugDraftContext.nodeId)}/confirm`
      : `/runs/${activeRun.runId}/bugs`;
    await apiRequest(endpoint, {
      method: 'POST',
      body: JSON.stringify({ title, problem, solution: bugSolution.value.trim() || null,
        technologyStack: bugTechnologyStack.value || null, nodeId: activeNodeId }),
      timeoutMs: 15000,
    });
    closeBugCaseDialog();
    if (!editorView.classList.contains('git-submission-mode')) {
      appendChatMessage('agent', '系统', 'Bug 案例已保存为个人记录，等待教师审核。');
    } else {
      setMessage(gitSubmissionMessage, 'Bug 案例已提交为个人记录，教师审核通过后才会进入本实验记忆池。');
    }
  } catch (error) {
    setMessage(bugMessage, error.message || '案例保存失败，请稍后重试。', true);
  } finally {
    saveBugButton.disabled = false;
  }
}

function workspaceFileUrl(path) {
  return path.split('/').map((part) => encodeURIComponent(part)).join('/');
}

function sha256HexFallback(bytes) {
  const constants = new Uint32Array([
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
  ]);
  const hash = new Uint32Array([
    0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
    0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19,
  ]);
  const paddedLength = Math.ceil((bytes.length + 9) / 64) * 64;
  const padded = new Uint8Array(paddedLength);
  padded.set(bytes);
  padded[bytes.length] = 0x80;
  const bitLength = bytes.length * 8;
  const view = new DataView(padded.buffer);
  view.setUint32(paddedLength - 8, Math.floor(bitLength / 0x100000000), false);
  view.setUint32(paddedLength - 4, bitLength >>> 0, false);
  const words = new Uint32Array(64);
  const rotateRight = (value, amount) => (value >>> amount) | (value << (32 - amount));

  for (let offset = 0; offset < paddedLength; offset += 64) {
    for (let index = 0; index < 16; index += 1) {
      words[index] = view.getUint32(offset + index * 4, false);
    }
    for (let index = 16; index < 64; index += 1) {
      const x = words[index - 15];
      const y = words[index - 2];
      const sigma0 = rotateRight(x, 7) ^ rotateRight(x, 18) ^ (x >>> 3);
      const sigma1 = rotateRight(y, 17) ^ rotateRight(y, 19) ^ (y >>> 10);
      words[index] = (words[index - 16] + sigma0 + words[index - 7] + sigma1) >>> 0;
    }

    let [a, b, c, d, e, f, g, h] = hash;
    for (let index = 0; index < 64; index += 1) {
      const sum1 = rotateRight(e, 6) ^ rotateRight(e, 11) ^ rotateRight(e, 25);
      const choice = (e & f) ^ (~e & g);
      const temp1 = (h + sum1 + choice + constants[index] + words[index]) >>> 0;
      const sum0 = rotateRight(a, 2) ^ rotateRight(a, 13) ^ rotateRight(a, 22);
      const majority = (a & b) ^ (a & c) ^ (b & c);
      const temp2 = (sum0 + majority) >>> 0;
      [a, b, c, d, e, f, g, h] = [
        (temp1 + temp2) >>> 0, a, b, c, (d + temp1) >>> 0, e, f, g,
      ];
    }
    hash[0] = (hash[0] + a) >>> 0;
    hash[1] = (hash[1] + b) >>> 0;
    hash[2] = (hash[2] + c) >>> 0;
    hash[3] = (hash[3] + d) >>> 0;
    hash[4] = (hash[4] + e) >>> 0;
    hash[5] = (hash[5] + f) >>> 0;
    hash[6] = (hash[6] + g) >>> 0;
    hash[7] = (hash[7] + h) >>> 0;
  }
  return Array.from(hash, (word) => word.toString(16).padStart(8, '0')).join('');
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
    if (!/^[a-f0-9]{64}$/.test(expectedHash) || !window.TextEncoder) {
      throw new Error('当前浏览器无法安全校验文件内容，请使用受支持的安全浏览器重试。');
    }
    const encodedContent = new window.TextEncoder().encode(response.content);
    const actualHash = window.crypto?.subtle
      ? Array.from(new Uint8Array(await window.crypto.subtle.digest('SHA-256', encodedContent)),
        (byte) => byte.toString(16).padStart(2, '0')).join('')
      : sha256HexFallback(encodedContent);
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

function renderCreationDraft(draft) {
  const row = document.createElement('article');
  row.className = 'creation-draft-item';
  const details = document.createElement('div');
  details.className = 'creation-draft-copy';
  const title = document.createElement('strong');
  title.textContent = draft.title || '未完成的实验规划';
  const status = document.createElement('small');
  const statusLabels = {
    direction_discussion: '方向讨论中', direction_ready: '等待确认方向', direction_confirmed: '方向已确认',
    configured: '等待生成方案', planning: '正在生成方案', plan_ready: '方案待确认', failed: '上次生成未完成',
  };
  const updatedAt = draft.updatedAt ? new Date(draft.updatedAt) : null;
  const updatedLabel = updatedAt && !Number.isNaN(updatedAt.getTime())
    ? ` · 更新于 ${updatedAt.toLocaleString()}` : '';
  status.textContent = `${statusLabels[draft.status] || '创建中'}${updatedLabel}`;
  details.append(title, status);
  const resume = document.createElement('button');
  resume.className = 'creation-draft-resume';
  resume.type = 'button';
  resume.dataset.sessionId = draft.sessionId;
  resume.textContent = '继续';
  resume.addEventListener('click', () => void resumeCreationDraft(draft.sessionId));
  row.append(details, resume);
  return row;
}

async function loadCreationDrafts(routeEpoch = routeResolutionEpoch) {
  if (!creationDraftList) return;
  creationDraftList.replaceChildren();
  const loading = document.createElement('p');
  loading.className = 'library-state';
  loading.textContent = '正在读取未完成草稿…';
  creationDraftList.append(loading);
  try {
    const drafts = await apiRequest('/experiment-creation/sessions/drafts', { method: 'GET', timeoutMs: 15000 });
    if (routeEpoch !== routeResolutionEpoch || !Array.isArray(drafts)) return;
    creationDraftList.replaceChildren();
    if (!drafts.length) {
      const empty = document.createElement('p');
      empty.className = 'library-state';
      empty.textContent = '目前没有未完成的实验规划。';
      creationDraftList.append(empty);
      return;
    }
    drafts.forEach((draft) => {
      if (draft?.sessionId) creationDraftList.append(renderCreationDraft(draft));
    });
  } catch (error) {
    if (routeEpoch !== routeResolutionEpoch) return;
    creationDraftList.replaceChildren();
    const state = document.createElement('p');
    state.className = 'library-state is-error';
    state.textContent = error.message || '未完成草稿暂时无法读取。';
    creationDraftList.append(state);
    const retry = document.createElement('button');
    retry.className = 'library-retry-button';
    retry.type = 'button';
    retry.textContent = '重试读取';
    retry.addEventListener('click', () => void loadCreationDrafts(routeEpoch));
    creationDraftList.append(retry);
  }
}

function formatDifficulty(value) {
  return ({ beginner: '入门', easy: '简单', normal: '普通', hard: '困难', challenge: '挑战' })[
    String(value || '').toLowerCase()
  ] || '入门';
}

function formatDurationMinutes(minutes) {
  const hours = Math.max(1, Math.round(Number(minutes || 1440) / 60));
  durationHoursInput.value = hours;
  updateDurationPreview();
}

async function saveCreationConfiguration() {
  updateDurationPreview();
  if (!Number.isInteger(durationTotalHours) || durationTotalHours <= 0) {
    throw new Error('学习周期必须是大于 0 的整数小时数。');
  }
  return createApiRequest(`/experiment-creation/sessions/${encodeURIComponent(creationSessionId)}/configuration`, {
    method: 'PATCH',
    body: JSON.stringify({
      durationDays: Math.floor(durationTotalHours / 24),
      durationHours: durationTotalHours % 24,
      difficulty: difficultyCode(selectedDifficulty),
    }),
  });
}

async function resumeCreationDraft(sessionId) {
  if (createBusy || createMaterializationInFlight) return;
  const ownerId = getCurrentUser()?.id;
  const epoch = ++createDialogEpoch;
  const controller = beginCreateRequest();
  createBusy = true;
  createDialog.classList.add('is-open');
  createPlan.classList.remove('is-active');
  createPlanReady = false;
  pendingPlan = null;
  creationSessionId = String(sessionId);
  creationSessionOwnerId = ownerId;
  materializationKey = null;
  creationConversation.replaceChildren();
  updateCreateStep();
  setCreateLoading(true, '正在恢复实验规划…');
  try {
    const encodedId = encodeURIComponent(sessionId);
    const [session, messages] = await Promise.all([
      createApiRequest(`/experiment-creation/sessions/${encodedId}`, { method: 'GET' }),
      createApiRequest(`/experiment-creation/sessions/${encodedId}/messages`, { method: 'GET' }),
    ]);
    if (epoch !== createDialogEpoch || ownerId !== getCurrentUser()?.id || creationSessionId !== String(sessionId)) return;
    if (!Array.isArray(messages)) throw new Error('草稿消息格式无效，请稍后重试。');
    if (!messages.length) appendCreationMessage('system', '实验规划对话已开始。请描述你想学习的内容或希望完成的成果。');
    messages.forEach((message) => appendCreationMessage(message.role === 'user' ? 'user' : 'assistant', message.content));
    const direction = session.direction || {};
    const intent = direction.intent || {};
    directionTitle.textContent = intent.observable_outcome || intent.raw_request || '方向待补充';
    directionDetail.textContent = intent.target_artifact || intent.target_skill || '';
    directionTitleReview.textContent = directionTitle.textContent;
    directionDetailReview.textContent = directionDetail.textContent;
    directionReady = session.status === 'direction_ready' || direction.status === 'ready_for_confirmation';
    directionReview.hidden = !directionReady;
    const statusSteps = {
      direction_discussion: 1, direction_ready: 1, direction_confirmed: 2,
      configured: 3, planning: 3, plan_ready: 3, failed: session.durationMinutes ? 3 : directionReady ? 2 : 1,
    };
    createStep = statusSteps[session.status] || 1;
    if (session.durationMinutes) formatDurationMinutes(session.durationMinutes);
    selectedDifficulty = formatDifficulty(session.difficulty);
    document.querySelectorAll('[data-choice-group="difficulty"]').forEach((choice) => {
      choice.classList.toggle('is-selected', choice.dataset.value === selectedDifficulty);
    });
    if (session.plan && session.status === 'plan_ready') {
      renderPlanPreview(session.plan);
      createPlanReady = true;
      createPlan.classList.add('is-active');
    }
    updateCreateStep();
    setCreateLoading(false);
  } catch (error) {
    if (epoch === createDialogEpoch && ownerId === getCurrentUser()?.id) {
      showCreationMessage(error.message || '草稿恢复失败，请重试。', true);
    }
  } finally {
    finishCreateRequest(controller);
    if (epoch === createDialogEpoch && ownerId === getCurrentUser()?.id) {
      createBusy = false;
      setCreateLoading(false);
      updateCreateStep();
    }
  }
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
    void loadCreationDrafts(routeEpoch);
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
  await startGitExperimentRun();
});

gitCommitSha.addEventListener('input', updateGitSubmitButton);
gitReviewNodeList.addEventListener('change', updateGitSubmitButton);
gitSubmissionForm.addEventListener('submit', submitGitCommitForReview);
selectAllReviewNodes.addEventListener('click', () => {
  gitReviewNodeList.querySelectorAll('input[name="nodeIds"]').forEach((input) => { input.checked = true; });
  updateGitSubmitButton();
});
refreshGitSubmissions.addEventListener('click', () => void loadGitSubmissions());
showGitCredentials.addEventListener('click', () => void revealGitCredentials());
gitCopyCloneCommand.addEventListener('click', async () => {
  if (!activeGitRepository?.cloneUrl) return;
  const command = `git clone ${activeGitRepository.cloneUrl}`;
  try {
    await navigator.clipboard.writeText(command);
    gitCredentialNotice.textContent = '克隆命令已复制。需要认证时请使用独立的 Git 凭据，不要把令牌写入命令或代码。';
  } catch {
    window.prompt('复制下方克隆命令：', command);
  }
});

saveFileButton.addEventListener('click', saveWorkspaceFile);
createSnapshotButton.addEventListener('click', createWorkspaceSnapshot);
desktopNewFolderButton.addEventListener('click', async () => {
  if (activeRun?.workspaceType !== 'desktop') return;
  const relativePath = window.prompt('输入新文件夹的相对路径（例如 src）：');
  if (relativePath === null) return;
  try {
    await window.BMDesktop.workspace.createDirectory(String(activeRun.runId), relativePath.trim());
    await refreshWorkspaceFilesAfterTerminal(String(activeRun.runId), String(activeRun.workspaceId), routeResolutionEpoch);
  } catch (error) {
    appendChatMessage('agent', '系统', error.message || '创建文件夹失败。');
  }
});
desktopNewFileButton.addEventListener('click', async () => {
  if (activeRun?.workspaceType !== 'desktop') return;
  const relativePath = window.prompt('输入新文件的相对路径（例如 manage.py）：');
  if (relativePath === null) return;
  try {
    const run = activeRun;
    const runId = String(run.runId);
    const path = relativePath.trim();
    await window.BMDesktop.workspace.write(runId, path, '', null);
    const files = await listDesktopWorkspaceFiles(run);
    activeWorkspaceFiles = files;
    renderWorkspaceFileTabs();
    await selectWorkspaceFile(path);
  } catch (error) {
    appendChatMessage('agent', '系统', error.message || '创建文件失败。');
  }
});
completeNodeButton.addEventListener('click', completeCurrentNode);

backToCanvas.addEventListener('click', () => {
  gitCredentials.textContent = '凭据只在你点击获取后显示；不要把它提交到代码或截图中。';
  showGitCredentials.textContent = '获取本实验 Git 凭据';
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

async function requestGuidance(question, draftToRestore = null) {
  if (!activeRun) {
    appendChatMessage('agent', '系统', '请先启动工作区，再请求代码指导。');
    if (draftToRestore !== null && chatInput.value === '') chatInput.value = draftToRestore;
    return;
  }
  const runId = String(activeRun.runId);
  if (guidanceRequestsInFlight.has(runId)) return;
  guidanceRequestsInFlight.add(runId);
  const epoch = routeResolutionEpoch;
  appendChatMessage('user', '你', question);
  syncGuidanceControls();
  try {
    if (activeRun.workspaceType === 'desktop') {
      const relevantFiles = activeWorkspaceFiles
        .filter((file) => /\.(py|html|css|js|json|toml|txt|sql|yml|yaml)$/i.test(file.path))
        .slice(0, 12);
      const includedNames = relevantFiles.map((file) => file.path);
      const omittedCount = Math.max(0, activeWorkspaceFiles.length - relevantFiles.length);
      const consentText = [
        '发送以下工作区文件和最近终端输出给 DeepSeek？',
        ...(includedNames.length ? includedNames : ['（当前没有可读取的源码文件）']),
        ...(omittedCount ? [`另有 ${omittedCount} 个文件未纳入本次上下文。`] : []),
        '常见密钥文件会被排除，代码只在你主动确认后发送。',
      ].join('\n');
      if (!window.confirm(consentText)) throw new Error('你取消了代码共享，本次未发送源码。');
      const keyAvailable = await window.BMDesktop.secrets.hasDeepSeekKey();
      if (!keyAvailable) {
        const key = window.prompt('请输入个人 DeepSeek API 密钥。该密钥将加密保存在本机，不会上传到 BM-HS。');
        if (!key) throw new Error('尚未设置个人 DeepSeek 密钥，本次未发送代码。');
        await window.BMDesktop.secrets.setDeepSeekKey(key);
      }
      const files = await Promise.all(relevantFiles.map(async (file) => {
        const result = await window.BMDesktop.workspace.read(runId, file.path);
        return { path: result.path, content: result.content };
      }));
      const localResult = await window.BMDesktop.guidance({
        confirmed: true,
        question,
        files,
        terminalOutput: lastDesktopTerminalOutput,
        currentNode: activeTreeNodes.find((node) => Number(node.nodeId) === Number(activeNodeId)) || null,
      });
      if (!isRunContextCurrent(runId, epoch)) return;
      appendChatMessage('agent', '导师', localResult.answer || '模型未返回有效指导。');
      appendChatMessage('agent', '系统', '本次由本机助手调用 DeepSeek，未使用课程 RAG。');
      return;
    }
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
    if (draftToRestore !== null && chatInput.value === '') chatInput.value = draftToRestore;
  } finally {
    guidanceRequestsInFlight.delete(runId);
    syncGuidanceControls();
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
  if (patchApplyInFlight) {
    if (!patchDialog.classList.contains('is-open')) {
      patchDialogDescription.textContent = '之前确认的补丁正在应用，请等待完成后重新选择要查看的补丁。';
      patchDialog.classList.add('is-open');
      patchDialog.setAttribute('aria-hidden', 'false');
    }
    setMessage(patchDialogMessage, '补丁正在应用，请等待完成后再查看其他补丁。');
    return;
  }
  if (patchReviewSourceButton && patchReviewSourceButton !== sourceButton && !patchReviewApplied) {
    patchReviewSourceButton.disabled = false;
  }
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

function isPatchApplyResponseValid(result, patchId, expectedPaths) {
  if (!result || typeof result !== 'object'
      || String(result.patchId) !== String(patchId)
      || result.status !== 'applied'
      || !Number.isSafeInteger(result.snapshotId) || result.snapshotId <= 0
      || !Array.isArray(result.appliedFiles)
      || result.appliedFiles.some((path) => typeof path !== 'string' || !path.trim())) return false;
  const actualPaths = [...result.appliedFiles].sort();
  const expected = [...expectedPaths].sort();
  return actualPaths.length === expected.length && actualPaths.every((path, index) => path === expected[index]);
}

function recoverPatchApplyResponse(result, patchId, expectedPaths) {
  if (!result || typeof result !== 'object'
      || String(result.id) !== String(patchId)
      || result.status !== 'applied'
      || !Number.isSafeInteger(result.snapshotId) || result.snapshotId <= 0
      || !Array.isArray(result.files)
      || result.files.some((file) => !file || typeof file.path !== 'string' || !file.path.trim())) return null;
  const appliedFiles = result.files.map((file) => file.path).sort();
  const expected = [...expectedPaths].sort();
  if (appliedFiles.length !== expected.length || !appliedFiles.every((path, index) => path === expected[index])) return null;
  return { patchId: result.id, status: result.status, snapshotId: result.snapshotId, appliedFiles };
}

async function confirmPatchReview() {
  if (!pendingPatchReview || !activeRun || patchApplyInFlight) return;
  const { detail, sourceButton } = pendingPatchReview;
  const runId = String(activeRun.runId);
  const workspaceId = String(activeRun.workspaceId);
  const epoch = routeResolutionEpoch;
  const requestedPatchReviewEpoch = patchReviewEpoch;
  patchApplyInFlight = true;
  let applySucceeded = false;
  let applyResponseInvalid = false;
  let applyCompletionMessage = '';
  let applyCompletionIsError = false;
  confirmPatchButton.disabled = true;
  setMessage(patchDialogMessage, '正在再次校验并应用补丁…');
  try {
    let result = await apiRequest(`/runs/${runId}/patches/${detail.id}/apply`, { method: 'POST', timeoutMs: 30000 });
    const expectedPaths = detail.files.map((file) => file.path);
    if (!isPatchApplyResponseValid(result, detail.id, expectedPaths)) {
      applyResponseInvalid = true;
      const current = await apiRequest(`/runs/${runId}/patches/${detail.id}`, { method: 'GET', timeoutMs: 15000 });
      result = recoverPatchApplyResponse(current, detail.id, expectedPaths);
      if (!result) throw new Error('补丁当前状态仍无法确认。');
      applyResponseInvalid = false;
    }
    applySucceeded = true;
    applyCompletionMessage = '补丁已应用，请重新打开要查看的补丁。';
    sourceButton.disabled = true;
    sourceButton.textContent = '已应用并生成快照';
    if (requestedPatchReviewEpoch !== patchReviewEpoch) return;
    patchReviewApplied = true;
    closePatchReview();
    if (!isRunContextCurrent(runId, epoch)) return;
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
    if (applyResponseInvalid) {
      applyCompletionMessage = '补丁应用结果无法确认：服务端返回了无效响应，且状态查询未能确认结果。为避免重复修改，已锁定该补丁；请重新打开文件核对工作区。';
      applyCompletionIsError = true;
      sourceButton.disabled = true;
      sourceButton.textContent = '应用结果待核实';
      patchReviewApplied = true;
      if (requestedPatchReviewEpoch === patchReviewEpoch) {
        closePatchReview();
      }
      if (isRunContextCurrent(runId, epoch)) appendChatMessage('agent', '系统', applyCompletionMessage);
      return;
    }
    if (applySucceeded) {
      applyCompletionMessage = `补丁已应用，但当前文件自动刷新失败：${error.message || '请重新打开文件确认。'}`;
      applyCompletionIsError = true;
      if (isRunContextCurrent(runId, epoch)) appendChatMessage('agent', '系统', applyCompletionMessage);
      return;
    }
    applyCompletionMessage = error.message || '补丁未应用，请重新检查后重试。';
    applyCompletionIsError = true;
    if (requestedPatchReviewEpoch !== patchReviewEpoch) return;
    if (!isRunContextCurrent(runId, epoch)) {
      closePatchReview();
      return;
    }
    confirmPatchButton.disabled = false;
    setMessage(patchDialogMessage, error.message || '补丁未应用，文件可能已经发生变化。', true);
  } finally {
    patchApplyInFlight = false;
    if (!pendingPatchReview && patchDialog.classList.contains('is-open') && applyCompletionMessage) {
      setMessage(patchDialogMessage, applyCompletionMessage, applyCompletionIsError);
    }
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
  const draft = chatInput.value;
  const text = draft.trim();
  if (!text) return;
  if (activeRun && guidanceRequestsInFlight.has(String(activeRun.runId))) return;
  chatInput.value = '';
  void requestGuidance(text, draft);
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
  event.preventDefault?.();
  const command = terminalInput.value;
  if (!command.trim()) return;
  const runId = String(activeTerminalRunId || '');
  if (!isValidDatabaseId(runId)) {
    appendChatMessage('agent', '系统', '实验运行信息无效，已阻止执行命令。');
    return;
  }
  const commandLockKey = terminalCommandLockKey(runId);
  if (terminalCommandsInFlight.has(commandLockKey)) return;
  if (!activeTerminalSession && activeRun?.workspaceType !== 'desktop') {
    appendChatMessage('agent', '系统', '终端尚未连接，请先启动工作区。');
    return;
  }
  if (activeRun?.workspaceType !== 'desktop' && (!activeRun || String(activeRun.runId) !== runId
    || !isValidOpenTerminalSession(activeTerminalSession, activeRun.workspaceId))) {
    if (activeRun && String(activeRun.runId) === runId
      && isValidDatabaseId(activeRun.workspaceId)) {
      invalidateClosedTerminalSession(runId, String(activeRun.workspaceId), String(activeTerminalSession.id), routeResolutionEpoch);
    }
    terminalInput.disabled = true;
    appendChatMessage('agent', '系统', '终端会话信息与当前工作区不匹配，请重新连接工作区。');
    return;
  }
  if (activeFilePath && codeEditor.value !== activeFileSavedContent) {
    appendChatMessage('agent', '系统', '请先保存当前代码再运行终端命令，避免终端执行旧版本文件。');
    return;
  }
  if (!isValidDatabaseId(activeRun?.workspaceId)) {
    appendChatMessage('agent', '系统', '工作区信息无效，已阻止执行命令。');
    return;
  }
  if (activeRun?.workspaceType === 'desktop') {
    const epoch = routeResolutionEpoch;
    terminalInput.value = '';
    terminalInput.disabled = true;
    codeEditor.disabled = true;
    saveFileButton.disabled = true;
    terminalCommandsInFlight.add(commandLockKey);
    const line = document.createElement('div');
    const prompt = document.createElement('span');
    prompt.className = 'terminal-prompt';
    prompt.textContent = '$';
    const commandText = document.createElement('span');
    commandText.textContent = ` ${command}`;
    line.append(prompt, commandText);
    terminalOutput.appendChild(line);
    try {
      const result = await window.BMDesktop.workspace.runCommand(runId, command);
      if (!isRunContextCurrent(runId, epoch)) return;
      lastDesktopTerminalOutput = result.output || '';
      const output = document.createElement('div');
      output.className = result.exitCode === 0 ? 'terminal-result' : 'terminal-error';
      output.textContent = result.output || `命令结束，退出码：${result.exitCode ?? '未知'}`;
      terminalOutput.appendChild(output);
      await refreshWorkspaceFilesAfterTerminal(runId, String(activeRun.workspaceId), epoch);
    } catch (error) {
      if (isRunContextCurrent(runId, epoch)) {
        const failure = document.createElement('div');
        failure.className = 'terminal-error';
        failure.textContent = error.message || '本地命令执行失败。';
        terminalOutput.appendChild(failure);
      }
    } finally {
      terminalCommandsInFlight.delete(commandLockKey);
      if (isRunContextCurrent(runId, epoch)) {
        codeEditor.disabled = !activeFilePath;
        saveFileButton.disabled = !activeFilePath;
        terminalInput.disabled = false;
        renderWorkspaceFileTabs();
        terminalOutput.scrollTop = terminalOutput.scrollHeight;
      }
    }
    return;
  }
  const terminalSessionId = String(activeTerminalSession.id);
  const workspaceId = String(activeRun.workspaceId);
  const epoch = routeResolutionEpoch;
  const userId = String(getCurrentUser()?.id || '');
  const clientCommandId = window.crypto?.randomUUID?.();
  if (!clientCommandId) {
    appendChatMessage('agent', '系统', '当前浏览器无法生成安全的命令标识，已阻止执行。');
    return;
  }
  const pendingCommand = {
    runId,
    workspaceId,
    userId,
    sessionId: terminalSessionId,
    clientCommandId,
  };
  try {
    persistPendingTerminalCommand(pendingCommand);
  } catch (error) {
    appendChatMessage('agent', '系统', error.message || '无法安全保留命令状态，已阻止执行。');
    return;
  }
  terminalInput.value = '';
  terminalCommandsInFlight.add(commandLockKey);
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
  let unresolved = false;
  try {
    const result = await apiRequest(`/terminal-sessions/${terminalSessionId}/commands`, {
      method: 'POST',
      headers: { 'Idempotency-Key': clientCommandId },
      body: JSON.stringify({ commandText: command, workingDirectory: '.' }),
      timeoutMs: 45000,
    });
    if (!result || !['succeeded', 'failed', 'cancelled'].includes(result.status)
      || typeof result.output !== 'string') {
      throw new Error('终端命令响应格式无效，命令结果无法确认。');
    }
    removeSessionValue(terminalCommandStorageKey(runId, userId));
    if (!isRunContextCurrent(runId, epoch) || String(activeTerminalSession?.id) !== terminalSessionId) return;
    const output = document.createElement('div');
    output.className = result.status === 'succeeded' ? 'terminal-result' : 'terminal-error';
    output.textContent = result.output || `命令结束，状态：${result.status}`;
    terminalOutput.appendChild(output);
  } catch (error) {
    if (!isRunContextCurrent(runId, epoch) || String(activeTerminalSession?.id) !== terminalSessionId) return;
    const failure = document.createElement('div');
    failure.className = 'terminal-error';
    if (isTerminalSessionClosedError(error)) {
      removeSessionValue(terminalCommandStorageKey(runId, userId));
      try {
        const recovered = await recoverClosedTerminalSession(runId, workspaceId, terminalSessionId, epoch);
        if (!recovered || !isRunContextCurrent(runId, epoch)) return;
        failure.textContent = '旧终端会话已关闭，已重新连接。刚才的命令未执行；确认代码状态后请手动重新运行。';
      } catch (recoveryError) {
        if (!isRunContextCurrent(runId, epoch)
          || String(activeTerminalSession?.id) !== terminalSessionId) return;
        invalidateClosedTerminalSession(runId, workspaceId, terminalSessionId, epoch);
        failure.textContent = `终端会话已关闭，刚才的命令未执行。自动重连失败：${recoveryError.message || '请重新启动工作区。'}`;
        failure.appendChild(createTerminalReconnectButton(runId, workspaceId, terminalSessionId, epoch, failure));
      }
      terminalOutput.appendChild(failure);
    } else if (!Number.isInteger(error.status) || error.status === 408 || error.status >= 500
        || error.code === 'IDEMPOTENCY_KEY_REUSED') {
      failure.textContent = `${error.message || '命令执行失败。'} 执行结果尚未确认，请先检查工作区状态后再决定是否重试。`;
      terminalOutput.appendChild(failure);
      const resolved = await presentTerminalCommandReconciliation(
        pendingCommand, runId, workspaceId, epoch, failure,
      );
      unresolved = resolved !== true;
    } else {
      removeSessionValue(terminalCommandStorageKey(runId, userId));
      failure.textContent = error.message || '命令执行失败。';
      terminalOutput.appendChild(failure);
    }
  } finally {
    try {
      if (isRunContextCurrent(runId, epoch) && String(activeRun?.workspaceId) === workspaceId) {
        try {
          await refreshWorkspaceFilesAfterTerminal(runId, workspaceId, epoch);
        } catch (error) {
          if (isRunContextCurrent(runId, epoch) && String(activeRun?.workspaceId) === workspaceId) {
            appendChatMessage('agent', '系统', error.message || '终端运行后同步工作区文件失败，请重新打开文件列表。');
          }
        }
      }
    } finally {
      if (!unresolved) terminalCommandsInFlight.delete(commandLockKey);
      if (!unresolved && isRunContextCurrent(runId, epoch) && String(activeRun?.workspaceId) === workspaceId) {
        codeEditor.disabled = !activeFilePath;
        saveFileButton.disabled = !activeFilePath;
        renderWorkspaceFileTabs();
        terminalInput.disabled = !activeTerminalSession;
        terminalOutput.scrollTop = terminalOutput.scrollHeight;
      }
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
  const { timeoutMs: requestedTimeoutMs = 60000, signal: requestSignal, ...fetchOptions } = options;
  const timeoutMs = Number.isFinite(requestedTimeoutMs) && requestedTimeoutMs > 0
    ? requestedTimeoutMs
    : 60000;
  const timeoutController = new AbortController();
  const timeout = window.setTimeout(() => timeoutController.abort(), timeoutMs);
  const abortForRequest = () => timeoutController.abort();
  if (requestSignal?.aborted) abortForRequest();
  else requestSignal?.addEventListener('abort', abortForRequest, { once: true });
  try {
    if (window.BMDesktop?.isDesktop && activeRun?.workspaceType === 'desktop') {
      const localResult = await desktopWorkspaceApiRequest(path, fetchOptions);
      if (localResult.handled) return localResult.value;
    }
    if (window.BMDesktop?.isDesktop) {
      if (requestSignal?.aborted) throw Object.assign(new Error('请求已取消。'), { name: 'AbortError' });
      const result = await window.BMDesktop.apiRequest({
        path,
        method: fetchOptions.method || 'GET',
        body: fetchOptions.body,
        headers: fetchOptions.headers || {},
        timeoutMs,
      });
      if (!result?.ok) {
        const requestError = new Error(result?.payload?.message || `请求失败（${result?.status || 0}）`);
        requestError.status = result?.status;
        requestError.code = typeof result?.payload?.code === 'string' ? result.payload.code : undefined;
        throw requestError;
      }
      return result.payload;
    }
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
      requestError.code = typeof payload.code === 'string' ? payload.code : undefined;
      throw requestError;
    }
    return payload;
  } catch (error) {
    if (requestSignal?.aborted) {
      const abortError = new Error('请求已取消。');
      abortError.name = 'AbortError';
      throw abortError;
    }
    if (error.name === 'AbortError') throw new Error('请求超时，请检查后端和 Harness 状态后重试。');
    if (error instanceof TypeError && error.message === 'Failed to fetch') {
      throw new Error('无法连接服务器，请检查后端服务是否已启动。');
    }
    throw error;
  } finally {
    window.clearTimeout(timeout);
    requestSignal?.removeEventListener('abort', abortForRequest);
  }
}

async function listDesktopWorkspaceFiles(run) {
  const files = await window.BMDesktop.workspace.list(String(run.runId));
  if (!Array.isArray(files)) throw new Error('本地工作区文件列表格式无效。');
  return files.map((file) => ({
    workspaceId: Number(run.workspaceId),
    path: file.path,
    displayName: file.displayName || file.path.split('/').pop(),
    language: file.path.includes('.') ? file.path.split('.').pop().toLowerCase() : null,
    sizeBytes: file.sizeBytes,
    contentHash: file.contentHash,
    entry: false,
    updatedAt: new Date().toISOString(),
  }));
}

async function readDesktopWorkspaceFile(run, relativePath) {
  const result = await window.BMDesktop.workspace.read(String(run.runId), relativePath);
  return {
    file: {
      workspaceId: Number(run.workspaceId),
      path: result.path,
      displayName: result.path.split('/').pop(),
      language: result.path.includes('.') ? result.path.split('.').pop().toLowerCase() : null,
      sizeBytes: new TextEncoder().encode(result.content).length,
      contentHash: result.contentHash,
      entry: false,
      updatedAt: new Date().toISOString(),
    },
    content: result.content,
  };
}

async function desktopWorkspaceApiRequest(path, options) {
  const match = /^\/workspaces\/(\d+)\/files(?:\/(.+))?$/.exec(path);
  if (!match || String(activeRun.workspaceId) !== match[1]) return { handled: false };
  const runId = String(activeRun.runId);
  const relativePath = match[2] ? decodeURIComponent(match[2]) : null;
  const method = String(options.method || 'GET').toUpperCase();
  if (method === 'GET' && relativePath === null) {
    return { handled: true, value: await listDesktopWorkspaceFiles(activeRun) };
  }
  if (method === 'GET' && relativePath !== null) {
    return { handled: true, value: await readDesktopWorkspaceFile(activeRun, relativePath) };
  }
  if (method === 'PUT' && relativePath !== null) {
    let body;
    try { body = JSON.parse(options.body || '{}'); } catch { throw new Error('文件保存请求格式无效。'); }
    return { handled: true, value: await window.BMDesktop.workspace.write(runId, relativePath, body.content, body.expectedHash) };
  }
  return { handled: false };
}

function beginCreateRequest() {
  cancelCreateRequest();
  const controller = new AbortController();
  activeCreateRequestController = controller;
  return controller;
}

function finishCreateRequest(controller) {
  if (activeCreateRequestController === controller) activeCreateRequestController = null;
}

function cancelCreateRequest() {
  const controller = activeCreateRequestController;
  activeCreateRequestController = null;
  controller?.abort();
}

function createApiRequest(path, options = {}) {
  const signal = activeCreateRequestController?.signal;
  return apiRequest(path, signal ? { ...options, signal } : options);
}

function difficultyCode(label) {
  return { 入门: 'BEGINNER', 简单: 'EASY', 普通: 'NORMAL', 困难: 'HARD', 挑战: 'CHALLENGE' }[label];
}

function appendCreationMessage(role, text) {
  const message = document.createElement('div');
  message.className = `direction-message ${role === 'user' ? 'is-user' : role === 'error' ? 'is-error' : role === 'system' ? 'is-system' : 'is-agent'}`;
  const avatar = document.createElement('span');
  avatar.className = 'direction-message-avatar';
  avatar.textContent = role === 'user' ? '我' : role === 'error' ? '!' : role === 'system' ? '·' : 'H';
  const content = document.createElement('div');
  content.className = 'direction-message-content';
  const label = document.createElement('strong');
  label.textContent = role === 'user' ? '你' : role === 'error' ? '连接提示' : role === 'system' ? '进度' : 'Harness';
  const body = document.createElement('p');
  body.textContent = text;
  content.append(label, body);
  message.append(avatar, content);
  creationConversation.append(message);
  creationConversation.scrollTop = creationConversation.scrollHeight;
  return message;
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

function appendCreationPending(text = '正在等待 Harness 回复') {
  const pending = document.createElement('div');
  pending.className = 'direction-message is-agent is-pending';
  pending.setAttribute('role', 'status');
  const avatar = document.createElement('span');
  avatar.className = 'direction-message-avatar';
  avatar.textContent = 'H';
  const content = document.createElement('div');
  content.className = 'direction-message-content';
  const label = document.createElement('strong');
  label.textContent = 'Harness';
  const body = document.createElement('p');
  body.className = 'direction-pending-copy';
  body.textContent = text;
  const dots = document.createElement('span');
  dots.className = 'direction-pending-dots';
  dots.setAttribute('aria-hidden', 'true');
  dots.textContent = '•••';
  content.append(label, body, dots);
  pending.append(avatar, content);
  creationConversation.append(pending);
  creationConversation.scrollTop = creationConversation.scrollHeight;
  creationPendingMessage = pending;
  return pending;
}

function appendCreationRetry(message, failedBubble = null) {
  const errorBubble = appendCreationMessage('error', message);
  const retry = document.createElement('button');
  retry.className = 'direction-retry-button';
  retry.type = 'button';
  retry.textContent = '重试发送';
  retry.addEventListener('click', () => {
    retry.disabled = true;
    errorBubble.remove();
    void sendCreationDirection(message, failedBubble);
  });
  errorBubble.querySelector('.direction-message-content').append(retry);
}

async function sendCreationDirection(message, existingBubble = null) {
  if (createBusy || !creationSessionId) return;
  const sessionId = creationSessionId;
  const ownerId = getCurrentUser()?.id;
  const controller = beginCreateRequest();
  const userBubble = existingBubble || appendCreationMessage('user', message);
  const pending = appendCreationPending();
  createBusy = true;
  createDialog.setAttribute('aria-busy', 'true');
  createLoading.hidden = true;
  updateCreateStep();
  try {
    const envelope = await createApiRequest(`/experiment-creation/sessions/${encodeURIComponent(sessionId)}/messages`, {
      method: 'POST', body: JSON.stringify({ message }),
    });
    if (ownerId !== getCurrentUser()?.id || creationSessionId !== sessionId) return;
    if (typeof envelope.assistantMessage !== 'string') throw new Error('Harness 未返回可显示的回复，请重试。');
    pending.remove();
    if (creationPendingMessage === pending) creationPendingMessage = null;
    showCreationMessage(envelope.assistantMessage);
    learningGoal.value = '';
    directionTitle.textContent = envelope.intent?.observable_outcome || envelope.intent?.raw_request || '方向待补充';
    directionDetail.textContent = envelope.intent?.target_artifact || envelope.intent?.target_skill || envelope.assistantMessage;
    directionReady = envelope.status === 'ready_for_confirmation';
    directionTitleReview.textContent = directionTitle.textContent;
    directionDetailReview.textContent = directionDetail.textContent;
    directionReview.hidden = !directionReady;
    learningGoal.placeholder = directionReady
      ? '如需修正或补充方向，可以继续告诉 Harness…'
      : envelope.nextQuestion?.text || '请继续补充…';
  } catch (error) {
    pending.remove();
    if (creationPendingMessage === pending) creationPendingMessage = null;
    if (ownerId === getCurrentUser()?.id && creationSessionId === sessionId) {
      learningGoal.value = message;
      appendCreationRetry(error.message || '发送失败，请检查连接后重试。', userBubble);
    }
  } finally {
    finishCreateRequest(controller);
    if (ownerId === getCurrentUser()?.id && creationSessionId === sessionId) {
      createBusy = false;
      setCreateLoading(false);
      updateCreateStep();
      if (!createDialog.classList.contains('is-open')) void loadCreationDrafts(routeResolutionEpoch);
    }
  }
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
    node.innerHTML = '<span class="plan-node-sequence"></span><div class="plan-node-content"><div class="plan-node-heading"><strong></strong><span class="plan-node-minutes"></span></div><p class="plan-node-description"></p><ol class="plan-task-list"></ol></div>';
    node.querySelector('.plan-node-sequence').textContent = nodeNumbers.get(planNode.key) || planNode.key;
    node.querySelector('strong').textContent = `${planNode.stage} · ${planNode.title}`;
    const estimatedMinutes = Number.isInteger(planNode.estimatedMinutes) && planNode.estimatedMinutes > 0
      ? `约 ${planNode.estimatedMinutes} 分钟`
      : '时长待确认';
    node.querySelector('.plan-node-minutes').textContent = estimatedMinutes;
    node.querySelector('.plan-node-description').textContent = planNode.description;
    const validationLabels = {
      manual: '人工 / 自检',
      command: '运行命令',
      test: '自动测试',
      agent: '助手辅助检查',
    };
    const taskList = node.querySelector('.plan-task-list');
    planNode.tasks.forEach((planTask) => {
      const item = document.createElement('li');
      item.className = 'plan-task-item';
      const heading = document.createElement('div');
      heading.className = 'plan-task-heading';
      const title = document.createElement('strong');
      title.textContent = planTask.title;
      const validation = document.createElement('span');
      validation.className = 'plan-task-validation';
      validation.textContent = validationLabels[planTask.validationType] || '人工 / 自检';
      const description = document.createElement('p');
      description.className = 'plan-task-description';
      description.textContent = planTask.description;
      heading.append(title, validation);
      item.append(heading, description);
      taskList.append(item);
    });
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
  if (createMaterializationInFlight) return;
  if (createBusy && creationSessionOwnerId === getCurrentUser()?.id) {
    createDialog.classList.add('is-open');
    updateCreateStep();
    return;
  }
  if (creationSessionId && creationSessionOwnerId === getCurrentUser()?.id) {
    createDialog.classList.add('is-open');
    updateCreateStep();
    if (!createBusy) learningGoal.focus();
    return;
  }
  const epoch = ++createDialogEpoch;
  const requestController = beginCreateRequest();
  createStep = 1;
  createPlanReady = false;
  directionReady = false;
  creationSessionId = null;
  creationSessionOwnerId = getCurrentUser()?.id;
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
  appendCreationMessage('system', '实验规划对话已开始。请描述你想学习的内容或希望完成的成果。');
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
    const session = await createApiRequest('/experiment-creation/sessions', { method: 'POST', body: '{}' });
    if (epoch !== createDialogEpoch || creationSessionOwnerId !== getCurrentUser()?.id) return;
    creationSessionId = session.sessionId;
    if (createDialog.classList.contains('is-open')) learningGoal.focus();
    if (!createDialog.classList.contains('is-open')) void loadCreationDrafts(routeResolutionEpoch);
  } catch (error) {
    if (epoch === createDialogEpoch && creationSessionOwnerId === getCurrentUser()?.id) {
      showCreationMessage(error.message, true);
    }
  } finally {
    finishCreateRequest(requestController);
    if (epoch === createDialogEpoch && creationSessionOwnerId === getCurrentUser()?.id) {
      createBusy = false;
      setCreateLoading(false);
      updateCreateStep();
    }
  }
}

function closeCreateDialogPanel() {
  createDialog.classList.remove('is-open');
  if (!createBusy) {
    setCreateLoading(false);
    void loadCreationDrafts(routeResolutionEpoch);
  }
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
  closeCreateDialog.disabled = false;
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
    if (group === 'difficulty') {
      selectedDifficulty = choice.dataset.value;
      if (creationSessionId && createStep === 3) {
        createBusy = true;
        updateCreateStep();
        setCreateLoading(true, '正在保存难度选择…');
        const sessionId = creationSessionId;
        const ownerId = getCurrentUser()?.id;
        void saveCreationConfiguration().catch((error) => {
          if (sessionId === creationSessionId && ownerId === getCurrentUser()?.id) {
            showCreationMessage(error.message || '难度暂未保存，请重试。', true);
          }
        }).finally(() => {
          if (sessionId !== creationSessionId || ownerId !== getCurrentUser()?.id) return;
          createBusy = false;
          setCreateLoading(false);
          updateCreateStep();
          if (!createDialog.classList.contains('is-open')) void loadCreationDrafts(routeResolutionEpoch);
        });
      }
    }
    if (group === 'duration') selectedDuration = choice.dataset.value;
  });
});

createStepButton.addEventListener('click', async () => {
  if (createBusy) return;
  if (!creationSessionId) {
    showCreationMessage('需求会话尚未建立，请关闭后重试。', true);
    return;
  }
  if (createStep === 1 && (!directionReady || learningGoal.value.trim())) {
    const message = learningGoal.value.trim();
    if (!message) {
      learningGoal.focus();
      return;
    }
    await sendCreationDirection(message);
    return;
  }
  createBusy = true;
  const requestEpoch = createDialogEpoch;
  const requestController = createPlanReady ? null : beginCreateRequest();
  let ownsMaterialization = false;
  updateCreateStep();
  setCreateLoading(true);
  try {
    if (createPlanReady) {
      createMaterializationInFlight = true;
      ownsMaterialization = true;
      updateCreateStep();
      setCreateLoading(true, '正在创建实验节点和连线…');
      materializationKey ||= crypto.randomUUID ? crypto.randomUUID() : `create-${creationSessionId}-${Date.now()}`;
      const created = await apiRequest(`/experiment-creation/sessions/${creationSessionId}/confirm`, {
        method: 'POST', headers: { 'Idempotency-Key': materializationKey }, body: '{}',
      });
      const id = String(created.experimentId);
      experimentData[id] = { ...created, description: created.description, nodes: created.nodes };
      await loadPersonalExperiments(routeResolutionEpoch);
      createMaterializationInFlight = false;
      ownsMaterialization = false;
      createBusy = false;
      creationSessionId = null;
      creationSessionOwnerId = null;
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

    if (createStep === 1 && directionReady) {
      setCreateLoading(true, '正在确认实验方向…');
      await createApiRequest(`/experiment-creation/sessions/${creationSessionId}/direction/confirm`, { method: 'POST', body: '{}' });
      appendCreationMessage('system', '实验方向已确认。');
      createStep = 2;
      updateCreateStep();
      return;
    }

    if (createStep === 2) {
      setCreateLoading(true, '正在保存学习周期…');
      await saveCreationConfiguration();
      appendCreationMessage('system', `学习周期已设置：${selectedDuration}。`);
      createStep = 3;
      updateCreateStep();
      return;
    }

    setCreateLoading(true, 'Harness 正在生成树状实验方案，请稍候…');
    appendCreationMessage('system', `方案生成设置：周期 ${selectedDuration}，难度 ${selectedDifficulty}。`);
    await saveCreationConfiguration();
    const plan = await createApiRequest(`/experiment-creation/sessions/${creationSessionId}/plan`, {
      method: 'POST',
      body: '{}',
      timeoutMs: 150000,
    });
    renderPlanPreview(plan);
    createPlanReady = true;
    document.querySelectorAll('.create-step').forEach((step) => step.classList.remove('is-active'));
    createPlan.classList.add('is-active');
    appendCreationMessage('system', '实验方案已生成，请检查清单后确认创建。');
  } catch (error) {
    if (requestEpoch === createDialogEpoch && !requestController?.signal.aborted) {
      showCreationMessage(error.message, true);
    }
  } finally {
    if (requestController) finishCreateRequest(requestController);
    if (ownsMaterialization) createMaterializationInFlight = false;
    if (requestEpoch === createDialogEpoch) {
      createBusy = false;
      setCreateLoading(false);
      updateCreateStep();
      if (!createDialog.classList.contains('is-open')) void loadCreationDrafts(routeResolutionEpoch);
    }
  }
});

createExperimentButton.addEventListener('click', openCreateDialog);
closeCreateDialog.addEventListener('click', closeCreateDialogPanel);
createDialog.addEventListener('click', (event) => {
  if (event.target === createDialog) event.stopPropagation();
});
window.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && createDialog.classList.contains('is-open')) {
    event.preventDefault();
    closeCreateDialogPanel();
  }
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
    if (window.BMDesktop?.isDesktop) {
      const configuration = await window.BMDesktop.getApiConfiguration();
      if (!configuration.configured) {
        const configuredUrl = window.prompt('请输入 BM-HS 平台 HTTPS API 地址（例如 https://example.edu/api）：');
        if (!configuredUrl) throw new Error('尚未配置平台地址，未发送登录信息。');
        await window.BMDesktop.setApiBaseUrl(configuredUrl.trim());
      }
    }
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
  const experimentId = button.dataset.experimentId;
  if (experimentId) openExperiment(experimentId);
});

studentLocalCodingButton.addEventListener('click', () => showRoute('library'));

document.querySelector('#studentLibraryButton').addEventListener('click', () => showRoute('library'));
document.querySelector('#teacherLibraryButton').addEventListener('click', () => showRoute('library'));
teacherDashboardCourse.addEventListener('change', () => void loadTeacherCourseDashboard(teacherDashboardCourse.value));
teacherDshExperiment.addEventListener('change', () => void loadTeacherDshRuns(teacherDshExperiment.value));
teacherDshReviewForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const runId = selectedTeacherDshRunId;
  if (!Number.isSafeInteger(runId) || runId <= 0) {
    setMessage(teacherDshReviewMessage, '请先选择一名学生。', true);
    return;
  }
  const rating = teacherDshRating.value.trim();
  if (rating && (!/^\d{1,3}$/.test(rating) || Number(rating) > 100)) {
    setMessage(teacherDshReviewMessage, '评分需为 0 到 100 的整数。', true);
    return;
  }
  teacherDshReviewSave.disabled = true;
  setMessage(teacherDshReviewMessage, '正在保存阶段评价…');
  try {
    await apiRequest(`/teacher/runs/${encodeURIComponent(runId)}/reviews`, {
      method: 'POST',
      body: JSON.stringify({ rating: rating ? Number(rating) : null, feedback: teacherDshFeedback.value.trim() || null }),
    });
    if (selectedTeacherDshRunId === runId) setMessage(teacherDshReviewMessage, '阶段评价已保存。');
  } catch (error) {
    if (selectedTeacherDshRunId === runId) setMessage(teacherDshReviewMessage, error.message || '阶段评价保存失败。', true);
  } finally {
    if (selectedTeacherDshRunId === runId) teacherDshReviewSave.disabled = false;
  }
});
adminExperiment.addEventListener('change', syncAdminReviewerSelection);
adminAssignReviewer.addEventListener('click', async () => {
  const experimentId = adminExperiment.value;
  const teacherId = adminReviewer.value;
  if (!/^\d+$/.test(experimentId) || !/^\d+$/.test(teacherId)) {
    setMessage(adminReviewerMessage, '请选择个人实验和审核教师。', true);
    return;
  }
  adminAssignReviewer.disabled = true;
  setMessage(adminReviewerMessage, '正在保存审核教师…');
  try {
    await apiRequest(`/admin/experiments/${encodeURIComponent(experimentId)}/reviewer`, {
      method: 'PUT', body: JSON.stringify({ teacherId: Number(teacherId) }),
    });
    setMessage(adminReviewerMessage, '审核教师已保存。');
    await renderAdminDashboard();
    adminExperiment.value = experimentId;
    syncAdminReviewerSelection();
  } catch (error) {
    setMessage(adminReviewerMessage, error.message || '审核教师保存失败。', true);
    adminAssignReviewer.disabled = false;
  }
});
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
    const role = getCurrentUser()?.role;
    const messageTarget = role === 'teacher' ? teacherMessage : role === 'admin' ? adminReviewerMessage : studentMessage;
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
  syncGuidanceControls();
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

document.querySelectorAll('#studentLogoutButton, #teacherLogoutButton, #adminLogoutButton, #libraryLogoutButton').forEach((button) => button.addEventListener('click', logout));

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
