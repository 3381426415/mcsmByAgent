<template>
  <showCard title="插件管理" subtitle="管理服务器插件">
    <div class="plugin-container">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-upload
          :show-file-list="false"
          :before-upload="handleUpload"
          accept=".jar"
        >
          <el-button type="primary">上传插件</el-button>
        </el-upload>
        <el-button @click="fetchPlugins" :loading="loading">刷新</el-button>
      </div>

      <!-- 插件列表 -->
      <el-table :data="plugins" v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="插件名称" />
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column label="大小">
          <template #default="{ row }">
            {{ formatSize(row.size) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button 
              v-if="!row.enabled" 
              type="success" 
              size="small" 
              @click="enablePlugin(row)"
            >
              启用
            </el-button>
            <el-button 
              v-if="row.enabled" 
              type="warning" 
              size="small" 
              @click="disablePlugin(row)"
            >
              禁用
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              @click="deletePlugin(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </showCard>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import showCard from '@/components/commonShowPanel.vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const plugins = ref([])
const loading = ref(false)

const fetchPlugins = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/plugins/list')
    if (res.code === 2000) {
      plugins.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

const formatSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

const enablePlugin = async (row) => {
  try {
    const res = await request.post('/api/plugins/enable', null, {
      params: { fileName: row.fileName }
    })
    if (res.code === 2000) {
      ElMessage.success('启用成功')
      fetchPlugins()
    } else {
      ElMessage.error(res.msg)
    }
  } catch (e) {
    ElMessage.error(e.friendlyMsg || '操作失败')
  }
}

const disablePlugin = async (row) => {
  try {
    const res = await request.post('/api/plugins/disable', null, {
      params: { fileName: row.fileName }
    })
    if (res.code === 2000) {
      ElMessage.success('禁用成功')
      fetchPlugins()
    } else {
      ElMessage.error(res.msg)
    }
  } catch (e) {
    ElMessage.error(e.friendlyMsg || '操作失败')
  }
}

const deletePlugin = async (row) => {
  await ElMessageBox.confirm('确定要删除该插件吗？', '警告', {
    type: 'warning'
  })
  try {
    const res = await request.delete('/api/plugins/delete', {
      params: { fileName: row.fileName }
    })
    if (res.code === 2000) {
      ElMessage.success('删除成功')
      fetchPlugins()
    } else {
      ElMessage.error(res.msg)
    }
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      ElMessage.error(e.friendlyMsg || '删除失败')
    }
  }
}

const handleUpload = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const res = await request.post('/api/plugins/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === 2000) {
      ElMessage.success('上传成功')
      fetchPlugins()
    } else {
      ElMessage.error(res.msg)
    }
  } catch (e) {
    ElMessage.error(e.friendlyMsg || '上传失败')
  }
  return false
}

onMounted(fetchPlugins)
</script>

<style scoped>
.plugin-container {
  padding: 20px;
}
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
</style>