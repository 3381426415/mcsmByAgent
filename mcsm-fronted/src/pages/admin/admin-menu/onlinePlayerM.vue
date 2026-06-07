<template>
  <showCard title="在线玩家管理" subtitle="可以操作正在游戏服务器里的玩家">
    <div class="player-container">

      <!-- Agent 离线提示条 -->
      <div v-if="!agentConnected" class="agent-offline-banner">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
        <span>Agent 未连接 — 部分功能暂不可用</span>
      </div>

      <!-- 数据加载错误提示 -->
      <div v-if="loadError" class="error-banner">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="15" y1="9" x2="9" y2="15"></line>
          <line x1="9" y1="9" x2="15" y2="15"></line>
        </svg>
        <span>{{ loadError }}</span>
      </div>

      <div class="stats-row">
        <div class="glass-card stat-item">
          <span class="label">当前在线</span>
          <span class="value online">{{ onlinePlayers?.length || 0 }}</span>
        </div>
      </div>

      <div class="glass-card table-container">
        <el-table :data="players" style="width: 100%" v-loading="loading" class="apple-table">
          <el-table-column label="玩家 ID" min-width="180">
            <template #default="scope">
              <span class="nickname">{{ scope.row.nickname }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="money" label="余额" width="140" />

          <el-table-column label="状态" width="120">
            <template #default="scope">
              <el-tag :type="scope.row.isOnline === 1 ? 'success' : 'info'" effect="plain" class="apple-tag">
                {{ scope.row.isOnline === 1 ? '在线' : '离线' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="220" align="right">
            <template #default="scope">
             <el-button 
            link 
            class="btn-apple btn-kick"
            :disabled="scope.row.isOnline === 0"
            @click="handleCommand('kick', scope.row)" 
          >
            踢出
          </el-button>

          <el-button 
            link 
            class="btn-apple btn-ban"
            @click="handleCommand('ban', scope.row)"
          >
            封禁
          </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </showCard>
</template>

<script setup>
import showCard from "@/components/commonShowPanel.vue"
import { ref, onMounted, computed } from 'vue';
import request from '@/utils/request';
import { ElMessage, ElMessageBox } from 'element-plus';

const players = ref([]);
const loading = ref(false);
const agentConnected = ref(true);
const loadError = ref('');

const onlinePlayers = computed(() => (players.value || []).filter(p => p.isOnline === 1));

// 检测 Agent 是否在线
const checkAgentStatus = async () => {
  try {
    const res = await request.get('/api/server/status', { silent: true, timeout: 3000 });
    agentConnected.value = res.code === 2000;
  } catch {
    agentConnected.value = false;
  }
};

const fetchPlayers = async () => {
  loading.value = true;
  loadError.value = '';
  try {
    const res = await request.get('/api/player/all', { silent: true });
    players.value = res.data;
  } catch (error) {
    console.error('获取列表失败:', error);
    loadError.value = error.friendlyMsg || '获取玩家列表失败';
  } finally {
    loading.value = false;
  }
};


const handleCommand = (type, row) => {
  const { nickname, uuid } = row; // 从行数据中解构出名字和 UUID
  const actionText = type === 'kick' ? '踢出' : '封禁';
  
  // 保持你要求的 Apple 风格圆角弹出框
  ElMessageBox.confirm(`确定要${actionText}玩家 ${nickname} 吗？`, '安全警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    customClass: 'apple-message-box',
    roundButton: true, 
  }).then(async () => {
    try {
      // 适配新接口：/api/player/kick 或 /api/player/ban
      // 使用 params 传递 uuid，对应后端的 @RequestParam
      const res = await request.post(`/api/player/${type}`, null, {
        params: { uuid: uuid }
      });

      if (res.code === 2000) {
        ElMessage.success(`${nickname} ${res.msg || (actionText + '成功')}`);
        fetchPlayers(); // 成功后刷新列表状态
      } else {
        ElMessage.error(res.msg || '操作失败');
      }
    } catch (error) {
      console.error(`${actionText}请求异常:`, error);
      ElMessage.error('网络请求失败，请检查后端服务');
    }
  }).catch(() => {
    // 用户取消操作
  });
};


onMounted(() => {
  checkAgentStatus();
  fetchPlayers();
});
</script>

<style scoped>
/* 核心容器保持原有风格 */
.player-container {
  padding: 10px; /* 适当调小，配合 showCard */
}

/* Agent 离线提示条 */
.agent-offline-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: rgba(255, 149, 0, 0.08);
  border: 1px solid rgba(255, 149, 0, 0.2);
  border-radius: 10px;
  color: #ff9500;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 16px;
}

.agent-offline-banner svg {
  flex-shrink: 0;
}

/* 错误状态标签 */
.error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: rgba(255, 59, 48, 0.08);
  border: 1px solid rgba(255, 59, 48, 0.2);
  border-radius: 10px;
  color: #ff3b30;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 16px;
}

.error-banner svg {
  flex-shrink: 0;
}

.title {
  font-size: 20px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 24px;
}

.glass-card {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px; /* 稍微调小一点，更显精致 */
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.03);
  margin-bottom: 20px;
}

.stats-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-item {
  width: 160px;
}

.label {
  font-size: 13px;
  color: #86868b;
  font-weight: 500;
}

.value {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.value.online { color: #34c759; }

.nickname {
  font-weight: 600;
  font-size: 15px;
  color: #1d1d1f;
  letter-spacing: -0.2px;
}


:deep(.el-table) {
  background: transparent !important;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}
:deep(.el-table tr) {
  background: transparent !important;
}
:deep(.el-table th.el-table__cell) {
  background: transparent !important;
  color: #86868b;
  font-weight: 600;
  font-size: 13px;
  border-bottom: 1px solid rgba(0,0,0,0.05);
}
:deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid rgba(0,0,0,0.03);
}

/* 自定义 Apple 文本按钮 */
.btn-apple {
  font-size: 14px;
  font-weight: 600; /* 字体加粗，更具质感 */
  letter-spacing: -0.2px;
  transition: all 0.2s ease;
  padding: 4px 8px;
  border-radius: 6px;
}

/* 踢出按钮：默认 iOS 蓝色 */
.btn-kick {
  color: #0071e3 !important; /* Apple Blue */
}
.btn-kick:hover:not(.is-disabled) {
  background-color: rgba(0, 113, 227, 0.1);
  color: #0056b3 !important;
}

/* 封禁按钮：警示红色 */
.btn-ban {
  color: #ff3b30 !important; /* Apple Red */
}
.btn-ban:hover:not(.is-disabled) {
  background-color: rgba(255, 59, 48, 0.1);
  color: #d72c23 !important;
}

/* 禁用状态样式 */
:deep(.el-button.is-disabled) {
  color: #c7c7cc !important; /* Apple 灰色 */
  background: transparent !important;
}

/* 微调 Tag */
.apple-tag {
  border-radius: 12px;
  font-weight: 600;
  font-size: 11px;
}
</style>