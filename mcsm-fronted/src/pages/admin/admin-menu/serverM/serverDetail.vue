<template>
  <showCard title="服务器管理" subtitle="控制服务器实例与查看控制台">
    <div class="back-bar">
      <el-button text @click="router.push('/adminPanel/serverM')">
        <el-icon><ArrowLeft /></el-icon> 返回服务器列表
      </el-button>
    </div>
    <div class="server-management-wrapper">

      <!-- 服务器数据看板 -->
      <ServerDashboard />

      <!-- 服务器控制 -->
      <el-row :gutter="20" class="control-row">
        <el-col :span="24">
          <el-card class="control-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>服务器控制</span>
                <span v-if="!agentConnected" class="agent-warning">
                  <el-icon><Warning /></el-icon> Agent 未连接
                </span>
              </div>
            </template>

            <div class="control-buttons">
              <el-button type="success" :disabled="serverStatus?.status === 'RUNNING' || !agentConnected"
                @click="handleStart" :loading="starting" class="ctrl-btn">
                <el-icon><VideoPlay /></el-icon> 启动服务器
              </el-button>
              <el-button type="warning" :disabled="serverStatus?.status !== 'RUNNING' || !agentConnected"
                @click="handleRestart" :loading="restarting" class="ctrl-btn">
                <el-icon><RefreshRight /></el-icon> 重启服务器
              </el-button>
              <el-button type="danger" :disabled="serverStatus?.status !== 'RUNNING' || !agentConnected"
                @click="handleStop" :loading="stopping" class="ctrl-btn">
                <el-icon><VideoPause /></el-icon> 停止服务器
              </el-button>
              <el-button type="danger" :disabled="serverStatus?.status === 'STOPPED' || !agentConnected" plain
                @click="handleForceStop" :loading="forceStopping" class="ctrl-btn">
                <el-icon><Warning /></el-icon> 强制停止
              </el-button>
              <el-button @click="refreshAll" :loading="loadingStatus" class="ctrl-btn">
                <el-icon><Refresh /></el-icon> 刷新状态
              </el-button>
            </div>

            <div class="server-info-row" v-if="serverStatus">
              <div class="stat-item">
                <span class="stat-label">状态</span>
                <span class="status-dot" :class="serverStatus?.status === 'RUNNING' ? 'running' : 'stopped'"></span>
                <span>{{ serverStatus?.status === 'RUNNING' ? '运行中' : '已停止' }}</span>
              </div>
              <div class="stat-item"><span class="stat-label">PID</span><span class="stat-value">{{ serverStatus?.pid || '-' }}</span></div>
              <div class="stat-item editable" @click.stop>
                <span class="stat-label">端口</span>
                <template v-if="editingPort">
                  <el-input v-model="editPortVal" size="small" style="width: 100px" @keyup.enter="savePort" />
                  <el-button size="small" type="primary" @click="savePort">保存</el-button>
                  <el-button size="small" @click="editingPort = false">取消</el-button>
                </template>
                <template v-else>
                  <span class="stat-value link" @click="startEditPort">{{ serverPort || '-' }}</span>
                </template>
              </div>
              <div class="stat-item"><span class="stat-label">目录</span><span class="stat-value">{{ serverStatus?.directory || '-' }}</span></div>
              <div class="stat-item editable" @click.stop>
                <span class="stat-label">JVM 参数</span>
                <template v-if="editingJavaArgs">
                  <el-input v-model="editJavaArgsVal" size="small" style="width: 200px" @keyup.enter="saveJavaArgs" />
                  <el-button size="small" type="primary" @click="saveJavaArgs">保存</el-button>
                  <el-button size="small" @click="editingJavaArgs = false">取消</el-button>
                </template>
                <template v-else>
                  <span class="stat-value link" @click="startEditJavaArgs">{{ javaArgs || '-' }}</span>
                </template>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 控制台 -->
      <el-row :gutter="20" class="console-section">
        <el-col :span="24">
          <el-card class="console-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>控制台日志</span>
                <el-button size="small" text @click="clearConsole">清屏</el-button>
              </div>
            </template>
            <div class="console-log-viewer" ref="consoleRef">
              <div v-if="consoleLines.length === 0" class="console-empty">等待日志...</div>
              <div v-for="line in consoleLines" :key="line.lineNumber + '-' + line.content" class="console-line">
                <span class="line-num" :class="{ 'line-cmd': line.lineNumber === '>' }">{{ line.lineNumber }}</span>
                <span class="line-content">{{ line.content }}</span>
              </div>
            </div>
            <div class="console-input-wrapper">
              <span class="prompt">></span>
              <el-input v-model="commandInput" placeholder="输入指令并回车" @keyup.enter="sendCommand" class="console-input" />
              <el-button type="primary" size="small" @click="sendCommand">执行</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 插件管理 -->
      <el-row :gutter="20" class="plugin-section">
        <el-col :span="24">
          <el-card class="plugin-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>插件管理</span>
                <div class="plugin-toolbar">
                  <el-upload :show-file-list="false" :before-upload="handlePluginUpload" accept=".jar" :disabled="!serverId()">
                    <el-button type="primary" size="small">上传插件</el-button>
                  </el-upload>
                  <el-button size="small" @click="fetchPlugins" :loading="pluginLoading">刷新</el-button>
                </div>
              </div>
            </template>
            <el-table :data="plugins" v-loading="pluginLoading" style="width: 100%">
              <el-table-column prop="name" label="插件名称" />
              <el-table-column prop="fileName" label="文件名" />
              <el-table-column label="大小" width="120">
                <template #default="{ row }">{{ formatPluginSize(row.size) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '已启用' : '已禁用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200">
                <template #default="{ row }">
                  <el-button v-if="!row.enabled" type="success" size="small" @click="enablePlugin(row)">启用</el-button>
                  <el-button v-if="row.enabled" type="warning" size="small" @click="disablePlugin(row)">禁用</el-button>
                  <el-button type="danger" size="small" @click="deletePlugin(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

    </div>
  </showCard>
</template>

<script setup>
import { ref, onUnmounted, onDeactivated, nextTick, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { VideoPlay, VideoPause, RefreshRight, Refresh, Warning, ArrowLeft } from '@element-plus/icons-vue';
import showCard from '@/components/commonShowPanel.vue';
import ServerDashboard from '@/pages/admin/admin-menu/serverM/ServerDashboard.vue';
import request from '@/utils/request';
import { ElMessage, ElMessageBox } from 'element-plus';

const route = useRoute();
const router = useRouter();
const serverId = () => route.params.serverId;

// 控制
const serverStatus = ref(null);
const loadingStatus = ref(false);
const starting = ref(false);
const stopping = ref(false);
const restarting = ref(false);
const agentConnected = ref(false);
// JVM 参数编辑
const javaArgs = ref('');
const editingJavaArgs = ref(false);
const editJavaArgsVal = ref('');

// 端口编辑
const serverPort = ref('');
const editingPort = ref(false);
const editPortVal = ref('');

const startEditPort = () => {
  editPortVal.value = serverPort.value;
  editingPort.value = true;
};

const savePort = async () => {
  try {
    const res = await request.put(`/api/server/${serverId()}/config`, { port: parseInt(editPortVal.value) });
    if (res.code === 2000) {
      serverPort.value = editPortVal.value;
      editingPort.value = false;
      ElMessage.success('端口已更新，重启后生效');
    } else { ElMessage.error(res.msg || '更新失败'); }
  } catch (e) { ElMessage.error('保存失败'); }
};

const startEditJavaArgs = () => {
  editJavaArgsVal.value = javaArgs.value;
  editingJavaArgs.value = true;
};

const saveJavaArgs = async () => {
  try {
    const res = await request.put(`/api/server/${serverId()}/config`, { javaArgs: editJavaArgsVal.value });
    if (res.code === 2000) {
      javaArgs.value = editJavaArgsVal.value;
      editingJavaArgs.value = false;
      ElMessage.success('JVM 参数已更新，重启后生效');
    } else { ElMessage.error(res.msg || '更新失败'); }
  } catch (e) { ElMessage.error(e.friendlyMsg || '请求失败'); }
};

// 控制台
const consoleRef = ref(null);
const consoleLines = ref([]);
const commandInput = ref('');
let latestLineNumber = -1;
let consoleTimer = null;
let pollTimer = null;

// ==================== 数据获取 ====================
let retryTimer = null;

const fetchServerStatus = async () => {
  try {
    const res = await request.get(`/api/server/${serverId()}/status`, { silent: true });
    if (res.code === 2000) { serverStatus.value = res.data; javaArgs.value = res.data?.javaArgs || '-'; serverPort.value = res.data?.port || ''; agentConnected.value = true; clearRetry(); return; }
    else { agentConnected.value = false; }
  } catch { serverStatus.value = null; agentConnected.value = false; }
  // 失败后每 5 秒自动重试，直到成功（先清理旧的避免并行重复）
  clearRetry();
  retryTimer = setInterval(() => fetchServerStatus(), 5000);
};

const clearRetry = () => { if (retryTimer) { clearInterval(retryTimer); retryTimer = null; } };

const fetchConsole = async () => {
  try {
    const res = await request.get(`/api/server/${serverId()}/console`, { silent: true, params: { since: latestLineNumber } });
    if (res.code === 2000 && res.data) {
      const data = res.data;
      if (data.lines && data.lines.length > 0) {
        consoleLines.value.push(...data.lines);
        if (consoleLines.value.length > 500) consoleLines.value = consoleLines.value.slice(-500);
      }
      latestLineNumber = data.latestLineNumber;
      nextTick(() => { if (consoleRef.value) consoleRef.value.scrollTop = consoleRef.value.scrollHeight; });
    }
  } catch { /* silent */ }
};

const startConsolePolling = () => {
  if (consoleTimer) return;
  fetchConsole();
  consoleTimer = setInterval(fetchConsole, 2000);
};

const stopConsolePolling = () => {
  if (consoleTimer) { clearInterval(consoleTimer); consoleTimer = null; }
};

const refreshAll = async () => {
  loadingStatus.value = true;
  await fetchServerStatus();
  loadingStatus.value = false;
  ElMessage.success('数据已刷新');
};

const clearConsole = () => { consoleLines.value = []; latestLineNumber = -1; };

// ==================== 状态轮询（操作后） ====================
const clearPoll = () => { if (pollTimer) { clearInterval(pollTimer); pollTimer = null; } };

const startPollUntil = (targetStatus, timeoutMs, onDone) => {
  clearPoll();
  let elapsed = 0;
  pollTimer = setInterval(async () => {
    await fetchServerStatus();
    elapsed += 3000;
    if (serverStatus.value?.status === targetStatus) { clearPoll(); if (onDone) onDone(true); return; }
    if (elapsed >= timeoutMs) {
      clearPoll();
      ElMessage.warning('操作超时，请手动刷新状态');
      if (onDone) onDone(false);
    }
  }, 3000);
};

// ==================== 命令 ====================
const sendCommand = async () => {
  const cmd = commandInput.value.trim();
  if (!cmd) return;
  commandInput.value = '';
  consoleLines.value.push({ lineNumber: '>', content: cmd });
  nextTick(() => { if (consoleRef.value) consoleRef.value.scrollTop = consoleRef.value.scrollHeight; });
  try {
    await request.post(`/api/server/${serverId()}/command`, null, { params: { cmd } });
  } catch (e) { ElMessage.error(e.friendlyMsg || '命令发送失败'); }
};

// ==================== 控制操作 ====================
const handleStart = async () => {
  try { await ElMessageBox.confirm('确定要启动 Minecraft 服务器吗？', '启动确认', { confirmButtonText: '启动', type: 'info' }); }
  catch { return; }
  starting.value = true;
  try {
    const res = await request.post(`/api/server/${serverId()}/start`);
    if (res.code === 2000) {
      ElMessage.success('启动指令已发送');
      startPollUntil('RUNNING', 120000, (ok) => {
        starting.value = false;
        if (ok) ElMessage.success('服务器已上线');
      });
    } else { ElMessage.error(res.msg || '启动失败'); starting.value = false; }
  } catch (e) { ElMessage.error(e.friendlyMsg || '操作失败'); starting.value = false; }
};

const handleStop = async () => {
  try { await ElMessageBox.confirm('确定要停止 Minecraft 服务器吗？', '停止确认', { confirmButtonText: '停止', type: 'warning' }); }
  catch { return; }
  stopping.value = true;
  try {
    const res = await request.post(`/api/server/${serverId()}/stop`);
    if (res.code === 2000) {
      ElMessage.success('停止指令已发送');
      startPollUntil('STOPPED', 60000, () => { stopping.value = false; });
    } else { ElMessage.error(res.msg || '停止失败'); stopping.value = false; }
  } catch (e) { ElMessage.error(e.friendlyMsg || '操作失败'); stopping.value = false; }
};

const forceStopping = ref(false);
const handleForceStop = async () => {
  try {
    await ElMessageBox.confirm('强制停止会直接杀掉服务器进程，确定继续？', '警告', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' });
  } catch { return; }
  forceStopping.value = true;
  try {
    const res = await request.post(`/api/server/${serverId()}/force-stop`);
    res.code === 2000 ? ElMessage.success('已终止') : ElMessage.error(res.msg);
    refreshAll();
  } catch (e) { ElMessage.error('操作失败'); }
  finally { forceStopping.value = false; }
};

const handleRestart = async () => {
  try { await ElMessageBox.confirm('确定要重启 Minecraft 服务器吗？', '重启确认', { confirmButtonText: '重启', type: 'warning' }); }
  catch { return; }
  restarting.value = true;
  try {
    const res = await request.post(`/api/server/${serverId()}/restart`);
    if (res.code === 2000) {
      ElMessage.success('重启指令已发送');
      startPollUntil('RUNNING', 180000, () => { restarting.value = false; });
    } else { ElMessage.error(res.msg || '重启失败'); restarting.value = false; }
  } catch (e) { ElMessage.error(e.friendlyMsg || '操作失败'); restarting.value = false; }
};

// ==================== 生命周期 ====================
watch(() => serverId(), (newId) => {
  if (!newId) return;
  stopConsolePolling();
  clearPoll();
  consoleLines.value = [];
  latestLineNumber = -1;
  serverStatus.value = null;
  fetchServerStatus();
  startConsolePolling();
}, { immediate: true });

// ==================== 插件管理 ====================
const plugins = ref([]);
const pluginLoading = ref(false);

const fetchPlugins = async () => {
  pluginLoading.value = true;
  try {
    const res = await request.get(`/api/plugins/${serverId()}/list`);
    if (res.code === 2000) plugins.value = res.data || [];
  } finally { pluginLoading.value = false; }
};

const formatPluginSize = (bytes) => {
  if (!bytes) return '-';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
};

const enablePlugin = async (row) => {
  try {
    const res = await request.post(`/api/plugins/${serverId()}/enable`, null, { params: { fileName: row.fileName } });
    if (res.code === 2000) { ElMessage.success('启用成功'); fetchPlugins(); }
    else ElMessage.error(res.msg);
  } catch (e) { ElMessage.error(e.friendlyMsg || '操作失败'); }
};

const disablePlugin = async (row) => {
  try {
    const res = await request.post(`/api/plugins/${serverId()}/disable`, null, { params: { fileName: row.fileName } });
    if (res.code === 2000) { ElMessage.success('禁用成功'); fetchPlugins(); }
    else ElMessage.error(res.msg);
  } catch (e) { ElMessage.error(e.friendlyMsg || '操作失败'); }
};

const deletePlugin = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该插件吗？', '警告', { type: 'warning' });
  } catch { return; }
  try {
    const res = await request.delete(`/api/plugins/${serverId()}/delete`, { params: { fileName: row.fileName } });
    if (res.code === 2000) { ElMessage.success('删除成功'); fetchPlugins(); }
    else ElMessage.error(res.msg);
  } catch (e) { ElMessage.error(e.friendlyMsg || '删除失败'); }
};

const handlePluginUpload = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  try {
    const res = await request.post(`/api/plugins/${serverId()}/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    if (res.code === 2000) { ElMessage.success('上传成功'); fetchPlugins(); }
    else ElMessage.error(res.msg);
  } catch (e) { ElMessage.error(e.friendlyMsg || '上传失败'); }
  return false;
};

// ==================== 生命周期 ====================
onDeactivated(() => {
  stopConsolePolling();
  clearPoll();
  clearRetry();
});

onUnmounted(() => {
  stopConsolePolling();
  clearPoll();
  clearRetry();
});

// watch serverId change → refresh plugins
watch(() => serverId(), (newId) => { if (newId) fetchPlugins(); }, { immediate: true });
</script>

<style scoped>
.back-bar { margin-bottom: 16px; }

.server-management-wrapper { padding: 10px 0; }

.control-row { margin-bottom: 20px; }

.control-card { border-radius: 16px; border: none; }

.control-card :deep(.el-card__header) { border-bottom: 1px solid #f2f2f7; padding: 16px 20px; }

.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; font-size: 15px; }

.agent-warning { color: #f56c6c; font-size: 12px; display: flex; align-items: center; gap: 4px; }

.control-buttons {
  display: flex; gap: 16px; flex-wrap: wrap; align-items: center;
  padding-bottom: 20px; border-bottom: 1px solid #f2f2f7;
}

.ctrl-btn { min-width: 130px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; gap: 6px; font-size: 14px; flex: 1 0 auto; max-width: 180px; }

.server-info-row { display: flex; gap: 32px; padding-top: 16px; flex-wrap: wrap; }

.stat-item { display: flex; align-items: center; gap: 8px; font-size: 14px; }

.stat-label { color: #909399; font-weight: 500; }

.stat-value { font-weight: 600; color: #303133; }

.stat-value.link { color: #0071e3; cursor: pointer; text-decoration: underline; text-decoration-style: dotted; text-underline-offset: 3px; }

.stat-item.editable { gap: 6px; }

.status-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }

.status-dot.running { background-color: #67c23a; box-shadow: 0 0 6px rgba(103,194,58,0.5); animation: pulse 2s infinite; }

.status-dot.stopped { background-color: #909399; }

@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.6} }

.console-section { margin-top: 0; }

.console-card { border-radius: 16px; border: none; }

.console-log-viewer {
  background: #1a1a1a; border-radius: 8px; height: 400px; overflow-y: auto;
  padding: 12px 16px; font-family: 'Menlo','Monaco','Courier New',monospace; font-size: 13px; line-height: 1.6;
}

.console-empty { color: #666; text-align: center; padding-top: 180px; }

.console-line { display: flex; gap: 12px; }

.line-num { color: #555; min-width: 50px; text-align: right; user-select: none; flex-shrink: 0; }

.line-cmd { color: #67c23a; }

.line-content { color: #e0e0e0; white-space: pre-wrap; word-break: break-all; }

.console-log-viewer::-webkit-scrollbar { width: 6px; }

.console-log-viewer::-webkit-scrollbar-track { background: #1a1a1a; }

.console-log-viewer::-webkit-scrollbar-thumb { background-color: #333; border-radius: 10px; }

.console-input-wrapper { display: flex; align-items: center; padding: 12px 0 0 0; gap: 10px; }

.prompt { color: #67c23a; font-family: 'Menlo',monospace; font-weight: bold; font-size: 16px; flex-shrink: 0; }

.console-input { flex: 1; }

.plugin-section { margin-top: 20px; }

.plugin-card { border-radius: 16px; border: none; }

.plugin-toolbar { display: flex; gap: 8px; align-items: center; }

@media (max-width: 768px) {
  .control-buttons { flex-direction: column; gap: 10px; }
  .ctrl-btn { width: 100%; max-width: none; }
  .server-info-row { gap: 16px; }
  .console-log-viewer { height: 280px; }
}
</style>
