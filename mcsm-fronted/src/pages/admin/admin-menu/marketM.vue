<template>
  <showCard title="市场管理" subtitle="管理所有在售商品">
    <div class="market-container">
      <!-- 数据加载错误提示 -->
      <div v-if="loadError" class="error-banner">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="15" y1="9" x2="9" y2="15"></line>
          <line x1="9" y1="9" x2="15" y2="15"></line>
        </svg>
        <span>{{ loadError }}</span>
      </div>

      <!-- 搜索栏 -->
      <div class="search-section">
        <div class="apple-search">
          <input 
            v-model="keyword" 
            placeholder="输入物品名称搜索..." 
            @keyup.enter="handleSearch"
            class="search-input"
          />
          <button @click="handleSearch" class="search-btn">搜索</button>
        </div>
      </div>

      <!-- 商品网格 -->
      <div v-loading="loading" class="goods-container">
        <div v-if="goodsList.length === 0 && !loading" class="empty-state">
          暂无商品在售
        </div>
        <div v-for="item in goodsList" :key="item.id" class="goods-card glass-card">
          <!-- 图片区域 -->
          <div class="item-image-wrapper">
            <div class="inv-slot has-item">
              <div class="item-container">
                <el-image 
                  :src="getIconUrl(item.itemKey)" 
                  fit="contain"
                  style="width: 40px; height: 40px; display: block;"
                >
                  <template #error>
                    <div class="fallback-icon">{{ defaultIcon }}</div>
                  </template>
                </el-image>
                <span class="item-amount">{{ item.amount }}</span>
              </div>
            </div>
          </div>

          <!-- 信息区域 -->
          <div class="item-info">
            <h3 class="display-name">{{ item.displayName || item.itemKey?.replace('minecraft:', '') }}</h3>
            <div class="info-row">
              <span class="seller">卖家: {{ item.sellerNickname || '未知' }}</span>
            </div>
            <div class="info-row">
              <span class="seller-id">UUID: {{ formatSellerId(item.sellerId) }}</span>
            </div>
            <div class="price-tag">
              <span class="currency">￥</span>
              <span class="amount">{{ item.price }}</span>
            </div>
          </div>

          <!-- 管理员操作按钮 -->
          <button class="apple-withdraw-btn" @click="handleAdminWithdraw(item)">
            <span>强制下架</span>
          </button>
        </div>
      </div>

      <!-- 分页器 -->
      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </showCard>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import showCard from '@/components/commonShowPanel.vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAssets } from '@/components/useAssets.js'

const { getIconUrl } = useAssets()
const defaultIcon = '📦'

const keyword = ref('')
const goodsList = ref([])
const loading = ref(false)
const loadError = ref('')

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const formatSellerId = (id) => {
  if (!id) return '未知'
  return id.substring(0, 8) + '...'
}

const fetchGoods = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await request.get('/api/market/list', {
      params: {
        keyword: keyword.value || null,
        page: currentPage.value,
        size: pageSize.value
      },
      silent: true
    })

    if (res.data && res.data.records) {
      goodsList.value = res.data.records
      total.value = res.data.total || 0
    } else {
      goodsList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取市场列表失败:', error)
    loadError.value = error.friendlyMsg || '获取商品列表失败'
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchGoods()
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchGoods()
}

// 管理员强制下架
const handleAdminWithdraw = (item) => {
  ElMessageBox.confirm(
    `确定要强制下架 "${item.displayName}" 吗？物品将返还给卖家。`, 
    '强制下架确认',
    {
      confirmButtonText: '确定下架',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await request.post(`/api/market/admin/withdraw/${item.id}`)
      if (res.code === 2000) {
        ElMessage.success('强制下架成功')
        await fetchGoods()
      } else if (res.code === 401) {
        ElMessage.error('权限不足，需要管理员权限')
      } else {
        ElMessage.error(res.msg || '下架失败')
      }
    } catch (error) {
      console.error('强制下架失败:', error)
      ElMessage.error('操作失败，请稍后重试')
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchGoods()
})
</script>

<style scoped>
/* 复用 tradeMarket.vue 的样式，修改按钮颜色 */
.market-container {
  padding: 20px;
  min-height: 400px;
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

.search-section {
  margin-bottom: 24px;
}

.apple-search {
  display: flex;
  gap: 12px;
  max-width: 500px;
}

.search-input {
  flex: 1;
  padding: 12px 16px;
  border: none;
  background: rgba(120, 120, 128, 0.08);
  border-radius: 12px;
  font-size: 15px;
  color: #1d1d1f;
  transition: all 0.2s;
}

.search-input:focus {
  outline: none;
  background: rgba(120, 120, 128, 0.12);
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.1);
}

.search-input::placeholder {
  color: #86868b;
}

.search-btn {
  padding: 12px 24px;
  background: #007aff;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.search-btn:hover {
  background: #0051d5;
  transform: scale(1.02);
}

.goods-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
  min-height: 300px;
}

.goods-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.goods-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.8);
}

.item-image-wrapper {
  margin-bottom: 16px;
}

.inv-slot {
  width: 64px;
  height: 64px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.06);
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

.item-amount {
  position: absolute;
  bottom: 4px;
  right: 4px;
  font-size: 11px;
  font-weight: 800;
  color: #1d1d1f;
  background: rgba(255, 255, 255, 0.95);
  padding: 2px 6px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.item-info {
  width: 100%;
  text-align: center;
  margin-bottom: 16px;
}

.display-name {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
  word-break: break-word;
}

.info-row {
  margin-bottom: 4px;
}

.seller {
  font-size: 12px;
  color: #86868b;
}

.seller-id {
  font-size: 10px;
  color: #a1a1a6;
  font-family: monospace;
}

.price-tag {
  color: #ff3b30;
  font-weight: 700;
  font-size: 20px;
  margin-top: 4px;
}

.price-tag .currency {
  font-size: 14px;
}

/* 强制下架按钮 - 红色警告风格 */
.apple-withdraw-btn {
  width: 100%;
  padding: 10px;
  background: #ff3b30;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: auto;
}

.apple-withdraw-btn:hover {
  background: #d70015;
  transform: scale(1.02);
}

.apple-withdraw-btn:active {
  transform: scale(0.98);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-bottom: 10px;
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 80px 20px;
  color: #86868b;
  font-size: 16px;
}

.fallback-icon {
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

@media (max-width: 768px) {
  .goods-container {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 16px;
  }
  
  .market-container {
    padding: 12px;
  }
}
</style>