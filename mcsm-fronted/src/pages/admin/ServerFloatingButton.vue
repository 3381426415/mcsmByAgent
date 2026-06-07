<template>
  <div class="server-floating">
    <el-button
      class="floating-btn"
      type="info"
      circle
      @click="drawerVisible = true"
      v-show="!drawerVisible"
    >
      <el-icon size="24"><Monitor /></el-icon>
    </el-button>

    <el-drawer
      v-model="drawerVisible"
      title="服务器管理"
      direction="ltr"
      size="380px"
    >
      <div class="server-list">
        <div
          v-for="s in serverList"
          :key="s.serverId"
          class="server-item"
          @click="goToServer(s.serverId)"
        >
          <div class="server-item-top">
            <span class="server-name">{{ s.name }}</span>
            <el-tag :type="statusType(s.status)" size="small">{{ s.status }}</el-tag>
          </div>
          <div class="server-item-info">
            <span>PID: {{ s.pid > 0 ? s.pid : '-' }}</span>
            <span>端口: {{ s.port || '-' }}</span>
          </div>
        </div>

        <div v-if="serverList.length === 0" class="empty-tip">
          暂无服务器
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { Monitor } from '@element-plus/icons-vue';
import request from '@/utils/request';

const router = useRouter();
const drawerVisible = ref(false);
const serverList = ref([]);

const fetchServerList = async () => {
  try {
    const res = await request.get('/api/server/servers');
    if (res.code === 2000) serverList.value = res.data || [];
  } catch (e) {
    console.error('获取服务器列表失败:', e);
  }
};

const goToServer = (serverId) => {
  drawerVisible.value = false;
  router.push(`/adminPanel/serverM/${serverId}`);
};

const statusType = (status) => {
  if (status === 'RUNNING') return 'success';
  if (status === 'STARTING' || status === 'STOPPING') return 'warning';
  return 'info';
};

// 打开 drawer 时刷新列表
import { watch } from 'vue';
watch(drawerVisible, (val) => {
  if (val) fetchServerList();
});
</script>

<style scoped>
.server-floating {
  position: fixed;
  bottom: 24px;
  left: 24px;
  z-index: 1100;
}

.floating-btn {
  width: 52px;
  height: 52px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.floating-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.3);
}

.server-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.server-item {
  padding: 14px 16px;
  border-radius: 10px;
  background: #f5f7fa;
  cursor: pointer;
  transition: background 0.15s, transform 0.15s;
}

.server-item:hover {
  background: #ecf5ff;
  transform: translateX(2px);
}

.server-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.server-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.server-item-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.empty-tip {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
  font-size: 14px;
}
</style>
