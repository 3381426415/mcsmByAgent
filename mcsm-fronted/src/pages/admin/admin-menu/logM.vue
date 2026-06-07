<template>
  <showCard title="操作日志" subtitle="查看管理员操作记录">
    <div class="log-container">
      <div class="search-bar">
        <el-input
          v-model="searchOperator"
          placeholder="操作人"
          style="width: 180px"
          clearable
          @keyup.enter="fetchLogs"
        />
        <el-select
          v-model="searchModule"
          placeholder="操作模块"
          style="width: 140px"
          clearable
        >
          <el-option label="全部模块" value="" />
          <el-option label="用户管理" value="用户管理" />
          <el-option label="市场管理" value="市场管理" />
          <el-option label="公告管理" value="公告管理" />
          <el-option label="插件管理" value="插件管理" />
          <el-option label="服务器管理" value="服务器管理" />
          <el-option label="角色管理" value="角色管理" />
          <el-option label="玩家管理" value="玩家管理" />
          <el-option label="权限管理" value="权限管理" />
        </el-select>
        <el-button type="primary" @click="fetchLogs">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table
        :data="logs"
        v-loading="loading"
        class="log-table"
        :default-sort="{ prop: 'createTime', order: 'descending' }"
      >
        <el-table-column prop="operatorName" label="操作人" min-width="100" />

        <el-table-column label="操作内容" min-width="180">
          <template #default="{ row }">
            <div class="action-cell">
              <el-tag size="small" type="info" effect="plain" class="module-tag">{{ row.module }}</el-tag>
              <span class="action-text">{{ row.action }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column 
          prop="description" 
          label="详细描述" 
          min-width="250" 
          show-overflow-tooltip 
        />

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.status === 1 ? 'success' : 'danger'"
              effect="light"
            >
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="operatorIp" label="IP地址" min-width="130" />

        <el-table-column label="耗时" width="100" align="right">
          <template #default="{ row }">
            <span :class="['time-tag', { 'slow-time': row.executeTime > 1000 }]">
              {{ row.executeTime }}ms
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchLogs"
        />
      </div>
    </div>
 <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="650px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="操作人">{{ currentLog?.operatorName }}</el-descriptions-item>
        <el-descriptions-item label="操作IP">{{ currentLog?.operatorIp }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ currentLog?.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ currentLog?.action }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentLog?.description }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ currentLog?.methodName }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentLog?.executeTime }}ms</el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag size="small" :type="currentLog?.status === 1 ? 'success' : 'danger'">
            {{ currentLog?.status === 1 ? '成功' : '失败' }}
          </el-tag>
          <span v-if="currentLog?.errorMsg" style="margin-left: 10px; color: #f56c6c;">
            {{ currentLog?.errorMsg }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="时间">{{ formatTime(currentLog?.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="json-block">{{ formatJson(currentLog?.requestParams) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="返回结果" :span="2" v-if="currentLog?.responseResult">
          {{ currentLog?.responseResult }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
    </showCard>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import showCard from '@/components/commonShowPanel.vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const logs = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchOperator = ref('')
const searchModule = ref('')

const detailVisible = ref(false)
const currentLog = ref(null)

const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/admin-log/list', {
      params: {
        page: currentPage.value,
        size: pageSize.value,
        module: searchModule.value || null,
        operatorName: searchOperator.value || null
      }
    })
    logs.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error('获取日志失败:', e)
    ElMessage.error(e.friendlyMsg || '获取日志失败')
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchOperator.value = ''
  searchModule.value = ''
  currentPage.value = 1
  fetchLogs()
}

const showDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const formatJson = (str) => {
  if (!str) return ''
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

onMounted(fetchLogs)
</script>

<style scoped>
/* 容器背景与留白：增加呼吸感 */
.log-container {
  padding: 24px;
  background-color: #ffffff;
}

/* 搜索栏：优化间距，符合 Apple 审美 */
.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

/* 统一输入组件圆角 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  border-radius: 8px !important;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

/* 表格整体精致化 */
.log-table {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #f0f0f2;
  --el-table-header-bg-color: #f5f5f7; /* 浅灰色表头 */
}

/* 操作内容单元格布局 */
.action-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.module-tag {
  border-radius: 4px;
  border: none;
  background-color: #f0f0f2;
  color: #606266;
}

.action-text {
  color: #303133;
  font-size: 13px;
}

/* 时间文本样式 */
.time-text {
  color: #909399;
  font-size: 13px;
  font-family: "SF Mono", "PingFang SC", monospace;
}

/* 耗时标签样式 */
.time-tag {
  font-weight: 500;
  color: #67c23a;
}

/* 耗时过长高亮：改用深橘色，视觉上更高级 */
.slow-time {
  color: #e6a23c;
  font-weight: 600;
}

/* 分页居中并增加间距 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

/* 详情弹窗内的代码块美化 */
.json-block {
  max-height: 300px;
  overflow: auto;
  background: #f5f5f7;
  padding: 16px;
  border-radius: 10px;
  font-family: "SF Mono", Menlo, monospace;
  font-size: 12px;
  line-height: 1.6;
  color: #475050;
  border: 1px solid #e5e5e7;
}

/* 调整表格行高，让弹性内容不拥挤 */
:deep(.el-table__row) {
  height: 54px;
}
</style>