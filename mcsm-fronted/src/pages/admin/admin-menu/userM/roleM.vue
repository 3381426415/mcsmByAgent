<template>
  <showCard>
    <div class="role-page-container">
      <div class="table-toolbar">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input 
            v-model="searchQuery" 
            type="text" 
            placeholder="搜索角色名称..." 
            @input="handleSearch"
          />
        </div>
        <button class="btn-add-role" @click="openAddDialog">
          <span class="plus-icon">+</span> 新增角色
        </button>
      </div>

      <div class="table-card">
        <table class="apple-table">
          <thead>
            <tr>
              <th>角色ID</th>
              <th>角色名称</th>
              <th>拥有权限</th> 
              <th>角色描述</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="role in paginatedRoles" :key="role.roleId">
              <td><code class="role-code">{{ role.roleId }}</code></td>
              <td><span class="role-name">{{ role.roleName }}</span></td>
              <td>
                <div class="perm-display-area">
                  <template v-if="role.permNames && role.permNames.length > 0">
                    <span v-for="name in role.permNames.slice(0, 2)" :key="name" class="perm-tag-simple">
                      {{ name }}
                    </span>
                    <el-popover
                      v-if="role.permNames.length > 2"
                      placement="bottom"
                      trigger="click"
                      width="200"
                    >
                      <template #reference>
                        <span class="more-link">更多({{ role.permNames.length - 2 }})</span>
                      </template>
                      <div class="popover-list">
                        <div v-for="name in role.permNames" :key="name" class="popover-item">{{ name }}</div>
                      </div>
                    </el-popover>
                  </template>
                  <span v-else class="empty-text">暂无权限</span>
                </div>
              </td>
              <td style="color: #86868b; font-size:13px;">
                {{ role.roleDescription || '无描述' }}
              </td>
              <td class="text-right">
                <button class="btn-text edit" @click="openPermissionDialog(role)">配置权限</button>
                <button class="btn-text delete" @click="deleteRole(role.roleId)">删除</button>
              </td>
            </tr>
            <tr v-if="paginatedRoles.length === 0">
              <td colspan="5" class="empty-state">未发现相关角色</td>
            </tr>
          </tbody>
        </table>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="filteredRoles.length"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next"
            @size-change="handlePageChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="addDialogVisible" title="新增系统角色" width="400px">
      <el-form :model="roleForm" label-position="top">
        <el-form-item label="角色名称">
          <el-input v-model="roleForm.roleName" placeholder="例如：高级管理员" />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input v-model="roleForm.roleDescription" type="textarea" placeholder="简述该角色的职责" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddRole" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permDialogVisible" :title="`为 [${currentRole?.roleName}] 分配权限`" width="500px">
      <div style="max-height: 400px; overflow-y: auto;">
        <el-checkbox-group v-model="selectedPermIds">
          <div v-for="perm in allPermissions" :key="perm.permId" style="margin-bottom: 12px; border-bottom: 1px solid #f2f2f7; padding-bottom: 8px;">
            <el-checkbox :label="perm.permId">
              <span style="font-weight: 500;">{{ perm.name }}</span>
              <p style="margin: 4px 0 0 24px; font-size: 12px; color: #86868b;">{{ perm.description }}</p>
            </el-checkbox>
          </div>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignPermissions" :loading="submitLoading">保存配置</el-button>
      </template>
    </el-dialog>
  </showCard>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import showCard from "@/components/commonShowPanel.vue"
import { ElMessage } from 'element-plus'
import { appleConfirm } from '@/components/MessageBox'

const searchQuery = ref('')
const roles = ref([])
const allPermissions = ref([])
const submitLoading = ref(false)
const addDialogVisible = ref(false)
const permDialogVisible = ref(false)
const currentRole = ref(null)
const roleForm = ref({ roleName: '', roleDescription: '' })
const selectedPermIds = ref([])

// --- 分页相关变量 ---
const currentPage = ref(1)
const pageSize = ref(10)

// 逻辑：1. 先过滤搜索内容
const filteredRoles = computed(() => {
  return roles.value.filter(r => r.roleName?.toLowerCase().includes(searchQuery.value.toLowerCase()))
})

// 逻辑：2. 对过滤后的结果进行分页切片
const paginatedRoles = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredRoles.value.slice(start, end)
})

const handleSearch = () => {
  // 搜索时重置页码到第一页
  currentPage.value = 1
}

const handlePageChange = () => {
  // 处理分页切换后的逻辑，如平滑滚动到顶部
  const container = document.querySelector('.table-card')
  if (container) container.scrollIntoView({ behavior: 'smooth' })
}

const initData = async () => {
  try {
    const [roleRes, permRes] = await Promise.all([
      request.get('/api/roles/list'),
      request.get('/api/roles/all-permissions')
    ])
    
    const rolesData = roleRes.data
    const rolesWithPerms = await Promise.all(rolesData.map(async (role) => {
      try {
        const pRes = await request.get(`/api/roles/role-permissions/names/${role.roleId}`)
        return { ...role, permNames: pRes.data }
      } catch (e) {
        return { ...role, permNames: [] }
      }
    }))

    roles.value = rolesWithPerms
    allPermissions.value = permRes.data
  } catch (err) {
    console.error('初始化数据失败', err)
    ElMessage.error(err.friendlyMsg || '获取数据失败')
  }
}

const openAddDialog = () => {
  roleForm.value = { roleName: '', roleDescription: '' }
  addDialogVisible.value = true
}

const submitAddRole = async () => {
  if (!roleForm.value.roleName) return ElMessage.warning('请输入角色名称')
  submitLoading.value = true
  try {
    const res = await request.post('/api/roles/add', roleForm.value)
    if (res.code === 2000) {
      ElMessage.success('角色添加成功')
      addDialogVisible.value = false
      initData()
    }
  } finally {
    submitLoading.value = false
  }
}

const openPermissionDialog = async (role) => {
  currentRole.value = role
  try {
    const res = await request.get(`/api/roles/role-permissions/ids/${role.roleId}`)
    selectedPermIds.value = res.data
  } catch (e) {
    selectedPermIds.value = []
  }
  permDialogVisible.value = true
}

const submitAssignPermissions = async () => {
  submitLoading.value = true
  try {
    const res = await request.post(`/api/roles/assign-permissions?roleId=${currentRole.value.roleId}`, selectedPermIds.value)
    if (res.code === 2000) {
      ElMessage.success('权限分配成功')
      permDialogVisible.value = false
      initData()
    }
  } finally {
    submitLoading.value = false
  }
}

const deleteRole = (id) => {
  appleConfirm('确定要删除这个角色吗？此操作不可撤销。')
    .then(async () => {
      const res = await request.delete(`/api/roles/${id}`)
      if (res.code === 2000) {
        ElMessage.success('已安全删除')
        initData()
      }
    })
    .catch(() => {})
}

onMounted(initData)
</script>

<style scoped>
/* 保持原有样式 */
.perm-display-area { display: flex; align-items: center; gap: 6px; }
.perm-tag-simple { 
  background: #f2f2f7; 
  padding: 2px 8px; 
  border-radius: 4px; 
  font-size: 12px; 
  color: #1d1d1f; 
}
.more-link { color: #0071e3; font-size: 12px; cursor: pointer; text-decoration: underline; }
.popover-list { padding: 5px 0; }
.popover-item { padding: 4px 0; border-bottom: 1px solid #f2f2f7; font-size: 13px; }
.empty-text { color: #86868b; font-size: 12px; }

.role-page-container { animation: fadeIn 0.4s ease-out; }
.table-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.search-box { position: relative; width: 280px; }
.search-box input { width: 100%; padding: 10px 15px 10px 38px; background: rgba(118, 118, 128, 0.12); border: none; border-radius: 10px; font-size: 14px; outline: none; }
.search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); opacity: 0.4; }
.btn-add-role { background: #0071e3; color: white; border: none; padding: 8px 16px; border-radius: 10px; font-weight: 500; cursor: pointer; }
.table-card { background: #ffffff; border-radius: 14px; overflow: hidden; border: 1px solid #f2f2f7; }
.apple-table { width: 100%; border-collapse: collapse; }
.apple-table th { background: #fafafa; padding: 14px 18px; text-align: left; font-size: 13px; color: #86868b; border-bottom: 1px solid #f2f2f7; }
.apple-table td { padding: 16px 18px; border-bottom: 1px solid #f2f2f7; font-size: 14px; }
.role-code { background: #f2f2f7; padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 12px; }

/* 补充分页器布局样式 */
.pagination-wrapper {
  padding: 16px 20px;
  display: flex;
  justify-content: flex-end;
  background: #fff;
  border-top: 1px solid #f2f2f7;
}

.btn-text { background: none; border: none; padding: 6px 12px; font-size: 14px; cursor: pointer; border-radius: 6px; }
.btn-text.edit { color: #0071e3; }
.btn-text.delete { color: #ff3b30; }
.text-right { text-align: right; }
.empty-state { text-align: center; padding: 40px; color: #86868b; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
</style>