<template>
  <div class="archive">
    <Sidebar :currentPage="'archive'" />
    <main class="main-content">
      <header class="header">
        <h1>📦 归档管理</h1>
      </header>

      <div class="tabs">
        <button 
          class="tab-btn" 
          :class="{ active: activeTab === 'archive' }"
          @click="switchTab('archive')"
        >
          <span>📁</span>
          <span>已归档</span>
          <span class="badge">{{ archivedDiaries.length }}</span>
        </button>
        <button 
          class="tab-btn" 
          :class="{ active: activeTab === 'recycle' }"
          @click="switchTab('recycle')"
        >
          <span>🗑️</span>
          <span>回收站</span>
          <span class="badge">{{ recycleDiaries.length }}</span>
        </button>
      </div>

      <div v-if="activeTab === 'archive'" class="diary-list-container">
        <div v-if="archivedDiaries.length === 0" class="empty-state">
          <div class="empty-icon">📭</div>
          <p>暂无归档日记</p>
          <p class="empty-hint">在日记列表中可以将日记归档</p>
        </div>
        <div v-else class="diary-list">
          <div v-for="diary in archivedDiaries" :key="diary.id" class="diary-card">
            <div class="diary-header">
              <span class="mood-icon">{{ getMoodEmoji(diary.mood) }}</span>
              <h3 class="diary-title">{{ diary.title || '无标题' }}</h3>
            </div>
            <p class="diary-content">{{ diary.content?.slice(0, 100) }}...</p>
            <div class="diary-footer">
              <span class="diary-date">{{ formatDate(diary.diaryDate) }}</span>
              <div class="diary-actions">
                <button class="action-btn restore-btn" @click="unarchiveDiary(diary.id)">
                  <span>📤</span>
                  <span>取消归档</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'recycle'" class="diary-list-container">
        <div v-if="recycleDiaries.length === 0" class="empty-state">
          <div class="empty-icon">🗑️</div>
          <p>回收站是空的</p>
          <p class="empty-hint">删除的日记会出现在这里</p>
        </div>
        <div v-else class="diary-list">
          <div v-for="diary in recycleDiaries" :key="diary.id" class="diary-card">
            <div class="diary-header">
              <span class="mood-icon deleted">{{ getMoodEmoji(diary.mood) }}</span>
              <h3 class="diary-title deleted">{{ diary.title || '无标题' }}</h3>
            </div>
            <p class="diary-content deleted">{{ diary.content?.slice(0, 100) }}...</p>
            <div class="diary-footer">
              <span class="diary-date">{{ formatDate(diary.diaryDate) }}</span>
              <div class="diary-actions">
                <button class="action-btn restore-btn" @click="restoreDiary(diary.id)">
                  <span>🔄</span>
                  <span>恢复</span>
                </button>
                <button class="action-btn delete-btn" @click="confirmDelete(diary.id)">
                  <span>🔥</span>
                  <span>永久删除</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="showConfirmModal" class="modal-overlay">
        <div class="modal-content">
          <div class="modal-header">
            <h3>⚠️ 确认永久删除</h3>
          </div>
          <div class="modal-body">
            <p>此操作无法撤销，确定要永久删除这篇日记吗？</p>
          </div>
          <div class="modal-footer">
            <button class="modal-btn cancel-btn" @click="showConfirmModal = false">取消</button>
            <button class="modal-btn confirm-btn" @click="permanentDelete">确认删除</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import { api } from '../services/api'

const activeTab = ref('archive')
const archivedDiaries = ref([])
const recycleDiaries = ref([])
const showConfirmModal = ref(false)
const deleteId = ref(null)

const getMoodEmoji = (mood) => {
  const emojis = { happy: '😊', sad: '😢', angry: '😠', calm: '😌', excited: '🤩', neutral: '😐' }
  return emojis[mood] || '😐'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

const switchTab = (tab) => {
  activeTab.value = tab
  if (tab === 'archive') {
    loadArchivedDiaries()
  } else {
    loadRecycleDiaries()
  }
}

const loadArchivedDiaries = async () => {
  try {
    const response = await api.diary.archive.list()
    const data = response.data?.data
    archivedDiaries.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('加载归档日记失败:', error)
    archivedDiaries.value = []
  }
}

const loadRecycleDiaries = async () => {
  try {
    const response = await api.diary.archive.recycle()
    const data = response.data?.data
    recycleDiaries.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('加载回收站失败:', error)
    recycleDiaries.value = []
  }
}

const unarchiveDiary = async (id) => {
  try {
    await api.diary.archive.unarchive(id)
    loadArchivedDiaries()
    alert('已取消归档')
  } catch (error) {
    console.error('取消归档失败:', error)
    alert('操作失败，请重试')
  }
}

const restoreDiary = async (id) => {
  try {
    await api.diary.archive.restore(id)
    loadRecycleDiaries()
    alert('已恢复日记')
  } catch (error) {
    console.error('恢复日记失败:', error)
    alert('操作失败，请重试')
  }
}

const confirmDelete = (id) => {
  deleteId.value = id
  showConfirmModal.value = true
}

const permanentDelete = async () => {
  try {
    await api.diary.archive.permanentDelete(deleteId.value)
    loadRecycleDiaries()
    showConfirmModal.value = false
    alert('已永久删除')
  } catch (error) {
    console.error('永久删除失败:', error)
    alert('操作失败，请重试')
  }
}

onMounted(() => {
  loadArchivedDiaries()
})
</script>

<style scoped>
.archive {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8ec 100%);
}

.main-content {
  flex: 1;
  padding: 28px;
  overflow-y: auto;
}

.header {
  margin-bottom: 24px;
}

.header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #2d3748;
  margin: 0;
}

.tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: white;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  color: #4a5568;
  cursor: pointer;
  transition: all 0.3s;
}

.tab-btn:hover {
  border-color: #667eea;
}

.tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
  color: white;
}

.badge {
  background: rgba(0, 0, 0, 0.1);
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
}

.tab-btn.active .badge {
  background: rgba(255, 255, 255, 0.2);
}

.diary-list-container {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state p {
  color: #718096;
  font-size: 16px;
  margin: 8px 0;
}

.empty-hint {
  font-size: 14px !important;
  color: #a0aec0 !important;
}

.diary-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.diary-card {
  padding: 20px;
  background: #f7fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;
}

.diary-card:hover {
  border-color: #667eea;
}

.diary-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.mood-icon {
  font-size: 28px;
}

.mood-icon.deleted {
  filter: grayscale(1);
}

.diary-title {
  font-size: 18px;
  font-weight: 600;
  color: #2d3748;
  margin: 0;
}

.diary-title.deleted {
  text-decoration: line-through;
  color: #a0aec0;
}

.diary-content {
  color: #718096;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 12px 0;
}

.diary-content.deleted {
  color: #cbd5e0;
}

.diary-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.diary-date {
  font-size: 13px;
  color: #a0aec0;
}

.diary-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.restore-btn {
  background: #eef2ff;
  color: #667eea;
}

.restore-btn:hover {
  background: #e0e7ff;
}

.delete-btn {
  background: #fed7d7;
  color: #c53030;
}

.delete-btn:hover {
  background: #feb2b2;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h3 {
  margin: 0;
  color: #2d3748;
}

.modal-body {
  padding: 24px;
}

.modal-body p {
  color: #4a5568;
  font-size: 15px;
  line-height: 1.6;
  margin: 0;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
}

.modal-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn {
  background: #e2e8f0;
  color: #4a5568;
}

.cancel-btn:hover {
  background: #cbd5e0;
}

.confirm-btn {
  background: linear-gradient(135deg, #f5576c 0%, #f093fb 100%);
  color: white;
}

.confirm-btn:hover {
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .main-content {
    padding: 16px;
  }
  
  .tab-btn span:last-child {
    display: none;
  }
  
  .diary-actions {
    flex-direction: column;
  }
  
  .action-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>