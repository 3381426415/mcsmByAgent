<template>
  <div>
    <loginBg :imgSrc="loginPg" />

    <div style="text-align: center;">
      <p class="title">MC玩家服务系统</p>
      <p class="subtitle">PaperMC Server Management</p>
    </div>

    <div class="loginWrite-box">
      <commomBlurBackground width="320px" :radius="20">
        <h1 class="reg-title">输入凭证</h1>

        <div class="input-group">
          <commoninputBox v-model="loginForm.username" label="账号" placeholder="请输入账号" @keyup.enter="handleLogin" />
        </div>

        <div class="input-group">
          <commoninputBox v-model="loginForm.password" type="password" placeholder="密码" @keyup.enter="handleLogin" />
        </div>

         <button class="apple-button" style="width: 100%; margin-top: 10px;" @click="handleLogin">
          登入
        </button>

        <div class="login-footer">
          <span class="footer-hint">还没有账号？</span>
          <router-link to="/register" class="register-link">立即注册</router-link>
        </div>
        
      </commomBlurBackground>
    </div>
  </div>
</template>


<script setup>

import commoninputBox from '@/components/commonInputBox.vue';
import  {ref}  from 'vue';
import { useRouter } from 'vue-router'
import commomBlurBackground from '@/components/commomBlurBackground.vue';
import loginBg from '@/components/commonBackground.vue' ;
import loginPg from '@/assets/images/login.png';
import { ElMessage } from 'element-plus'
const router = useRouter() 


import request from '@/utils/request';




// login
const loginForm = ref({
  username: '',
  password: ''
})
// 防重复提交标志
const loginLoading = ref(false)

const handleLogin = async () => {
  // 防止重复提交
  if (loginLoading.value) {
    return
  }
  
  console.log('handleLogin 被调用'); 
  
  loginLoading.value = true
  
  try {
    // 1. 发起登录请求
    const res = await request.post('/api/login', loginForm.value);
    
    // 2. 解构后端返回的 LoginVo 数据
    const { token, roles, permissions } = res.data;

    // 3. 持久化存储
    localStorage.setItem('token', token);
    localStorage.setItem('username', loginForm.value.username);
    localStorage.setItem('user_roles', JSON.stringify(roles || []));
    localStorage.setItem('user_permissions', JSON.stringify(permissions || []));

    // 4. 成功提示与跳转（基于权限码判断，与路由守卫一致）
    ElMessage.success(res.msg || "欢迎回来");
    const permList = permissions || [];
    if (permList.some(p => p.startsWith('admin:'))) {
      router.push({ name: 'adminPanel' });
    } else {
      router.push({ name: 'userPanel' });
    }

  } catch (err) {
    // 5. 异常处理
    // 注意：业务错误（如密码错）和 HTTP 错误（如 500）已经在 request.js 拦截器里弹过 ElMessage 了
    // 这里只需处理组件内部的状态重置
    console.error("登录逻辑执行失败:", err);
    
    // 清空密码框，提升安全性（Apple 风格的严谨感）
    loginForm.value.password = '';
  } finally {
    // 无论成功还是失败，都重置 loading 状态
    loginLoading.value = false
  }
};




</script>

<style scoped>
:deep(*) {
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Myriad Set Pro", "SF Pro Icons", "Helvetica Neue", Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* 2. 标题：纯黑、稳重、去除所有特效 */
.title {
  color: #ffffff !important;
  font-size: 3.5rem;
  font-weight: 700;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.2); /* 淡淡的投影确保在白云处也能看清 */
  background: none !important;

 
  -webkit-background-clip: initial !important; /* 兼容旧版 WebKit 浏览器 */
  background-clip: initial !important;         /* 标准属性，确保未来兼容性 */
}

.subtitle {
  color: rgba(255, 255, 255, 0.7) !important; /* 70% 透明度的白色 */
  font-size: 1.1rem;
  letter-spacing: 2px;
  text-shadow: 0 1px 10px rgba(0, 0, 0, 0.2);
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
}

.apple-button:hover {
  opacity: 0.85;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.15);
}

.apple-button:active {
  transform: scale(0.98);
  opacity: 0.7;
}

/* 4. 登录卡片位置调整 */
.loginWrite-box {
  position: fixed;
  left: 70%;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
}

/* 5. 输入框间距微调 */
.input-group {
  margin-bottom: 18px;
  width: 100%;
}

/* 6. 底部注册区域：极致简约 */
.login-footer {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #d2d2d7; /* 极细的分割线 */
  text-align: center;
  font-size: 14px;
  width: 100%;
}
.reg-title {
  color: #1d1d1f;
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
  text-align: center;
}
.footer-hint {
  color: #86868b;
}

.register-link {
  color: #0066cc;          
  text-decoration: none;
  font-weight: 400;
  margin-left: 6px;
  transition: all 0.2s ease;
}

.register-link:hover {
  color: #004499;          /* 悬停加深 */
  text-decoration: underline;
  /* 移除发光特效 */
  filter: none !important;
  text-shadow: none !important;
}




/* 移除不再需要的冗余样式 */
.login-btn, .message-box, .mobile-only {
  display: none;
}
</style>
