<template>
  <div class="home">
    <header>
      <div class="header-left">
        <h1>拾码日记</h1>
      </div>
      <div class="header-right">
        <span class="user-info">
          <span class="role-badge" :class="user.role === 'admin' ? 'admin' : 'user'">
            {{ user.role === 'admin' ? '管理员' : '用户' }}
          </span>
          {{ user.nickname || user.username }}
        </span>
        <button class="btn btn-sm btn-secondary" @click="handleLogout">退出</button>
      </div>
    </header>

    <nav>
      <ul>
        <li :class="{ active: currentPage === 'home' }" @click="navigate('home')">
          <span>🏠</span> 首页
        </li>
        <li :class="{ active: currentPage === 'diary' }" @click="navigate('diary')">
          <span>📝</span> 日记
        </li>
        <li :class="{ active: currentPage === 'checkin' }" @click="navigate('checkin')">
          <span>✅</span> 打卡
        </li>
        <li :class="{ active: currentPage === 'task' }" @click="navigate('task')">
          <span>📋</span> 任务
        </li>
        <li :class="{ active: currentPage === 'friends' }" @click="navigate('friends')">
          <span>👥</span> 好友
        </li>
        <li :class="{ active: currentPage === 'categories' }" @click="navigate('categories')">
          <span>📁</span> 分类
        </li>
        <li :class="{ active: currentPage === 'stats' }" @click="navigate('stats')">
          <span>📊</span> 统计
        </li>
        <li v-if="user.role === 'admin'" :class="{ active: currentPage === 'admin' }" @click="navigate('admin')">
          <span>⚙️</span> 管理后台
        </li>
      </ul>
    </nav>

    <main>
      <!-- 快捷入口 -->
      <div class="quick-actions">
        <div class="action-card" @click="openDiaryModal">
          <div class="action-icon">📝</div>
          <div class="action-text">写日记</div>
        </div>
        <div class="action-card" @click="openCheckinModal">
          <div class="action-icon">✅</div>
          <div class="action-text">去打卡</div>
        </div>
        <div class="action-card" @click="openTaskModal">
          <div class="action-icon">📋</div>
          <div class="action-text">创任务</div>
        </div>
        <div class="action-card" @click="navigate('friends')">
          <div class="action-icon">👥</div>
          <div class="action-text">找好友</div>
        </div>
        <div class="action-card" @click="openCategoryModal">
          <div class="action-icon">📁</div>
          <div class="action-text">新分类</div>
        </div>
      </div>

      <!-- 今日数据 -->
      <div class="today-section">
        <h2>今日数据</h2>
        <div class="today-grid">
          <div class="today-item">
            <div class="today-value">{{ todayDiaryCount }}</div>
            <div class="today-label">今日日记</div>
          </div>
          <div class="today-item">
            <div class="today-value">{{ todayCheckinCount }}</div>
            <div class="today-label">今日打卡</div>
          </div>
          <div class="today-item">
            <div class="today-value">{{ todayTaskCount }}</div>
            <div class="today-label">待办任务</div>
          </div>
          <div class="today-item">
            <div class="today-value">{{ continuousDays }}</div>
            <div class="today-label">连续天数</div>
          </div>
        </div>
      </div>

      <!-- 最近日记列表 -->
      <div class="section">
        <div class="section-header">
          <h2>最近日记</h2>
          <button class="btn btn-sm btn-primary" @click="openDiaryModal">写一篇</button>
        </div>
        <!-- 日记搜索框 -->
        <div class="search-box">
          <input 
            type="text" 
            v-model="diarySearchKeyword" 
            placeholder="搜索日记标题、内容或标签..." 
            @input="searchDiaries"
            class="search-input"
          />
          <select v-model="diarySortBy" class="sort-select" @change="searchDiaries">
            <option value="date-desc">最新优先</option>
            <option value="date-asc">最早优先</option>
            <option value="title">按标题</option>
          </select>
          <button class="btn btn-sm btn-secondary" @click="clearDiarySearch" v-if="diarySearchKeyword">
            清除
          </button>
        </div>
        <div v-if="filteredDiaries.length === 0" class="empty-state">
          {{ diarySearchKeyword ? '未找到匹配的日记' : '暂无日记，点击上方按钮写第一篇吧！' }}
        </div>
        <div class="diary-list">
          <div v-for="diary in filteredDiaries" :key="diary.id" class="diary-card">
            <h3>{{ diary.title || '无标题' }}</h3>
            <p class="diary-content">{{ diary.content?.substring(0, 100) }}{{ diary.content?.length > 100 ? '...' : '' }}</p>
            <div class="diary-footer">
              <span>{{ diary.diaryDate }}</span>
              <div class="diary-actions">
                <button class="btn btn-xs btn-primary" @click="viewDiary(diary)">查看</button>
                <button class="btn btn-xs btn-secondary" @click="editDiary(diary)">编辑</button>
                <button class="btn btn-xs btn-danger" @click="deleteDiary(diary.id)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 待办任务 -->
      <div class="section">
        <div class="section-header">
          <h2>待办任务</h2>
          <button class="btn btn-sm btn-primary" @click="openTaskModal">添加任务</button>
        </div>
        <!-- 任务搜索框 -->
        <div class="search-box">
          <input 
            type="text" 
            v-model="taskSearchKeyword" 
            placeholder="搜索任务标题或描述..." 
            @input="searchTasks"
            class="search-input"
          />
          <select v-model="taskPriorityFilter" class="priority-filter" @change="searchTasks">
            <option value="">全部优先级</option>
            <option value="1">高优先级</option>
            <option value="2">中优先级</option>
            <option value="3">低优先级</option>
          </select>
          <select v-model="taskSortBy" class="sort-select" @change="searchTasks">
            <option value="priority">按优先级</option>
            <option value="dueDate">按截止日期</option>
            <option value="createTime">按创建时间</option>
          </select>
          <button class="btn btn-sm btn-secondary" @click="clearTaskSearch" v-if="taskSearchKeyword || taskPriorityFilter">
            清除
          </button>
        </div>
        <div v-if="filteredPendingTasks.length === 0" class="empty-state">
          {{ taskSearchKeyword || taskPriorityFilter ? '未找到匹配的任务' : '暂无待办任务' }}
        </div>
        <div class="task-list">
          <div v-for="task in filteredPendingTasks" :key="task.id" class="task-card">
            <div class="task-content">
              <input type="checkbox" :checked="task.status === 1" @change="toggleTask(task)">
              <span :class="{ completed: task.status === 1 }">{{ task.title }}</span>
              <span v-if="task.priority" class="priority-badge" :class="'priority-' + task.priority">
                {{ task.priority === 1 ? '高' : task.priority === 2 ? '中' : '低' }}
              </span>
            </div>
            <div class="task-meta">
              <span>{{ task.categoryName || '未分类' }}</span>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 日记弹窗 -->
    <div class="modal" :class="{ active: showDiaryModal }">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingDiary ? '编辑日记' : '写日记' }}</h3>
          <button class="modal-close" @click="closeDiaryModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>标题</label>
            <input type="text" v-model="diaryForm.title" placeholder="输入日记标题">
          </div>
          <div class="form-group">
            <label>日期</label>
            <input type="date" v-model="diaryForm.diaryDate">
          </div>
          <div class="form-group">
            <label>内容</label>
            <textarea v-model="diaryForm.content" rows="6" placeholder="写下今天的心情..."></textarea>
          </div>
          <div class="form-group">
            <label>分类</label>
            <select v-model="diaryForm.categoryId">
              <option value="">选择分类</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeDiaryModal">取消</button>
          <button class="btn btn-primary" @click="saveDiary">保存</button>
        </div>
      </div>
    </div>

    <!-- 打卡弹窗 -->
    <div class="modal" :class="{ active: showCheckinModal }">
      <div class="modal-content">
        <div class="modal-header">
          <h3>打卡</h3>
          <button class="modal-close" @click="closeCheckinModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>打卡类型</label>
            <select v-model="checkinForm.checkType">
              <option value="">选择类型</option>
              <option v-for="config in checkinConfigs" :key="config.id" :value="config.checkType">
                {{ config.checkName }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>打卡名称</label>
            <input type="text" v-model="checkinForm.checkName" placeholder="输入打卡名称">
          </div>
          <div class="form-group">
            <label>打卡日期</label>
            <input type="date" v-model="checkinForm.checkDate">
          </div>
          <div class="form-group">
            <label>描述</label>
            <textarea v-model="checkinForm.description" rows="3" placeholder="添加描述..."></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeCheckinModal">取消</button>
          <button class="btn btn-primary" @click="saveCheckin">打卡</button>
        </div>
      </div>
    </div>

    <!-- 任务弹窗 -->
    <div class="modal" :class="{ active: showTaskModal }">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingTask ? '编辑任务' : '创建任务' }}</h3>
          <button class="modal-close" @click="closeTaskModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>标题</label>
            <input type="text" v-model="taskForm.title" placeholder="输入任务标题" required>
          </div>
          <div class="form-group">
            <label>描述</label>
            <textarea v-model="taskForm.description" rows="3" placeholder="任务描述..."></textarea>
          </div>
          <div class="form-group">
            <label>分类</label>
            <select v-model="taskForm.categoryId">
              <option value="">选择分类</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>截止日期</label>
            <input type="date" v-model="taskForm.dueDate">
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeTaskModal">取消</button>
          <button class="btn btn-primary" @click="saveTask">保存</button>
        </div>
      </div>
    </div>

    <!-- 新建分类弹窗 -->
    <div class="modal" :class="{ active: showCategoryModal }">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingCategory ? '编辑分类' : '新建分类' }}</h3>
          <button class="modal-close" @click="closeCategoryModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>分类名称</label>
            <input type="text" v-model="categoryForm.name" placeholder="输入分类名称" required>
          </div>
          <div class="form-group">
            <label>分类颜色</label>
            <div class="color-picker">
              <input type="color" v-model="categoryForm.color" class="color-input">
              <div class="color-preview" :style="{ backgroundColor: categoryForm.color }"></div>
            </div>
          </div>
          <div class="form-group">
            <label>分类描述</label>
            <textarea v-model="categoryForm.description" rows="3" placeholder="分类描述（可选）"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeCategoryModal">取消</button>
          <button class="btn btn-primary" @click="saveCategory">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../services/api'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const currentPage = ref('home')
const diaries = ref([])
const tasks = ref([])
const categories = ref([])
const checkinConfigs = ref([])

// 搜索相关变量
const diarySearchKeyword = ref('')
const taskSearchKeyword = ref('')
const taskPriorityFilter = ref('')

// 排序相关变量
const diarySortBy = ref('date-desc') // date-desc, date-asc, title
const taskSortBy = ref('priority') // priority, dueDate, createTime

const showDiaryModal = ref(false)
const showCheckinModal = ref(false)
const showTaskModal = ref(false)
const showCategoryModal = ref(false)
const editingDiary = ref(null)
const editingTask = ref(null)
const editingCategory = ref(null)

const diaryForm = ref({
  title: '',
  content: '',
  diaryDate: new Date().toISOString().split('T')[0],
  categoryId: ''
})

const checkinForm = ref({
  checkType: '',
  checkName: '',
  checkDate: new Date().toISOString().split('T')[0],
  description: ''
})

const taskForm = ref({
  title: '',
  description: '',
  categoryId: '',
  dueDate: ''
})

const categoryForm = ref({
  name: '',
  color: '#667eea',
  description: ''
})

const today = new Date().toISOString().split('T')[0]

const todayDiaryCount = computed(() => 
  diaries.value.filter(d => d.diaryDate === today).length
)

const todayCheckinCount = computed(() => 0)
const todayTaskCount = computed(() => 
  tasks.value.filter(t => t.status === 0).length
)
const continuousDays = ref(0)

const pendingTasks = computed(() => 
  tasks.value.filter(t => t.status === 0).slice(0, 5)
)

// 日记搜索过滤（支持多种排序方式）
const filteredDiaries = computed(() => {
  let result = diaries.value
  if (diarySearchKeyword.value) {
    const keyword = diarySearchKeyword.value.toLowerCase()
    result = result.filter(d => 
      (d.title && d.title.toLowerCase().includes(keyword)) ||
      (d.content && d.content.toLowerCase().includes(keyword)) ||
      (d.tags && d.tags.toLowerCase().includes(keyword))
    )
  }
  // 根据选择的排序方式排序
  return result.sort((a, b) => {
    switch (diarySortBy.value) {
      case 'date-desc':
        return new Date(b.diaryDate) - new Date(a.diaryDate)
      case 'date-asc':
        return new Date(a.diaryDate) - new Date(b.diaryDate)
      case 'title':
        return (a.title || '').localeCompare(b.title || '')
      default:
        return new Date(b.diaryDate) - new Date(a.diaryDate)
    }
  })
})

// 任务搜索过滤（支持多种排序方式）
const filteredPendingTasks = computed(() => {
  let result = tasks.value.filter(t => t.status === 0)
  
  // 关键词搜索
  if (taskSearchKeyword.value) {
    const keyword = taskSearchKeyword.value.toLowerCase()
    result = result.filter(t => 
      (t.title && t.title.toLowerCase().includes(keyword)) ||
      (t.description && t.description.toLowerCase().includes(keyword))
    )
  }
  
  // 优先级筛选
  if (taskPriorityFilter.value) {
    result = result.filter(t => t.priority === parseInt(taskPriorityFilter.value))
  }
  
  // 根据选择的排序方式排序
  return result.sort((a, b) => {
    switch (taskSortBy.value) {
      case 'priority': {
        const priorityA = a.priority || 3
        const priorityB = b.priority || 3
        if (priorityA !== priorityB) {
          return priorityA - priorityB
        }
        // 优先级相同则按截止日期排序
        if (a.dueDate && b.dueDate) {
          return new Date(a.dueDate) - new Date(b.dueDate)
        }
        return 0
      }
      case 'dueDate': {
        // 按截止日期排序（越近越靠前，无截止日期的排最后）
        if (!a.dueDate && !b.dueDate) return 0
        if (!a.dueDate) return 1
        if (!b.dueDate) return -1
        return new Date(a.dueDate) - new Date(b.dueDate)
      }
      case 'createTime': {
        // 按创建时间排序（最新的在前）
        return new Date(b.createTime || 0) - new Date(a.createTime || 0)
      }
      default: {
        const priorityA = a.priority || 3
        const priorityB = b.priority || 3
        if (priorityA !== priorityB) {
          return priorityA - priorityB
        }
        if (a.dueDate && b.dueDate) {
          return new Date(a.dueDate) - new Date(b.dueDate)
        }
        return 0
      }
    }
  }).slice(0, 10)
})

// 搜索日记函数
function searchDiaries() {
  // computed会自动响应diarySearchKeyword的变化
}

// 清除日记搜索
function clearDiarySearch() {
  diarySearchKeyword.value = ''
}

// 搜索任务函数
function searchTasks() {
  // computed会自动响应taskSearchKeyword和taskPriorityFilter的变化
}

// 清除任务搜索
function clearTaskSearch() {
  taskSearchKeyword.value = ''
  taskPriorityFilter.value = ''
}

async function loadData() {
  try {
    const [diaryRes, taskRes, categoryRes, checkinConfigRes] = await Promise.all([
      api.diary.list(),
      api.task.list(),
      api.category.list(),
      api.checkin.config.list()
    ])
    
    diaries.value = diaryRes.data.data || []
    tasks.value = taskRes.data.data || []
    categories.value = categoryRes.data.data || []
    checkinConfigs.value = checkinConfigRes.data.data || []
    
    calculateContinuousDays()
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

function calculateContinuousDays() {
  const checkinDates = diaries.value.map(d => d.diaryDate).filter(Boolean)
  const uniqueDates = [...new Set(checkinDates)].sort((a, b) => new Date(b) - new Date(a))
  
  let count = 0
  const today = new Date()
  
  for (let i = 0; i < 365; i++) {
    const checkDate = new Date(today)
    checkDate.setDate(checkDate.getDate() - i)
    const dateStr = checkDate.toISOString().split('T')[0]
    
    if (uniqueDates.includes(dateStr)) {
      count++
    } else if (i > 0) {
      break
    }
  }
  
  continuousDays.value = count
}

function navigate(page) {
  currentPage.value = page
  switch(page) {
    case 'home':
      router.push('/home')
      break
    case 'friends':
      router.push('/friends')
      break
    case 'categories':
      router.push('/categories')
      break
    case 'stats':
      router.push('/stats')
      break
    case 'admin':
      router.push('/admin')
      break
  }
}

async function handleLogout() {
  localStorage.removeItem('user')
  router.push('/')
}

function openDiaryModal(diary = null) {
  if (diary) {
    editingDiary.value = diary
    diaryForm.value = {
      title: diary.title || '',
      content: diary.content || '',
      diaryDate: diary.diaryDate || new Date().toISOString().split('T')[0],
      categoryId: diary.categoryId || ''
    }
  } else {
    editingDiary.value = null
    diaryForm.value = {
      title: '',
      content: '',
      diaryDate: new Date().toISOString().split('T')[0],
      categoryId: ''
    }
  }
  showDiaryModal.value = true
}

function closeDiaryModal() {
  showDiaryModal.value = false
  editingDiary.value = null
}

async function saveDiary() {
  try {
    if (editingDiary.value) {
      await api.diary.update(editingDiary.value.id, diaryForm.value)
    } else {
      await api.diary.create(diaryForm.value)
    }
    closeDiaryModal()
    loadData()
  } catch (error) {
    console.error('保存日记失败:', error)
    alert('保存失败')
  }
}

function viewDiary(diary) {
  alert(`日记内容:\n\n${diary.content}`)
}

function editDiary(diary) {
  openDiaryModal(diary)
}

async function deleteDiary(id) {
  if (confirm('确定删除这篇日记吗？')) {
    try {
      await api.diary.delete(id)
      loadData()
    } catch (error) {
      console.error('删除日记失败:', error)
      alert('删除失败')
    }
  }
}

function openCheckinModal() {
  checkinForm.value = {
    checkType: '',
    checkName: '',
    checkDate: new Date().toISOString().split('T')[0],
    description: ''
  }
  showCheckinModal.value = true
}

function closeCheckinModal() {
  showCheckinModal.value = false
}

async function saveCheckin() {
  try {
    await api.checkin.create(checkinForm.value)
    closeCheckinModal()
    loadData()
  } catch (error) {
    console.error('打卡失败:', error)
    alert('打卡失败')
  }
}

function openTaskModal(task = null) {
  if (task) {
    editingTask.value = task
    taskForm.value = {
      title: task.title || '',
      description: task.description || '',
      categoryId: task.categoryId || '',
      dueDate: task.dueDate || ''
    }
  } else {
    editingTask.value = null
    taskForm.value = {
      title: '',
      description: '',
      categoryId: '',
      dueDate: ''
    }
  }
  showTaskModal.value = true
}

function closeTaskModal() {
  showTaskModal.value = false
  editingTask.value = null
}

async function saveTask() {
  try {
    if (editingTask.value) {
      await api.task.update(editingTask.value.id, taskForm.value)
    } else {
      await api.task.create(taskForm.value)
    }
    closeTaskModal()
    loadData()
  } catch (error) {
    console.error('保存任务失败:', error)
    alert('保存失败')
  }
}

async function toggleTask(task) {
  try {
    if (task.status === 1) {
      await api.task.update(task.id, { status: 0 })
    } else {
      await api.task.complete(task.id)
    }
    loadData()
  } catch (error) {
    console.error('更新任务失败:', error)
  }
}

function openCategoryModal(category = null) {
  if (category) {
    editingCategory.value = category
    categoryForm.value = {
      name: category.name || '',
      color: category.color || '#667eea',
      description: category.description || ''
    }
  } else {
    editingCategory.value = null
    categoryForm.value = {
      name: '',
      color: '#667eea',
      description: ''
    }
  }
  showCategoryModal.value = true
}

function closeCategoryModal() {
  showCategoryModal.value = false
  editingCategory.value = null
}

async function saveCategory() {
  try {
    if (editingCategory.value) {
      await api.category.update(editingCategory.value.id, categoryForm.value)
    } else {
      await api.category.create(categoryForm.value)
    }
    closeCategoryModal()
    loadData()
  } catch (error) {
    console.error('保存分类失败:', error)
    alert('保存失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.home {
  display: flex;
  min-height: 100vh;
  background: #f5f7fa;
}

header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  z-index: 100;
}

.header-left h1 {
  font-size: 20px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.role-badge.admin {
  background: #ffb74d;
  color: #e65100;
}

.role-badge.user {
  background: #81c784;
  color: #1b5e20;
}

nav {
  width: 200px;
  background: white;
  padding-top: 80px;
  padding-bottom: 20px;
  box-shadow: 2px 0 10px rgba(0,0,0,0.05);
}

nav ul {
  list-style: none;
  padding: 0;
}

nav li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  cursor: pointer;
  color: #666;
  transition: background 0.2s;
}

nav li:hover {
  background: #f5f7fa;
}

nav li.active {
  background: #e8eaf6;
  color: #667eea;
  font-weight: 500;
}

main {
  flex: 1;
  padding: 80px 20px 20px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.action-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.1);
}

.action-icon {
  font-size: 32px;
}

.action-text {
  font-size: 14px;
  color: #666;
}

.today-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 30px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.today-section h2 {
  font-size: 18px;
  margin-bottom: 20px;
  color: #333;
}

.today-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.today-item {
  text-align: center;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.today-value {
  font-size: 28px;
  font-weight: 700;
  color: #667eea;
  margin-bottom: 4px;
}

.today-label {
  font-size: 13px;
  color: #999;
}

.section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 30px;
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

/* 搜索框样式 */
.search-box {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.search-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.priority-filter {
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  cursor: pointer;
}

.priority-filter:focus {
  outline: none;
  border-color: #667eea;
}

/* 排序选择器样式 */
.sort-select {
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  min-width: 120px;
}

.sort-select:focus {
  outline: none;
  border-color: #667eea;
}

/* 优先级徽章样式 */
.priority-badge {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  margin-left: 8px;
}

.priority-badge.priority-1 {
  background: #ffcdd2;
  color: #c62828;
}

.priority-badge.priority-2 {
  background: #ffe0b2;
  color: #ef6c00;
}

.priority-badge.priority-3 {
  background: #c8e6c9;
  color: #2e7d32;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

.diary-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.diary-card {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.diary-card h3 {
  font-size: 16px;
  margin-bottom: 8px;
  color: #333;
}

.diary-content {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 12px;
}

.diary-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.diary-actions {
  display: flex;
  gap: 8px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.task-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-content span.completed {
  text-decoration: line-through;
  color: #999;
}

.task-meta span {
  font-size: 12px;
  color: #999;
  padding: 4px 8px;
  background: #e9ecef;
  border-radius: 4px;
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

.btn-xs {
  padding: 4px 8px;
  font-size: 12px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-secondary {
  background: #e9ecef;
  color: #666;
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
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
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
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  border-color: #667eea;
}

.color-picker {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-input {
  width: 50px;
  height: 40px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.color-preview {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  border: 2px solid #e0e0e0;
}
</style>
