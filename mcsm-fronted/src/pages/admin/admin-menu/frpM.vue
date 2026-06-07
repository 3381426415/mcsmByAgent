<template>
  <div class="frp-page">
    <div class="frp-page__header">
      <h2 class="frp-page__title">内网穿透 (FRP)</h2>
      <p class="frp-page__subtitle">管理 frpc 客户端，让外网玩家可以连接你的服务器</p>
    </div>

    <!-- 状态卡片 -->
    <div class="frp-section">
      <div class="frp-status-row">
        <div class="frp-status-info">
          <span class="frp-status-label">运行状态(不会操作？让智能体来！)</span>
          <el-tag
            :type="frpStatus === 'running' ? 'success' : frpStatus === 'not_installed' ? 'danger' : 'info'"
            effect="dark"
          >
            {{ frpStatus === 'running' ? '运行中' : frpStatus === 'not_installed' ? '未安装' : '已停止' }}
          </el-tag>
        </div>
        <div class="frp-actions">
          <el-button type="success" :disabled="frpStatus !== 'stopped'" @click="frpStart">
            启动
          </el-button>
          <el-button type="danger" :disabled="frpStatus !== 'running'" @click="frpStop">
            停止
          </el-button>
          <el-button @click="fetchFrpStatus">刷新状态</el-button>
        </div>
      </div>
    </div>

    <!-- 配置编辑 -->
    <div class="frp-section">
      <div class="frp-section__header">
        <span class="frp-section__title">配置文件 (frpc.toml)</span>
        <el-button type="primary" size="small" @click="saveFrpConfig">保存</el-button>
      </div>
      <textarea
        v-model="frpConfig"
        class="frp-config-editor"
        spellcheck="false"
        placeholder="加载中..."
      ></textarea>
    </div>

    <!-- 运行日志 -->
    <div class="frp-section">
      <div class="frp-section__header">
        <span class="frp-section__title">运行日志</span>
        <el-button size="small" @click="fetchFrpLogs">刷新日志</el-button>
      </div>
      <div class="frp-logs">
        <div v-if="frpLogs.length === 0" class="frp-logs-empty">暂无日志</div>
        <div v-for="(line, i) in frpLogs" :key="i" class="frp-log-line">{{ line }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';

const frpStatus = ref('stopped');
const frpConfig = ref('');
const frpLogs = ref([]);

async function fetchFrpStatus() {
  try {
    const res = await request.get('/api/frp/status');
    if (res.code === 2000) {
      frpStatus.value = res.data.status;
    }
  } catch (e) { /* ignore */ }
}

async function fetchFrpConfig() {
  try {
    const res = await request.get('/api/frp/config');
    if (res.code === 2000) {
      frpConfig.value = res.data;
    }
  } catch (e) { /* ignore */ }
}

async function saveFrpConfig() {
  try {
    const res = await request.put('/api/frp/config', { content: frpConfig.value });
    if (res.code === 2000) {
      ElMessage.success('配置已保存');
    } else {
      ElMessage.error(res.msg || '保存失败');
    }
  } catch (e) {
    ElMessage.error('保存失败');
  }
}

async function frpStart() {
  try {
    const res = await request.post('/api/frp/start');
    if (res.code === 2000) {
      ElMessage.success('frpc 已启动');
      fetchFrpStatus();
    } else {
      ElMessage.error(res.msg || '启动失败');
    }
  } catch (e) {
    ElMessage.error('启动失败');
  }
}

async function frpStop() {
  try {
    const res = await request.post('/api/frp/stop');
    if (res.code === 2000) {
      ElMessage.success('frpc 已停止');
      fetchFrpStatus();
    } else {
      ElMessage.error(res.msg || '停止失败');
    }
  } catch (e) {
    ElMessage.error('停止失败');
  }
}

async function fetchFrpLogs() {
  try {
    const res = await request.get('/api/frp/logs', { params: { lines: 100 } });
    if (res.code === 2000) {
      frpLogs.value = res.data.logs || [];
    }
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  fetchFrpStatus();
  fetchFrpConfig();
  fetchFrpLogs();
});
</script>

<style scoped>
.frp-page {
  max-width: 800px;
}

.frp-page__header {
  margin-bottom: 24px;
}

.frp-page__title {
  font-size: 22px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 6px 0;
}

.frp-page__subtitle {
  font-size: 13px;
  color: #86868b;
  margin: 0;
}

.frp-section {
  background: #fff;
  border: 1px solid #d1d1d6;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}

.frp-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.frp-status-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.frp-status-label {
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
}

.frp-actions {
  display: flex;
  gap: 8px;
}

.frp-section__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.frp-section__title {
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
}

.frp-config-editor {
  width: 100%;
  min-height: 200px;
  padding: 12px;
  border: 1px solid #d1d1d6;
  border-radius: 8px;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  resize: vertical;
  background: #fafafa;
  color: #1d1d1f;
  box-sizing: border-box;
}

.frp-config-editor:focus {
  outline: none;
  border-color: #0071e3;
}

.frp-logs {
  max-height: 300px;
  overflow-y: auto;
  background: #1d1d1f;
  border-radius: 8px;
  padding: 12px;
  font-family: 'SF Mono', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.6;
}

.frp-logs-empty {
  text-align: center;
  color: #86868b;
  padding: 20px 0;
}

.frp-log-line {
  color: #e5e5e7;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
