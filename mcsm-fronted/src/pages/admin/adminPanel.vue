<template>
  <div class="apple-admin-panel">
    <!-- Apple 风格头部 -->
    <header class="apple-header">
      <div class="apple-header__left">
        <div class="apple-header__logo">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
            <line x1="8" y1="21" x2="16" y2="21"></line>
            <line x1="12" y1="17" x2="12" y2="21"></line>
          </svg>
          <h1 class="apple-header__title">服务器管理终端</h1>
        </div>
      </div>

      <div class="apple-header__right">
        <div class="apple-user-info">
          <span class="apple-user-avatar">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
          </span>
          <span class="apple-user-name">{{ username }}</span>
          <span class="apple-user-role">管理员</span>
        </div>

        <div class="apple-header__actions">
          <button @click="settingsVisible = true" class="apple-settings-btn" title="系统设置">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="3"></circle>
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
            </svg>
          </button>

          <router-link to="/userPanel" class="apple-user-mode-btn">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            用户模式
          </router-link>

          <button @click="handleLogout" class="apple-logout-btn">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
              <polyline points="16 17 21 12 16 7"></polyline>
              <line x1="21" y1="12" x2="9" y2="12"></line>
            </svg>
            退出登录
          </button>
        </div>
      </div>
    </header>

    <div class="apple-body">
      <!-- Apple 风格侧边栏 -->
      <aside class="apple-sidebar">
        <nav class="apple-nav">
          <router-link
            to="/adminPanel/serverM"
            class="apple-nav-item"
            active-class="apple-nav-item--active"
            v-if="hasPermission('admin:server')"
          >
            <svg class="apple-nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect>
              <rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect>
              <line x1="6" y1="6" x2="6.01" y2="6"></line>
              <line x1="6" y1="18" x2="6.01" y2="18"></line>
            </svg>
            <span class="apple-nav-text">服务器管理</span>
          </router-link>

          <router-link
            to="/adminPanel/onlinePlayerM"
            class="apple-nav-item"
            active-class="apple-nav-item--active"
            v-if="hasPermission('admin:player')"
          >
            <svg class="apple-nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
            <span class="apple-nav-text">在线玩家管理</span>
          </router-link>

          <router-link
            to="/adminPanel/offlinePlayerM"
            class="apple-nav-item"
            active-class="apple-nav-item--active"
            v-if="hasPermission('admin:player')"
          >
            <svg class="apple-nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
              <circle cx="9" cy="7" r="4"></circle>
              <line x1="18" y1="8" x2="23" y2="13"></line>
              <line x1="23" y1="8" x2="18" y2="13"></line>
            </svg>
            <span class="apple-nav-text">离线玩家管理</span>
          </router-link>

          <router-link
            to="/adminPanel/marketM"
            class="apple-nav-item"
            active-class="apple-nav-item--active"
            v-if="hasPermission('admin:market:view') || hasPermission('admin:market:withdraw')"
          >
            <svg class="apple-nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="9" cy="21" r="1"></circle>
              <circle cx="20" cy="21" r="1"></circle>
              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
            </svg>
            <span class="apple-nav-text">市场管理</span>
          </router-link>

          <router-link
            to="/adminPanel/userM"
            class="apple-nav-item"
            active-class="apple-nav-item--active"
            v-if="hasPermission('admin:user')"
          >
            <svg class="apple-nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            <span class="apple-nav-text">用户管理</span>
          </router-link>

          <router-link
            to="/adminPanel/pluginM"
            class="apple-nav-item"
            active-class="apple-nav-item--active"
            v-if="hasPermission('admin:plugin')"
          >
            <svg class="apple-nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
            </svg>
            <span class="apple-nav-text">插件管理</span>
          </router-link>

          <router-link
            to="/adminPanel/frpM"
            class="apple-nav-item"
            active-class="apple-nav-item--active"
            v-if="hasPermission('admin:server')"
          >
            <svg class="apple-nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path>
              <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>
            </svg>
            <span class="apple-nav-text">内网穿透</span>
          </router-link>

          <router-link
            to="/adminPanel/announcementM"
            class="apple-nav-item"
            active-class="apple-nav-item--active"
            v-if="hasPermission('admin:announcement')"
          >
            <svg class="apple-nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
            </svg>
            <span class="apple-nav-text">公告管理</span>
          </router-link>

          <router-link
            to="/adminPanel/logM"
            class="apple-nav-item"
            active-class="apple-nav-item--active"
            v-if="hasPermission('admin:log')"
          >
            <svg class="apple-nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="16" y1="13" x2="8" y2="13"></line>
              <line x1="16" y1="17" x2="8" y2="17"></line>
              <polyline points="10 9 9 9 8 9"></polyline>
            </svg>
            <span class="apple-nav-text">操作日志</span>
          </router-link>
        </nav>
      </aside>

      <!-- 主内容区 -->
      <main class="apple-main">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </main>
    </div>

    <!-- 设置面板 -->
    <SettingsPanel v-model:visible="settingsVisible" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox, ElMessage } from 'element-plus';
import SettingsPanel from '@/pages/admin/SettingsPanel.vue';

const router = useRouter();
const settingsVisible = ref(false);

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
    // 1. 清理本地存储
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('user_roles');
    localStorage.removeItem('user_permissions');
    
    // 2. 提示并跳转
    ElMessage({
      message: '已安全退出',
      type: 'success',
    });
    router.push('/');
  }).catch(() => {
    // 用户取消退出
  });
};

const username = ref('user'); // 定义响应式变量

// 修改 ref 的值必须使用 .value
const storedName = localStorage.getItem('username');
if (storedName) {
  username.value = storedName;
}


const permissions = computed(() => {
    return JSON.parse(localStorage.getItem('user_permissions') || '[]');
});
const hasPermission = (perm) => {
    return permissions.value.includes(perm);
};

</script>





<style scoped>
.apple-admin-panel {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  background: var(--apple-bg-primary);
  font-family: var(--apple-font-body);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* Apple 风格头部 */
.apple-header {
  height: 52px;
  background: var(--apple-glass-bg);
  backdrop-filter: var(--apple-backdrop-blur);
  -webkit-backdrop-filter: var(--apple-backdrop-blur);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: var(--apple-glass-border);
  z-index: 100;
}

.apple-header__left {
  display: flex;
  align-items: center;
}

.apple-header__logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.apple-header__logo svg {
  color: var(--apple-text-secondary);
}

.apple-header__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--apple-text-primary);
  margin: 0;
  letter-spacing: var(--apple-tracking-tight);
}

.apple-header__right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 用户信息 */
.apple-user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: var(--apple-bg-secondary);
  border-radius: var(--apple-radius-full);
}

.apple-user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: var(--apple-accent-light);
  border-radius: 50%;
  color: var(--apple-accent);
}

.apple-user-avatar svg {
  width: 14px;
  height: 14px;
}

.apple-user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--apple-text-primary);
}

.apple-user-role {
  font-size: 11px;
  color: var(--apple-text-secondary);
  background: var(--apple-accent-light);
  padding: 2px 8px;
  border-radius: var(--apple-radius-full);
}

/* 头部操作按钮 */
.apple-header__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.apple-user-mode-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--apple-bg-secondary);
  border: 1px solid var(--apple-border);
  border-radius: var(--apple-radius-md);
  color: var(--apple-text-secondary);
  font-size: 12px;
  font-weight: 500;
  text-decoration: none;
  transition: all var(--apple-transition-fast);
  cursor: pointer;
}

.apple-user-mode-btn:hover {
  background: var(--apple-bg-tertiary);
  color: var(--apple-text-primary);
  border-color: var(--apple-border-hover);
}

.apple-user-mode-btn svg {
  color: var(--apple-text-muted);
}

.apple-logout-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: transparent;
  border: 1px solid var(--apple-border);
  border-radius: var(--apple-radius-md);
  color: var(--apple-text-secondary);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--apple-transition-fast);
  font-family: var(--apple-font-body);
}

.apple-logout-btn:hover {
  background: var(--apple-error-light);
  color: var(--apple-error);
  border-color: var(--apple-error);
}

.apple-logout-btn svg {
  color: var(--apple-text-muted);
}

.apple-logout-btn:hover svg {
  color: var(--apple-error);
}

/* 设置按钮 */
.apple-settings-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: 1px solid var(--apple-border);
  border-radius: var(--apple-radius-md);
  color: var(--apple-text-secondary);
  cursor: pointer;
  transition: all var(--apple-transition-fast);
}

.apple-settings-btn:hover {
  background: var(--apple-bg-tertiary);
  color: var(--apple-text-primary);
  border-color: var(--apple-border-hover);
}

.apple-settings-btn svg {
  color: var(--apple-text-muted);
}

.apple-settings-btn:hover svg {
  color: var(--apple-accent);
}

/* 主体布局 */
.apple-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* Apple 风格侧边栏 */
.apple-sidebar {
  width: 220px;
  background: var(--apple-bg-secondary);
  border-right: 1px solid var(--apple-border);
  padding: 16px 0;
  overflow-y: auto;
}

.apple-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 8px;
}

.apple-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  color: var(--apple-text-secondary);
  text-decoration: none;
  border-radius: var(--apple-radius-md);
  transition: all var(--apple-transition-fast);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: var(--apple-tracking-normal);
}

.apple-nav-item:hover {
  background: rgba(0, 0, 0, 0.03);
  color: var(--apple-text-primary);
}

.apple-nav-item--active {
  background: var(--apple-accent-light);
  color: var(--apple-accent);
}

.apple-nav-item--active:hover {
  background: rgba(0, 113, 227, 0.15);
}

.apple-nav-icon {
  width: 16px;
  height: 16px;
  color: var(--apple-text-muted);
  flex-shrink: 0;
}

.apple-nav-item--active .apple-nav-icon {
  color: var(--apple-accent);
}

.apple-nav-text {
  flex: 1;
}

/* 主内容区 */
.apple-main {
  flex: 1;
  background: var(--apple-bg-primary);
  overflow-y: auto;
  padding: 32px 40px;
  position: relative;
}

.apple-main::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--apple-bg-primary);
  pointer-events: none;
  z-index: -1;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .apple-sidebar {
    width: 60px;
  }

  .apple-nav-text {
    display: none;
  }

  .apple-nav-item {
    justify-content: center;
    padding: 12px;
  }

  .apple-main {
    padding: 20px;
  }

  .apple-header__title {
    font-size: 14px;
  }

  .apple-user-name,
  .apple-user-role {
    display: none;
  }

  .apple-user-mode-btn span,
  .apple-logout-btn span {
    display: none;
  }
}
</style>