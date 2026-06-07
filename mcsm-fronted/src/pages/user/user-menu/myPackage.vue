<template>
  <showCard title="我的背包" :subtitle="`玩家 ${userInfo.nickname || '加载中...'} 的个人随身物品`">
    <div class="inventory-container">
      <!-- 错误状态标签 -->
      <div v-if="errorMsg" class="error-banner">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="15" y1="9" x2="9" y2="15"></line>
          <line x1="9" y1="9" x2="15" y2="15"></line>
        </svg>
        <span>{{ errorMsg }}</span>
      </div>

      <div class="glass-card stat-item">
        <span class="label">物品格占用</span>
        <span class="value">{{ inventoryData.length }} / 36</span>
      </div>

      <div class="glass-card inventory-wrapper" v-loading="loading">
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
                  </el-image>
                </div>
                <span class="item-count">{{ getSlotItem(i-1).Count }}</span>
              </div>
            </el-tooltip>
            <div v-else class="empty-placeholder"></div>
          </div>
        </div>
        <p class="inventory-tip">查看你的在线/离线背包数据</p>
      </div>
    </div>

<el-dialog
  v-model="uploadDialog.visible"
  title="上架商品"
  width="340px"
  class="apple-dialog"
  :show-close="false"
  align-center
>
  <div class="upload-form" v-if="uploadDialog.item">
    <div class="item-preview">
      <el-image :src="getIconUrl(uploadDialog.item.id)" class="preview-icon" />
      <div class="item-info">
        <div class="name">{{ uploadDialog.item.id.replace('minecraft:', '') }}</div>
        <div class="stock">库存可用: {{ uploadDialog.item.Count }}</div>
      </div>
    </div>

    <div class="input-group">
      <span class="input-label">上架数量</span>
      <el-input-number 
        v-model="uploadDialog.amount" 
        :min="1" 
        :max="uploadDialog.item.Count" 
        controls-position="right"
        class="apple-input"
      />
    </div>

    <div class="input-group">
      <span class="input-label">单价 (游戏币)</span>
      <el-input 
        v-model="uploadDialog.price" 
        placeholder="请输入价格"
        class="apple-input"
        type="number"
      />
    </div>
  </div>

  <template #footer>
    <div class="dialog-footer">
      <button class="btn-cancel" @click="uploadDialog.visible = false">取消</button>
      <button class="btn-confirm" @click="submitUpload">确认上架</button>
    </div>
  </template>
</el-dialog>

  </showCard>
</template>

<script setup>
import showCard from '@/components/commonShowPanel.vue'
import { ref, onMounted } from 'vue'
import { useAssets } from '@/components/useAssets.js'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const { getIconUrl } = useAssets()
const defaultIcon = '📦'
const loading = ref(false)
const userInfo = ref({})
const inventoryData = ref([])
const errorMsg = ref('')

// 1. 定义对话框状态
const uploadDialog = ref({
  visible: false,
  slot: null,
  item: null,
  amount: 1,
  price: ''
})

// 获取特定格子的数据
const getSlotItem = (slotIndex) => {
  return inventoryData.value.find(item => item.Slot === slotIndex)
}

// 2. 点击格子逻辑：打开对话框而非 Prompt
const handleItemClick = (slotIndex) => {
  const item = getSlotItem(slotIndex)
  if (!item) return

  // 初始化对话框数据
  uploadDialog.value = {
    visible: true,
    slot: slotIndex,
    item: item,
    amount: item.Count, // 默认上架全部
    price: ''
  }
}

// 3. 提交上架请求
const submitUpload = async () => {
  const { slot, item, amount, price } = uploadDialog.value
  
  // 基础校验
  if (!price || price <= 0) {
    return ElMessage.warning('请输入有效价格')
  }

  loading.value = true
  try {
    const uploadData = {
      slot: slot,
      amount: parseInt(amount),  // 传入用户选定的数量
      price: parseInt(price),
      displayName: item.tag?.display?.Name || item.id.replace('minecraft:', ''),
      itemKey: item.id,
      nbtData: item.tag ? JSON.stringify(item.tag) : "{}"
    }

    // 注意：这里使用你配置好的 request 拦截器
    const res = await request.post('/api/market/upload', uploadData)

    if (res.code === 2000) {
      ElMessage.success('上架成功！')
      uploadDialog.value.visible = false
      await fetchMyInventory() // 刷新背包
    } else {
      ElMessage.error(res.msg || '上架失败')
    }
  } catch (error) {
    console.error('上架异常:', error)
    ElMessage.error('服务器异常')
  } finally {
    loading.value = false
  }
}


// 获取个人信息及背包
const fetchMyInventory = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const userRes = await request.get('/api/users/me', { silent: true })
    userInfo.value = userRes.data
    const uuid = userRes.data.bindId

    if (!uuid) {
      errorMsg.value = '尚未绑定游戏角色'
      return
    }

    const invRes = await request.get('/api/player/inventory', { params: { uuid }, silent: true })
    inventoryData.value = invRes.data || []
  } catch (e) {
    console.error("加载背包失败:", e)
    errorMsg.value = e.friendlyMsg || '获取背包数据失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchMyInventory()
})
</script>

<style scoped>
.inventory-container {
  padding: 20px;
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

/* 沿用 Apple 风格的玻璃拟态设计 */
.glass-card {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}

.label {
  display: block;
  font-size: 13px;
  color: #86868b;
  font-weight: 600;
  margin-bottom: 4px;
  text-transform: uppercase;
}

.value {
  font-size: 28px;
  font-weight: 700;
  color: #1d1d1f;
}

.inventory-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.inventory-grid {
  display: grid;
  grid-template-columns: repeat(9, 1fr);
  gap: 10px;
  background: rgba(0, 0, 0, 0.05);
  padding: 16px;
  border-radius: 20px;
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.08);
}

.inv-slot {
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.2s ease;
}

.inv-slot.has-item {
  background: #ffffff;
  cursor: pointer;
}

.inv-slot.has-item:hover {
  transform: scale(1.1);
  z-index: 2;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.item-count {
  position: absolute;
  bottom: 1px;
  right: 3px;
  font-size: 10px;
  font-weight: 800;
  color: #1d1d1f;
  background: rgba(255, 255, 255, 0.8);
  padding: 0 4px;
  border-radius: 4px;
}

.inventory-tip {
  margin-top: 16px;
  font-size: 12px;
  color: #86868b;
}

.fallback-icon {
  font-size: 20px;
}
:deep(.apple-dialog) {
  border-radius: 28px !important;
  overflow: hidden;
}

.upload-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 10px 0;
}

.item-preview {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f5f5f7;
  border-radius: 16px;
}

.preview-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: #ffffff;
  padding: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.item-preview .item-info {
  flex: 1;
}

.item-preview .name {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 4px;
}

.item-preview .stock {
  font-size: 13px;
  color: #86868b;
}

.upload-form .input-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.upload-form .input-label {
  min-width: 70px;
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
}

.upload-form .apple-input {
  flex: 1;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}

.btn-cancel {
  padding: 10px 20px;
  background: #f5f5f7;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #e5e5ea;
}

.btn-confirm {
  padding: 10px 24px;
  background: #007aff;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-confirm:hover {
  background: #0051d5;
}
</style>