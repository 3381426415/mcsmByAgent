<template>
  <div class="baned-page">
    <div class="baned-card">
      <div class="baned-icon">🚫</div>
      <h1 class="baned-title">账号已被封禁</h1>
      <p class="baned-desc">您的账号已被管理员封禁，暂时无法访问系统。</p>
      <p class="baned-hint">如有疑问，请联系管理员。</p>
      <button class="baned-btn" @click="handleLogout">返回登录页</button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('user_permissions')
  localStorage.removeItem('user_roles')
  router.push('/')
}
</script>

<style scoped>
.baned-page {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f7;
}

.baned-card {
  background: #fff;
  border-radius: 20px;
  padding: 48px;
  text-align: center;
  max-width: 400px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}

.baned-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.baned-title {
  font-size: 20px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 12px;
}

.baned-desc {
  font-size: 14px;
  color: #86868b;
  margin: 0 0 8px;
  line-height: 1.5;
}

.baned-hint {
  font-size: 13px;
  color: #aeaeb2;
  margin: 0 0 24px;
}

.baned-btn {
  background: #0071e3;
  color: #fff;
  border: none;
  padding: 10px 24px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.baned-btn:hover {
  background: #0077ed;
}
</style>
