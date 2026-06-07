<template>
  <showCard title="账户设定" subtitle="修改账户资料">
    <div class="profile-container">
      <!-- 错误状态标签 -->
      <div v-if="loadError" class="error-banner">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="15" y1="9" x2="9" y2="15"></line>
          <line x1="9" y1="9" x2="15" y2="15"></line>
        </svg>
        <span>{{ loadError }}</span>
      </div>
      <BaseInput
        label="用户名"
        v-model="formData.username"
        readonly
        class="readonly-input"
      />

      <BaseInput
        label="电子邮箱"
        v-model="formData.email"
        placeholder="请输入新的邮箱地址"
      />

      <div class="action-input-group">
        <BaseInput
          label="绑定 ID"
          v-model="formData.bindId"
          readonly
          class="readonly-input"
          width="100%"
        />
        <button 
          v-if="formData.bindId" 
          @click="handleUnbind" 
          class="action-btn unbind-btn"
        >
          解绑
        </button>
      </div>

      <!-- 余额行：添加充值按钮 -->
      <div class="money-row">
        <BaseInput
          label="账户余额"
          v-model="formattedMoney"
          readonly
          class="readonly-input money-input"
        />
        <button @click="openRedeemDialog" class="recharge-btn">
          充值
        </button>
      </div>

      <div class="form-actions">
        <button 
          @click="handleSave" 
          class="save-btn" 
          :disabled="!isChanged"
        >
          保存更改
        </button>
      </div>
    </div>

    <!-- 兑换码弹窗 -->
    <el-dialog v-model="redeemDialogVisible" title="兑换码充值" width="350px" align-center>
      <div class="redeem-form">
        <p style="margin-bottom: 16px; color: #86868b; font-size: 14px;">
          请输入您获得的兑换码，兑换后将自动充值到账户余额。
        </p>
        <el-input 
          v-model="redeemCode" 
          placeholder="请输入兑换码" 
          clearable
          size="large"
          @keyup.enter="handleRedeem"
        />
      </div>
      <template #footer>
        <el-button @click="redeemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRedeem" :loading="redeemLoading">
          确认兑换
        </el-button>
      </template>
    </el-dialog>
  </showCard>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import showCard from '@/components/commonShowPanel.vue'
import BaseInput from '@/components/commonInputBox.vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

// 1. 响应式表单数据
const formData = reactive({
  username: '',
  email: '',
  bindId: '',
  money: 0
})

// 2. 原始数据备份
const originalData = ref(null)

// 3. 兑换码相关
const redeemDialogVisible = ref(false)
const redeemCode = ref('')
const redeemLoading = ref(false)
const loadError = ref('')

// 4. 计算属性：格式化余额显示
const formattedMoney = computed(() => {
  return `￥${(formData.money || 0).toLocaleString()}`
})

// 5. 变动检测
const isChanged = computed(() => {
  if (!originalData.value) return false
  return formData.email !== originalData.value.email
})

// 6. 获取当前登录用户信息
const fetchUserData = async () => {
  loadError.value = ''
  try {
    const res = await request.get('/api/users/me', { silent: true })
    const userData = res.data

    formData.username = userData.username || ''
    formData.email = userData.email || ''
    formData.bindId = userData.bindId || ''
    formData.money = userData.money || 0

    originalData.value = { ...userData }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    loadError.value = error.friendlyMsg || '获取用户信息失败'
  }
}

// 7. 提交更新
const handleSave = async () => {
  if (!isChanged.value) return

  const updatePayload = {
    username: formData.username,
    email: formData.email
  }

  try {
    await request.put('/api/users/updateByUsername', updatePayload)
    ElMessage.success("账户资料已更新")
    originalData.value.email = formData.email
  } catch (error) {
    console.error('更新失败:', error)
    ElMessage.error(error.friendlyMsg || '更新失败，请稍后重试')
  }
}

// 8. 解绑逻辑
const handleUnbind = () => {
  if (!formData.bindId) return

  ElMessageBox.confirm(
    '确定要解除当前的绑定 ID 吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await request.post('/api/users/unbind', { username: formData.username })
      ElMessage.success('解绑成功')
      formData.bindId = ''
      if (originalData.value) {
        originalData.value.bindId = ''
      }
    } catch (error) {
      console.error('解绑失败:', error)
      ElMessage.error(error.friendlyMsg || '解绑失败，请稍后重试')
    }
  }).catch(() => {})
}

// 9. 打开兑换弹窗
const openRedeemDialog = () => {
  redeemCode.value = ''
  redeemDialogVisible.value = true
}

// 10. 执行兑换
const handleRedeem = async () => {
  const code = redeemCode.value?.trim()
  if (!code) {
    ElMessage.warning('请输入兑换码')
    return
  }

  redeemLoading.value = true
  try {
    const res = await request.post('/api/redeem/use', { code })
    
    if (res.code === 2000) {
      ElMessage.success(res.data || '兑换成功')
      redeemDialogVisible.value = false
      // 刷新用户数据
      await fetchUserData()
    } else {
      ElMessage.error(res.msg || '兑换失败')
    }
  } catch (error) {
    console.error('兑换异常:', error)
  } finally {
    redeemLoading.value = false
  }
}

// 页面加载时初始化数据
onMounted(() => {
  fetchUserData()
})
</script>

<style scoped>
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

.profile-container {
  max-width: 480px;
  margin: 40px auto;
}

.readonly-input :deep(.base-input) {
  background: #f5f5f7;
  color: #86868b;
  border-color: #d2d2d7;
  cursor: not-allowed;
}

.action-input-group {
  position: relative;
}

.unbind-btn {
  position: absolute;
  right: 12px;
  top: 36px;
  padding: 4px 12px;
  background: #fff;
  border: 1px solid #d2d2d7;
  border-radius: 8px;
  color: #ff3b30;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.unbind-btn:hover {
  background: #ff3b30;
  color: #fff;
  border-color: #ff3b30;
}

/* 余额行样式 */
.money-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.money-input {
  flex: 1;
}

.recharge-btn {
  padding: 10px 20px;
  background: #34c759;
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 20px;
  white-space: nowrap;
}

.recharge-btn:hover {
  background: #28a745;
  transform: scale(1.02);
}

.recharge-btn:active {
  transform: scale(0.98);
}

.form-actions {
  margin-top: 24px;
}

.save-btn {
  width: 100%;
  padding: 14px;
  background: #0071e3;
  color: #fff;
  border: none;
  border-radius: 12px;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.save-btn:hover {
  background: #0051d5;
}

.save-btn:disabled {
  background: #d2d2d7;
  cursor: not-allowed;
}

/* 兑换弹窗样式 */
.redeem-form {
  padding: 10px 0;
}
</style>