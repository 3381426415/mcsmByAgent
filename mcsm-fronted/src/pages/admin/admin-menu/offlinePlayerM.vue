<template>
<showCard title="离线玩家管理" subtitle="操作不在游戏服务器里的玩家"> 






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
          <span class="label">离线总数</span>
          <span class="value offline">{{ offlinePlayers.length }}</span>
        </div>
      </div>

      <div class="glass-card table-container">
        <el-table :data="offlinePlayers" style="width: 100%" v-loading="loading" class="apple-table">
          <el-table-column label="玩家" min-width="180">
            <template #default="scope">
              <div class="player-info">
                <span class="nickname">{{ scope.row.nickname }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="money" label="账户余额" width="140" />

          <el-table-column label="最后在线" width="180">
            <template #default="scope">
              <span class="time-text">{{ formatTime(scope.row.lastPlayed) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="220" align="right">
            <template #default="scope">
              <el-button 
                link 
                class="btn-apple btn-manage"
                @click="handleManage(scope.row)"
              >
                管理
              </el-button>
              
              <el-button 
                link 
                class="btn-apple btn-ban"
                @click="handleBan(scope.row)"
              >
                封禁
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>


      <el-dialog v-model="editVisible" title="物品属性编辑" width="320px" append-to-body class="apple-dialog">
  <div style="text-align: center; padding: 10px 0;">
    <p style="margin-bottom: 15px; color: #86868b;">修改物品堆叠数量或彻底删除</p>
    <el-input-number v-model="tempCount" :min="1" :max="64" class="apple-number" />
  </div>
  
  <template #footer>
    <div class="dialog-footer">
      <el-button @click="handleDelete" type="danger" plain round style="float: left;">删除</el-button>
      <el-button @click="editVisible = false" round>取消</el-button>
      <el-button @click="confirmUpdate" type="primary" round>保存</el-button>
    </div>
  </template>
</el-dialog>

<el-dialog v-model="invDialogVisible" :title="`玩家 ${currentManagedPlayer.nickname} 的背包`" width="480px" class="apple-dialog">
  <div v-loading="invLoading" class="inventory-wrapper">
    <div class="inventory-grid">



     <div 
    v-for="i in 36" 
    :key="i-1" 
    class="inv-slot"
    :class="{ 'has-item': getSlotItem(i-1) }"
    @click="handleItemClick(i-1)"
  >
    <el-tooltip
      v-if="getSlotItem(i-1)"
      :content="getSlotItem(i-1).id.replace('minecraft:', '')"
      placement="top"
      effect="light"
    >
      <div class="item-container">
        <div class="item-icon">
          <el-image 
            :src="getIconUrl(getSlotItem(i-1).id)" 
            fit="contain"
            style="width: 28px; height: 28px; display: block;"
          >
            <template #error>
              <div class="fallback-icon">{{ defaultIcon }}</div>
            </template>
            <template #placeholder>
              <div class="loading-dot">...</div>
            </template>
          </el-image>
        </div>
        <span class="item-count">{{ getSlotItem(i-1).Count }}</span>
      </div>
    </el-tooltip>
    <div v-else class="empty-placeholder"></div>
  </div>




    </div>
    <p class="inventory-tip">点击物品格进行数量编辑或删除</p>
  </div>
</el-dialog>

    </div>
  </showCard>






    
</template>
<script setup>
import showCard from "@/components/commonShowPanel.vue"
import { useAssets } from '@/components/useAssets.js'
import { ref, onMounted, computed } from 'vue';
import request from '@/utils/request'; 
import { ElMessage, ElMessageBox } from 'element-plus';
import dayjs from 'dayjs'; // 建议使用 dayjs 格式化时间

const { getIconUrl } = useAssets();
const defaultIcon = '📦';
const players = ref([]);
const loading = ref(false);
const agentConnected = ref(true);
const loadError = ref('');

// 检测 Agent 是否在线
const checkAgentStatus = async () => {
  try {
    const res = await request.get('/api/server/status', { silent: true, timeout: 3000 });
    agentConnected.value = res.code === 2000;
  } catch {
    agentConnected.value = false;
  }
};


const invDialogVisible = ref(false);
const invLoading = ref(false);
const currentManagedPlayer = ref({});
const inventoryData = ref([]); // 存放解析后的 JSON 数组
const editVisible = ref(false);
const tempCount = ref(1);
const activeSlot = ref(null);

// 管理按钮点击
const handleManage = async (row) => {
  currentManagedPlayer.value = row
  invLoading.value = true
  try {
    const res = await request.get('/api/player/inventory', { params: { uuid: row.uuid } })
    if (res.code === 2000) {
      // 后端已清洗并转为标准 JSON，直接使用
      inventoryData.value = res.data || []
      invDialogVisible.value = true
    }
  } catch (e) {
    console.error("获取背包失败:", e)
    ElMessage.error("获取背包数据失败")
  } finally {
    invLoading.value = false
  }
}

// 获取特定格子的数据
const getSlotItem = (slotIndex) => {
  return inventoryData.value.find(item => item.Slot === slotIndex);
};

// 点击格子
const handleItemClick = (slotIndex) => {
  const item = getSlotItem(slotIndex);
  if (!item) return;
  activeSlot.value = slotIndex;
  tempCount.value = item.Count;
  editVisible.value = true;
};

// 提交修改逻辑提取
const confirmUpdate = () => {
  doUpdate(tempCount.value);
};

// 核心逻辑：只过滤出离线玩家 (isOnline === 0)
const offlinePlayers = computed(() => (players.value || []).filter(p => p.isOnline === 0));

// 获取所有数据
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

// 封禁逻辑：调用后端 UUID 接口
const handleBan = (row) => {
  const { nickname, uuid } = row;
  ElMessageBox.confirm(`确定要封禁离线玩家 ${nickname} 吗？`, '安全警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    roundButton: true, // 保持 R-angle 风格
  }).then(async () => {
    try {
      const res = await request.post('/api/player/ban', null, {
        params: { uuid: uuid }
      });
      if (res.code === 2000) {
        ElMessage.success(`${nickname} 已被远程封禁`);
        fetchPlayers(); 
      }
    } catch (error) {}
  }).catch(() => {});
};

// 删除逻辑：将数量设为 0 传递给后端
const handleDelete = () => {
  ElMessageBox.confirm('确定要从背包中移除该物品吗？', '操作确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
    roundButton: true
  }).then(() => {
    // 核心逻辑：将数量传为 0
    doUpdate(0); 
  }).catch(() => {});
};

// 执行更新的统一方法
const doUpdate = async (count) => {
  try {
    const res = await request.post('/api/player/inventory/updateBySlot', null, {
      params: { 
        uuid: currentManagedPlayer.value.uuid,
        slot: activeSlot.value,
        newCount: count
      }
    });
    
    if (res.code === 2000) {
      ElMessage.success(res.msg);
      editVisible.value = false;
      // 重新加载背包数据，看到删除/修改后的效果
      handleManage(currentManagedPlayer.value); 
    } else {
      ElMessage.error(res.msg);
    }
  } catch (e) {
    ElMessage.error("网络异常，操作失败");
  }
};

// 时间格式化
const formatTime = (timestamp) => {
  if (!timestamp) return '无记录';
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm');
};

onMounted(() => {
  checkAgentStatus();
  fetchPlayers();
});
</script>







<style scoped>
/* ============================================================
   LAYOUT & CONTAINERS
   ============================================================ */
.player-container {
  padding: 20px;
  /* 确保整体字体遵循苹果的紧凑感 */
  letter-spacing: -0.022em;
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

/* 顶部统计卡片 */
.glass-card {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px; /* Apple 典型的 R-Corner */
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
  transition: transform 0.3s ease;
}

.stats-row {
  display: flex;
  gap: 20px;
}

.label {
  display: block;
  font-size: 13px;
  color: #86868b;
  font-weight: 600;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.value {
  font-size: 32px;
  font-weight: 700;
  color: #1d1d1f;
}

.value.offline { color: #86868b; }


:deep(.el-table) {
  background: transparent !important;
  --el-table-border-color: rgba(0, 0, 0, 0.05);
  --el-table-header-bg-color: transparent;
}

:deep(.el-table tr) {
  background: transparent !important;
  transition: background-color 0.2s ease;
}

:deep(.el-table .el-table__cell) {
  padding: 16px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

:deep(.el-table__row:hover > td) {
  background-color: rgba(0, 0, 0, 0.02) !important;
}

.nickname {
  font-weight: 600;
  font-size: 15px;
  color: #1d1d1f;
}

.time-text {
  font-size: 13px;
  color: #86868b;
}


.btn-apple {
  font-size: 14px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-manage { color: #0071e3 !important; }
.btn-manage:hover { background: rgba(0, 113, 227, 0.1); }

.btn-ban { color: #ff3b30 !important; }
.btn-ban:hover { background: rgba(255, 59, 48, 0.1); }


.inventory-wrapper {
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.inventory-grid {
  display: grid;
  grid-template-columns: repeat(9, 1fr);
  gap: 10px;
  background: rgba(0, 0, 0, 0.05); /* 模拟内陷的底盘 */
  padding: 16px;
  border-radius: 20px;
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.08); /* 关键：内阴影营造物理深度 */
  border: 1px solid rgba(0, 0, 0, 0.02);
}

.inv-slot {
  width: 42px;
  height: 42px;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.2s cubic-bezier(0.23, 1, 0.32, 1);
}

.inv-slot.has-item {
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
}

.inv-slot.has-item:hover {
  transform: translateY(-3px) scale(1.1);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.12);
  border-color: #0071e3;
  z-index: 10;
}

.empty-placeholder {
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at center, rgba(0,0,0,0.01) 0%, transparent 80%);
}

.item-icon {
  font-size: 22px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1));
}

.item-count {
  position: absolute;
  bottom: 1px;
  right: 3px;
  font-size: 10px;
  font-weight: 800;
  color: #1d1d1f;
  background: rgba(255, 255, 255, 0.85);
  padding: 0 4px;
  border-radius: 4px;
  backdrop-filter: blur(4px);
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.inventory-tip {
  margin-top: 16px;
  font-size: 12px;
  color: #86868b;
  font-weight: 500;
}

.item-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fallback-icon {
  font-size: 20px;
  opacity: 0.6;
}


:deep(.el-dialog.apple-dialog) {
  border-radius: 28px !important; /* 极大的圆角 */
  overflow: hidden;
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(40px) saturate(180%) !important;
  -webkit-backdrop-filter: blur(40px) saturate(180%) !important;
  border: 1px solid rgba(255, 255, 255, 0.4) !important;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15) !important;
}

:deep(.el-dialog__header) {
  padding: 24px 24px 12px !important;
  text-align: center !important;
  margin-right: 0 !important;
}

:deep(.el-dialog__title) {
  font-weight: 700 !important;
  font-size: 17px !important;
  color: #1d1d1f !important;
  letter-spacing: -0.4px !important;
}

:deep(.el-dialog__footer) {
  padding: 12px 24px 24px !important;
  border-top: 0.5px solid rgba(0, 0, 0, 0.05) !important;
}

/* 数字输入框 Apple 化 */
:deep(.apple-number .el-input__wrapper) {
  border-radius: 12px !important;
  background: rgba(0, 0, 0, 0.05) !important;
  box-shadow: none !important;
  border: none !important;
}

:deep(.el-button.is-round) {
  padding: 10px 20px !important;
  font-weight: 600 !important;
  border-radius: 12px !important; /* 苹果风格按钮圆角 */
}
</style>