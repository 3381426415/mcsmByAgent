<template>
  <showCard title="服务器管理" subtitle="查看主机状态和操作主机">
    <div class="server-management-wrapper">
      <!-- 概览卡片 -->
      <el-row :gutter="20" class="overview-row">
        <!-- 服务器状态概览 -->
        <el-col :xs="24" :sm="12" :lg="8">
          <el-card class="overview-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>🎮 服务器状态</span>
              </div>
            </template>
            <div class="overview-content">
              <div class="status-indicator">
                <span class="status-dot" :class="serverStatus?.running ? 'running' : 'stopped'"></span>
                <span class="status-text">{{ serverStatus?.running ? '运行中' : '已停止' }}</span>
              </div>
              <div class="quick-info" v-if="serverStatus?.running">
                <div class="info-row">
                  <span class="label">玩家数:</span>
                  <span class="value">{{ serverInfo.gamePlayerNums || 0 }}</span>
                </div>
                <div class="info-row">
                  <span class="label">TPS:</span>
                  <span class="value">{{ serverInfo.gameTps || 'N/A' }}</span>
                </div>
                <div class="info-row">
                  <span class="label">PID:</span>
                  <span class="value">{{ serverStatus.pid }}</span>
                </div>
              </div>
              <div class="agent-status">
                <span class="label">Agent:</span>
                <el-tag :type="agentConnected ? 'success' : 'danger'" size="small">
                  {{ agentConnected ? '已连接' : '未连接' }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- CPU 概览 -->
        <el-col :xs="24" :sm="12" :lg="8">
          <el-card class="overview-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>💻 系统负载</span>
              </div>
            </template>
            <div class="overview-content">
              <div class="mini-chart-container">
                <div class="metric-row">
                  <span class="metric-label">CPU</span>
                  <el-progress
                    :percentage="metrics.cpuUsage"
                    :color="customColors"
                    :stroke-width="8"
                  />
                </div>
                <div class="metric-row">
                  <span class="metric-label">内存</span>
                  <el-progress
                    :percentage="metrics.memUsage"
                    :status="metrics.memUsage > 80 ? 'exception' : ''"
                    :stroke-width="8"
                  />
                </div>
                <div class="metric-detail">
                  {{ metrics.memUsed }} / {{ metrics.memTotal }}
                </div>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 快速操作 -->
        <el-col :xs="24" :sm="24" :lg="8">
          <el-card class="overview-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>⚡ 快速操作</span>
              </div>
            </template>
            <div class="overview-content">
              <div class="quick-actions">
                <el-button type="primary" @click="openMonitorDialog" class="action-btn">
                  <el-icon><Monitor /></el-icon>
                  <span>系统监控</span>
                </el-button>
                <el-button type="success" @click="openTerminalDialog" class="action-btn">
                  <el-icon><Operation /></el-icon>
                  <span>RCON终端</span>
                </el-button>
                <el-button @click="refreshAll" class="action-btn">
                  <el-icon><Refresh /></el-icon>
                  <span>刷新数据</span>
                </el-button>
              </div>
              <div class="last-update">
                最后更新: {{ lastUpdateTime }}
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 服务器控制 -->
      <el-row :gutter="20" class="control-row">
        <el-col :span="24">
          <el-card class="control-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>🖥️ 服务器控制</span>
                <span v-if="!agentConnected" class="agent-warning">
                  <el-icon><Warning /></el-icon>
                  Agent 未连接
                </span>
              </div>
            </template>

            <div class="control-buttons">
              <el-button
                type="success"
                :disabled="serverStatus?.running || !agentConnected"
                @click="handleStart"
                :loading="starting"
                class="ctrl-btn"
              >
                <el-icon><VideoPlay /></el-icon>
                <span>启动服务器</span>
              </el-button>

              <el-button
                type="warning"
                :disabled="!serverStatus?.running || !agentConnected"
                @click="handleRestart"
                :loading="restarting"
                class="ctrl-btn"
              >
                <el-icon><RefreshRight /></el-icon>
                <span>重启服务器</span>
              </el-button>

              <el-button
                type="danger"
                :disabled="!serverStatus?.running || !agentConnected"
                @click="handleStop"
                :loading="stopping"
                class="ctrl-btn"
              >
                <el-icon><VideoPause /></el-icon>
                <span>停止服务器</span>
              </el-button>

              <el-button @click="fetchServerStatus" :loading="loadingStatus" class="ctrl-btn">
                <el-icon><Refresh /></el-icon>
                <span>刷新状态</span>
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 系统监控弹窗 -->
      <el-dialog
        v-model="monitorDialogVisible"
        title="系统性能监控"
        width="80%"
        :close-on-click-modal="false"
        @opened="handleMonitorDialogOpened"
        @closed="handleMonitorDialogClosed"
      >
        <div class="monitor-dialog-content">
          <el-row :gutter="20" class="monitor-row">
            <el-col :xs="24" :sm="12" :lg="6">
              <el-card class="monitor-card" shadow="hover">
                <template #header><div class="card-header"><span>CPU 负载</span></div></template>
                <div class="chart-container">
                  <el-progress
                    type="dashboard"
                    :percentage="metrics.cpuUsage"
                    :color="customColors"
                    :stroke-width="10"
                  />
                  <div class="stat-desc">核心数: {{ metrics.cpuCores || '8' }} 核</div>
                </div>
              </el-card>
            </el-col>

            <el-col :xs="24" :sm="12" :lg="6">
              <el-card class="monitor-card" shadow="hover">
                <template #header><div class="card-header"><span>内存占用</span></div></template>
                <div class="chart-container">
                  <el-progress
                    type="dashboard"
                    :percentage="metrics.memUsage"
                    :status="metrics.memUsage > 80 ? 'exception' : ''"
                    :stroke-width="10"
                  />
                  <div class="stat-desc">{{ metrics.memUsed }} / {{ metrics.memTotal }}</div>
                </div>
              </el-card>
            </el-col>

            <el-col :xs="24" :lg="12">
              <el-card class="monitor-card" shadow="hover">
                <template #header><div class="card-header"><span>网络实时流量 (KB/s)</span></div></template>
                <div class="net-info">
                  <div class="net-item down">
                    <el-icon><Download /></el-icon>
                    <span class="label">下载:</span>
                    <span class="value">{{ metrics.netIn }}</span>
                  </div>
                  <div class="net-item up">
                    <el-icon><Upload /></el-icon>
                    <span class="label">上传:</span>
                    <span class="value">{{ metrics.netOut }}</span>
                  </div>
                </div>
                <div ref="netChartRef" class="net-chart"></div>
              </el-card>
            </el-col>
          </el-row>

          <el-row :gutter="20" class="desc-row">
            <el-col :span="24">
              <el-descriptions title="系统运行状态" :column="3" :border="true">
                <el-descriptions-item label="操作系统">{{ metrics.os }}</el-descriptions-item>
                <el-descriptions-item label="运行时间">{{ metrics.upTime }}</el-descriptions-item>
                <el-descriptions-item label="服务器状态">
                  <el-tag size="small" type="success">运行中</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="游戏中人数">{{ serverInfo.gamePlayerNums }}</el-descriptions-item>
                <el-descriptions-item label="游戏TPS">{{ serverInfo.gameTps }}</el-descriptions-item>
                <el-descriptions-item label="Agent状态">
                  <el-tag :type="agentConnected ? 'success' : 'danger'" size="small">
                    {{ agentConnected ? '已连接' : '未连接' }}
                  </el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </el-col>
          </el-row>
        </div>
      </el-dialog>

      <!-- RCON 终端弹窗 -->
      <el-dialog
        v-model="terminalDialogVisible"
        title="远程控制台 (RCON)"
        width="70%"
        :close-on-click-modal="false"
        @opened="handleTerminalDialogOpened"
        @closed="handleTerminalDialogClosed"
      >
        <div class="terminal-dialog-content">
          <div ref="terminalRef" class="xterm-container"></div>
          <div class="terminal-input-wrapper">
            <span class="prompt">></span>
            <input
              v-model="commandInput"
              @keyup.enter="sendRconCommand"
              placeholder="输入指令并回车 (例如: list, help...)"
              class="rcon-input"
            />
            <el-button type="primary" size="small" @click="sendRconCommand">执行</el-button>
          </div>
        </div>
      </el-dialog>
    </div>
  </showCard>
</template>

<script setup>
import { ref, onMounted, onUnmounted, onActivated, onDeactivated, nextTick, shallowRef } from 'vue';
import {
  Download, Upload, VideoPlay, VideoPause, RefreshRight, Refresh, Warning,
  Monitor, Operation, Close
} from '@element-plus/icons-vue';
import showCard from "@/components/commonShowPanel.vue"
import * as echarts from 'echarts';
import { Terminal } from 'xterm';
import { FitAddon } from 'xterm-addon-fit';
import 'xterm/css/xterm.css';
import request from '@/utils/request';
import { ElMessage, ElMessageBox } from 'element-plus';
import client, { onReconnect } from '@/utils/wsClient';

// ==================== 基础数据 ====================
const metrics = ref({
  cpuUsage: 0,
  memUsage: 0,
  memTotal: '0 GB',
  memUsed: '0 GB',
  netIn: 0,
  netOut: 0,
  upTime: '加载中...',
  os: '检测中...',
  cpuCores: 0,
});

const serverInfo = ref({
  gamePlayerNums: 0,
  gameTps: 0
});

const lastUpdateTime = ref('--:--:--');

// ==================== 服务器控制 ====================
const serverStatus = ref(null);
const loadingStatus = ref(false);
const starting = ref(false);
const stopping = ref(false);
const restarting = ref(false);
const agentConnected = ref(false);

// ==================== 弹窗控制 ====================
const monitorDialogVisible = ref(false);
const terminalDialogVisible = ref(false);

// ==================== 图表相关 ====================
const netChartRef = ref(null);
const myChart = shallowRef(null);
const netDataIn = ref([]);
const netDataOut = ref([]);
const timeLabels = ref([]);

// ==================== RCON 终端相关 ====================
const terminalRef = ref(null);
const commandInput = ref('');
const terminalInstance = shallowRef(null);
const fitAddon = new FitAddon();

// ==================== 颜色映射 ====================
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

// ==================== 打开弹窗 ====================
const openMonitorDialog = () => {
  monitorDialogVisible.value = true;
};

const openTerminalDialog = () => {
  terminalDialogVisible.value = true;
};

// ==================== 弹窗生命周期 ====================
const handleMonitorDialogOpened = () => {
  nextTick(() => {
    initChart();
    if (!monitorTimer) {
      monitorTimer = setInterval(updateMetrics, 2000);
    }
  });
};

const handleMonitorDialogClosed = () => {
  if (monitorTimer) {
    clearInterval(monitorTimer);
    monitorTimer = null;
  }
  if (myChart.value) {
    myChart.value.dispose();
    myChart.value = null;
  }
};

const handleTerminalDialogOpened = () => {
  nextTick(() => {
    initTerminal();
  });
};

const handleTerminalDialogClosed = () => {
  if (terminalInstance.value) {
    terminalInstance.value.dispose();
    terminalInstance.value = null;
  }
};

// ==================== 初始化图表 ====================
const initChart = () => {
  if (!netChartRef.value) return;

  if (myChart.value) {
    myChart.value.dispose();
  }

  myChart.value = echarts.init(netChartRef.value);
  const option = {
    grid: { left: '0', right: '2%', bottom: '0', top: '10px', containLabel: false },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category', boundaryGap: false, data: timeLabels.value,
      axisLabel: { show: false }, axisLine: { show: false }, axisTick: { show: false }
    },
    yAxis: { type: 'value', show: false },
    series: [
      {
        name: '下载', type: 'line', smooth: true, showSymbol: false,
        areaStyle: { opacity: 0.1, color: '#67c23a' },
        lineStyle: { width: 2 }, color: '#67c23a', data: netDataIn.value
      },
      {
        name: '上传', type: 'line', smooth: true, showSymbol: false,
        areaStyle: { opacity: 0.1, color: '#409eff' },
        lineStyle: { width: 2 }, color: '#409eff', data: netDataOut.value
      }
    ]
  };
  myChart.value.setOption(option);
};

// ==================== 初始化终端 ====================
const initTerminal = () => {
  if (!terminalRef.value) return;

  if (terminalInstance.value) {
    terminalInstance.value.dispose();
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
    rows: 15,
    convertEol: true
  });

  term.loadAddon(fitAddon);
  term.open(terminalRef.value);
  nextTick(() => fitAddon.fit());

  term.writeln('\x1b[1;32m[MCSN]\x1b[0m 远程 RCON 终端已连接。');
  term.writeln('输入指令管理您的 Minecraft 服务器。\r\n');
  terminalInstance.value = term;
};

// ==================== 发送 RCON 命令 ====================
const sendRconCommand = async () => {
  if (!commandInput.value.trim()) return;
  const cmd = commandInput.value;
  const term = terminalInstance.value;

  term.writeln(`\x1b[1;34m> ${cmd}\x1b[0m`);
  commandInput.value = '';

  try {
    const res = await request.post('/api/mcsn/rcon/send', null, { params: { cmd } });
    if (res.code === 2000) {
      const coloredMsg = parseMinecraftColors(res.data || '指令已执行');
      term.writeln(coloredMsg);
    } else {
      term.writeln(`\x1b[1;31m[错误] ${res.msg}\x1b[0m`);
    }
  } catch (error) {
    term.writeln(`\x1b[1;31m[系统异常] 无法访问后端接口\x1b[0m`);
  }
};

// ==================== 获取数据 ====================
const getGameplayerNumsAndGameTps = async () => {
  try {
    const res = await request.get('/api/gameServer/status', { silent: true });
    if (res.code == 2000) {
      serverInfo.value.gamePlayerNums = res.data.onlineCount;
      serverInfo.value.gameTps = res.data.tps;
    }
  } catch (error) {
    console.log('游戏服务器异常：', error);
  }
};

const fetchServerStatus = async () => {
  loadingStatus.value = true;
  try {
    const res = await request.get('/api/server/status', { silent: true });
    if (res.code === 2000) {
      serverStatus.value = res.data;
      agentConnected.value = true;
    } else {
      agentConnected.value = false;
    }
  } catch (error) {
    console.error('获取服务器状态失败:', error);
    serverStatus.value = null;
    agentConnected.value = false;
  } finally {
    loadingStatus.value = false;
  }
};

const updateMetrics = async () => {
  try {
    const res = await request.get('/api/server/metrics', { silent: true, params: { t: Date.now() } });
    const monitorData = res.data || res;
    if (monitorData) {
      metrics.value = { ...monitorData };

      const now = new Date().toLocaleTimeString().split(' ')[0];
      timeLabels.value.push(now);
      netDataIn.value.push(Number(monitorData.netIn || 0));
      netDataOut.value.push(Number(monitorData.netOut || 0));

      if (timeLabels.value.length > 20) {
        timeLabels.value.shift();
        netDataIn.value.shift();
        netDataOut.value.shift();
      }

      if (monitorDialogVisible.value && myChart.value) {
        myChart.value.setOption({
          xAxis: { data: timeLabels.value },
          series: [
            { name: '下载', data: [...netDataIn.value] },
            { name: '上传', data: [...netDataOut.value] }
          ]
        });
      }
    }
  } catch (e) {
    console.error('监控更新失败', e);
  }
};

const refreshAll = async () => {
  await Promise.all([
    fetchServerStatus(),
    getGameplayerNumsAndGameTps(),
    updateMetrics()
  ]);
  lastUpdateTime.value = new Date().toLocaleTimeString();
  ElMessage.success('数据已刷新');
};

// ==================== 服务器控制 ====================
const handleStart = async () => {
  try {
    await ElMessageBox.confirm('确定要启动 Minecraft 服务器吗？', '启动确认', {
      confirmButtonText: '启动',
      type: 'info'
    });
  } catch {
    return;
  }

  starting.value = true;
  try {
    const res = await request.post('/api/server/start');
    if (res.code === 2000) {
      ElMessage.success('启动指令已发送，请等待服务器启动');
      setTimeout(() => fetchServerStatus(), 5000);
    } else {
      ElMessage.error(res.msg || '启动失败');
    }
  } catch (error) {
    ElMessage.error(error.friendlyMsg || '操作失败');
  } finally {
    starting.value = false;
  }
};

const handleStop = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要停止 Minecraft 服务器吗？在线玩家将被断开连接。',
      '停止确认',
      {
        confirmButtonText: '停止',
        type: 'warning'
      }
    );
  } catch {
    return;
  }

  stopping.value = true;
  try {
    const res = await request.post('/api/server/stop');
    if (res.code === 2000) {
      ElMessage.success('停止指令已发送');
      setTimeout(() => fetchServerStatus(), 3000);
    } else {
      ElMessage.error(res.msg || '停止失败');
    }
  } catch (error) {
    ElMessage.error(error.friendlyMsg || '操作失败');
  } finally {
    stopping.value = false;
  }
};

const handleRestart = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要重启 Minecraft 服务器吗？服务器将暂时不可用。',
      '重启确认',
      {
        confirmButtonText: '重启',
        type: 'warning'
      }
    );
  } catch {
    return;
  }

  restarting.value = true;
  try {
    const res = await request.post('/api/server/restart');
    if (res.code === 2000) {
      ElMessage.success('重启指令已发送，请等待服务器重启');
      setTimeout(() => fetchServerStatus(), 8000);
    } else {
      ElMessage.error(res.msg || '重启失败');
    }
  } catch (error) {
    ElMessage.error(error.friendlyMsg || '操作失败');
  } finally {
    restarting.value = false;
  }
};

// ==================== 定时器和生命周期 ====================
let statusTimer = null;
let gameInfoTimer = null;
let monitorTimer = null;
let metricsSubscription = null;
let gameStatusSubscription = null;
let unregisterReconnect = null;

const subscribeAll = () => {
  // 先取消旧订阅，防止重复
  unsubscribeAll();

  // 订阅系统性能指标
  metricsSubscription = client.subscribe('/topic/server/metrics', (message) => {
    try {
      const monitorData = JSON.parse(message.body);
      if (monitorData) {
        metrics.value = { ...monitorData };

        const now = new Date().toLocaleTimeString().split(' ')[0];
        timeLabels.value.push(now);
        netDataIn.value.push(Number(monitorData.netIn || 0));
        netDataOut.value.push(Number(monitorData.netOut || 0));

        // 只保留最近 20 个点
        if (timeLabels.value.length > 20) {
          timeLabels.value.shift();
          netDataIn.value.shift();
          netDataOut.value.shift();
        }

        // 如果监控弹窗开着，更新图表
        if (monitorDialogVisible.value && myChart.value) {
          myChart.value.setOption({
            xAxis: { data: timeLabels.value },
            series: [
              { name: '下载', data: [...netDataIn.value] },
              { name: '上传', data: [...netDataOut.value] }
            ]
          });
        }

        lastUpdateTime.value = new Date().toLocaleTimeString();
      }
    } catch (e) {
      console.error('处理监控推送数据失败:', e);
    }
  });

  // 订阅游戏服务器状态
  gameStatusSubscription = client.subscribe('/topic/server/game-status', (message) => {
    try {
      const data = JSON.parse(message.body);
      if (data) {
        serverInfo.value.gamePlayerNums = data.onlineCount ?? 0;
        serverInfo.value.gameTps = data.tps ?? 0;
      }
    } catch (e) {
      console.error('处理游戏状态推送数据失败:', e);
    }
  });
};

const unsubscribeAll = () => {
  if (metricsSubscription) { metricsSubscription.unsubscribe(); metricsSubscription = null; }
  if (gameStatusSubscription) { gameStatusSubscription.unsubscribe(); gameStatusSubscription = null; }
};

onMounted(() => {
  // ===== WebSocket 连接与订阅 =====
  if (!client.active) {
    client.activate();
  }

  if (client.connected) {
    subscribeAll();
  }

  // 重连时重新订阅
  unregisterReconnect = onReconnect(() => {
    subscribeAll();
    fetchServerStatus();
  });

  // HTTP 降级轮询（WebSocket 断线时才触发）
  statusTimer = setInterval(() => {
    if (!client.connected) {
      fetchServerStatus();
      getGameplayerNumsAndGameTps();
    }
  }, 30000);

  // 首次加载
  fetchServerStatus();
  getGameplayerNumsAndGameTps();
  lastUpdateTime.value = new Date().toLocaleTimeString();

  // ===== 窗口大小监听 =====
  window.addEventListener('resize', handleResize);
});

onActivated(() => {
  if (!client.active) {
    client.activate();
  }
  if (client.connected) {
    subscribeAll();
  }
  if (unregisterReconnect) { unregisterReconnect(); unregisterReconnect = null; }
  unregisterReconnect = onReconnect(() => {
    subscribeAll();
    fetchServerStatus();
  });
  if (!statusTimer) {
    statusTimer = setInterval(() => {
      if (!client.connected) {
        fetchServerStatus();
        getGameplayerNumsAndGameTps();
      }
    }, 30000);
  }
  window.addEventListener('resize', handleResize);
});

onDeactivated(() => {
  unsubscribeAll();
  if (unregisterReconnect) { unregisterReconnect(); unregisterReconnect = null; }
  if (statusTimer) { clearInterval(statusTimer); statusTimer = null; }
  if (gameInfoTimer) { clearInterval(gameInfoTimer); gameInfoTimer = null; }
  if (monitorTimer) { clearInterval(monitorTimer); monitorTimer = null; }
  window.removeEventListener('resize', handleResize);
});

onUnmounted(() => {
  unsubscribeAll();
  if (unregisterReconnect) { unregisterReconnect(); unregisterReconnect = null; }
  // 清除 HTTP 降级定时器
  if (statusTimer) {
    clearInterval(statusTimer);
    statusTimer = null;
  }
  if (gameInfoTimer) {
    clearInterval(gameInfoTimer);
    gameInfoTimer = null;
  }
  if (monitorTimer) {
    clearInterval(monitorTimer);
    monitorTimer = null;
  }

  // 清理图表实例
  if (myChart.value) {
    myChart.value.dispose();
    myChart.value = null;
  }

  // 清理终端实例
  if (terminalInstance.value) {
    terminalInstance.value.dispose();
    terminalInstance.value = null;
  }

  // 移除窗口大小监听
  window.removeEventListener('resize', handleResize);

  // 注意：不要在这里 deactivate WebSocket 客户端
  // 因为其他页面（如 userPanel）可能还在使用同一个连接
  // WebSocket 客户端由 wsClient.js 统一管理生命周期
});

const handleResize = () => {
  if (myChart.value) myChart.value.resize();
  if (terminalInstance.value) {
    try {
      fitAddon.fit();
    } catch (e) {
      // ignore
    }
  }
};

const customColors = [
  { color: '#409eff', percentage: 60 },
  { color: '#e6a23c', percentage: 80 },
  { color: '#f56c6c', percentage: 100 },
];
</script>

<style scoped>
/* ==================== 主容器 ==================== */
.server-management-wrapper {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

/* ==================== 概览行 ==================== */
.overview-row {
  align-items: stretch;
}

.overview-row .el-col {
  display: flex;
}

/* ==================== 概览卡片 ==================== */
.overview-card {
  width: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  border: none;
  background-color: #ffffff;
  min-height: 260px;
}

.overview-card :deep(.el-card__header) {
  border-bottom: 1px solid #f2f2f7;
  padding: 16px 20px;
  flex-shrink: 0;
}

.overview-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  font-size: 15px;
}

.overview-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
  justify-content: space-between;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.status-dot.running {
  background-color: #67c23a;
  box-shadow: 0 0 8px rgba(103, 194, 58, 0.5);
  animation: pulse 2s infinite;
}

.status-dot.stopped {
  background-color: #909399;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.status-text {
  font-size: 16px;
  font-weight: 600;
}

.quick-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.info-row .label {
  color: #909399;
}

.info-row .value {
  font-weight: 600;
  color: #303133;
}

.agent-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  padding-top: 4px;
}

/* ==================== 迷你图表 ==================== */
.mini-chart-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
  justify-content: center;
}

.metric-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.metric-label {
  min-width: 40px;
  font-weight: 600;
  color: #606266;
}

.metric-row :deep(.el-progress) {
  flex: 1;
}

.metric-detail {
  text-align: center;
  font-size: 13px;
  color: #909399;
}

/* ==================== 快速操作 ==================== */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  justify-content: center;
}
/* 删除原来的 .action-btn .el-icon 样式，换成下面这段 */
.action-btn :deep(.el-icon),
.action-btn :deep(i) {
  display: inline-flex !important;
  justify-content: flex-start !important;
  align-items: center;
  width: 24px !important;
  height: 24px !important;
  overflow: hidden;
  flex-shrink: 0;
  margin-right: 10px;
}

.action-btn :deep(svg) {
  width: 24px !important;
  height: 24px !important;
  display: block;
}

/* 保持不变的部分 */
.action-btn :deep(> span) {
  display: flex !important;
  justify-content: flex-start !important;
  align-items: center !important;
  width: 100% !important;
  padding-left: 32px !important;
}
/* 快速操作区域 - 去掉垂直排列按钮之间的左边距 */
.quick-actions .el-button + .el-button {
  margin-left: 0 !important;
}
.last-update {
  margin-top: auto;
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
  padding-top: 8px;
}

/* ==================== 控制行 ==================== */
.control-row {
  margin-top: 20px;
}

/* ==================== 控制卡片 ==================== */
.control-card {
  border-radius: 16px;
}

.control-card :deep(.el-card__header) {
  border-bottom: 1px solid #f2f2f7;
  padding: 16px 20px;
}

.agent-warning {
  color: #f56c6c;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.control-buttons {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.ctrl-btn {
  min-width: 130px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 14px;
  flex: 1 0 auto;
  max-width: 180px;
}

.ctrl-btn .el-icon {
  font-size: 15px;
}

/* ==================== 监控弹窗 ==================== */
.monitor-dialog-content {
  padding: 10px;
}

.monitor-row {
  align-items: stretch;
}

.monitor-row .el-col {
  display: flex;
  margin-bottom: 0;
}

.monitor-card {
  width: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  border: none;
  background-color: #ffffff;
  min-height: 260px;
}

.monitor-card :deep(.el-card__header) {
  border-bottom: 1px solid #f2f2f7;
  padding: 14px 18px;
  flex-shrink: 0;
}

.monitor-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.chart-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
}

.stat-desc {
  margin-top: 10px;
  font-size: 13px;
  color: #909399;
}

.net-info {
  display: flex;
  justify-content: space-around;
  margin-bottom: 10px;
}

.net-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 14px;
}

.down { color: #67c23a; }
.up { color: #409eff; }

.net-chart {
  height: 150px;
  width: 100%;
  flex: 1;
}

.desc-row {
  margin-top: 20px;
}

/* ==================== 终端弹窗 ==================== */
.terminal-dialog-content {
  background-color: #1a1a1a;
  border-radius: 8px;
  overflow: hidden;
}

.xterm-container {
  padding: 15px;
  background-color: #1a1a1a;
  height: 320px;
}

.terminal-input-wrapper {
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
  flex-shrink: 0;
}

.rcon-input {
  flex: 1;
  background: transparent;
  border: none;
  color: #ffffff;
  outline: none;
  font-family: 'Menlo', monospace;
  font-size: 14px;
}

/* ==================== 终端滚动条 ==================== */
.xterm-container :deep(.xterm-viewport) {
  overflow-y: auto !important;
}

.xterm-container :deep(.xterm-viewport)::-webkit-scrollbar {
  width: 6px !important;
  display: block !important;
}

.xterm-container :deep(.xterm-viewport)::-webkit-scrollbar-track {
  background: #1a1a1a;
}

.xterm-container :deep(.xterm-viewport)::-webkit-scrollbar-thumb {
  background-color: #333;
  border-radius: 10px;
}

.xterm-container :deep(.xterm-viewport)::-webkit-scrollbar-thumb:hover {
  background-color: #4a4a4a;
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .server-management-wrapper {
    padding: 12px;
  }

  .control-buttons {
    flex-direction: column;
    gap: 10px;
  }

  .ctrl-btn {
    width: 100%;
    max-width: none;
  }

  .monitor-dialog-content {
    padding: 4px;
  }

  .xterm-container {
    height: 240px;
  }
}
</style>