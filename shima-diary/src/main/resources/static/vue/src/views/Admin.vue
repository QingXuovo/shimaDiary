<template>
  <div class="admin">
    <Sidebar :currentPage="'admin'" />
    <main class="main-content">
      <div class="admin-section">
        <div class="section-header">
          <h2>用户管理</h2>
          <button class="btn btn-primary" @click="refreshUsers">刷新列表</button>
        </div>

        <div class="user-table-container">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>用户名</th>
                <th>昵称</th>
                <th>角色</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="userItem in users" :key="userItem.id">
                <td>{{ userItem.id }}</td>
                <td>{{ userItem.username }}</td>
                <td>{{ userItem.nickname || '-' }}</td>
                <td>
                  <span class="badge" :class="userItem.role === 'admin' ? 'badge-admin' : 'badge-user'">
                    {{ userItem.role === 'admin' ? '管理员' : '普通用户' }}
                  </span>
                </td>
                <td>
                  <button class="btn btn-sm btn-success" @click="viewUserDetails(userItem.id)">查看详情</button>
                  <button class="btn btn-sm btn-primary" @click="editUser(userItem)">编辑</button>
                  <button 
                    v-if="userItem.id !== user.id" 
                    class="btn btn-sm btn-danger" 
                    @click="deleteUser(userItem.id, userItem.username)"
                  >删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </main>

    <!-- 用户详情模态框 -->
    <div class="modal" :class="{ active: showViewModal }">
      <div class="modal-content large">
        <div class="modal-header">
          <h3>用户详情</h3>
          <button class="modal-close" @click="closeViewModal">×</button>
        </div>
        <div class="modal-body" id="userDetailsContent">
          <div v-if="viewLoading" class="loading">加载中...</div>
          <div v-else-if="viewUser">
            <!-- 基本信息卡片 -->
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 12px; padding: 20px; margin-bottom: 20px; color: white;">
              <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;">
                <div>
                  <div style="font-size: 24px; font-weight: 700;">{{ viewUser.username }}</div>
                  <div style="opacity: 0.9; font-size: 14px;">{{ viewUser.nickname || '暂无昵称' }}</div>
                </div>
                <span style="padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 500; background: rgba(255,255,255,0.2);">
                  {{ viewUser.role === 'admin' ? '管理员' : '普通用户' }}
                </span>
              </div>
              <div style="opacity: 0.8; font-size: 13px;">用户ID: {{ viewUser.id }}</div>
            </div>
            
            <!-- 数据统计卡片 -->
            <div style="background: #f8f9fa; border-radius: 12px; padding: 20px; margin-bottom: 20px;">
              <h3 style="margin-bottom: 16px; color: #333; font-size: 16px; font-weight: 600;">📊 数据统计</h3>
              <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;">
                <div style="background: white; padding: 16px; border-radius: 10px; text-align: center;">
                  <div style="font-size: 28px; font-weight: 700; color: #667eea;">{{ viewStats.diaryCount }}</div>
                  <div style="color: #666; font-size: 13px;">📝 日记</div>
                </div>
                <div style="background: white; padding: 16px; border-radius: 10px; text-align: center;">
                  <div style="font-size: 28px; font-weight: 700; color: #10b981;">{{ viewStats.checkinCount }}</div>
                  <div style="color: #666; font-size: 13px;">✅ 打卡</div>
                </div>
                <div style="background: white; padding: 16px; border-radius: 10px; text-align: center;">
                  <div style="font-size: 28px; font-weight: 700; color: #f59e0b;">{{ viewStats.taskCount }}</div>
                  <div style="color: #666; font-size: 13px;">📋 任务</div>
                </div>
              </div>
            </div>

            <!-- 最近记录 -->
            <div v-if="viewRecentDiaries.length > 0" style="background: white; border-radius: 12px; padding: 20px; margin-bottom: 20px;">
              <h3 style="margin-bottom: 16px; color: #333; font-size: 16px; font-weight: 600;">📝 最近日记</h3>
              <div style="max-height: 150px; overflow-y: auto;">
                <div v-for="(d, i) in viewRecentDiaries" :key="i" style="padding: 8px 0; border-bottom: 1px solid #f0f0f0;">
                  <div style="font-weight: 500;">{{ d.title || '无标题' }}</div>
                  <div style="font-size: 12px; color: #999;">{{ d.diaryDate }}</div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">用户不存在</div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeViewModal">关闭</button>
        </div>
      </div>
    </div>

    <!-- 编辑用户模态框 -->
    <div class="modal" :class="{ active: showEditModal }">
      <div class="modal-content">
        <div class="modal-header">
          <h3>编辑用户</h3>
          <button class="modal-close" @click="closeEditModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>用户名</label>
            <input type="text" v-model="editForm.username" readonly>
          </div>
          <div class="form-group">
            <label>昵称</label>
            <input type="text" v-model="editForm.nickname" placeholder="输入昵称">
          </div>
          <div class="form-group">
            <label>角色</label>
            <select v-model="editForm.role">
              <option value="user">普通用户</option>
              <option value="admin">管理员</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeEditModal">取消</button>
          <button class="btn btn-primary" @click="saveUser">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../services/api'
import Sidebar from '../components/Sidebar.vue'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const users = ref([])
const showViewModal = ref(false)
const showEditModal = ref(false)
const viewLoading = ref(false)
const viewUser = ref(null)
const viewStats = ref({ diaryCount: 0, checkinCount: 0, taskCount: 0 })
const viewRecentDiaries = ref([])
const editingUserId = ref(null)
const editForm = ref({
  username: '',
  nickname: '',
  role: 'user'
})

async function loadUsers() {
  try {
    const response = await api.user.getAll()
    users.value = response.data.data || []
  } catch (error) {
    console.error('加载用户列表失败:', error)
  }
}

async function viewUserDetails(userId) {
  showViewModal.value = true
  viewLoading.value = true
  
  try {
    const userResponse = await api.user.getById(userId)
    if (userResponse.data.code === 200 && userResponse.data.data) {
      viewUser.value = userResponse.data.data
      
      const [diaryRes, checkinRes, taskRes] = await Promise.all([
        api.diary.list(),
        api.checkin.list(),
        api.task.list()
      ])
      
      const userDiaries = (diaryRes.data.data || []).filter(d => d.userId === userId)
      const userCheckins = (checkinRes.data.data || []).filter(c => c.userId === userId)
      const userTasks = (taskRes.data.data || []).filter(t => t.userId === userId)
      
      viewStats.value = {
        diaryCount: userDiaries.length,
        checkinCount: userCheckins.length,
        taskCount: userTasks.length
      }
      
      viewRecentDiaries.value = userDiaries.slice(0, 5)
    } else {
      viewUser.value = null
    }
  } catch (error) {
    console.error('加载用户详情失败:', error)
    viewUser.value = null
  } finally {
    viewLoading.value = false
  }
}

function closeViewModal() {
  showViewModal.value = false
  viewUser.value = null
}

function editUser(userItem) {
  editingUserId.value = userItem.id
  editForm.value = {
    username: userItem.username,
    nickname: userItem.nickname || '',
    role: userItem.role
  }
  showEditModal.value = true
}

function closeEditModal() {
  showEditModal.value = false
  editingUserId.value = null
}

async function saveUser() {
  try {
    await api.user.updateAdmin(editingUserId.value, editForm.value)
    closeEditModal()
    loadUsers()
  } catch (error) {
    console.error('更新用户失败:', error)
    alert('更新失败')
  }
}

async function deleteUser(userId, username) {
  if (confirm(`确定删除用户 "${username}" 吗？`)) {
    try {
      await api.user.delete(userId)
      loadUsers()
    } catch (error) {
      console.error('删除用户失败:', error)
      alert('删除失败')
    }
  }
}

function refreshUsers() {
  loadUsers()
}

async function handleLogout() {
  localStorage.removeItem('user')
  router.push('/')
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.admin {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f5f7fa;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.admin-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 {
  font-size: 18px;
  color: #333;
}

.user-table-container {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

table th,
table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

table th {
  background: #f8f9fa;
  font-weight: 600;
  color: #666;
}

table tr:hover {
  background: #fafafa;
}

.badge {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.badge-admin {
  background: #ffe0b2;
  color: #e65100;
}

.badge-user {
  background: #c8e6c9;
  color: #2e7d32;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-secondary {
  background: #e9ecef;
  color: #666;
}

.btn-success {
  background: #81c784;
  color: white;
}

.btn-danger {
  background: #ffcdd2;
  color: #c62828;
}

.modal {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal.active {
  display: flex;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-content.large {
  max-width: 600px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  background: white;
}

.modal-header h3 {
  font-size: 18px;
  color: #333;
}

.modal-close {
  font-size: 24px;
  background: none;
  border: none;
  cursor: pointer;
  color: #999;
}

.modal-body {
  padding: 24px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #eee;
  background: #fafafa;
  border-radius: 0 0 12px 12px;
  position: sticky;
  bottom: 0;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.form-group input:focus,
.form-group select:focus {
  border-color: #667eea;
}

.form-group input[readonly] {
  background: #f5f5f5;
  color: #999;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #667eea;
}
</style>
