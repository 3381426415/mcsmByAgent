<template>
  <div class="admin-layout">
    <!-- 公告弹窗 -->
    <el-dialog
      v-model="announcementDialogVisible"
      :show-close="false"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      width="500px"
      class="announcement-dialog"
    >
      <template #header>
        <div class="dialog-header">
          <span class="dialog-title">📢 {{ currentAnnouncement?.title || '系统公告' }}</span>
        </div>
      </template>
      
      <div class="announcement-content">
        <p>{{ currentAnnouncement?.content }}</p>
        <div class="announcement-time" v-if="currentAnnouncement?.createTime">
          {{ formatTime(currentAnnouncement.createTime) }}
        </div>
      </div>

      <div class="announcement-pagination" v-if="announcements.length > 1">
        <span class="page-info">{{ currentIndex + 1 }} / {{ announcements.length }}</span>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button 
            v-if="announcements.length > 1 && currentIndex > 0" 
            @click="prevAnnouncement"
          >
            上一条
          </el-button>
          <el-button 
            v-if="announcements.length > 1 && currentIndex < announcements.length - 1" 
            @click="nextAnnouncement"
          >
            下一条
          </el-button>
          <el-button type="primary" @click="handleClose">
            {{ currentIndex < announcements.length - 1 ? '下一条' : '我知道了' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 头部 -->
    <header class="app-header">
      <div class="logo">MC玩家服务系统</div>
      <div class="user-actions">
        <!-- 通知图标 -->
        <el-popover
          placement="bottom-end"
          :width="360"
          trigger="click"
          @show="fetchNotifications"
        >
          <template #reference>
            <div class="notification-icon">
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
                <el-icon :size="20"><Bell /></el-icon>
              </el-badge>
            </div>
          </template>
          
          <div class="notification-panel">
            <div class="notification-header">
              <span>通知中心</span>
              <el-button type="primary" link size="small" @click="markAllAsRead" v-if="unreadCount > 0">
                全部已读
              </el-button>
            </div>
            
            <div class="notification-list" v-loading="notificationLoading">
              <div v-if="notifications.length === 0" class="empty-notification">
                暂无通知
              </div>
              <div 
                v-for="item in notifications" 
                :key="item.id" 
                class="notification-item"
                :class="{ unread: item.isRead === 0 }"
                @click="readNotification(item)"
              >
                <div class="notification-title">
                  <span class="dot" v-if="item.isRead === 0"></span>
                  {{ item.title }}
                </div>
                <div class="notification-content">{{ item.content }}</div>
                <div class="notification-time">{{ formatTime(item.createTime) }}</div>
              </div>
            </div>
            
            <div class="notification-footer" v-if="total > 5">
              <el-button type="primary" link @click="viewAllNotifications">查看全部</el-button>
            </div>
          </div>
        </el-popover>

        <span class="user-name">{{ username }}</span>
        <div class="divider"></div>
        <button @click="handleLogout" class="logout-btn">退出登录</button>
      </div>
    </header>

    <div class="app-body">
      <aside class="app-aside">
        <nav class="side-nav">
          <router-link to="/userPanel/accountSetting" class="nav-item" active-class="active">
            账户设定
          </router-link>
          <router-link to="/userPanel/myPackage" class="nav-item" active-class="active">
            背包物品
          </router-link>
          <router-link to="/userPanel/personalTradeRecord" class="nav-item" active-class="active">
            交易管理
          </router-link>
          <router-link to="/userPanel/tradeMarket" class="nav-item" active-class="active">
            交易市场
          </router-link>
        </nav>
      </aside>

      <main class="app-main">
        <router-view></router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox, ElMessage } from 'element-plus';
import { Bell } from '@element-plus/icons-vue';
import request from '@/utils/request';

const router = useRouter();
const username = ref('user');

// 公告相关
const announcementDialogVisible = ref(false);
const announcements = ref([]);
const currentIndex = ref(0);

const currentAnnouncement = computed(() => {
  return announcements.value[currentIndex.value] || null;
});

// 通知相关
const notifications = ref([]);
const unreadCount = ref(0);
const notificationLoading = ref(false);
const total = ref(0);
let unreadTimer = null;

// 获取最新公告（只弹未看过的）
const fetchAnnouncements = async () => {
  try {
    const res = await request.get('/api/announcement/latest');
    if (res.code === 2000 && res.data && res.data.length > 0) {
      const allAnnouncements = res.data;
      
      // 获取上次看到的最后一个公告ID
      const lastSeenId = localStorage.getItem('last_seen_announcement_id');
      
      // 过滤出比上次更新的公告
      const newAnnouncements = lastSeenId 
        ? allAnnouncements.filter(a => a.id > parseInt(lastSeenId))
        : allAnnouncements;
      
      if (newAnnouncements.length > 0) {
        announcements.value = newAnnouncements;
        currentIndex.value = 0;
        announcementDialogVisible.value = true;
      }
    }
  } catch (error) {
    console.error('获取公告失败:', error);
  }
};

// 下一条公告
const nextAnnouncement = () => {
  if (currentIndex.value < announcements.value.length - 1) {
    currentIndex.value++;
  }
};

// 上一条公告
const prevAnnouncement = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--;
  }
};

// 关闭公告弹窗（记录最后看到的ID）
const handleClose = () => {
  if (currentIndex.value < announcements.value.length - 1) {
    currentIndex.value++;
  } else {
    announcementDialogVisible.value = false;
    
    // 记录本次看到的最后一个公告ID
    if (announcements.value.length > 0) {
      const maxId = Math.max(...announcements.value.map(a => a.id));
      const lastSeenId = localStorage.getItem('last_seen_announcement_id');
      if (!lastSeenId || maxId > parseInt(lastSeenId)) {
        localStorage.setItem('last_seen_announcement_id', maxId);
      }
    }
  }
};

// 获取未读通知数量
const fetchUnreadCount = async () => {
  try {
    const res = await request.get('/api/notification/unread-count', { silent: true });
    if (res.code === 2000) {
      unreadCount.value = res.data || 0;
    }
  } catch (error) {
    console.error('获取未读数量失败:', error);
  }
};

// 获取通知列表
const fetchNotifications = async () => {
  notificationLoading.value = true;
  try {
    const res = await request.get('/api/notification/list', {
      params: { page: 1, size: 5 }
    });
    if (res.code === 2000) {
      notifications.value = res.data.records || [];
      total.value = res.data.total || 0;
    }
  } finally {
    notificationLoading.value = false;
  }
};

// 阅读单条通知
const readNotification = async (item) => {
  if (item.isRead === 0) {
    try {
      await request.post(`/api/notification/read/${item.id}`);
      item.isRead = 1;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    } catch (error) {
      console.error('标记已读失败:', error);
    }
  }
};

// 全部已读
const markAllAsRead = async () => {
  try {
    await request.post('/api/notification/read-all');
    notifications.value.forEach(n => n.isRead = 1);
    unreadCount.value = 0;
    ElMessage.success('已全部标记为已读');
  } catch (error) {
    console.error('全部已读失败:', error);
  }
};

// 查看全部通知
const viewAllNotifications = () => {
  ElMessage.info('功能开发中');
};

// 格式化时间
const formatTime = (time) => {
  if (!time) return '';
  return time.replace('T', ' ').substring(0, 16);
};

// 退出登录
const handleLogout = () => {
  ElMessageBox.confirm(
    '您确定要退出当前账号吗？',
    '退出登录',
    {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
      center: true,
      customClass: 'apple-message-box'
    }
  ).then(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('user_roles');
    localStorage.removeItem('user_permissions');
    // 注意：不清除 last_seen_announcement_id，保留用户的公告阅读状态
    ElMessage({ message: '已安全退出', type: 'success' });
    router.push('/');
  }).catch(() => {});
};

// 初始化用户名
const storedName = localStorage.getItem('username');
if (storedName) {
  username.value = storedName;
}

onMounted(() => {
  fetchAnnouncements();
  fetchUnreadCount();
  // 每30秒刷新一次未读数量
  unreadTimer = setInterval(fetchUnreadCount, 30000);
});

onUnmounted(() => {
  if (unreadTimer) {
    clearInterval(unreadTimer);
  }
});
</script>

<style scoped>
/* 公告弹窗样式 */
.announcement-dialog :deep(.el-dialog) {
  border-radius: 20px;
}

.announcement-dialog :deep(.el-dialog__header) {
  padding: 24px 24px 16px;
  margin: 0;
}

.dialog-header {
  display: flex;
  align-items: center;
}

.dialog-title {
  font-size: 20px;
  font-weight: 600;
  color: #1d1d1f;
}

.announcement-content {
  padding: 0 24px;
}

.announcement-content p {
  font-size: 15px;
  line-height: 1.6;
  color: #424245;
  margin-bottom: 16px;
  white-space: pre-wrap;
  word-break: break-word;
}

.announcement-time {
  font-size: 12px;
  color: #86868b;
  text-align: right;
}

.announcement-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.page-info {
  font-size: 13px;
  color: #86868b;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-footer .el-button {
  border-radius: 8px;
  padding: 10px 24px;
}

/* 通知样式 */
.notification-icon {
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: background 0.2s;
  margin-right: 8px;
  display: flex;
  align-items: center;
}

.notification-icon:hover {
  background: rgba(0, 0, 0, 0.05);
}

.notification-panel {
  max-height: 400px;
  display: flex;
  flex-direction: column;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f2f2f7;
  font-weight: 600;
  color: #1d1d1f;
}

.notification-list {
  flex: 1;
  overflow-y: auto;
  max-height: 300px;
}

.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f2f2f7;
  cursor: pointer;
  transition: background 0.2s;
}

.notification-item:hover {
  background: #f5f5f7;
}

.notification-item.unread {
  background: rgba(0, 113, 227, 0.03);
}

.notification-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
  display: flex;
  align-items: center;
  gap: 6px;
}

.dot {
  width: 6px;
  height: 6px;
  background: #0071e3;
  border-radius: 50%;
}

.notification-content {
  font-size: 13px;
  color: #86868b;
  margin-top: 4px;
}

.notification-time {
  font-size: 11px;
  color: #a1a1a6;
  margin-top: 6px;
}

.notification-footer {
  padding: 12px 16px;
  text-align: center;
  border-top: 1px solid #f2f2f7;
}

.empty-notification {
  padding: 40px;
  text-align: center;
  color: #86868b;
  font-size: 14px;
}

/* 原有样式 */
.admin-layout {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro SC", "PingFang SC", sans-serif;
  -webkit-font-smoothing: antialiased;
}

.app-header {
  height: 52px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.logo {
  font-weight: 600;
  font-size: 15px;
  color: #1d1d1f;
  letter-spacing: -0.01em;
}

.app-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.app-aside {
  width: 210px;
  background-color: #f5f5f7;
  border-right: 0.5px solid rgba(0, 0, 0, 0.1);
  padding: 12px 0;
}

.nav-item {
  margin: 2px 10px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #424245;
  text-decoration: none;
  display: flex;
  align-items: center;
  transition: all 0.2s ease;
}

.nav-item:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.nav-item.active {
  background-color: #0071e3;
  color: #ffffff;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(0, 113, 227, 0.3);
}

.app-main {
  flex: 1;
  background-color: #ffffff;
  overflow-y: auto;
  padding: 40px 60px;
}

.card {
  background: #ffffff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(0, 0, 0, 0.02);
}

.user-actions {
  margin-left: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 13px;
  color: #86868b;
  font-weight: 400;
}

.divider {
  width: 1px;
  height: 14px;
  background-color: rgba(0, 0, 0, 0.1);
}

.logout-btn {
  background: none;
  border: none;
  color: #0071e3;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.logout-btn:hover {
  background-color: rgba(0, 113, 227, 0.05);
  text-decoration: underline;
}

.logout-btn:active {
  opacity: 0.7;
}
</style>