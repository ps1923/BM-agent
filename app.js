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
const createExperimentButton = document.querySelector('#createExperimentButton');
const createDialog = document.querySelector('#createDialog');
const closeCreateDialog = document.querySelector('#closeCreateDialog');
const manageFoldersButton = document.querySelector('#manageFoldersButton');
const folderDialog = document.querySelector('#folderDialog');
const closeFolderDialog = document.querySelector('#closeFolderDialog');
const folderManageList = document.querySelector('#folderManageList');
const newFolderName = document.querySelector('#newFolderName');
const addFolderButton = document.querySelector('#addFolderButton');
const folderGrid = document.querySelector('.folder-grid');
const folderNavigation = document.querySelector('#folderNavigation');
const libraryMainColumn = document.querySelector('#libraryMainColumn');
const libraryOverviewButton = document.querySelector('#libraryOverviewButton');
const sidebarExperimentCount = document.querySelector('#sidebarExperimentCount');
const folderSectionCount = document.querySelector('#folderSectionCount');
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
const springBootExperimentList = document.querySelector('.folder-card-wide .experiment-list');
const defaultLearningGoalPlaceholder = learningGoal.placeholder;
const API_BASE_URL = window.BM_API_BASE_URL || 'http://127.0.0.1:8080/api';

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
let experimentCount = 3;
let creationSessionId = null;
let directionReady = false;
let pendingPlan = null;
let createBusy = false;
let materializationKey = null;
let createDialogEpoch = 0;
let activeTreeNodes = [];
let selectedTreeNode = null;
let treeEdgeLayer = null;
let explicitFolderSelection = '';
let folderSelectionTimer = 0;

const experimentData = {
  'spring-init': { name: 'Spring Boot 项目初始化', description: '创建第一个 Spring Boot 项目，理解应用入口、依赖管理与启动流程。' },
  'spring-rest': { name: 'Spring Boot REST 接口', description: '从 Controller 到 JSON 响应，完成第一个可访问的 REST 接口。' },
  'spring-data': { name: '连接 MySQL 数据库', description: '配置数据源，理解实体、仓储与数据库之间的基本关系。' },
};

function renderCanvas() {
  bubble.style.left = `${bubbleX}px`;
  bubble.style.top = `${bubbleY}px`;
  const root = activeTreeNodes.find((node) => !node.parentKey);
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
  if (selectedTreeNode) {
    inspector.querySelector('h2').textContent = selectedTreeNode.title;
    inspector.querySelector('.inspector-intro').textContent = selectedTreeNode.description;
    inspector.querySelector('.inspector-sequence span').textContent = selectedTreeNode.key;
    inspector.querySelector('.node-fact:nth-child(1) dd').textContent = `约 ${selectedTreeNode.estimatedMinutes} 分钟`;
    inspector.querySelector('.node-fact:nth-child(4) dd').textContent = selectedTreeNode.title;
    const taskList = editorView.querySelector('.task-list');
    const taskCount = editorView.querySelector('.panel-count');
    taskList.replaceChildren();
    selectedTreeNode.tasks.forEach((task, index) => {
      const button = document.createElement('button');
      button.className = `task-item${index === 0 ? ' is-active' : ''}`;
      button.type = 'button';
      const check = document.createElement('span');
      check.className = 'task-check';
      const title = document.createElement('span');
      title.textContent = task.title;
      button.append(check, title);
      button.addEventListener('click', () => {
        taskList.querySelectorAll('.task-item').forEach((item) => item.classList.remove('is-active'));
        button.classList.add('is-active');
      });
      taskList.append(button);
    });
    taskCount.textContent = selectedTreeNode.tasks.length;
  }
  inspector.classList.add('is-open');
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
  const ordered = [...activeTreeNodes].sort((a, b) => a.depth - b.depth || a.key.localeCompare(b.key));
  ordered.forEach((node, index) => {
    const element = index === 0 ? bubble : bubble.cloneNode(true);
    element.id = index === 0 ? 'bubble' : '';
    element.classList.add('tree-node');
    element.dataset.treeNodeKey = node.key;
    element.querySelector('.bubble-sequence').textContent = node.key;
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
  const data = experimentData[experimentId] || { name: '新的学习实验', description: '根据学习目标拆分的实验节点。' };
  touchRecentExperiment(experimentId);
  touchFolderExperiment(experimentId);
  libraryView.style.display = 'none';
  editorView.classList.remove('is-visible');
  canvasView.classList.remove('is-hidden');
  closeInspector();
  document.querySelector('#currentExperimentName').textContent = data.name;
  document.querySelector('#bubble h1').textContent = data.name;
  document.querySelector('#bubble p').textContent = data.description;
  document.querySelector('#inspector h2').textContent = data.name;
  document.querySelector('#inspector .inspector-intro').textContent = data.description;
  renderExperimentTree(data.nodes);
  renderCanvas();
}

function touchRecentExperiment(experimentId) {
  const recentButton = [...recentExperiments.querySelectorAll('.experiment-link')]
    .find((button) => button.dataset.experimentId === experimentId);
  if (!recentButton) return;
  const timeLabel = recentButton.querySelector('small');
  if (timeLabel) timeLabel.textContent = '刚刚打开';
  recentExperiments.prepend(recentButton);
}

function touchFolderExperiment(experimentId) {
  const folderButton = [...springBootExperimentList.querySelectorAll('.experiment-link')]
    .find((button) => button.dataset.experimentId === experimentId);
  if (!folderButton) return;
  const timeLabel = folderButton.querySelector('small');
  if (timeLabel) timeLabel.textContent = '刚刚打开';
  const folderRow = folderButton.closest('.experiment-row') || folderButton;
  springBootExperimentList.prepend(folderRow);
}

function updateLibraryCount() {
  const folderCards = [...document.querySelectorAll('.folder-card')];
  const totalExperiments = document.querySelectorAll('.folder-card .experiment-link').length;
  const libraryCount = document.querySelector('.library-count');
  if (libraryCount) libraryCount.textContent = `${folderCards.length} 个分组 · ${totalExperiments} 个实验`;
  if (sidebarExperimentCount) sidebarExperimentCount.textContent = totalExperiments;
  if (folderSectionCount) folderSectionCount.textContent = `${folderCards.length} 个分组`;
  folderCards.forEach((folder) => {
    const count = folder.querySelectorAll('.experiment-link').length;
    const countLabel = folder.querySelector('.folder-count');
    if (countLabel) countLabel.textContent = count ? `${count} 个实验` : '0';
  });
  experimentCount = totalExperiments;
  renderFolderNavigation();
  updateActiveFolderFromScroll();
}

function renderFolderNavigation() {
  if (!folderNavigation) return;
  const activeFolderId = folderNavigation.querySelector('.folder-nav-item.is-active')?.dataset.folderId || '';
  folderNavigation.replaceChildren();
  const folders = [...document.querySelectorAll('.folder-card')];
  if (explicitFolderSelection && !folders.some((folder) => folder.dataset.folderId === explicitFolderSelection)) {
    explicitFolderSelection = '';
    window.clearTimeout(folderSelectionTimer);
  }
  folders.forEach((folder) => {
    const button = document.createElement('button');
    button.className = `folder-nav-item${folder.dataset.folderId === activeFolderId ? ' is-active' : ''}`;
    button.type = 'button';
    button.dataset.folderId = folder.dataset.folderId;
    if (folder.dataset.folderId === activeFolderId) button.setAttribute('aria-current', 'page');
    button.innerHTML = '<span class="folder-nav-icon" aria-hidden="true">▰</span><span class="folder-nav-name"></span><span class="folder-nav-count"></span>';
    button.querySelector('.folder-nav-name').textContent = folder.dataset.folderName;
    button.querySelector('.folder-nav-count').textContent = folder.querySelectorAll('.experiment-link').length;
    folderNavigation.append(button);
  });
  if (activeFolderId && !folders.some((folder) => folder.dataset.folderId === activeFolderId)) {
    setActiveLibraryNavigation();
  }
}

function setActiveLibraryNavigation(folderId = '') {
  libraryOverviewButton.classList.toggle('is-active', !folderId);
  if (folderId) libraryOverviewButton.removeAttribute('aria-current');
  else libraryOverviewButton.setAttribute('aria-current', 'page');
  folderNavigation.querySelectorAll('.folder-nav-item').forEach((button) => {
    const isActive = button.dataset.folderId === folderId;
    button.classList.toggle('is-active', isActive);
    if (isActive) button.setAttribute('aria-current', 'page');
    else button.removeAttribute('aria-current');
  });
}

function updateActiveFolderFromScroll() {
  if (explicitFolderSelection) {
    setActiveLibraryNavigation(explicitFolderSelection);
    window.clearTimeout(folderSelectionTimer);
    folderSelectionTimer = window.setTimeout(() => { explicitFolderSelection = ''; }, 250);
    return;
  }

  const columnRect = libraryMainColumn.getBoundingClientRect();
  const stickyTop = document.querySelector('.library-topbar').getBoundingClientRect().bottom;
  const folders = [...document.querySelectorAll('.folder-card')];
  if (!folders.length) {
    setActiveLibraryNavigation();
    return;
  }
  const firstFolderTop = folders[0].getBoundingClientRect().top - columnRect.top + libraryMainColumn.scrollTop;
  if (libraryMainColumn.scrollTop < firstFolderTop - 80) {
    setActiveLibraryNavigation();
    return;
  }
  const visibleFolders = folders
    .map((folder) => ({ folder, rect: folder.getBoundingClientRect() }))
    .map(({ folder, rect }) => ({
      folder,
      visibleHeight: Math.max(0, Math.min(rect.bottom, columnRect.bottom) - Math.max(rect.top, stickyTop)),
    }))
    .filter(({ visibleHeight }) => visibleHeight > 0)
    .sort((left, right) => right.visibleHeight - left.visibleHeight);

  setActiveLibraryNavigation(visibleFolders[0]?.folder.dataset.folderId || '');
}

libraryOverviewButton.addEventListener('click', () => {
  setActiveLibraryNavigation();
  libraryMainColumn.scrollTo({ top: 0, behavior: 'smooth' });
});

folderNavigation.addEventListener('click', (event) => {
  const button = event.target.closest('.folder-nav-item');
  if (!button) return;
  explicitFolderSelection = button.dataset.folderId;
  window.clearTimeout(folderSelectionTimer);
  folderSelectionTimer = window.setTimeout(() => { explicitFolderSelection = ''; }, 700);
  setActiveLibraryNavigation(button.dataset.folderId);
  const folder = [...document.querySelectorAll('.folder-card')]
    .find((item) => item.dataset.folderId === button.dataset.folderId);
  folder?.scrollIntoView({ behavior: 'smooth', block: 'start' });
});

libraryMainColumn.addEventListener('scroll', updateActiveFolderFromScroll, { passive: true });
window.addEventListener('resize', updateActiveFolderFromScroll);

function deleteExperiment(experimentId) {
  const data = experimentData[experimentId];
  const name = data?.name || '这个实验';
  if (!window.confirm(`确定删除「${name}」吗？`)) return;
  document.querySelectorAll('.experiment-link').forEach((button) => {
    if (button.dataset.experimentId !== experimentId) return;
    const row = button.closest('.experiment-row');
    (row || button).remove();
  });
  delete experimentData[experimentId];
  updateLibraryCount();
  renderFolderManager();
}

function bindDeleteButton(button) {
  button.addEventListener('click', (event) => {
    event.stopPropagation();
    deleteExperiment(button.dataset.deleteExperimentId);
  });
}

function renderFolderManager() {
  folderManageList.replaceChildren();
  document.querySelectorAll('.folder-card').forEach((folder) => {
    const row = document.createElement('div');
    row.className = 'folder-manage-row';

    const name = document.createElement('span');
    name.className = 'folder-manage-name';
    name.textContent = folder.dataset.folderName;

    const actions = document.createElement('div');
    actions.className = 'folder-manage-actions';

    const renameButton = document.createElement('button');
    renameButton.className = 'folder-manage-button';
    renameButton.type = 'button';
    renameButton.textContent = '重命名';
    renameButton.addEventListener('click', () => {
      const nextName = window.prompt('请输入新的分组名称', folder.dataset.folderName);
      if (!nextName?.trim()) return;
      const cleanName = nextName.trim();
      folder.dataset.folderName = cleanName;
      folder.querySelector('.folder-name h2').textContent = cleanName;
      name.textContent = cleanName;
      renderFolderNavigation();
      updateLibraryCount();
    });

    const deleteButton = document.createElement('button');
    deleteButton.className = 'folder-manage-button folder-delete-button';
    deleteButton.type = 'button';
    deleteButton.textContent = '删除';
    deleteButton.addEventListener('click', () => {
      if (folder.querySelector('.experiment-link')) {
        window.alert('请先删除该分组内的实验，再删除分组。');
        return;
      }
      if (!window.confirm(`确定删除「${folder.dataset.folderName}」分组吗？`)) return;
      folder.remove();
      row.remove();
      updateLibraryCount();
    });

    actions.append(renameButton, deleteButton);
    row.append(name, actions);
    folderManageList.append(row);
  });
}

function createFolderCard(name) {
  const folder = document.createElement('section');
  folder.className = 'folder-card';
  folder.dataset.folderId = `folder-${Date.now()}`;
  folder.id = folder.dataset.folderId;
  folder.dataset.folderName = name;
  folder.innerHTML = '<div class="folder-header"><div class="folder-name"><span class="folder-icon">▰</span><div><h2></h2><p>自定义实验分组</p></div></div><span class="folder-count">0</span></div><div class="experiment-list"><div class="empty-folder"><span>＋</span><p>还没有实验</p></div></div>';
  folder.querySelector('.folder-name h2').textContent = name;
  folderGrid.append(folder);
}

openExperimentButtons.forEach((button) => {
  button.addEventListener('click', () => openExperiment(button.dataset.experimentId));
});

document.querySelectorAll('.delete-experiment').forEach(bindDeleteButton);

backToLibrary.addEventListener('click', () => {
  closeInspector();
  canvasView.classList.add('is-hidden');
  editorView.classList.remove('is-visible');
  libraryView.style.display = 'block';
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

startNodeButton.addEventListener('click', () => {
  inspector.classList.remove('is-open');
  canvasView.classList.add('is-hidden');
  editorView.classList.add('is-visible');
});

backToCanvas.addEventListener('click', () => {
  editorView.classList.remove('is-visible');
  canvasView.classList.remove('is-hidden');
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

function sendChatMessage() {
  const text = chatInput.value.trim();
  if (!text) return;
  appendChatMessage('user', '你', text);
  chatInput.value = '';
  appendChatMessage('agent', '导师', '收到。先保持当前范围，我们从 DemoApplication.java 这一小步开始。');
}

sendButton.addEventListener('click', sendChatMessage);
chatInput.addEventListener('keydown', (event) => {
  if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
    event.preventDefault();
    sendChatMessage();
  }
});

softenButton.addEventListener('click', () => {
  appendChatMessage('soften', '服软建议', '建议下一步：打开 DemoApplication.java，确认 @SpringBootApplication 位于启动类上，然后运行 ./mvnw spring-boot:run。');
  terminalOutput.insertAdjacentHTML('beforeend', '<div><span class="terminal-prompt">$</span> 服软：已定位 DemoApplication.java，等待你的下一步操作。</div>');
  terminalOutput.scrollTop = terminalOutput.scrollHeight;
});

terminalInput.addEventListener('keydown', (event) => {
  if (event.key !== 'Enter') return;
  const command = terminalInput.value.trim();
  if (!command) return;
  const line = document.createElement('div');
  line.innerHTML = `<span class="terminal-prompt">$</span> ${command.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')}`;
  terminalOutput.appendChild(line);
  terminalInput.value = '';
  terminalOutput.scrollTop = terminalOutput.scrollHeight;
});

document.querySelectorAll('.task-item').forEach((task) => {
  task.addEventListener('click', () => {
    document.querySelectorAll('.task-item').forEach((item) => item.classList.remove('is-active'));
    task.classList.add('is-active');
  });
});

function createLibraryExperimentButton(id, name, sequence, meta, recent = false) {
  const button = document.createElement('button');
  button.className = `${recent ? 'recent-item' : 'experiment-item'} experiment-link`;
  button.type = 'button';
  button.dataset.experimentId = id;
  if (recent) {
    button.innerHTML = `<span class="recent-sequence">${sequence}</span><span class="recent-copy"><strong></strong><small>${meta}</small></span><span class="recent-arrow">›</span>`;
    button.querySelector('strong').textContent = name;
    recentExperiments.prepend(button);
  } else {
    button.innerHTML = `<span class="experiment-icon">${sequence}</span><span class="experiment-copy"><strong></strong><small>${meta}</small></span><span class="experiment-arrow">›</span>`;
    button.querySelector('strong').textContent = name;
    const row = document.createElement('div');
    row.className = 'experiment-row';
    const deleteButton = document.createElement('button');
    deleteButton.className = 'delete-experiment';
    deleteButton.type = 'button';
    deleteButton.dataset.deleteExperimentId = id;
    deleteButton.setAttribute('aria-label', `删除 ${name}`);
    deleteButton.textContent = '×';
    bindDeleteButton(deleteButton);
    row.append(button, deleteButton);
    const emptyState = springBootExperimentList.querySelector('.empty-folder');
    if (emptyState) emptyState.remove();
    springBootExperimentList.prepend(row);
  }
  button.addEventListener('click', () => openExperiment(id));
}

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
  const { timeoutMs: requestedTimeoutMs = 60000, ...fetchOptions } = options;
  const timeoutMs = Number.isFinite(requestedTimeoutMs) && requestedTimeoutMs > 0
    ? requestedTimeoutMs
    : 60000;
  const timeoutController = new AbortController();
  const timeout = window.setTimeout(() => timeoutController.abort(), timeoutMs);
  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...fetchOptions,
      signal: timeoutController.signal,
      headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(payload.message || `请求失败（${response.status}）`);
    return payload;
  } catch (error) {
    if (error.name === 'AbortError') throw new Error('请求超时，请检查后端和 Harness 状态后重试。');
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
  const children = new Map();
  plan.nodes.forEach((planNode) => {
    const parent = planNode.parentKey || '__root__';
    if (!children.has(parent)) children.set(parent, []);
    children.get(parent).push(planNode);
  });
  children.forEach((items) => items.sort((a, b) => a.canvasX - b.canvasX || a.key.localeCompare(b.key)));
  let sequence = 0;
  const buildBranch = (planNode) => {
    sequence += 1;
    const branch = document.createElement('div');
    branch.className = 'plan-tree-branch';
    const node = document.createElement('div');
    node.className = 'plan-node';
    node.innerHTML = '<span class="plan-node-sequence"></span><div><strong></strong><p></p></div>';
    node.querySelector('.plan-node-sequence').textContent = sequence;
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
      createLibraryExperimentButton(id, created.name, 'start', '刚刚创建', true);
      createLibraryExperimentButton(id, created.name, 'start', `难度 ${created.difficulty} · ${created.nodes.length} 个节点`);
      updateLibraryCount();
      createBusy = false;
      closeCreateDialogPanel();
      learningGoal.value = '';
      createPlanReady = false;
      openExperiment(id);
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

function closeFolderDialogPanel() {
  folderDialog.classList.remove('is-open');
}

manageFoldersButton.addEventListener('click', () => {
  renderFolderManager();
  folderDialog.classList.add('is-open');
  newFolderName.focus();
});
closeFolderDialog.addEventListener('click', closeFolderDialogPanel);
folderDialog.addEventListener('click', (event) => {
  if (event.target === folderDialog) closeFolderDialogPanel();
});
addFolderButton.addEventListener('click', () => {
  const name = newFolderName.value.trim();
  if (!name) {
    newFolderName.focus();
    return;
  }
  createFolderCard(name);
  newFolderName.value = '';
  updateLibraryCount();
  renderFolderManager();
  newFolderName.focus();
});
newFolderName.addEventListener('keydown', (event) => {
  if (event.key === 'Enter') addFolderButton.click();
});

window.addEventListener('resize', () => {
  if (!panState && !bubbleDragState) renderCanvas();
});

renderCanvas();
updateLibraryCount();
