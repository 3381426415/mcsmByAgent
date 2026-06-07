<template>
  <showCard title="公告管理" subtitle="发布网站公告和游戏公告">
    <div class="announcement-container">
      <!-- 错误状态标签 -->
      <div v-if="loadError" class="error-banner">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="15" y1="9" x2="9" y2="15"></line>
          <line x1="9" y1="9" x2="15" y2="15"></line>
        </svg>
        <span>{{ loadError }}</span>
      </div>

      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button type="primary" @click="openAddDialog">新建公告</el-button>
        <el-select v-model="filterType" placeholder="公告类型" clearable style="width: 150px; margin-left: 12px;" @change="fetchList">
          <el-option label="全部" :value="null" />
          <el-option label="网站公告" :value="1" />
          <el-option label="游戏公告" :value="2" />
          <el-option label="双端公告" :value="3" />
        </el-select>
      </div>

      <!-- 公告列表 -->
      <el-table :data="tableData" v-loading="loading" style="width: 100%; margin-top: 20px;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
            <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isPublished === 1 ? 'success' : 'info'">
            {{ row.isPublished === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
        <el-table-column prop="createByName" label="发布人" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.isPublished" type="primary" size="small" @click="publish(row)">发布</el-button>
            <el-button type="warning" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="deleteAnnouncement(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="formData" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="formData.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="formData.type">
            <el-radio :value="1">网站公告</el-radio>
            <el-radio :value="2">游戏公告</el-radio>
            <el-radio :value="3">双端公告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="formData.type === 2 || formData.type === 3" label="生效服务器">
          <el-checkbox-group v-model="formData.serverIds">
            <el-checkbox
              v-for="s in serverList"
              :key="s.serverId"
              :value="s.serverId"
            >
              {{ s.name }} ({{ s.serverId }})
            </el-checkbox>
          </el-checkbox-group>
          <div v-if="serverList.length === 0" style="color: #999; font-size: 13px;">暂无已注册的服务器</div>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="formData.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="saveDraft" :loading="saving">保存草稿</el-button>
        <el-button type="primary" @click="publishNow" :loading="publishing">立即发布</el-button>
      </template>
    </el-dialog>
  </showCard>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import showCard from '@/components/commonShowPanel.vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterType = ref(null)
const loadError = ref('')
const serverList = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const formData = ref({
  id: null,
  title: '',
  content: '',
  type: 1,
  serverIds: []
})

const dialogTitle = computed(() => isEdit.value ? '编辑公告' : '新建公告')

const fetchList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await request.get('/api/announcement/list', {
      params: {
        page: currentPage.value,
        size: pageSize.value,
        type: filterType.value
      },
      silent: true
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error('获取公告列表失败:', e)
    loadError.value = e.friendlyMsg || '获取公告列表失败'
  } finally {
    loading.value = false
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const getTypeText = (type) => {
  const map = { 1: '网站', 2: '游戏', 3: '双端' }
  return map[type] || '未知'
}

const getTypeTag = (type) => {
  const map = { 1: 'primary', 2: 'success', 3: 'warning' }
  return map[type] || 'info'
}

const openAddDialog = () => {
  isEdit.value = false
  formData.value = { id: null, title: '', content: '', type: 1, serverIds: [] }
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  let parsedServerIds = []
  if (row.serverIds) {
    try {
      parsedServerIds = JSON.parse(row.serverIds)
    } catch (e) {
      parsedServerIds = []
    }
  }
  formData.value = { ...row, serverIds: parsedServerIds }
  dialogVisible.value = true
}

const buildPayload = () => {
  const payload = { ...formData.value }
  if (payload.serverIds && payload.serverIds.length > 0) {
    payload.serverIds = JSON.stringify(payload.serverIds)
  } else {
    payload.serverIds = null
  }
  return payload
}

const saveDraft = async () => {
  if (!formData.value.title || !formData.value.content) {
    ElMessage.warning('请填写完整信息')
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    const res = await request.post('/api/announcement/draft', {
      title: payload.title,
      content: payload.content,
      type: payload.type,
      serverIds: payload.serverIds
    });
    if (res.code === 2000) {
      ElMessage.success('草稿已保存')
      dialogVisible.value = false
      fetchList()
    }
  } finally {
    saving.value = false
  }
}

const publishNow = async () => {
  if (!formData.value.title || !formData.value.content) {
    ElMessage.warning('请填写完整信息')
    return
  }
  publishing.value = true
  try {
    const payload = buildPayload()
    const res = await request.post('/api/announcement/publish', payload)
    if (res.code === 2000) {
      ElMessage.success('发布成功')
      dialogVisible.value = false
      fetchList()
    }
  } finally {
    publishing.value = false
  }
}

const publish = async (row) => {
  try {
    await ElMessageBox.confirm('确定发布该公告吗？', '确认发布')
    const res = await request.post('/api/announcement/publish', { ...row, isPublished: 1 })
    if (res.code === 2000) {
      ElMessage.success('发布成功')
      fetchList()
    }
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      console.error('发布失败:', e)
      ElMessage.error(e.friendlyMsg || '发布失败')
    }
  }
}

const deleteAnnouncement = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该公告吗？', '警告', { type: 'warning' })
    const res = await request.delete(`/api/announcement/delete/${row.id}`)
    if (res.code === 2000) {
      ElMessage.success('删除成功')
      fetchList()
    }
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      console.error('删除失败:', e)
      ElMessage.error(e.friendlyMsg || '删除失败')
    }
  }
}

const fetchServers = async () => {
  try {
    const res = await request.get('/api/server/list', { silent: true })
    serverList.value = res.data || []
  } catch (e) {
    console.error('获取服务器列表失败:', e)
  }
}

onMounted(() => {
  fetchList()
  fetchServers()
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

.announcement-container {
  padding: 20px;
}
.toolbar {
  display: flex;
  align-items: center;
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>