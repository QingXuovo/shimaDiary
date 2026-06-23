<template>
  <div class="diary-page">
    <Sidebar :currentPage="'diary'" />
    <main class="main-content">
      <header class="page-header">
        <h1>📝 我的日记</h1>
        <button class="btn btn-primary" @click="openCreateModal">写日记</button>
      </header>

      <div class="filter-bar">
        <input 
          type="text" 
          v-model="searchKeyword" 
          placeholder="搜索日记..." 
          @input="handleSearch"
          class="search-input"
        >
        <select v-model="filterMood" @change="filterByMood" class="filter-select">
          <option value="">全部心情</option>
          <option value="happy">😊 开心</option>
          <option value="sad">😢 难过</option>
          <option value="angry">😠 生气</option>
          <option value="calm">😌 平静</option>
          <option value="excited">🤩 兴奋</option>
        </select>
      </div>

      <div class="diary-grid">
        <div v-for="diary in filteredDiaries" :key="diary.id" class="diary-card" @click="openDetail(diary)">
          <div class="diary-mood">{{ getMoodEmoji(diary.mood) }}</div>
          <h3>{{ diary.title || '无标题' }}</h3>
          <p>{{ diary.content?.slice(0, 100) }}...</p>
          <div class="diary-meta">
            <span>{{ formatDate(diary.diaryDate) }}</span>
            <span v-if="diary.categoryName" class="category-tag">{{ diary.categoryName }}</span>
          </div>
        </div>
      </div>

      <div v-if="filteredDiaries.length === 0" class="empty-state">
        <div class="empty-icon">📔</div>
        <p>暂无日记，开始记录你的第一篇日记吧</p>
      </div>

      <!-- 创建/编辑日记弹窗 -->
      <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal-content">
          <div class="modal-header">
            <h2>{{ editingDiary ? '编辑日记' : '写日记' }}</h2>
            <button class="close-btn" @click="closeModal">✕</button>
          </div>
          <form @submit.prevent="saveDiary">
            <div class="form-group">
              <label>标题</label>
              <input type="text" v-model="formData.title" placeholder="输入日记标题（可选）">
            </div>
            <div class="form-group">
              <label>心情</label>
              <div class="mood-selector">
                <button 
                  v-for="mood in moods" 
                  :key="mood.value"
                  type="button"
                  class="mood-btn"
                  :class="{ active: formData.mood === mood.value }"
                  @click="formData.mood = mood.value"
                >
                  {{ mood.emoji }}
                </button>
              </div>
            </div>
            <div class="form-group">
              <label>分类</label>
              <div class="category-options">
                <label 
                  v-for="preset in presetCategories" 
                  :key="preset.id"
                  class="category-option"
                  :class="{ active: formData.categoryId === preset.id }"
                >
                  <input 
                    type="radio" 
                    :value="preset.id" 
                    v-model="formData.categoryId"
                    style="display:none"
                  >
                  <span class="category-icon">{{ preset.icon }}</span>
                  <span class="category-name">{{ preset.name }}</span>
                </label>
                <label 
                  class="category-option"
                  :class="{ active: formData.categoryId === '' }"
                >
                  <input 
                    type="radio" 
                    value="" 
                    v-model="formData.categoryId"
                    style="display:none"
                  >
                  <span class="category-icon">🚫</span>
                  <span class="category-name">不选</span>
                </label>
              </div>
            </div>
            <div class="form-group">
              <label>内容</label>
              <textarea v-model="formData.content" placeholder="记录今天的心情和故事..." rows="6"></textarea>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeModal">取消</button>
              <button type="submit" class="btn btn-primary">保存</button>
            </div>
          </form>
        </div>
      </div>

      <!-- 日记详情弹窗 -->
      <div v-if="showDetail" class="modal-overlay" @click.self="closeDetail">
        <div class="modal-content detail-modal">
          <div class="modal-header">
            <h2>{{ selectedDiary?.title || '无标题' }}</h2>
            <button class="close-btn" @click="closeDetail">✕</button>
          </div>
          <div class="diary-detail">
            <div class="detail-meta">
              <span class="detail-mood">{{ getMoodEmoji(selectedDiary?.mood) }}</span>
              <span>{{ formatDate(selectedDiary?.diaryDate) }}</span>
              <span v-if="selectedDiary?.categoryName" class="category-tag">{{ selectedDiary.categoryName }}</span>
            </div>
            <p class="detail-content">{{ selectedDiary?.content }}</p>
          </div>
          <div class="detail-actions">
            <button class="btn btn-secondary" @click="editDiary">编辑</button>
            <button class="btn btn-share" @click="shareDiary">🔗 分享</button>
            <button class="btn btn-archive" @click="archiveDiary">📦 归档</button>
            <button class="btn btn-danger" @click="deleteDiary">删除</button>
          </div>
        </div>
      </div>

      <!-- 分享弹窗 -->
      <div v-if="showShareModal" class="modal-overlay" @click.self="showShareModal = false">
        <div class="modal-content">
          <div class="modal-header">
            <h2>🔗 分享日记</h2>
            <button class="close-btn" @click="showShareModal = false">✕</button>
          </div>
          <div class="share-content">
            <div class="share-url-box">
              <input type="text" :value="shareUrl" readonly class="share-url-input">
              <button class="btn btn-primary" @click="copyShareUrl">复制链接</button>
            </div>
            <p class="share-tips">分享链接有效期：{{ shareExpireTime ? new Date(shareExpireTime).toLocaleString() : '7天' }}</p>
            <p class="share-tips">任何人通过此链接都可以查看这篇日记</p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import { api } from '../services/api'

const diaries = ref([])
const categories = ref([])
const searchKeyword = ref('')
const filterMood = ref('')
const showModal = ref(false)
const showDetail = ref(false)
const showCategoryModal = ref(false)
const editingDiary = ref(null)
const selectedDiary = ref(null)
const showShareModal = ref(false)
const shareUrl = ref('')
const shareExpireTime = ref('')
const formData = ref({
  title: '',
  content: '',
  mood: 'calm',
  categoryId: ''
})

const presetCategories = [
  { id: 'work', name: '工作', icon: '💼' },
  { id: 'life', name: '生活', icon: '🏠' },
  { id: 'study', name: '学习', icon: '📚' },
  { id: 'travel', name: '旅行', icon: '✈️' },
  { id: 'food', name: '美食', icon: '🍳' },
  { id: 'sport', name: '运动', icon: '⚽' },
  { id: 'emotion', name: '情感', icon: '❤️' },
  { id: 'other', name: '其他', icon: '📝' }
]

const categoryIcons = ['📝', '💼', '🎨', '📚', '🏖️', '🎯', '💡', '❤️', '🎵', '🍳']

const moods = [
  { value: 'happy', emoji: '😊' },
  { value: 'sad', emoji: '😢' },
  { value: 'angry', emoji: '😠' },
  { value: 'calm', emoji: '😌' },
  { value: 'excited', emoji: '🤩' }
]

const filteredDiaries = computed(() => {
  let result = diaries.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(d => 
      (d.title && d.title.toLowerCase().includes(keyword)) ||
      (d.content && d.content.toLowerCase().includes(keyword))
    )
  }
  if (filterMood.value) {
    result = result.filter(d => d.mood === filterMood.value)
  }
  return result
})

const getMoodEmoji = (mood) => {
  const emojis = { happy: '😊', sad: '😢', angry: '😠', calm: '😌', excited: '🤩' }
  return emojis[mood] || '😐'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

const handleSearch = () => {}

const filterByMood = () => {}

const loadDiaries = async () => {
  try {
    const response = await api.diary.list()
    const diaryList = response.data.data || []
    
    // 为每个日记匹配分类名称
    diaryList.forEach(diary => {
      if (diary.categoryId) {
        const category = categories.value.find(c => c.id === diary.categoryId)
        diary.categoryName = category ? `${category.icon} ${category.name}` : ''
      } else {
        diary.categoryName = ''
      }
    })
    
    diaries.value = diaryList
  } catch (error) {
    console.error('加载日记失败:', error)
  }
}

const loadCategories = async () => {
  try {
    const response = await api.category.list()
    categories.value = response.data.data || []
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const openCreateModal = () => {
  editingDiary.value = null
  formData.value = { title: '', content: '', mood: 'calm', categoryId: '' }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingDiary.value = null
}

const openDetail = (diary) => {
  selectedDiary.value = diary
  showDetail.value = true
}

const closeDetail = () => {
  showDetail.value = false
  selectedDiary.value = null
}

const editDiary = () => {
  editingDiary.value = selectedDiary.value
  formData.value = {
    title: selectedDiary.value.title || '',
    content: selectedDiary.value.content || '',
    mood: selectedDiary.value.mood || 'calm',
    categoryId: selectedDiary.value.categoryId || ''
  }
  showDetail.value = false
  showModal.value = true
}

const saveDiary = async () => {
  try {
    const data = {
      title: formData.value.title,
      content: formData.value.content,
      mood: formData.value.mood,
      categoryId: formData.value.categoryId ? Number(formData.value.categoryId) : null,
      diaryDate: new Date().toISOString().split('T')[0]
    }
    
    if (editingDiary.value) {
      await api.diary.update(editingDiary.value.id, data)
    } else {
      await api.diary.create(data)
    }
    
    closeModal()
    loadDiaries()
  } catch (error) {
    console.error('保存日记失败:', error)
    alert('保存失败，请重试')
  }
}

const deleteDiary = async () => {
  if (!confirm('确定要删除这篇日记吗？')) return
  
  try {
    await api.diary.delete(selectedDiary.value.id)
    closeDetail()
    loadDiaries()
  } catch (error) {
    console.error('删除日记失败:', error)
    alert('删除失败，请重试')
  }
}

const archiveDiary = async () => {
  if (!confirm('确定要将这篇日记归档吗？归档后可以在归档页面恢复。')) return
  
  try {
    await api.diary.archive.archive(selectedDiary.value.id)
    closeDetail()
    loadDiaries()
    alert('日记已归档')
  } catch (error) {
    console.error('归档日记失败:', error)
    alert('归档失败，请重试')
  }
}

const shareDiary = async () => {
  try {
    const response = await api.diary.share(selectedDiary.value.id, 7)
    if (response.data.code === 200 && response.data.data) {
      shareUrl.value = response.data.data.shareUrl
      shareExpireTime.value = response.data.data.expireTime
      showShareModal.value = true
    } else {
      console.error('分享日记失败:', response.data.message)
      alert(response.data.message || '分享失败，请重试')
    }
  } catch (error) {
    console.error('分享日记失败:', error)
    const msg = error.response?.data?.message || '分享失败，请重试'
    alert(msg)
  }
}

const copyShareUrl = async () => {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    alert('分享链接已复制到剪贴板')
  } catch (error) {
    const textarea = document.createElement('textarea')
    textarea.value = shareUrl.value
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    alert('分享链接已复制到剪贴板')
  }
}

onMounted(async () => {
  await loadCategories()
  await loadDiaries()
})
</script>

<style scoped>
.diary-page {
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

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.search-input {
  flex: 1;
  max-width: 300px;
  padding: 10px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
}

.filter-select {
  padding: 10px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: white;
}

.diary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.diary-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
}

.diary-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.diary-card .diary-mood {
  font-size: 24px;
  margin-bottom: 12px;
}

.diary-card h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.diary-card p {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.diary-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #999;
  font-size: 12px;
}

.category-tag {
  padding: 2px 8px;
  background: #e8f5e9;
  color: #2e7d32;
  border-radius: 4px;
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
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.detail-modal {
  max-width: 600px;
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

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
}

.form-group textarea {
  resize: vertical;
}

.mood-selector {
  display: flex;
  gap: 12px;
}

.mood-btn {
  width: 48px;
  height: 48px;
  border: 2px solid #e0e0e0;
  border-radius: 50%;
  font-size: 24px;
  cursor: pointer;
  transition: border-color 0.3s;
}

.mood-btn.active {
  border-color: #667eea;
  background: #f5f3ff;
}

.form-actions,
.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  color: #666;
}

.detail-mood {
  font-size: 28px;
}

.detail-content {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  white-space: pre-wrap;
}

.category-select-wrapper {
  display: flex;
  gap: 10px;
}

.category-select-wrapper select {
  flex: 1;
}

.btn-small {
  padding: 10px 14px;
  font-size: 13px;
}

.category-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.category-option {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #f5f5f7;
  border: 2px solid transparent;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.category-option:hover {
  background: #ebebef;
}

.category-option.active {
  background: #e8e8ff;
  border-color: #667eea;
  color: #667eea;
  font-weight: 500;
}

.category-icon {
  font-size: 16px;
}

.category-name {
  font-size: 14px;
}

.icon-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.icon-btn {
  width: 48px;
  height: 48px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 24px;
  cursor: pointer;
  transition: all 0.3s;
}

.icon-btn.active {
  border-color: #667eea;
  background: #f5f3ff;
}

.btn-share {
  background: #667eea;
  color: white;
}

.btn-archive {
  background: #805ad5;
  color: white;
}

.share-content {
  padding-top: 16px;
}

.share-url-box {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.share-url-input {
  flex: 1;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 13px;
  font-family: monospace;
  background: #fafafa;
}

.share-tips {
  color: #999;
  font-size: 13px;
  margin-bottom: 8px;
}

@media (max-width: 768px) {
  .main-content {
    padding-left: 84px;
  }
  .diary-grid {
    grid-template-columns: 1fr;
  }
}
</style>