<template>
  <div class="friends-page">
    <Sidebar :currentPage="'friends'" />
    <main class="main-content">
      <header class="page-header">
        <h1>👥 我的好友</h1>
        <button class="btn btn-primary" @click="openAddModal">添加好友</button>
      </header>

      <div class="tabs">
        <button 
          v-for="tab in tabs" 
          :key="tab.value"
          class="tab-btn"
          :class="{ active: activeTab === tab.value }"
          @click="activeTab = tab.value"
        >
          {{ tab.label }}
          <span class="tab-count" v-if="getTabCount(tab.value) > 0">{{ getTabCount(tab.value) }}</span>
        </button>
      </div>

      <div v-if="activeTab === 'friends'" class="friends-list">
        <div v-if="friends.length === 0" class="empty-state">
          <div class="empty-icon">👥</div>
          <p>暂无好友，快去添加好友吧</p>
        </div>
        <div v-else class="friend-grid">
          <div v-for="friend in friends" :key="friend.id" class="friend-card">
            <div class="friend-avatar">{{ friend.nickname?.charAt(0) || '?' }}</div>
            <div class="friend-info">
              <h3>{{ friend.nickname || friend.username }}</h3>
              <p class="friend-status">好友</p>
            </div>
            <button class="friend-action" @click="viewFriend(friend)">查看</button>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'requests'" class="requests-list">
        <div v-if="requests.length === 0" class="empty-state">
          <div class="empty-icon">📭</div>
          <p>暂无好友请求</p>
        </div>
        <div v-else class="request-list">
          <div v-for="request in requests" :key="request.id" class="request-item">
            <div class="request-avatar">{{ request.nickname?.charAt(0) || request.username?.charAt(0) || '?' }}</div>
            <div class="request-info">
              <h3>{{ request.nickname || request.username }}</h3>
              <p>请求添加你为好友</p>
            </div>
            <div class="request-actions">
              <button class="action-btn accept" @click="acceptRequest(request)">✓ 接受</button>
              <button class="action-btn reject" @click="rejectRequest(request)">✕ 拒绝</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 添加好友弹窗 -->
      <div v-if="showAddModal" class="modal-overlay" @click.self="closeAddModal">
        <div class="modal-content">
          <div class="modal-header">
            <h2>添加好友</h2>
            <button class="close-btn" @click="closeAddModal">✕</button>
          </div>
          <form @submit.prevent="sendRequest">
            <div class="form-group">
              <label>搜索用户名</label>
              <div class="search-input-group">
                <input 
                  type="text" 
                  v-model="searchUsername" 
                  placeholder="输入好友用户名" 
                  required
                  @keyup.enter="searchUsers"
                >
                <button type="button" class="btn btn-search" @click="searchUsers">搜索</button>
              </div>
            </div>
            <div class="search-results" v-if="searchResults.length > 0">
              <p>搜索结果：</p>
              <div 
                v-for="user in searchResults" 
                :key="user.id" 
                class="search-item"
                :class="{ selected: selectedUser?.id === user.id }"
                @click="selectUser(user)"
              >
                <span class="search-avatar">{{ user.nickname?.charAt(0) || user.username?.charAt(0) || '?' }}</span>
                <div class="search-info">
                  <span class="search-name">{{ user.nickname || user.username }}</span>
                  <span class="search-username">@{{ user.username }}</span>
                </div>
                <span v-if="selectedUser?.id === user.id" class="selected-icon">✓</span>
              </div>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeAddModal">取消</button>
              <button type="submit" class="btn btn-primary" :disabled="!selectedUser">发送请求</button>
            </div>
          </form>
        </div>
      </div>

      <!-- 好友详情弹窗 -->
      <div v-if="showFriendDetail" class="modal-overlay" @click.self="closeFriendDetail">
        <div class="modal-content detail-modal">
          <div class="modal-header">
            <h2>{{ selectedFriend?.nickname || selectedFriend?.username }}</h2>
            <button class="close-btn" @click="closeFriendDetail">✕</button>
          </div>
          <div class="friend-detail">
            <div class="detail-avatar">{{ selectedFriend?.nickname?.charAt(0) || '?' }}</div>
            <div class="detail-info">
              <p><strong>用户名：</strong>{{ selectedFriend?.username }}</p>
              <p><strong>昵称：</strong>{{ selectedFriend?.nickname }}</p>
              <p><strong>好友状态：</strong>已添加</p>
            </div>
          </div>
          <div class="detail-actions">
            <button class="btn btn-danger" @click="removeFriend">删除好友</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import { api } from '../services/api'

const friends = ref([])
const requests = ref([])
const activeTab = ref('friends')
const showAddModal = ref(false)
const showFriendDetail = ref(false)
const searchUsername = ref('')
const searchResults = ref([])
const selectedFriend = ref(null)

const selectedUser = ref(null)

const tabs = [
  { value: 'friends', label: '好友列表' },
  { value: 'requests', label: '好友请求' }
]

const getTabCount = (tab) => {
  if (tab === 'friends') return friends.value.length
  if (tab === 'requests') return requests.value.length
  return 0
}

const loadFriends = async () => {
  try {
    const response = await api.friend.list()
    friends.value = response.data.data || []
  } catch (error) {
    console.error('加载好友列表失败:', error)
  }
}

const loadRequests = async () => {
  try {
    const response = await api.friend.requests()
    requests.value = response.data.data || []
  } catch (error) {
    console.error('加载好友请求失败:', error)
  }
}

const searchUsers = async () => {
  if (!searchUsername.value.trim()) {
    searchResults.value = []
    return
  }
  
  console.log('开始搜索用户:', searchUsername.value)
  
  try {
    const response = await api.friend.search(searchUsername.value.trim())
    console.log('搜索响应:', response)
    searchResults.value = response.data.data || []
    console.log('搜索结果数量:', searchResults.value.length)
  } catch (error) {
    console.error('搜索用户失败:', error)
    console.error('搜索错误详情:', error.response ? error.response.data : error.message)
  }
}

const selectUser = (user) => {
  selectedUser.value = user
  searchUsername.value = user.username
}

const sendRequest = async () => {
  if (!selectedUser.value) {
    alert('请先选择要添加的用户')
    return
  }

  console.log('发送好友请求，目标用户ID:', selectedUser.value.id)

  try {
    const response = await api.friend.add(selectedUser.value.id)
    console.log('发送好友请求响应:', response)
    
    if (response.data.code === 200) {
      alert('好友请求已发送')
      closeAddModal()
    } else {
      alert(response.data.message || '发送失败')
    }
  } catch (error) {
    console.error('发送好友请求失败:', error)
    console.error('错误详情:', error.response ? error.response.data : error.message)
    alert('发送失败，请重试')
  }
}

const acceptRequest = async (request) => {
  try {
    await api.friend.accept(request.id)
    loadRequests()
    loadFriends()
    alert('已接受好友请求')
  } catch (error) {
    console.error('接受好友请求失败:', error)
    alert('操作失败，请重试')
  }
}

const rejectRequest = async (request) => {
  try {
    await api.friend.reject(request.id)
    loadRequests()
    alert('已拒绝好友请求')
  } catch (error) {
    console.error('拒绝好友请求失败:', error)
    alert('操作失败，请重试')
  }
}

const viewFriend = (friend) => {
  selectedFriend.value = friend
  showFriendDetail.value = true
}

const openAddModal = () => {
  showAddModal.value = true
}

const closeAddModal = () => {
  showAddModal.value = false
  searchUsername.value = ''
  searchResults.value = []
  selectedUser.value = null
}

const closeFriendDetail = () => {
  showFriendDetail.value = false
  selectedFriend.value = null
}

const removeFriend = async () => {
  if (!confirm('确定要删除这个好友吗？')) return

  try {
    await api.friend.delete(selectedFriend.value.id)
    closeFriendDetail()
    loadFriends()
    alert('已删除好友')
  } catch (error) {
    console.error('删除好友失败:', error)
    alert('删除失败，请重试')
  }
}

watch(searchUsername, (newVal) => {
  if (newVal.trim().length > 0) {
    searchUsers()
  } else {
    searchResults.value = []
    selectedUser.value = null
  }
})

onMounted(() => {
  loadFriends()
  loadRequests()
  
  searchUsername.value = ''
})
</script>

<style scoped>
.friends-page {
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  color: #333;
}

.btn {
  padding: 10px 20px;
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

.btn-danger {
  background: #ef5350;
  color: white;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
}

.tab-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 20px;
  background: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-btn:hover {
  background: #f0f0f0;
}

.tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.tab-count {
  background: rgba(255, 255, 255, 0.3);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.empty-state {
  text-align: center;
  padding: 60px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state p {
  color: #999;
  font-size: 16px;
}

.friend-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.friend-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.friend-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
}

.friend-info {
  flex: 1;
}

.friend-info h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 4px;
}

.friend-status {
  color: #999;
  font-size: 13px;
}

.friend-action {
  padding: 8px 16px;
  border: 1px solid #667eea;
  border-radius: 6px;
  color: #667eea;
  background: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.friend-action:hover {
  background: #f5f3ff;
}

.request-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.request-item {
  background: white;
  padding: 16px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.request-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}

.request-info h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 4px;
}

.request-info p {
  color: #999;
  font-size: 13px;
}

.request-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.action-btn.accept {
  background: #e8f5e9;
  color: #2e7d32;
}

.action-btn.reject {
  background: #ffebee;
  color: #c62828;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  padding: 24px;
  width: 100%;
  max-width: 450px;
}

.detail-modal {
  max-width: 400px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.modal-header h2 {
  font-size: 18px;
  color: #333;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #f0f0f0;
  border-radius: 50%;
  font-size: 16px;
  cursor: pointer;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #333;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
}

.search-results {
  margin-bottom: 16px;
}

.search-results p {
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.search-item:hover {
  background: #e8e8e8;
}

.search-item.selected {
  background: #e8f5e9;
  border-color: #667eea;
}

.search-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.search-name {
  font-weight: 500;
  color: #333;
}

.search-username {
  font-size: 12px;
  color: #999;
}

.selected-icon {
  color: #667eea;
  font-weight: bold;
  font-size: 16px;
}

.search-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.form-actions,
.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.detail-avatar {
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
  margin: 0 auto 20px;
}

.detail-info {
  margin-bottom: 24px;
}

.detail-info p {
  margin-bottom: 8px;
  color: #666;
}

@media (max-width: 768px) {
  .main-content {
    padding-left: 84px;
  }
  .friend-grid {
    grid-template-columns: 1fr;
  }
}
</style>