<template>
  <div class="top">
    <registerBg :img-src="registerPg" />
    <div class="dark-overlay"></div>

    <div class="second">
      <commomBlurBackground width="360px" :radius="24" :padding="40">
        
        <h1 class="reg-title">创建凭证</h1>
        <p class="reg-subtitle">一个账号，开启你的 Minecraft 旅程。</p>

        <div class="input-group">
          <commoninputBox v-model="registerForm.username" label="基本信息" placeholder="账号" />
        </div>

        <div class="input-group">
          <commoninputBox v-model="registerForm.password" type="password" placeholder="密码" />
        </div>

        <div class="input-group sub-email-row">
          <div class="email-input">
            <commoninputBox v-model="registerForm.email" placeholder="邮箱地址" />
          </div>
         <!--  <commonButton class="sub-getSms" width="100px">获取</commonButton>  --> 
        </div>

        <button class="apple-button" style="width: 100%; margin-top: 10px;" @click="handleRegister">
          提交
        </button>

        <div class="login-footer">
          <span class="footer-hint">已有账号？</span>
          <router-link to="/" class="register-link">立即登录</router-link>
        </div>

      </commomBlurBackground>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import registerBg from '@/components/commonBackground.vue';
import registerPg from '@/assets/images/register.png';
import commomBlurBackground from '@/components/commomBlurBackground.vue';
import commoninputBox from '@/components/commonInputBox.vue';

import request from '@/utils/request';
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
const registerForm = ref({
  username: '',
  password: '',
  email: ''
});
const router = useRouter();



const handleRegister = async () => {
  try {
    const res = await request.post('/api/register', registerForm.value);
    
    // 如果执行到这里，说明 request.js 已经判断过 res.code === 2000 了
    ElMessage.success("注册成功");
    router.push('/'); 
    
  } catch (err) {
    console.error("捕捉到注册错误:", err);
    ElMessage.error(err.friendlyMsg || '注册失败，请稍后重试');
  }
};
</script>

<style scoped>
.top {
  position: fixed;
  inset: 0;
  z-index: 0;
}

/* 蒙层确保文字清晰 */
.dark-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 1;
  pointer-events: none;
}

.second {
  position: fixed;
  top: 50%;
  left: 15%;
  transform: translateY(-50%);
  z-index: 10;
}

/* 标题样式：苹果官网典型排版 */
.reg-title {
  color: #1d1d1f;
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
  text-align: center;
}

.reg-subtitle {
  color: #86868b;
  font-size: 14px;
  margin-bottom: 32px;
  text-align: center;
}

/* 输入框组间距 */
.input-group {
  width: 100%;
  margin-bottom: 16px;
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

/* 邮箱与验证码按钮的对齐核心 */
.sub-email-row {
  display: flex;
  align-items: flex-start; /* 顶部对齐 */
  gap: 10px;
}

.email-input {
  flex: 1;
}

/* 深度选择器：强制去掉邮箱输入框自带的下边距 */
.email-input :deep(.input-container) {
  margin-bottom: 0 !important;
}

/* 验证码按钮：高度与输入框保持一致 */
.sub-getSms {
  height: 50px; 
  margin-top: 0;
  white-space: nowrap;
  font-size: 14px;
}

.login-footer {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f2f2f7;
  text-align: center;
  font-size: 13px;
}

.footer-hint {
  color: #86868b;
}

.register-link {
  color: #0066cc;
  text-decoration: none;
  margin-left: 4px;
}

.register-link:hover {
  text-decoration: underline;
}
</style>