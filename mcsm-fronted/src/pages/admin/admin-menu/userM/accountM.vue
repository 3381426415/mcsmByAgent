<template>
  <div class="edit-page-container">
    <div class="table-toolbar">
      <div class="search-box">
        <span class="search-icon">🔍</span>
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="搜索用户名...  " 
          @input="handleSearch"
        />
      </div>
      <button class="btn-primary" @click="addUser">添加新用户</button>
    </div>

    <div class="table-card">
      <table class="apple-table">
        <thead>
          <tr>
            <th>UID</th>
            <th>用户信息</th>
            <th>当前角色</th>
            <th>发布权限</th>
            <th>注册时间</th>
            <th class="text-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in paginatedUsers" :key="user.id">
            <td><span class="uid-tag">#{{ user.id }}</span></td>
            <td>
              <div class="user-info">
                <div class="avatar">{{ user.username.charAt(0).toUpperCase() }}</div>
                <div style="display: flex; flex-direction: column;">
                  <span class="username">{{ user.username }}</span>
                  <small style="color: #86868b; font-size: 11px;">{{ user.nickname || user.email }}</small>
                </div>
              </div>
            </td>
            <td>
              <span class="role-badge" :class="getRoleClass(user.roleName)">
                {{ user.roleName || '未分配' }}
              </span>
              <span v-if="user.baned" style="margin-left: 8px; color: #ff3b30; font-size: 12px;">(已封禁)</span>
            </td>
            <!-- ✅ 新增：发布权限开关 -->
            <td>
              <el-switch
                v-model="user.banPublish"
                :active-value="true"
                :inactive-value="false"
                active-text="禁"
                inactive-text="允"
                @change="handleBanPublishChange(user)"
              />
            </td>
            <td class="last-login">{{ user.creatTime || '-' }}</td>
            <td class="text-right">
              <button class="btn-text edit" @click="openRoleDialog(user)">更改角色</button>
              <button class="btn-text edit" @click="toggleBan(user)">
                {{ user.baned ? '解封' : '封禁' }}
              </button>
              <button class="btn-text delete" @click="deleteUser(user.id)">删除</button>
            </td>
          </tr>
          
          <tr v-if="paginatedUsers.length === 0">
            <td colspan="6" class="empty-state">暂无用户数据</td>
          </tr>
        </tbody>
      </table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="allUsers.length"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handlePageChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog v-model="roleDialogVisible" :title="`更改用户 [${currentUser?.username}] 的角色`" width="350px">
      <div style="padding: 10px 0;">
        <p style="margin-bottom: 15px; font-size: 13px; color: #86868b;">请选择一个新的系统角色：</p>
        <el-radio-group v-model="selectedRoleId" style="width: 100%;">
          <div v-for="role in allRoles" :key="role.roleId" style="margin-bottom: 12px; width: 100%;">
            <el-radio :label="role.roleId" border style="width: 100%; margin-right: 0;">
              {{ role.roleName }}
            </el-radio>
          </div>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUpdateRole" :loading="submitLoading">确认更改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { appleConfirm } from '@/components/MessageBox'

const searchQuery = ref('')
const allUsers = ref([])
const allRoles = ref([]) 
const roleDialogVisible = ref(false)
const currentUser = ref(null)
const selectedRoleId = ref(null)
const submitLoading = ref(false)
let timer = null

const currentPage = ref(1)
const pageSize = ref(10)

const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return allUsers.value.slice(start, end)
})

const getRoleClass = (name) => {
  if (!name) return 'user'
  if (name.includes('管理员') || name.toLowerCase().includes('admin')) return 'admin'
  if (name.includes('维护') || name.toLowerCase().includes('mod')) return 'moderator'
  return 'user'
}

const fetchUsers = async () => {
  try {
    const res = await request.get('/api/users/list', {
      params: { query: searchQuery.value }
    })
    const userData = res.data || []
    
    const usersWithRole = await Promise.all(userData.map(async (u) => {
      try {
        const roleRes = await request.get(`/api/permissions/user/roles/${u.id}`)
        const roleName = roleRes.data && roleRes.data.length > 0 ? roleRes.data[0] : '普通用户'
        return { ...u, roleName }
      } catch (e) {
        return { ...u, roleName: '普通用户' }
      }
    }))
    
    allUsers.value = usersWithRole
    currentPage.value = 1 
  } catch (error) {
    console.error('获取列表失败', error)
  }
}

const fetchAllRoles = async () => {
  try {
    const res = await request.get('/api/roles/list')
    allRoles.value = res.data || []
  } catch (e) {}
}

// ✅ 新增：处理禁止发布开关
const handleBanPublishChange = async (user) => {
  try {
    const res = await request.put('/api/users/ban-publish', {
      userId: user.id,
      banPublish: user.banPublish
    })
    if (res.code === 2000) {
      ElMessage.success(user.banPublish ? '已禁止该用户发布商品' : '已允许该用户发布商品')
    } else {
      ElMessage.error(res.msg || '设置失败')
      user.banPublish = !user.banPublish
    }
  } catch (error) {
    ElMessage.error('操作失败')
    user.banPublish = !user.banPublish
  }
}

const handleSearch = () => {
  clearTimeout(timer)
  timer = setTimeout(() => fetchUsers(), 300)
}

const handlePageChange = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const openRoleDialog = (user) => {
  currentUser.value = user
  const currentRoleObj = allRoles.value.find(r => r.roleName === user.roleName)
  selectedRoleId.value = currentRoleObj ? currentRoleObj.roleId : null
  roleDialogVisible.value = true
}

const submitUpdateRole = async () => {
  if (!selectedRoleId.value) return ElMessage.warning('请选择一个角色')
  submitLoading.value = true
  try {
    const payload = { userId: currentUser.value.id, roleId: selectedRoleId.value }
    await request.post('/api/permissions/user/update-roles', payload)
    ElMessage.success('角色更改成功')
    roleDialogVisible.value = false
    fetchUsers()
  } catch (error) {
    console.error(error)
  } finally {
    submitLoading.value = false
  }
}

const toggleBan = async (user) => {
  const actionText = user.baned ? '解封' : '封禁';
  try {
    await appleConfirm(`确定要${actionText}用户 [${user.username}] 吗？`, '系统通知', user.baned ? 'info' : 'warning');
    await request.put('/api/users/update', { ...user, baned: !user.baned });
    user.baned = !user.baned;
    ElMessage.success(`${actionText}成功`);
  } catch (err) {}
};

const deleteUser = async (id) => {
  try {
    await appleConfirm('确定要永久删除该用户吗？', '警告');
    await request.delete(`/api/users/${id}`);
    ElMessage.success('用户已从系统中移除');
    fetchUsers();
  } catch (err) {}
};

onMounted(() => {
  fetchUsers()
  fetchAllRoles()
})
</script>

<style scoped>
.edit-page-container {
  animation: slideUp 0.4s ease-out;
  width: 100%;
  position: relative;
  z-index: 1;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-box {
  position: relative;
  width: 300px;
}

.search-box input {
  width: 100%;
  padding: 10px 15px 10px 40px;
  background: #f2f2f7;
  border: none;
  border-radius: 10px;
  outline: none;
  font-size: 14px;
  transition: all 0.3s;
}

.search-box input:focus {
  background: #e5e5ea;
  box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.1);
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  opacity: 0.5;
}

.table-card {
  background: #ffffff;
  border-radius: 12px;
  overflow: visible;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
  position: relative;
  z-index: 1;
}

.apple-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  table-layout: auto;
}

.apple-table th {
  padding: 16px;
  font-size: 13px;
  font-weight: 600;
  color: #86868b;
  border-bottom: 1px solid #f2f2f7;
  white-space: nowrap;
  background: #fafafa;
}

.apple-table td {
  padding: 16px;
  border-bottom: 1px solid #f2f2f7;
  font-size: 14px;
  color: #1d1d1f;
  vertical-align: middle;
}

.apple-table tr:hover td {
  background: #f9f9fb;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 32px;
  height: 32px;
  background: #0071e3;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 12px;
  flex-shrink: 0;
}

.uid-tag {
  color: #86868b;
  font-family: monospace;
}

.role-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}

.role-badge.admin {
  background: rgba(255, 59, 48, 0.1);
  color: #ff3b30;
}

.role-badge.user {
  background: rgba(0, 113, 227, 0.1);
  color: #0071e3;
}

.role-badge.moderator {
  background: rgba(255, 159, 10, 0.1);
  color: #ff9f0a;
}

/* 开关样式 */
:deep(.el-switch) {
  --el-switch-on-color: #ff3b30;
  --el-switch-off-color: #34c759;
}

.pagination-wrapper {
  padding: 20px;
  display: flex;
  justify-content: flex-end;
  background: #fff;
  border-top: 1px solid #f2f2f7;
}

.btn-primary {
  background: #0071e3;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary:hover {
  background: #0077ed;
}

.btn-text {
  background: none;
  border: none;
  padding: 6px 10px;
  cursor: pointer;
  font-weight: 500;
  border-radius: 6px;
  transition: 0.2s;
}

.btn-text.edit {
  color: #0071e3;
}

.btn-text.edit:hover {
  background: rgba(0, 113, 227, 0.1);
}

.btn-text.delete {
  color: #ff3b30;
}

.btn-text.delete:hover {
  background: rgba(255, 59, 48, 0.1);
}

.text-right {
  text-align: right;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #86868b;
}

.last-login {
  color: #86868b;
  font-size: 13px;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>