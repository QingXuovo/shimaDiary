<template>
  <div class="task-page">
    <Sidebar :currentPage="'task'" />
    <main class="main-content">
      <header class="page-header">
        <h1>✅ 我的任务</h1>
        <button class="btn btn-primary" @click="openCreateModal">新建任务</button>
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

      <div class="task-list">
        <div 
          v-for="task in filteredTasks" 
          :key="task.id" 
          class="task-card"
          :class="{ completed: task.status === 1 }"
        >
          <div class="task-header">
            <input 
              type="checkbox" 
              :checked="task.status === 1" 
              @change="toggleTask(task)"
            >
            <div class="task-info">
              <h3>{{ task.title }}</h3>
              <div class="task-meta">
                <span class="priority" :class="getPriorityClass(task.priority)">
                  {{ getPriorityText(task.priority) }}
                </span>
                <span v-if="task.dueDate">截止: {{ formatDate(task.dueDate) }}</span>
                <span v-if="task.progress > 0 && task.progress < 100" class="progress-text">
                  进度: {{ task.progress }}%
                </span>
              </div>
            </div>
          </div>
          <div v-if="task.progress > 0" class="progress-bar-wrapper">
            <div class="progress-bar">
              <div 
                class="progress-fill" 
                :style="{ width: task.progress + '%' }"
                :class="getProgressClass(task.progress)"
              ></div>
            </div>
          </div>
          <p v-if="task.description" class="task-description">{{ task.description }}</p>
          
          <div v-if="subTasks[task.id]?.length > 0" class="sub-tasks">
            <div 
              v-for="sub in subTasks[task.id]" 
              :key="sub.id" 
              class="sub-task"
              :class="{ completed: sub.status === 1 }"
            >
              <input 
                type="checkbox" 
                :checked="sub.status === 1" 
                @change="toggleSubTask(sub)"
              >
              <span>{{ sub.title }}</span>
            </div>
          </div>
          
          <div class="task-actions">
            <button class="action-btn" @click="toggleSubTaskExpand(task)">
              {{ expandedSubTasks.includes(task.id) ? '▼' : '▲' }}
            </button>
            <button class="action-btn edit" @click="editTask(task)">✏️</button>
            <button class="action-btn delete" @click="deleteTask(task)">🗑️</button>
          </div>
        </div>
      </div>

      <div v-if="filteredTasks.length === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <p>暂无{{ getCurrentTabLabel() }}任务</p>
      </div>

      <!-- 创建/编辑任务弹窗 -->
      <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal-content">
          <div class="modal-header">
            <h2>{{ editingTask ? '编辑任务' : '新建任务' }}</h2>
            <button class="close-btn" @click="closeModal">✕</button>
          </div>
          <form @submit.prevent="saveTask">
            <div class="form-group">
              <label>任务标题 *</label>
              <input type="text" v-model="formData.title" placeholder="输入任务标题" required>
            </div>
            <div class="form-group">
              <label>任务描述</label>
              <textarea v-model="formData.description" placeholder="输入任务描述（可选）" rows="3"></textarea>
            </div>
            <div class="form-group">
              <label>优先级</label>
              <div class="priority-selector">
                <button 
                  v-for="p in priorities" 
                  :key="p.value"
                  type="button"
                  class="priority-btn"
                  :class="[p.class, { active: formData.priority === p.value }]"
                  @click="formData.priority = p.value"
                >
                  {{ p.label }}
                </button>
              </div>
            </div>
            <div class="form-group">
              <label>截止日期</label>
              <input type="date" v-model="formData.dueDate">
            </div>
            <div class="form-group">
              <label>进度: {{ formData.progress }}%</label>
              <input type="range" v-model="formData.progress" min="0" max="100" class="progress-input">
            </div>
            <div class="form-group">
              <label>设置为子任务</label>
              <select v-model="formData.parentId" class="parent-select">
                <option :value="null">不设置为子任务（创建为主任务）</option>
                <option v-for="task in availableParentTasks" :key="task.id" :value="task.id">
                  {{ task.title }}
                </option>
              </select>
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
import { ref, computed, onMounted } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import { api } from '../services/api'

const tasks = ref([])
const activeTab = ref('all')
const showModal = ref(false)
const editingTask = ref(null)
const subTasks = ref({})
const expandedSubTasks = ref([])
const formData = ref({
  title: '',
  description: '',
  priority: 2,
  dueDate: '',
  progress: 0,
  parentId: null
})

const tabs = [
  { value: 'all', label: '全部' },
  { value: 'pending', label: '待完成' },
  { value: 'completed', label: '已完成' }
]

const priorities = [
  { value: 1, label: '高', class: 'high' },
  { value: 2, label: '中', class: 'medium' },
  { value: 3, label: '低', class: 'low' }
]

const filteredTasks = computed(() => {
  if (activeTab.value === 'pending') {
    return tasks.value.filter(t => t.status === 0)
  } else if (activeTab.value === 'completed') {
    return tasks.value.filter(t => t.status === 1)
  }
  return tasks.value
})

const availableParentTasks = computed(() => {
  return tasks.value.filter(t => t.parentId === null)
})

const getTabCount = (tab) => {
  if (tab === 'pending') return tasks.value.filter(t => t.status === 0).length
  if (tab === 'completed') return tasks.value.filter(t => t.status === 1).length
  return tasks.value.length
}

const getCurrentTabLabel = () => {
  const tab = tabs.find(t => t.value === activeTab.value)
  return tab?.label || ''
}

const getPriorityClass = (priority) => {
  const classes = { 1: 'high', 2: 'medium', 3: 'low' }
  return classes[priority] || 'low'
}

const getPriorityText = (priority) => {
  const texts = { 1: '高优先级', 2: '中优先级', 3: '低优先级' }
  return texts[priority] || '低优先级'
}

const getProgressClass = (progress) => {
  if (progress >= 100) return 'complete'
  if (progress >= 50) return 'medium'
  return 'low'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const loadTasks = async () => {
  try {
    const response = await api.task.list()
    tasks.value = response.data.data || []
    await loadAllSubTasks()
  } catch (error) {
    console.error('加载任务失败:', error)
  }
}

const loadAllSubTasks = async () => {
  for (const task of tasks.value) {
    await loadSubTasks(task.id)
  }
}

const loadSubTasks = async (parentId) => {
  try {
    const response = await api.task.getSubTasks(parentId)
    subTasks.value[parentId] = response.data.data || []
  } catch (error) {
    console.error('加载子任务失败:', error)
    subTasks.value[parentId] = []
  }
}

const toggleSubTaskExpand = (task) => {
  const index = expandedSubTasks.value.indexOf(task.id)
  if (index > -1) {
    expandedSubTasks.value.splice(index, 1)
  } else {
    expandedSubTasks.value.push(task.id)
  }
}

const toggleSubTask = async (subTask) => {
  try {
    await api.task.updateStatus(subTask.id, subTask.status === 1 ? 0 : 1)
    subTask.status = subTask.status === 1 ? 0 : 1
    await updateParentProgress(subTask.parentId)
  } catch (error) {
    console.error('更新子任务状态失败:', error)
  }
}

const updateParentProgress = async (parentId) => {
  try {
    const response = await api.task.getSubTasks(parentId)
    const subs = response.data.data || []
    if (subs.length > 0) {
      const completed = subs.filter(s => s.status === 1).length
      const progress = Math.round((completed / subs.length) * 100)
      await api.task.updateProgress(parentId, progress)
      const parent = tasks.value.find(t => t.id === parentId)
      if (parent) {
        parent.progress = progress
      }
    }
  } catch (error) {
    console.error('更新父任务进度失败:', error)
  }
}

const toggleTask = async (task) => {
  try {
    await api.task.updateStatus(task.id, task.status === 1 ? 0 : 1)
    task.status = task.status === 1 ? 0 : 1
  } catch (error) {
    console.error('更新任务状态失败:', error)
  }
}

const openCreateModal = () => {
  editingTask.value = null
  formData.value = { title: '', description: '', priority: 2, dueDate: '' }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  editingTask.value = null
}

const editTask = (task) => {
  editingTask.value = task
  formData.value = {
    title: task.title,
    description: task.description || '',
    priority: task.priority,
    dueDate: task.dueDate || '',
    progress: task.progress || 0,
    parentId: null
  }
  showModal.value = true
}

const saveTask = async () => {
  if (!formData.value.title.trim()) {
    alert('请输入任务标题')
    return
  }

  try {
    const data = {
      title: formData.value.title,
      description: formData.value.description,
      priority: formData.value.priority,
      dueDate: formData.value.dueDate || null,
      progress: formData.value.progress || 0,
      parentId: formData.value.parentId || null
    }

    if (editingTask.value) {
      await api.task.update(editingTask.value.id, data)
    } else {
      await api.task.create(data)
    }

    closeModal()
    loadTasks()
  } catch (error) {
    console.error('保存任务失败:', error)
    alert('保存失败，请重试')
  }
}

const deleteTask = async (task) => {
  if (!confirm('确定要删除这个任务吗？')) return

  try {
    await api.task.delete(task.id)
    loadTasks()
  } catch (error) {
    console.error('删除任务失败:', error)
    alert('删除失败，请重试')
  }
}

onMounted(() => {
  loadTasks()
})
</script>

<style scoped>
.task-page {
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

.task-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.task-card.completed {
  opacity: 0.6;
}

.task-card.completed h3 {
  text-decoration: line-through;
}

.task-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.task-header input[type="checkbox"] {
  width: 20px;
  height: 20px;
  cursor: pointer;
  margin-top: 2px;
}

.task-info h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 8px;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #999;
  font-size: 13px;
}

.priority {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.priority.high {
  background: #ffebee;
  color: #c62828;
}

.priority.medium {
  background: #fff3e0;
  color: #ef6c00;
}

.priority.low {
  background: #e8f5e9;
  color: #2e7d32;
}

.task-description {
  color: #666;
  font-size: 14px;
  margin: 12px 0;
  padding-left: 36px;
}

.task-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-left: 36px;
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

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
}

.form-group textarea {
  resize: vertical;
}

.priority-selector {
  display: flex;
  gap: 12px;
}

.priority-btn {
  flex: 1;
  padding: 10px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.priority-btn.high {
  border-color: #ef5350;
  color: #c62828;
}

.priority-btn.medium {
  border-color: #ff9800;
  color: #ef6c00;
}

.priority-btn.low {
  border-color: #4caf50;
  color: #2e7d32;
}

.priority-btn.active {
  background: currentColor;
  color: white;
}

.parent-select {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  cursor: pointer;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.progress-text {
  color: #667eea;
}

.progress-bar-wrapper {
  padding-left: 36px;
  margin: 8px 0;
}

.progress-bar {
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-fill.low {
  background: #ff9800;
}

.progress-fill.medium {
  background: #667eea;
}

.progress-fill.complete {
  background: #4caf50;
}

.progress-input {
  width: 100%;
  height: 6px;
  -webkit-appearance: none;
  appearance: none;
  background: #f0f0f0;
  border-radius: 3px;
  cursor: pointer;
}

.progress-input::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  background: #667eea;
  border-radius: 50%;
  cursor: pointer;
}

.sub-tasks {
  padding-left: 52px;
  margin-top: 12px;
  border-left: 2px solid #e0e0e0;
}

.sub-task {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-bottom: 4px;
  background: #fafafa;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
}

.sub-task.completed {
  opacity: 0.6;
}

.sub-task.completed span {
  text-decoration: line-through;
}

.sub-task input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

@media (max-width: 768px) {
  .main-content {
    padding-left: 84px;
  }
}
</style>