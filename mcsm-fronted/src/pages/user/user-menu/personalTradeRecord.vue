<template>
  <showCard title="交易管理" subtitle="在这里管理你的在售商品并查看往期账单">
    <div class="transaction-container">
      <div class="apple-tabs">
        <div 
          class="tab-item" 
          :class="{ active: activeTab === 'onSale' }" 
          @click="activeTab = 'onSale'"
        >我的上架</div>
        <div 
          class="tab-item" 
          :class="{ active: activeTab === 'history' }" 
          @click="activeTab = 'history'"
        >交易记录</div>
      </div>

      <div v-if="activeTab === 'onSale'" class="tab-content fade-in">
        <div v-if="onSaleGoods.length === 0" class="empty-state">暂无正在售卖的商品</div>
        <div class="goods-grid">
          <div v-for="item in onSaleGoods" :key="item.id" class="glass-card goods-item">
            <div class="item-info">
              <div class="inv-slot has-item">
                <div class="item-container">
                  <div class="item-icon">
                    <!-- ✅ 修改：使用与 myPackage 相同的 el-image 写法 -->
                    <el-image 
                      :src="getIconUrl(item.itemKey)" 
                      fit="contain"
                      style="width: 28px; height: 28px; display: block;"
                    >
                      <template #error>
                        <div class="fallback-icon">{{ defaultIcon }}</div>
                      </template>
                    </el-image>
                  </div>
                  <div class="item-count">{{ item.amount }}</div>
                </div>
              </div>

              <div class="item-details">
                <h4>{{ item.displayName || item.itemKey?.replace('minecraft:', '') }}</h4>
                <p class="time-stamp">{{ formatDate(item.creatTime) }} 上架</p>
              </div>
            </div>
            
            <div class="item-actions">
              <!-- ✅ 修改：价格显示逻辑 -->
              <div class="price-tag">
                <span class="currency">￥</span>
                <span class="amount">{{ item.price || 0 }}</span>
              </div>
              <div class="btn-group">
                <button class="apple-btn secondary" @click="handleWithdraw(item.id)">下架</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="tab-content fade-in">
        <div v-if="orderHistory.length === 0" class="empty-state">暂无交易往来</div>
        <div class="order-list">
          <div v-for="order in orderHistory" :key="order.id" class="glass-card order-card">
            <div class="order-main">
              <div class="inv-slot has-item">
                <div class="item-container">
                  <!-- ✅ 修改：交易记录的图片显示 -->
                  <el-image 
                    :src="getIconUrl(order.itemKey)" 
                    fit="contain"
                    style="width: 28px; height: 28px; display: block;"
                  >
                    <template #error>
                      <div class="fallback-icon">{{ defaultIcon }}</div>
                    </template>
                  </el-image>
                </div>
              </div>
              <div class="order-info">
                <div class="order-header">
                  <span :class="['order-tag', order.buyerId === userBindId ? 'buy-tag' : 'sell-tag']">
                    {{ order.buyerId === userBindId ? '买入' : '售出' }}
                  </span>
                  <h4>{{ order.displayName || order.itemKey?.replace('minecraft:', '') }}</h4>
                </div>
                <p class="order-sub-info">
                  <span class="order-id">单号: {{ order.id }}</span>
                  <span class="dot">·</span>
                  <span class="order-time">{{ formatDate(order.completeTime) }}</span>
                </p>
              </div>
              <div class="order-price">
                <!-- ✅ 修改：价格显示 -->
                <span class="price-val">￥{{ order.finalPrice || 0 }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </showCard>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import showCard from '@/components/commonShowPanel.vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
// 导入统一的资产管理钩子
import { useAssets } from '@/components/useAssets.js'

const activeTab = ref('onSale')
const onSaleGoods = ref([])
const orderHistory = ref([])
const userBindId = ref('')

// 初始化后端图片获取方法
const { getIconUrl } = useAssets()
const defaultIcon = '📦'

// 格式化时间
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  // ✅ 修改：更健壮的时间格式化
  try {
    return dateStr.replace('T', ' ').split('.')[0]
  } catch {
    return dateStr
  }
}

// 获取核心数据
const fetchData = async () => {
  try {
    const userRes = await request.get('/api/users/me')
    if (userRes && userRes.code === 2000) {
      const userData = userRes.data
      userBindId.value = userData.bindId

      // 并发请求在售和历史数据
      const [onSaleRes, historyRes] = await Promise.all([
        request.get('/api/market/my/on-sale'),
        request.get('/api/market/my/orders')
      ])

      // ✅ 关键修改：处理分页数据
      console.log('在售商品原始响应:', onSaleRes)
      
      // 检查是否是分页对象
      if (onSaleRes.data && onSaleRes.data.records) {
        onSaleGoods.value = onSaleRes.data.records || []
      } else if (Array.isArray(onSaleRes.data)) {
        onSaleGoods.value = onSaleRes.data
      } else {
        onSaleGoods.value = []
      }
      
      // 历史订单处理
      if (historyRes.data && historyRes.data.records) {
        orderHistory.value = historyRes.data.records || []
      } else if (Array.isArray(historyRes.data)) {
        orderHistory.value = historyRes.data
      } else {
        orderHistory.value = []
      }
      
      // ✅ 调试日志
      console.log('处理后的在售商品:', onSaleGoods.value)
      console.log('第一件商品的 itemKey:', onSaleGoods.value[0]?.itemKey)
      console.log('图片URL:', getIconUrl(onSaleGoods.value[0]?.itemKey))
    }
  } catch (error) {
    console.error('加载交易数据失败:', error)
    ElMessage.error(error.friendlyMsg || '加载数据失败')
  }
}

// 下架处理
const handleWithdraw = (id) => {
  ElMessageBox.confirm('确定要下架该商品吗？物品将通过游戏内邮件或指令返还。', '下架确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.post(`/api/market/withdraw/${id}`)
      ElMessage.success('下架成功')
      await fetchData()
    } catch (error) {
      console.error('下架失败:', error)
      ElMessage.error(error.friendlyMsg || '下架失败')
    }
  }).catch(() => {
    // 用户取消操作
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
/* 容器布局 */
.transaction-container {
  padding: 10px;
}

/* Apple 风格分段控制器 */
.apple-tabs {
  display: flex;
  background: rgba(120, 120, 128, 0.12);
  padding: 2px;
  border-radius: 10px;
  margin-bottom: 24px;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  color: #86868b;
}

.tab-item.active {
  background: #ffffff;
  color: #1d1d1f;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

/* 物品槽位样式 (移植自 myPackage.vue) */
.inv-slot {
  width: 48px;
  height: 48px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.inv-slot.has-item {
  background: #ffffff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03), inset 0 1px 1px rgba(255, 255, 255, 0.8);
}

.item-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

/* ✅ 移除 slot-img 类，统一使用内联样式 */
.item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-count {
  position: absolute;
  bottom: 2px;
  right: 4px;
  font-size: 10px;
  font-weight: 800;
  color: #1d1d1f;
  background: rgba(255, 255, 255, 0.9);
  padding: 0 4px;
  border-radius: 4px;
  line-height: 1.4;
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

/* 卡片与网格 */
.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.goods-item {
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.item-details h4 {
  margin: 0;
  font-size: 17px;
  color: #1d1d1f;
}

.time-stamp {
  margin-top: 4px;
  font-size: 12px;
  color: #86868b;
}

.item-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid rgba(0,0,0,0.05);
  padding-top: 15px;
}

/* 价格标签 */
.price-tag {
  color: #ff3b30;
  font-weight: 700;
}

.price-tag .amount {
  font-size: 20px;
}

/* 按钮样式 */
.apple-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.apple-btn.secondary {
  background: rgba(0, 122, 255, 0.1);
  color: #007aff;
}

.apple-btn.secondary:hover {
  background: rgba(0, 122, 255, 0.2);
}

/* 交易记录特有样式 */
.order-card {
  padding: 16px 20px;
  margin-bottom: 12px;
}

.order-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.order-info {
  flex: 1;
}

.order-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-header h4 {
  margin: 0;
  font-size: 16px;
}

.order-tag {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 700;
}

.buy-tag { background: rgba(0, 122, 255, 0.1); color: #007aff; }
.sell-tag { background: rgba(52, 199, 89, 0.1); color: #34c759; }

.order-sub-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.order-id {
  font-size: 11px;
  font-family: 'SF Mono', 'PingFang SC', monospace;
  color: #86868b;
  background: rgba(0, 0, 0, 0.05);
  padding: 1px 5px;
  border-radius: 4px;
}

.dot {
  color: #86868b;
  font-weight: bold;
}

.order-time {
  font-size: 12px;
  color: #86868b;
  margin: 0;
}

.order-price .price-val {
  font-weight: 600;
  color: #1d1d1f;
  font-size: 18px;
}

.fade-in {
  animation: fadeIn 0.4s ease-out;
}

.fallback-icon {
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

.empty-state {
  text-align: center;
  padding: 60px;
  color: #86868b;
  font-size: 14px;
}
</style>