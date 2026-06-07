<template>
  <div class="server-m-container">
    <el-row :gutter="16" class="main-row">
      <!-- 左侧：系统状态面板 -->
      <el-col :md="8" :sm="24" class="left-panel">
        <!-- 系统资源卡片 -->
        <div class="metrics-card">
          <div class="metrics-card__header">
            <span class="metrics-card__icon">📊</span>
            <span class="metrics-card__title">系统资源</span>
            <el-tag
              :type="agentConnected ? 'success' : 'danger'"
              size="small"
              effect="dark"
              class="agent-tag"
            >
              Agent {{ agentConnected ? '在线' : '离线' }}
            </el-tag>
            <button class="refresh-btn" @click="refreshAll" :class="{ spinning: refreshing }">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 4 23 10 17 10"></polyline>
                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
              </svg>
            </button>
          </div>
          <div class="metrics-card__body">
            <!-- CPU -->
            <div class="metric-item">
              <div class="metric-item__header">
                <span class="metric-item__label">CPU</span>
                <span class="metric-item__value">{{ metrics.cpuUsage }}%</span>
              </div>
              <div class="metric-bar">
                <div
                  class="metric-bar__fill"
                  :style="{ width: metrics.cpuUsage + '%' }"
                  :class="getBarColor(metrics.cpuUsage)"
                ></div>
              </div>
              <span class="metric-item__detail">{{ metrics.cpuCores || '--' }} 核心</span>
            </div>

            <!-- 内存 -->
            <div class="metric-item">
              <div class="metric-item__header">
                <span class="metric-item__label">内存</span>
                <span class="metric-item__value">{{ metrics.memUsage }}%</span>
              </div>
              <div class="metric-bar">
                <div
                  class="metric-bar__fill"
                  :style="{ width: metrics.memUsage + '%' }"
                  :class="getBarColor(metrics.memUsage)"
                ></div>
              </div>
              <span class="metric-item__detail">{{ metrics.memUsed }} / {{ metrics.memTotal }}</span>
            </div>

            <!-- 网络 -->
            <div class="metric-item">
              <div class="metric-item__header">
                <span class="metric-item__label">网络</span>
              </div>
              <div class="net-stats">
                <div class="net-stat">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2">
                    <polyline points="7 13 12 18 17 13"></polyline>
                    <line x1="12" y1="6" x2="12" y2="18"></line>
                  </svg>
                  <span>{{ metrics.netIn }} KB/s</span>
                </div>
                <div class="net-stat">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#409eff" stroke-width="2">
                    <polyline points="17 11 12 6 7 11"></polyline>
                    <line x1="12" y1="18" x2="12" y2="6"></line>
                  </svg>
                  <span>{{ metrics.netOut }} KB/s</span>
                </div>
              </div>
            </div>

            <div class="metrics-footer">
              <span class="last-update">更新: {{ lastUpdateTime }}</span>
            </div>
          </div>
        </div>

        <!-- 快速操作卡片 -->
        <div class="actions-card">
          <div class="actions-card__header">
            <span class="actions-card__icon">⚡</span>
            <span class="actions-card__title">快速操作</span>
            <el-select
              v-model="selectedServerId"
              placeholder="选择服务器"
              size="small"
              class="server-select"
              @change="handleServerChange"
            >
              <el-option
                v-for="s in serverList"
                :key="s.serverId"
                :label="s.name || s.serverId"
                :value="s.serverId"
              >
                <span>{{ s.name || s.serverId }}</span>
                <el-tag
                  :type="s.status === 'RUNNING' ? 'success' : 'info'"
                  size="small"
                  style="margin-left: 8px"
                >
                  {{ s.status === 'RUNNING' ? '运行中' : '已停止' }}
                </el-tag>
              </el-option>
            </el-select>
          </div>
          <div class="actions-card__body">
            <div class="action-grid">
              <button
                class="action-btn action-btn--start"
                :disabled="isServerRunning || !agentConnected"
                @click="handleStart"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polygon points="5 3 19 12 5 21 5 3"></polygon>
                </svg>
                <span>启动</span>
              </button>
              <button
                class="action-btn action-btn--restart"
                :disabled="!isServerRunning || !agentConnected"
                @click="handleRestart"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="23 4 23 10 17 10"></polyline>
                  <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                </svg>
                <span>重启</span>
              </button>
              <button
                class="action-btn action-btn--stop"
                :disabled="!isServerRunning || !agentConnected"
                @click="handleStop"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="6" y="6" width="12" height="12" rx="2"></rect>
                </svg>
                <span>停止</span>
              </button>
              <button class="action-btn action-btn--console" @click="openConsoleDialog">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                  <line x1="8" y1="21" x2="16" y2="21"></line>
                  <line x1="12" y1="17" x2="12" y2="21"></line>
                </svg>
                <span>控制台</span>
              </button>
            </div>
            <button class="manage-server-btn" @click="showServerManager = true">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="3"></circle>
                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
              </svg>
              <span>服务器实例管理</span>
            </button>
          </div>
        </div>

        <!-- 智能体推送卡片 -->
        <div class="agent-push-card">
          <div class="agent-push-card__header">
            <span class="agent-push-card__icon">📬</span>
            <span class="agent-push-card__title">智能体推送</span>
            <span class="agent-push-card__badge" v-if="agentPushMessages.length > 0">
              {{ agentPushMessages.length }}
            </span>
          </div>
          <div class="agent-push-card__body">
            <div v-if="agentPushMessages.length === 0" class="agent-push-empty">
              暂无推送消息
            </div>
            <div
              v-for="(msg, index) in agentPushMessages"
              :key="index"
              class="agent-push-item"
              :class="'agent-push-item--' + msg.type"
            >
              <div class="agent-push-item__header">
                <span class="agent-push-item__type">{{ getTypeLabel(msg.type) }}</span>
                <span class="agent-push-item__time">{{ msg.time }}</span>
              </div>
              <div class="agent-push-item__content">{{ msg.content }}</div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右侧：智能体对话框 -->
      <el-col :md="16" :sm="24" class="right-panel">
        <div class="agent-chat-panel">
          <AIchat />
        </div>
      </el-col>
    </el-row>

    <!-- 游戏控制台弹窗 -->
    <el-dialog
      v-model="consoleDialogVisible"
      title="游戏控制台"
      width="70%"
      :close-on-click-modal="false"
      @opened="handleConsoleDialogOpened"
      @closed="handleConsoleDialogClosed"
    >
      <div class="console-dialog-content">
        <div ref="consoleRef" class="xterm-container"></div>
        <div class="console-input-wrapper">
          <span class="prompt">></span>
          <input
            v-model="commandInput"
            @keyup.enter="sendConsoleCommand"
            placeholder="输入指令并回车 (例如: list, tp, give...)"
            class="console-input"
          />
          <el-button type="primary" size="small" @click="sendConsoleCommand">执行</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 服务器实例管理抽屉 -->
    <el-drawer
      v-model="showServerManager"
      title="服务器实例管理"
      direction="rtl"
      size="500px"
    >
      <div class="server-manager-content">
        <div class="server-manager-toolbar">
          <el-button type="primary" @click="showAddServerDialog = true">
            <el-icon><Plus /></el-icon>
            创建新实例
          </el-button>
          <el-button @click="showAddExistingDialog = true">
            <el-icon><Link /></el-icon>
            添加已有实例
          </el-button>
          <el-button @click="fetchServerList">
            <el-icon><Refresh /></el-icon>
            刷新列表
          </el-button>
        </div>

        <div class="server-instance-list">
          <div
            v-for="server in serverList"
            :key="server.serverId"
            class="server-instance-item"
            :class="{ active: server.serverId === selectedServerId }"
            @click="selectServer(server.serverId)"
          >
            <div class="instance-header">
              <div class="instance-name">
                <span class="status-dot" :class="server.status === 'RUNNING' ? 'running' : 'stopped'"></span>
                <span>{{ server.name || server.serverId }}</span>
              </div>
              <el-tag
                :type="server.status === 'RUNNING' ? 'success' : 'info'"
                size="small"
              >
                {{ server.status === 'RUNNING' ? '运行中' : '已停止' }}
              </el-tag>
            </div>
            <div class="instance-info">
              <span>ID: {{ server.serverId }}</span>
              <span>端口: {{ server.port || '-' }}</span>
              <span>PID: {{ server.pid > 0 ? server.pid : '-' }}</span>
              <el-tag v-if="server.directoryValid === false" type="warning" size="small" effect="plain">
                目录无效
              </el-tag>
            </div>
            <div class="instance-actions">
              <el-button
                v-if="server.status !== 'RUNNING'"
                type="success"
                size="small"
                @click.stop="handleStartServer(server.serverId)"
              >
                启动
              </el-button>
              <el-button
                v-if="server.status === 'RUNNING'"
                type="danger"
                size="small"
                @click.stop="handleStopServer(server.serverId)"
              >
                停止
              </el-button>
              <el-button
                type="primary"
                size="small"
                text
                @click.stop="goToServerDetail(server.serverId)"
              >
                详情
              </el-button>
              <el-button
                v-if="server.status !== 'RUNNING'"
                type="danger"
                size="small"
                text
                @click.stop="handleDeleteServer(server)"
              >
                删除
              </el-button>
            </div>
          </div>

          <div v-if="serverList.length === 0" class="empty-tip">
            <p>暂无服务器实例</p>
            <p class="sub-tip">点击上方按钮创建或添加服务器</p>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 创建/添加服务器弹窗 -->
    <AddServerDialog v-model:visible="showAddServerDialog" @added="handleServerAdded" />

    <!-- 添加已有实例弹窗 -->
    <el-dialog
      v-model="showAddExistingDialog"
      title="添加已有实例"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="addExistingForm" label-width="100px" label-position="right">
        <el-form-item label="服务器名称" required>
          <el-input v-model="addExistingForm.name" placeholder="例如：生存服务器" />
        </el-form-item>
        <el-form-item label="服务器ID" required>
          <el-input v-model="addExistingForm.serverId" placeholder="例如：survival（英文唯一标识）" />
        </el-form-item>
        <el-form-item label="服务器目录" required>
          <el-input v-model="addExistingForm.directory" placeholder="例如：/opt/mcserver/survival" />
        </el-form-item>
        <el-form-item label="JAR 文件">
          <el-input v-model="addExistingForm.jarFile" placeholder="server.jar" />
        </el-form-item>
        <el-form-item label="服务端口">
          <el-input-number v-model="addExistingForm.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="最大玩家">
          <el-input-number v-model="addExistingForm.maxPlayers" :min="1" :max="1000" />
        </el-form-item>
        <el-form-item label="Java 参数">
          <el-input v-model="addExistingForm.javaArgs" placeholder="-Xms1G -Xmx2G" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="addExistingForm.description" type="textarea" :rows="2" placeholder="可选描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddExistingDialog = false">取消</el-button>
        <el-button type="primary" :loading="addExistingLoading" @click="handleAddExisting">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onActivated, onDeactivated, onUnmounted, nextTick, shallowRef } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Refresh, Link } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import client, { onReconnect } from '@/utils/wsClient';
import AIchat from "@/pages/admin/admin-menu/serverM/agentChat.vue";
import AddServerDialog from "@/pages/admin/admin-menu/serverM/AddServerDialog.vue";
import { Terminal } from 'xterm';
import { FitAddon } from 'xterm-addon-fit';
import 'xterm/css/xterm.css';

const router = useRouter();

// ==================== 系统数据 ====================
const metrics = ref({
  cpuUsage: 0,
  memUsage: 0,
  memTotal: '0 GB',
  memUsed: '0 GB',
  netIn: 0,
  netOut: 0,
  cpuCores: 0,
});

const lastUpdateTime = ref('--:--:--');
const refreshing = ref(false);

// ==================== 服务器实例 ====================
const serverList = ref([]);
const selectedServerId = ref('default');
const showAddServerDialog = ref(false);
const showAddExistingDialog = ref(false);
const showServerManager = ref(false);

// ==================== 添加已有实例表单 ====================
const addExistingForm = ref({
  name: '',
  serverId: '',
  directory: '',
  jarFile: 'server.jar',
  port: 25565,
  javaArgs: '-Xms1G -Xmx2G',
  maxPlayers: 20,
  description: ''
});
const addExistingLoading = ref(false);

// ==================== 服务器控制 ====================
const serverStatus = ref(null);
const agentConnected = ref(false);
const starting = ref(false);
const stopping = ref(false);
const restarting = ref(false);

// ==================== 弹窗控制 ====================
const consoleDialogVisible = ref(false);

// ==================== 智能体推送消息 ====================
const agentPushMessages = ref([]);

// ==================== 控制台 ====================
const consoleRef = ref(null);
const commandInput = ref('');
const consoleInstance = shallowRef(null);
const fitAddon = new FitAddon();
let consolePollingTimer = null;
let lastConsoleLine = -1;

// ==================== 计算属性 ====================
const isServerRunning = computed(() => {
  if (!serverStatus.value) return false;
  // 兼容两种格式：agent 返回 status 字符串，旧接口返回 running 布尔
  if (serverStatus.value.status) {
    return serverStatus.value.status === 'RUNNING';
  }
  return !!serverStatus.value.running;
});

// ==================== 工具函数 ====================
const getBarColor = (value) => {
  if (value < 60) return 'bar--normal';
  if (value < 80) return 'bar--warning';
  return 'bar--danger';
};

const getTypeLabel = (type) => {
  const map = { info: '信息', warning: '警告', error: '错误', success: '成功' };
  return map[type] || '信息';
};

const parseMinecraftColors = (text) => {
  const colorMap = {
    '0': '\x1b[30m', '1': '\x1b[34m', '2': '\x1b[32m', '3': '\x1b[36m',
    '4': '\x1b[31m', '5': '\x1b[35m', '6': '\x1b[33m', '7': '\x1b[37m',
    '8': '\x1b[90m', '9': '\x1b[94m', 'a': '\x1b[92m', 'b': '\x1b[96m',
    'c': '\x1b[91m', 'd': '\x1b[95m', 'e': '\x1b[93m', 'f': '\x1b[97m',
    'l': '\x1b[1m',  'o': '\x1b[3m',  'n': '\x1b[4m',  'm': '\x1b[9m',
    'r': '\x1b[0m'
  };
  return text.replace(/§([0-9a-fk-or])/g, (match, code) => colorMap[code] || '');
};

// ==================== 服务器实例管理 ====================
const fetchServerList = async () => {
  try {
    const res = await request.get('/api/server/list', { silent: true });
    if (res.code === 2000 && Array.isArray(res.data) && res.data.length > 0) {
      serverList.value = res.data;
      // 如果当前选中的服务器不在列表中，选择第一个
      if (!res.data.find(s => s.serverId === selectedServerId.value)) {
        selectedServerId.value = res.data[0].serverId;
      }
    } else {
      serverList.value = [];
      selectedServerId.value = '';
    }
  } catch (e) {
    serverList.value = [];
    selectedServerId.value = '';
  }
  // 确保 selectedServerId 有效
  if (serverList.value.length > 0 && !serverList.value.find(s => s.serverId === selectedServerId.value)) {
    selectedServerId.value = serverList.value[0].serverId;
  }
  // 同步刷新当前选中服务器的状态
  if (selectedServerId.value) {
    fetchServerStatus();
  } else {
    serverStatus.value = null;
  }
};

const handleServerChange = (serverId) => {
  selectedServerId.value = serverId;
  fetchServerStatus();
};

const selectServer = (serverId) => {
  selectedServerId.value = serverId;
  fetchServerStatus();
  showServerManager.value = false;
  ElMessage.success(`已切换到服务器: ${serverId}`);
};

const goToServerDetail = (serverId) => {
  showServerManager.value = false;
  router.push(`/adminPanel/serverM/${serverId}`);
};

const handleServerAdded = () => {
  fetchServerList();
  showAddServerDialog.value = false;
};

const handleAddExisting = async () => {
  if (!addExistingForm.value.name || !addExistingForm.value.serverId || !addExistingForm.value.directory) {
    ElMessage.warning('请填写必填字段：服务器名称、服务器ID、服务器目录');
    return;
  }

  addExistingLoading.value = true;
  try {
    const res = await request.post('/api/server/register', {
      serverId: addExistingForm.value.serverId,
      name: addExistingForm.value.name,
      directory: addExistingForm.value.directory,
      jarFile: addExistingForm.value.jarFile,
      port: addExistingForm.value.port,
      javaArgs: addExistingForm.value.javaArgs,
      maxPlayers: addExistingForm.value.maxPlayers,
      description: addExistingForm.value.description
    });
    if (res.code === 2000) {
      ElMessage.success('实例添加成功');
      showAddExistingDialog.value = false;
      addExistingForm.value = {
        name: '', serverId: '', directory: '', jarFile: 'server.jar',
        port: 25565, javaArgs: '-Xms1G -Xmx2G', maxPlayers: 20, description: ''
      };
      fetchServerList();
    } else {
      ElMessage.error(res.msg || '添加失败');
    }
  } catch (e) {
    ElMessage.error(e.friendlyMsg || '操作失败');
  } finally {
    addExistingLoading.value = false;
  }
};

const handleStartServer = async (serverId) => {
  try {
    const res = await request.post(`/api/server/${serverId}/start`);
    if (res.code === 2000) {
      ElMessage.success('启动指令已发送');
      setTimeout(() => fetchServerList(), 3000);
    } else {
      ElMessage.error(res.msg || '启动失败');
    }
  } catch (e) {
    ElMessage.error(e.friendlyMsg || '操作失败');
  }
};

const handleStopServer = async (serverId) => {
  try {
    await ElMessageBox.confirm('确定要停止该服务器吗？', '停止确认', {
      confirmButtonText: '停止',
      type: 'warning'
    });
  } catch { return; }

  try {
    const res = await request.post(`/api/server/${serverId}/stop`);
    if (res.code === 2000) {
      ElMessage.success('停止指令已发送');
      setTimeout(() => fetchServerList(), 3000);
    } else {
      ElMessage.error(res.msg || '停止失败');
    }
  } catch (e) {
    ElMessage.error(e.friendlyMsg || '操作失败');
  }
};

const handleDeleteServer = async (server) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除服务器「${server.name || server.serverId}」吗？\n此操作将删除服务器目录下的所有文件，且不可恢复！`,
      '删除确认',
      { confirmButtonText: '删除', type: 'error', confirmButtonClass: 'el-button--danger' }
    );
  } catch { return; }

  try {
    const res = await request.delete(`/api/server/${server.serverId}`);
    if (res.code === 2000) {
      ElMessage.success('服务器已删除');
      fetchServerList();
    } else {
      ElMessage.error(res.msg || '删除失败');
    }
  } catch (e) {
    ElMessage.error(e.friendlyMsg || '操作失败');
  }
};

// ==================== 弹窗操作 ====================
const openConsoleDialog = () => {
  consoleDialogVisible.value = true;
};

const handleConsoleDialogOpened = () => {
  nextTick(() => initConsole());
};

const handleConsoleDialogClosed = () => {
  if (consoleInstance.value) {
    consoleInstance.value.dispose();
    consoleInstance.value = null;
  }
  if (consolePollingTimer) {
    clearInterval(consolePollingTimer);
    consolePollingTimer = null;
  }
  lastConsoleLine = -1;
};

// ==================== 初始化控制台 ====================
const initConsole = () => {
  if (!consoleRef.value) return;
  if (consoleInstance.value) {
    consoleInstance.value.dispose();
  }

  const term = new Terminal({
    cursorBlink: true,
    fontSize: 14,
    fontFamily: 'Menlo, Monaco, "Courier New", monospace',
    theme: {
      background: '#1a1a1a',
      foreground: '#d4d4d4',
      cursor: '#67c23a'
    },
    rows: 20,
    convertEol: true
  });

  term.loadAddon(fitAddon);
  term.open(consoleRef.value);
  nextTick(() => fitAddon.fit());

  term.writeln('\x1b[1;32m[MCSN]\x1b[0m 游戏控制台已连接。');
  term.writeln('\x1b[90m正在加载控制台日志...\x1b[0m');
  consoleInstance.value = term;

  // 首次加载控制台日志
  fetchConsoleLogs();

  // 开始轮询控制台日志
  consolePollingTimer = setInterval(fetchConsoleLogs, 2000);
};

// ==================== 获取控制台日志 ====================
const fetchConsoleLogs = async () => {
  try {
    const res = await request.get(`/api/server/${selectedServerId.value}/console`, {
      silent: true,
      params: { since: lastConsoleLine }
    });
    if (res.code === 2000 && res.data) {
      const { lines, latestLineNumber } = res.data;
      if (lines && lines.length > 0 && consoleInstance.value) {
        lines.forEach(line => {
          const coloredLine = parseMinecraftColors(line.content || line);
          consoleInstance.value.writeln(coloredLine);
        });
        lastConsoleLine = latestLineNumber;
      }
    }
  } catch (e) {
    // 静默处理，避免频繁报错
  }
};

// ==================== 发送控制台命令 ====================
const sendConsoleCommand = async () => {
  if (!commandInput.value.trim()) return;
  const cmd = commandInput.value;
  const term = consoleInstance.value;

  if (term) {
    term.writeln(`\x1b[1;33m> ${cmd}\x1b[0m`);
  }
  commandInput.value = '';

  try {
    const res = await request.post(`/api/server/${selectedServerId.value}/command`, null, {
      params: { cmd }
    });
    if (res.code === 2000) {
      if (term) {
        term.writeln(`\x1b[1;32m[OK]\x1b[0m ${res.data || '命令已发送'}`);
      }
    } else {
      if (term) {
        term.writeln(`\x1b[1;31m[错误] ${res.msg}\x1b[0m`);
      }
    }
  } catch (error) {
    if (term) {
      term.writeln(`\x1b[1;31m[系统异常] 无法访问后端接口\x1b[0m`);
    }
  }
};

// ==================== 数据获取 ====================
const checkAgentHealth = async () => {
  try {
    const res = await request.get('/api/server/agent/health', { silent: true });
    if (res.code === 2000 && res.data?.online) {
      agentConnected.value = true;
    } else {
      agentConnected.value = false;
    }
  } catch (error) {
    agentConnected.value = false;
  }
};

const fetchServerStatus = async () => {
  if (!selectedServerId.value) {
    serverStatus.value = null;
    return;
  }
  try {
    const res = await request.get(`/api/server/${selectedServerId.value}/status`, { silent: true });
    if (res.code === 2000) {
      serverStatus.value = res.data;
    } else {
      serverStatus.value = null;
    }
  } catch (error) {
    serverStatus.value = null;
  }
};

const refreshAll = async () => {
  refreshing.value = true;
  await Promise.all([
    checkAgentHealth(),
    fetchServerList()
  ]);
  lastUpdateTime.value = new Date().toLocaleTimeString();
  setTimeout(() => { refreshing.value = false; }, 600);
  ElMessage.success('数据已刷新');
};

// ==================== 服务器控制 ====================
const handleStart = async () => {
  try {
    await ElMessageBox.confirm('确定要启动 Minecraft 服务器吗？', '启动确认', {
      confirmButtonText: '启动', type: 'info'
    });
  } catch { return; }

  starting.value = true;
  try {
    const res = await request.post(`/api/server/${selectedServerId.value}/start`);
    if (res.code === 2000) {
      ElMessage.success('启动指令已发送');
      setTimeout(() => fetchServerStatus(), 5000);
    } else {
      ElMessage.error(res.msg || '启动失败');
    }
  } catch (e) { ElMessage.error(e.friendlyMsg || '操作失败'); }
  finally { starting.value = false; }
};

const handleStop = async () => {
  try {
    await ElMessageBox.confirm('确定要停止服务器吗？在线玩家将被断开。', '停止确认', {
      confirmButtonText: '停止', type: 'warning'
    });
  } catch { return; }

  stopping.value = true;
  try {
    const res = await request.post(`/api/server/${selectedServerId.value}/stop`);
    if (res.code === 2000) {
      ElMessage.success('停止指令已发送');
      setTimeout(() => fetchServerStatus(), 3000);
    } else {
      ElMessage.error(res.msg || '停止失败');
    }
  } catch (e) { ElMessage.error(e.friendlyMsg || '操作失败'); }
  finally { stopping.value = false; }
};

const handleRestart = async () => {
  try {
    await ElMessageBox.confirm('确定要重启服务器吗？', '重启确认', {
      confirmButtonText: '重启', type: 'warning'
    });
  } catch { return; }

  restarting.value = true;
  try {
    const res = await request.post(`/api/server/${selectedServerId.value}/restart`);
    if (res.code === 2000) {
      ElMessage.success('重启指令已发送');
      setTimeout(() => fetchServerStatus(), 8000);
    } else {
      ElMessage.error(res.msg || '重启失败');
    }
  } catch (e) { ElMessage.error(e.friendlyMsg || '操作失败'); }
  finally { restarting.value = false; }
};

// ==================== WebSocket 订阅 ====================
let statusFallbackTimer = null;
let serverPollingTimer = null;
let unregisterReconnect = null;

const subscribeAll = () => {
  // 订阅系统性能指标
  client.subscribe('/topic/server/metrics', (message) => {
    try {
      const data = JSON.parse(message.body);
      if (data) {
        metrics.value = { ...data };
        lastUpdateTime.value = new Date().toLocaleTimeString();
      }
    } catch (e) {
      console.error('处理监控数据失败:', e);
    }
  });

  // 订阅智能体推送消息
  client.subscribe('/topic/agent/push', (message) => {
    try {
      const data = JSON.parse(message.body);
      if (data) {
        agentPushMessages.value.unshift({
          type: data.type || 'info',
          content: data.content || data.message || '',
          time: new Date().toLocaleTimeString()
        });
        // 最多保留 20 条
        if (agentPushMessages.value.length > 20) {
          agentPushMessages.value.pop();
        }
      }
    } catch (e) {
      console.error('处理智能体推送失败:', e);
    }
  });
};

onActivated(() => {
  // 清理可能残留的旧资源（keep-alive 场景下 onDeactivated 不清理）
  if (statusFallbackTimer) { clearInterval(statusFallbackTimer); statusFallbackTimer = null; }
  if (serverPollingTimer) { clearInterval(serverPollingTimer); serverPollingTimer = null; }
  if (unregisterReconnect) { unregisterReconnect(); unregisterReconnect = null; }

  if (!client.active) {
    client.activate();
  }

  // 首次连接时订阅
  if (client.connected) {
    subscribeAll();
  }

  // 重连时重新订阅（不篡改 onConnect）
  unregisterReconnect = onReconnect(() => {
    subscribeAll();
    checkAgentHealth();
    fetchServerList();
  });

  // HTTP 降级轮询
  statusFallbackTimer = setInterval(() => {
    if (!client.connected) {
      checkAgentHealth();
      fetchServerList(); // 内部会调用 fetchServerStatus
    }
  }, 30000);

  // 每 3 秒刷新服务器状态
  serverPollingTimer = setInterval(() => {
    fetchServerList();
  }, 3000);

  // 首次加载
  checkAgentHealth();
  fetchServerList(); // 内部会调用 fetchServerStatus
  lastUpdateTime.value = new Date().toLocaleTimeString();
});

onDeactivated(() => {
  if (statusFallbackTimer) { clearInterval(statusFallbackTimer); statusFallbackTimer = null; }
  if (serverPollingTimer) { clearInterval(serverPollingTimer); serverPollingTimer = null; }
  if (unregisterReconnect) { unregisterReconnect(); unregisterReconnect = null; }
  if (consolePollingTimer) { clearInterval(consolePollingTimer); consolePollingTimer = null; }
});

onUnmounted(() => {
  if (statusFallbackTimer) {
    clearInterval(statusFallbackTimer);
    statusFallbackTimer = null;
  }
  if (consolePollingTimer) {
    clearInterval(consolePollingTimer);
    consolePollingTimer = null;
  }
  if (serverPollingTimer) {
    clearInterval(serverPollingTimer);
    serverPollingTimer = null;
  }
  if (unregisterReconnect) {
    unregisterReconnect();
    unregisterReconnect = null;
  }
});
</script>

<style scoped>
.server-m-container {
  padding: 20px;
  background: #f5f7fa;
  height: calc(100vh - 100px);
  overflow: hidden;
}

.main-row {
  height: 100%;
}

.left-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

.right-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

/* ==================== 通用卡片样式 ==================== */
.metrics-card,
.actions-card,
.agent-push-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.metrics-card__header,
.actions-card__header,
.agent-push-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #f2f2f7;
  font-weight: 600;
  font-size: 14px;
  color: #1d1d1f;
}

.metrics-card__icon,
.actions-card__icon,
.agent-push-card__icon {
  font-size: 16px;
}

.metrics-card__body,
.actions-card__body,
.agent-push-card__body {
  padding: 12px 16px;
}

.agent-tag {
  margin-left: auto;
}

/* ==================== 系统资源 ==================== */
.refresh-btn {
  margin-left: auto;
  background: none;
  border: none;
  cursor: pointer;
  color: #86868b;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
}

.refresh-btn:hover {
  background: #f5f5f7;
  color: #1d1d1f;
}

.refresh-btn.spinning svg {
  animation: spin 0.6s linear;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.metric-item {
  margin-bottom: 12px;
}

.metric-item:last-of-type {
  margin-bottom: 8px;
}

.metric-item__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.metric-item__label {
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
}

.metric-item__value {
  font-size: 13px;
  font-weight: 700;
  color: #1d1d1f;
  font-variant-numeric: tabular-nums;
}

.metric-bar {
  height: 6px;
  background: #f2f2f7;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 4px;
}

.metric-bar__fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}

.bar--normal { background: #34c759; }
.bar--warning { background: #ff9500; }
.bar--danger { background: #ff3b30; }

.metric-item__detail {
  font-size: 11px;
  color: #aeaeb2;
}

.net-stats {
  display: flex;
  gap: 16px;
}

.net-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
  font-variant-numeric: tabular-nums;
}

.metrics-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid #f2f2f7;
  margin-top: 4px;
}

.last-update {
  font-size: 11px;
  color: #aeaeb2;
}

/* ==================== 快速操作 ==================== */
.actions-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.server-select {
  margin-left: auto;
  width: 140px;
}

.action-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 10px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid #d1d1d6;
  border-radius: 8px;
  background: #ffffff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  color: #1d1d1f;
}

.action-btn:hover:not(:disabled) {
  border-color: #0071e3;
  background: rgba(0, 113, 227, 0.03);
}

.action-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.action-btn--start:hover:not(:disabled) {
  border-color: #34c759;
  color: #34c759;
}

.action-btn--restart:hover:not(:disabled) {
  border-color: #ff9500;
  color: #ff9500;
}

.action-btn--stop:hover:not(:disabled) {
  border-color: #ff3b30;
  color: #ff3b30;
}

.action-btn--console:hover:not(:disabled) {
  border-color: #5856d6;
  color: #5856d6;
}

.manage-server-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 8px 12px;
  border: 1px dashed #c0c4cc;
  border-radius: 8px;
  background: #fafafa;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
}

.manage-server-btn:hover {
  border-color: #0071e3;
  color: #0071e3;
  background: rgba(0, 113, 227, 0.03);
}

/* ==================== 智能体推送 ==================== */
.agent-push-card__header {
  position: relative;
}

.agent-push-card__badge {
  margin-left: auto;
  background: #ff3b30;
  color: white;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

.agent-push-card__body {
  max-height: 200px;
  overflow-y: auto;
}

.agent-push-empty {
  text-align: center;
  color: #aeaeb2;
  font-size: 13px;
  padding: 20px 0;
}

.agent-push-item {
  padding: 10px 0;
  border-bottom: 1px solid #f2f2f7;
}

.agent-push-item:last-child {
  border-bottom: none;
}

.agent-push-item__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.agent-push-item__type {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.agent-push-item--info .agent-push-item__type {
  background: rgba(0, 113, 227, 0.1);
  color: #0071e3;
}

.agent-push-item--warning .agent-push-item__type {
  background: rgba(255, 149, 0, 0.1);
  color: #ff9500;
}

.agent-push-item--error .agent-push-item__type {
  background: rgba(255, 59, 48, 0.1);
  color: #ff3b30;
}

.agent-push-item--success .agent-push-item__type {
  background: rgba(52, 199, 89, 0.1);
  color: #34c759;
}

.agent-push-item__time {
  font-size: 11px;
  color: #aeaeb2;
}

.agent-push-item__content {
  font-size: 13px;
  color: #424245;
  line-height: 1.5;
}

/* ==================== 右侧对话面板 ==================== */
.agent-chat-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ==================== 游戏控制台 ==================== */
.console-dialog-content {
  background-color: #1a1a1a;
  border-radius: 8px;
  overflow: hidden;
}

.xterm-container {
  padding: 15px;
  background-color: #1a1a1a;
  height: 400px;
}

.console-input-wrapper {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  background-color: #242424;
  border-top: 1px solid #333;
  gap: 12px;
}

.prompt {
  color: #67c23a;
  font-family: monospace;
  font-weight: bold;
}

.console-input {
  flex: 1;
  background: transparent;
  border: none;
  color: #ffffff;
  outline: none;
  font-family: 'Menlo', monospace;
  font-size: 14px;
}

/* ==================== 服务器实例管理 ==================== */
.server-manager-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.server-manager-toolbar {
  display: flex;
  gap: 10px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.server-instance-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.server-instance-item {
  padding: 14px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.server-instance-item:hover {
  border-color: #0071e3;
  background: rgba(0, 113, 227, 0.02);
}

.server-instance-item.active {
  border-color: #0071e3;
  background: rgba(0, 113, 227, 0.05);
}

.instance-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.instance-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.running {
  background: #34c759;
  box-shadow: 0 0 6px rgba(52, 199, 89, 0.5);
  animation: pulse 2s infinite;
}

.status-dot.stopped {
  background: #8e8e93;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.instance-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.instance-actions {
  display: flex;
  gap: 8px;
}

.empty-tip {
  text-align: center;
  color: #909399;
  padding: 60px 20px;
}

.empty-tip p {
  margin: 0;
}

.empty-tip .sub-tip {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 8px;
}

/* ==================== 响应式 ==================== */
@media (max-width: 992px) {
  .left-panel {
    margin-bottom: 16px;
  }

  .agent-chat-panel {
    min-height: 500px;
  }

  .right-panel {
    height: auto;
    min-height: 500px;
  }

  .server-m-container {
    height: auto;
    overflow: auto;
  }
}
</style>
