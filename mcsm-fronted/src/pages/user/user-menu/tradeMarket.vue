<template>
  <showCard title="交易市场" subtitle="和所有人交易">
    <div class="market-container">
      <!-- 错误状态标签 -->
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
                  @error="handleImgError"
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
              <p class="seller">卖家: {{ item.sellerNickname || '未知' }}</p>
            </div>
            <div class="price-tag">
              <span class="currency">￥</span>
              <span class="amount">{{ item.price }}</span>
            </div>
          </div>

          <!-- 购买按钮 -->
          <button class="apple-buy-btn" @click="handleBuy(item)">
            <span>立即购买</span>
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

// 响应式数据
const keyword = ref('')
const goodsList = ref([])
const loading = ref(false)
const loadError = ref('')

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 格式化卖家ID
const formatSellerId = (id) => {
  if (!id) return '未知'
  return id.substring(0, 8) + '...'
}

// 获取商品列表
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
    } else if (Array.isArray(res.data)) {
      goodsList.value = res.data
      total.value = res.data.length
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

// 搜索（重置页码）
const handleSearch = () => {
  currentPage.value = 1
  fetchGoods()
}

// 分页切换
const handlePageChange = (page) => {
  currentPage.value = page
  fetchGoods()
}

// 图片加载失败处理
const handleImgError = (e) => {
  // 如果是 el-image 组件，通过 $el 访问
  console.warn('图片加载失败，使用默认图标')
}

// 购买操作
const handleBuy = (item) => {
  ElMessageBox.confirm(
    `确定要花费 ￥${item.price} 购买 ${item.amount} 个 ${item.displayName || '物品'} 吗？`, 
    '确认交易',
    {
      confirmButtonText: '确定购买',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(async () => {
    try {
      await request.post(`/api/market/buy/${item.id}`)
      ElMessage.success('购买成功！请前往游戏内领取')
      await fetchGoods()
    } catch (error) {
      console.error('交易异常:', error)
      ElMessage.error(error.friendlyMsg || '购买失败')
    }
  }).catch(() => {
    // 用户取消操作
  })
}

onMounted(() => {
  fetchGoods()
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

/* 容器 */
.market-container {
  padding: 20px;
  min-height: 400px;
}

/* 搜索栏 - Apple风格 */
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

/* 商品网格 */
.goods-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
  min-height: 300px; /* ✅ 防止空状态时高度塌陷导致抖动 */
}

/* 商品卡片 */
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

/* 图片包装器 */
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

/* 商品信息 */
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
  margin-bottom: 8px;
}

.seller {
  font-size: 12px;
  color: #86868b;
}

.price-tag {
  color: #ff3b30;
  font-weight: 700;
  font-size: 20px;
}

.price-tag .currency {
  font-size: 14px;
}

/* 购买按钮 */
.apple-buy-btn {
  width: 100%;
  padding: 10px;
  background: #007aff;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: auto; /* 底部对齐 */
}

.apple-buy-btn:hover {
  background: #0051d5;
  transform: scale(1.02);
}

.apple-buy-btn:active {
  transform: scale(0.98);
}

/* 分页器 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-bottom: 10px;
}

/* 空状态 */
.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 80px 20px;
  color: #86868b;
  font-size: 16px;
}

/* 备用图标 */
.fallback-icon {
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

/* 加载状态优化 */
.goods-container {
  position: relative;
}

/* 响应式调整 */
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