<template>
  <div class="categories-page">
    <Sidebar :currentPage="'categories'" />
    <main class="main-content">
      <header class="page-header">
        <h1>📁 日记分类</h1>
        <button class="btn btn-primary" @click="openCreateModal">新建分类</button>
      </header>

      <div class="categories-grid">
        <div v-for="category in categories" :key="category.id" class="category-card">
          <div class="category-header">
            <span class="category-icon">{{ category.icon }}</span>
            <h3>{{ category.name }}</h3>
          </div>
          <p class="category-count">{{ getDiaryCount(category.id) }} 篇日记</p>
          <div class="category-actions">
            <button class="action-btn edit" @click="editCategory(category)">✏️</button>
            <button class="action-btn delete" @click="deleteCategory(category)">🗑️</button>
          </div>
        </div>
      </div>

      <div v-if="categories.length === 0" class="empty-state">
        <div class="empty-icon">📁</div>
        <p>暂无分类，创建一个新分类吧</p>
      </div>

      <!-- 创建/编辑分类弹窗 -->
      <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal-content">
          <div class="modal-header">
            <h2>{{ editingCategory ? '编辑分类' : '新建分类' }}</h2>
            <button class="close-btn" @click="closeModal">✕</button>
          </div>
          <form @submit.prevent="saveCategory">
            <div class="form-group">
              <label>分类名称 *</label>
              <input type="text" v-model="formData.name" placeholder="输入分类名称" required>
            </div>
            <div class="form-group">
              <label>分类图标</label>
              <div class="icon-selector">
                <button 
                  v-for="icon in icons" 
                  :key="icon"
                  type="button"
                  class="icon-btn"
                  :class="{ active: formData.icon === icon }"
                  @click="formData.icon = icon"
                >
                  {{ icon }}
                </button>
              </div>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeModal">取消</button>
              <button type="submit" class="btn btn-primary">保存</button>
            </div>
          </form>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import { api } from '../services/api'

const categories = ref([])
const diaries = ref([])
const showModal = ref(false)
const editingCategory = ref(null)
const formData = ref({
  name: '',
  icon: '📝'
})

const icons = ['📝', '💼', '🎨', '📚', '🏖️', '🎯', '💡', '❤️', '🎵', '🍳']

const getDiaryCount = (categoryId) => {
  return diaries.value.filter(d => d.categoryId === categoryId).length
}

const loadCategories = async () => {
  try {
    const response = await api.category.list()
    categories.value = response.data.data || []
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

const loadDiaries = async () => {
  try {
    const response = await api.diary.list()
    diaries.value = response.data.data || []
  } catch (error) {
    console.error('加载日记失败:', error)
  }
}

const openCreateModal = () => {
  editingCategory.value = null
  formData.value = { name: '', icon: '📝' }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingCategory.value = null
}

const editCategory = (category) => {
  editingCategory.value = category
  formData.value = {
    name: category.name,
    icon: category.icon
  }
  showModal.value = true
}

const saveCategory = async () => {
  if (!formData.value.name.trim()) {
    alert('请输入分类名称')
    return
  }

  try {
    const data = {
      name: formData.value.name,
      icon: formData.value.icon
    }

    if (editingCategory.value) {
      await api.category.update(editingCategory.value.id, data)
    } else {
      await api.category.create(data)
    }

    closeModal()
    loadCategories()
  } catch (error) {
    console.error('保存分类失败:', error)
    alert('保存失败，请重试')
  }
}

const deleteCategory = async (category) => {
  if (!confirm('确定要删除这个分类吗？该分类下的日记将不会被删除。')) return

  try {
    await api.category.delete(category.id)
    loadCategories()
  } catch (error) {
    console.error('删除分类失败:', error)
    alert('删除失败，请重试')
  }
}

onMounted(() => {
  loadCategories()
  loadDiaries()
})
</script>

<style scoped>
.categories-page {
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

.categories-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.category-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.category-icon {
  font-size: 28px;
}

.category-header h3 {
  font-size: 16px;
  color: #333;
}

.category-count {
  color: #999;
  font-size: 14px;
  margin-bottom: 16px;
}

.category-actions {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.action-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: #f5f5f5;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.3s;
}

.action-btn:hover {
  background: #e0e0e0;
}

.action-btn.delete:hover {
  background: #ffebee;
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
  max-width: 450px;
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

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .main-content {
    padding-left: 84px;
  }
  .categories-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>