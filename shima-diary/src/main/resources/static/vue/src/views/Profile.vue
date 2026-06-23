<template>
  <div class="profile-page">
    <Sidebar :currentPage="'profile'" />
    <main class="main-content">
      <header class="page-header">
        <h1>👤 个人中心</h1>
      </header>

      <div class="profile-section">
        <div class="profile-card">
          <div class="profile-avatar">{{ user?.nickname?.charAt(0) || '?' }}</div>
          <div class="profile-info">
            <h2>{{ user?.nickname || user?.username }}</h2>
            <p class="username">@{{ user?.username }}</p>
            <p class="join-date">加入于 {{ formatJoinDate() }}</p>
          </div>
        </div>

        <div class="stats-row">
          <div class="stat-item">
            <span class="stat-value">{{ stats.diaryCount }}</span>
            <span class="stat-label">日记</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.taskCount }}</span>
            <span class="stat-label">任务</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.checkinCount }}</span>
            <span class="stat-label">打卡</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.friendCount }}</span>
            <span class="stat-label">好友</span>
          </div>
        </div>
      </div>

      <div class="settings-section">
        <h2>账户设置</h2>
        <div class="settings-card">
          <div class="settings-item">
            <label>昵称</label>
            <input type="text" v-model="formData.nickname" placeholder="输入昵称">
          </div>
          <div class="settings-item">
            <label>邮箱</label>
            <input type="email" v-model="formData.email" placeholder="输入邮箱">
          </div>
          <div class="settings-item">
            <label>个性签名</label>
            <textarea v-model="formData.bio" placeholder="输入个性签名" rows="3"></textarea>
          </div>
          <button class="btn btn-primary" @click="updateProfile">保存修改</button>
        </div>
      </div>

      <div class="password-section">
        <h2>修改密码</h2>
        <div class="settings-card">
          <div class="settings-item">
            <label>当前密码</label>
            <input type="password" v-model="passwordData.oldPassword" placeholder="输入当前密码">
          </div>
          <div class="settings-item">
            <label>新密码</label>
            <input type="password" v-model="passwordData.newPassword" placeholder="输入新密码">
          </div>
          <div class="settings-item">
            <label>确认新密码</label>
            <input type="password" v-model="passwordData.confirmPassword" placeholder="确认新密码">
          </div>
          <button class="btn btn-secondary" @click="changePassword">修改密码</button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import { api } from '../services/api'

const user = ref(null)
const stats = reactive({
  diaryCount: 0,
  taskCount: 0,
  checkinCount: 0,
  friendCount: 0
})

const formData = reactive({
  nickname: '',
  email: '',
  bio: ''
})

const passwordData = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const formatJoinDate = () => {
  if (!user.value?.createTime) return ''
  const date = new Date(user.value.createTime)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

const loadUser = async () => {
  try {
    const response = await api.user.getCurrent()
    if (response.data.code === 200 && response.data.data) {
      user.value = response.data.data
      formData.nickname = user.value.nickname || ''
      formData.email = user.value.email || ''
      formData.bio = user.value.bio || ''
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
  }
}

const loadStats = async () => {
  try {
    const [diaryRes, taskRes, checkinRes, friendRes] = await Promise.all([
      api.diary.count(),
      api.task.list(),
      api.checkin.count(),
      api.friend.list()
    ])
    stats.diaryCount = diaryRes.data.data || 0
    stats.taskCount = taskRes.data.data?.length || 0
    stats.checkinCount = checkinRes.data.data || 0
    stats.friendCount = friendRes.data.data?.length || 0
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const updateProfile = async () => {
  try {
    const data = {
      nickname: formData.nickname,
      email: formData.email,
      bio: formData.bio
    }
    
    const response = await api.user.update(data)
    if (response.data.code === 200) {
      await loadUser()
      alert('修改成功')
    } else {
      alert(response.data.message || '修改失败')
    }
  } catch (error) {
    console.error('修改资料失败:', error)
    alert('修改失败，请重试')
  }
}

const changePassword = async () => {
  if (!passwordData.oldPassword) {
    alert('请输入当前密码')
    return
  }
  if (!passwordData.newPassword) {
    alert('请输入新密码')
    return
  }
  if (passwordData.newPassword.length < 6) {
    alert('新密码长度不能少于6位')
    return
  }
  if (passwordData.newPassword !== passwordData.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }

  try {
    const response = await api.user.changePassword({
      oldPassword: passwordData.oldPassword,
      newPassword: passwordData.newPassword
    })
    
    if (response.data.code === 200) {
      alert('密码修改成功，请重新登录')
      passwordData.oldPassword = ''
      passwordData.newPassword = ''
      passwordData.confirmPassword = ''
    } else {
      alert(response.data.message || '修改失败')
    }
  } catch (error) {
    console.error('修改密码失败:', error)
    alert('修改失败，请重试')
  }
}

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    user.value = JSON.parse(userStr)
  }
  loadUser()
  loadStats()
})
</script>

<style scoped>
.profile-page {
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

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  color: #333;
}

.profile-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 600;
}

.profile-info h2 {
  font-size: 20px;
  color: #333;
  margin-bottom: 4px;
}

.profile-info .username {
  color: #667eea;
  font-size: 14px;
  margin-bottom: 4px;
}

.profile-info .join-date {
  color: #999;
  font-size: 13px;
}

.stats-row {
  display: flex;
  justify-content: space-around;
  padding-top: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  display: block;
}

.stat-label {
  color: #999;
  font-size: 13px;
}

.settings-section,
.password-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.settings-section h2,
.password-section h2 {
  font-size: 16px;
  color: #333;
  margin-bottom: 20px;
}

.settings-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.settings-item {
  display: flex;
  flex-direction: column;
}

.settings-item label {
  margin-bottom: 6px;
  color: #333;
  font-weight: 500;
  font-size: 14px;
}

.settings-item input,
.settings-item textarea {
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
}

.settings-item textarea {
  resize: vertical;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.3s;
}

.btn:hover {
  opacity: 0.9;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-secondary {
  background: #f0f0f0;
  color: #666;
}

@media (max-width: 768px) {
  .main-content {
    padding-left: 84px;
  }
}
</style>