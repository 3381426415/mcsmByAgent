<template>
  <div>
    <loginBg :imgSrc="loginPg" />

    <div style="text-align: center;">
      <p class="title">MCSM 安装向导</p>
      <p class="subtitle">Minecraft Server Manager Setup</p>
    </div>

    <div class="setup-box">
      <commomBlurBackground width="420px" :radius="20">
        <!-- 创建管理员 -->
        <div v-if="!finished" class="step-content">
          <h1 class="reg-title">创建管理员</h1>
          <p class="hint" v-if="!dbReady">请先运行 install-mysql.bat 配置数据库</p>

          <div class="input-group">
            <commoninputBox v-model="adminForm.username" label="管理员账号" placeholder="admin" />
          </div>
          <div class="input-group">
            <commoninputBox v-model="adminForm.password" type="password" label="密码" placeholder="至少6位" />
          </div>
          <div class="input-group">
            <commoninputBox v-model="adminForm.confirmPassword" type="password" label="确认密码" placeholder="再次输入密码" />
          </div>

          <button class="apple-button primary" :disabled="!dbReady || loading" @click="createAdmin">
            {{ loading ? '创建中...' : '完成安装' }}
          </button>
        </div>

        <!-- 完成 -->
        <div v-else class="step-content" style="text-align: center;">
          <h1 class="reg-title">安装完成</h1>
          <p style="color: #86868b; margin: 20px 0;">配置已保存，请重启后端服务后登录</p>
          <button class="apple-button primary" @click="goLogin">前往登录</button>
        </div>
      </commomBlurBackground>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import commoninputBox from '@/components/commonInputBox.vue'
import commomBlurBackground from '@/components/commomBlurBackground.vue'
import loginBg from '@/components/commonBackground.vue'
import loginPg from '@/assets/images/login.png'
import request from '@/utils/request'

const router = useRouter()
const finished = ref(false)
const dbReady = ref(false)
const loading = ref(false)

const adminForm = ref({
  username: 'admin',
  password: '',
  confirmPassword: ''
})

onMounted(async () => {
  try {
    const res = await request.get('/api/setup/status', { silent: true })
    dbReady.value = res.data?.dbReady || false
  } catch (e) {
    dbReady.value = false
  }
})

async function createAdmin() {
  if (adminForm.value.password !== adminForm.value.confirmPassword) {
    ElMessage.error('两次密码输入不一致')
    return
  }
  if (adminForm.value.password.length < 6) {
    ElMessage.error('密码长度不能少于6位')
    return
  }

  loading.value = true
  try {
    const payload = {
      username: adminForm.value.username,
      password: adminForm.value.password
    }
    await request.post('/api/setup/admin', payload)
    await request.post('/api/setup/complete')

    ElMessage.success('安装完成')
    finished.value = true
  } catch (e) {
    ElMessage.error(e.friendlyMsg || e.msg || '创建失败')
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/')
}
</script>

<style scoped>
:deep(*) {
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Myriad Set Pro", "SF Pro Icons", "Helvetica Neue", Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

.title {
  color: #ffffff !important;
  font-size: 3.5rem;
  font-weight: 700;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.2);
  background: none !important;
  -webkit-background-clip: initial !important;
  background-clip: initial !important;
}

.subtitle {
  color: rgba(255, 255, 255, 0.7) !important;
  font-size: 1.1rem;
  letter-spacing: 2px;
  text-shadow: 0 1px 10px rgba(0, 0, 0, 0.2);
}

.setup-box {
  position: fixed;
  left: 50%;
  top: 55%;
  transform: translate(-50%, -50%);
  z-index: 10;
}

.reg-title {
  color: #1d1d1f;
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
  text-align: center;
}

.hint {
  color: #ff3b30;
  font-size: 14px;
  text-align: center;
  margin-bottom: 16px;
}

.input-group {
  margin-bottom: 14px;
  width: 100%;
}

.step-content {
  width: 100%;
}

.apple-button {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 24px;
  border-radius: 12px;
  border: none;
  cursor: pointer;
  background: #1d1d1f;
  color: #ffffff;
  font-size: 17px;
  font-weight: 500;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  outline: none;
  width: 100%;
  margin-top: 20px;
}

.apple-button:hover {
  opacity: 0.85;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.15);
}

.apple-button:active {
  transform: scale(0.98);
  opacity: 0.7;
}

.apple-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.apple-button.primary {
  background: #0066cc;
}

.apple-button.primary:hover {
  background: #0055aa;
}
</style>
