<template>
  <div class="dashboard">
    <Sidebar :currentPage="'dashboard'" />
    <main class="main-content">
      <header class="header">
        <div class="header-left">
          <h1>欢迎回来，{{ user?.nickname || user?.username }}</h1>
          <p class="date-text">{{ greeting }} · {{ currentDate }}</p>
        </div>
        <div class="header-right">
          <div class="weather-card">
            <span class="weather-icon">☀️</span>
            <span class="weather-text">天气晴朗</span>
          </div>
        </div>
      </header>
      
      <div class="stats-grid">
        <div class="stat-card stat-card-diary">
          <div class="stat-icon-wrapper">
            <span class="stat-icon">📝</span>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ diaryCount }}</div>
            <div class="stat-label">日记总数</div>
          </div>
          <div class="stat-trend">
            <span class="trend-icon">📈</span>
          </div>
        </div>
        <div class="stat-card stat-card-task">
          <div class="stat-icon-wrapper">
            <span class="stat-icon">✅</span>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ taskStats?.completed || 0 }}</div>
            <div class="stat-label">已完成任务</div>
          </div>
          <div class="stat-trend">
            <span class="trend-icon">🎯</span>
          </div>
        </div>
        <div class="stat-card stat-card-checkin">
          <div class="stat-icon-wrapper">
            <span class="stat-icon">📅</span>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ checkinCount }}</div>
            <div class="stat-label">打卡天数</div>
          </div>
          <div class="stat-trend">
            <span class="trend-icon">🔥</span>
          </div>
        </div>
        <div class="stat-card stat-card-friend">
          <div class="stat-icon-wrapper">
            <span class="stat-icon">👥</span>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ friendCount }}</div>
            <div class="stat-label">好友数量</div>
          </div>
          <div class="stat-trend">
            <span class="trend-icon">💬</span>
          </div>
        </div>
      </div>

      <div class="section-row">
        <div class="section section-tasks">
          <div class="section-header">
            <h2>📋 今日任务</h2>
            <span class="task-progress">{{ completedToday }}/{{ todayTasks.length }}</span>
          </div>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
          </div>
          <div v-if="todayTasks.length === 0" class="empty-state">
            <div class="empty-icon">🎉</div>
            <p>今日暂无任务，享受轻松时光吧！</p>
          </div>
          <div v-else class="task-list">
            <div 
              v-for="task in todayTasks" 
              :key="task.id" 
              class="task-item"
              :class="{ completed: task.status === 1 }"
              @click="toggleTask(task)"
            >
              <div class="checkbox-wrapper">
                <input 
                  type="checkbox" 
                  :checked="task.status === 1" 
                  @change.stop="toggleTask(task)"
                >
                <div class="checkmark"></div>
              </div>
              <div class="task-content">
                <span class="task-title">{{ task.title }}</span>
                <span class="task-desc">{{ task.description?.slice(0, 30) }}...</span>
              </div>
              <span class="priority" :class="getPriorityClass(task.priority)">
                {{ getPriorityText(task.priority) }}
              </span>
            </div>
          </div>
          <router-link to="/task" class="view-all">查看全部任务 →</router-link>
        </div>

        <div class="section section-diaries">
          <div class="section-header">
            <h2>📔 最近日记</h2>
            <router-link to="/diary" class="add-diary">+ 写日记</router-link>
          </div>
          <div v-if="recentDiaries.length === 0" class="empty-state">
            <div class="empty-icon">📝</div>
            <p>还没有日记，记录下今天的心情吧！</p>
          </div>
          <div v-else class="diary-list">
            <div v-for="diary in recentDiaries" :key="diary.id" class="diary-item" @click="goToDiary(diary.id)">
              <div class="diary-mood-icon">{{ getMoodEmoji(diary.mood) }}</div>
              <div class="diary-content">
                <div class="diary-header">
                  <span class="diary-title">{{ diary.title || '无标题' }}</span>
                </div>
                <p class="diary-text">{{ diary.content?.slice(0, 60) }}...</p>
                <div class="diary-footer">
                  <span class="diary-date">{{ formatDate(diary.diaryDate) }}</span>
                  <span class="diary-category">{{ getCategoryName(diary.categoryId) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="bottom-section">
        <div class="section section-checkin">
          <div class="section-header">
            <h2>🏃 连续打卡</h2>
            <span class="streak-count">{{ checkinStreak }} 天</span>
          </div>
          <div class="checkin-calendar">
            <div v-for="(day, index) in last7Days" :key="index" class="calendar-day">
              <span class="day-name">{{ day.name }}</span>
              <div class="day-circle" :class="{ checked: day.checked, today: day.isToday }">
                <span v-if="day.checked">✓</span>
              </div>
            </div>
          </div>
          <button v-if="!todayChecked" class="checkin-btn" @click="doCheckin">
            <span class="btn-icon">✨</span>
            <span>今日打卡</span>
          </button>
          <div v-else class="checked-status">
            <span class="checked-icon">✓</span>
            <span>今日已打卡</span>
          </div>
        </div>
      </div>

      <div class="charts-section">
        <div class="section section-chart">
          <div class="section-header">
            <h2>📊 心情分布</h2>
          </div>
          <div ref="moodChartRef" class="chart-container"></div>
        </div>
        <div class="section section-chart">
          <div class="section-header">
            <h2>📈 日记趋势（近30天）</h2>
          </div>
          <div ref="trendChartRef" class="chart-container"></div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import Sidebar from '../components/Sidebar.vue'
import { api } from '../services/api'

const router = useRouter()

const user = ref(null)
const diaryCount = ref(0)
const taskStats = ref({})
const checkinCount = ref(0)
const friendCount = ref(0)
const todayTasks = ref([])
const recentDiaries = ref([])
const checkinStreak = ref(0)
const todayChecked = ref(false)
const last7Days = ref([])

const moodChartRef = ref(null)
const trendChartRef = ref(null)
let moodChart = null
let trendChart = null

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const currentDate = computed(() => {
  const now = new Date()
  const options = { month: 'long', day: 'numeric', weekday: 'long' }
  return now.toLocaleDateString('zh-CN', options)
})

const completedToday = computed(() => {
  return todayTasks.value.filter(t => t.status === 1).length
})

const progressPercent = computed(() => {
  if (todayTasks.value.length === 0) return 0
  return Math.round((completedToday.value / todayTasks.value.length) * 100)
})

const getPriorityClass = (priority) => {
  const classes = { 1: 'high', 2: 'medium', 3: 'low' }
  return classes[priority] || 'low'
}

const getPriorityText = (priority) => {
  const texts = { 1: '高', 2: '中', 3: '低' }
  return texts[priority] || '低'
}

const getMoodEmoji = (mood) => {
  const emojis = { happy: '😊', sad: '😢', angry: '😠', calm: '😌', excited: '🤩', neutral: '😐' }
  return emojis[mood] || '😐'
}

const getCategoryName = (categoryId) => {
  const categories = { 1: '生活', 2: '工作', 3: '学习', 4: '运动', 5: '其他' }
  return categories[categoryId] || ''
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const toggleTask = async (task) => {
  try {
    await api.task.updateStatus(task.id, task.status === 1 ? 0 : 1)
    task.status = task.status === 1 ? 0 : 1
    loadTaskStats()
  } catch (error) {
    console.error('更新任务状态失败:', error)
  }
}

const goToDiary = (id) => {
  router.push(`/diary/${id}`)
}

const doCheckin = async () => {
  try {
    await api.checkin.create()
    todayChecked.value = true
    loadCheckinData()
  } catch (error) {
    console.error('打卡失败:', error)
  }
}

const loadStats = async () => {
  try {
    const [diaryRes, taskRes, checkinRes, friendRes] = await Promise.all([
      api.diary.count(),
      api.task.stats(),
      api.checkin.count(),
      api.friend.list()
    ])
    diaryCount.value = diaryRes.data.data || 0
    taskStats.value = taskRes.data.data || {}
    checkinCount.value = checkinRes.data.data || 0
    friendCount.value = friendRes.data.data?.length || 0
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const loadTodayTasks = async () => {
  try {
    const response = await api.task.today()
    todayTasks.value = response.data.data || []
  } catch (error) {
    console.error('加载今日任务失败:', error)
  }
}

const loadRecentDiaries = async () => {
  try {
    const response = await api.diary.list()
    recentDiaries.value = response.data.data?.slice(0, 5) || []
  } catch (error) {
    console.error('加载日记失败:', error)
  }
}

const loadTaskStats = async () => {
  try {
    const response = await api.task.stats()
    taskStats.value = response.data.data || {}
  } catch (error) {
    console.error('加载任务统计失败:', error)
  }
}

const loadCheckinData = async () => {
  try {
    const statsRes = await api.checkin.stats()
    checkinStreak.value = statsRes.data.data?.continuousDays || 0
    
    const today = new Date()
    const days = []
    const dayNames = ['日', '一', '二', '三', '四', '五', '六']
    
    for (let i = 6; i >= 0; i--) {
      const date = new Date(today)
      date.setDate(date.getDate() - i)
      const dateStr = date.toISOString().split('T')[0]
      days.push({
        name: dayNames[date.getDay()],
        date: dateStr,
        checked: false,
        isToday: i === 0
      })
    }
    
    const historyRes = await api.checkin.recent(7)
    const history = historyRes.data.data || []
    const checkinDates = new Set(history.map(c => c.checkDate))
    
    days.forEach(day => {
      day.checked = checkinDates.has(day.date)
    })
    
    todayChecked.value = checkinDates.has(days[6].date)
    last7Days.value = days
  } catch (error) {
    console.error('加载打卡数据失败:', error)
  }
}

const loadChartData = async () => {
  try {
    const [moodRes, trendRes] = await Promise.all([
      api.stats.moodDistribution(),
      api.stats.diaryTrend(30)
    ])
    
    nextTick(() => {
      initMoodChart(moodRes.data.data)
      initTrendChart(trendRes.data.data)
    })
  } catch (error) {
    console.error('加载图表数据失败:', error)
  }
}

const initMoodChart = (data) => {
  if (!moodChartRef.value) return
  
  moodChart = echarts.init(moodChartRef.value)
  
  const chartData = data?.chartData || []
  const labels = chartData.map(item => item.label)
  const counts = chartData.map(item => item.count)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'horizontal',
      bottom: 0
    },
    series: [
      {
        name: '心情分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 18,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: counts[0] || 0, name: labels[0] || '开心', itemStyle: { color: '#667eea' } },
          { value: counts[1] || 0, name: labels[1] || '难过', itemStyle: { color: '#f093fb' } },
          { value: counts[2] || 0, name: labels[2] || '生气', itemStyle: { color: '#f5576c' } },
          { value: counts[3] || 0, name: labels[3] || '平静', itemStyle: { color: '#4facfe' } },
          { value: counts[4] || 0, name: labels[4] || '兴奋', itemStyle: { color: '#38ef7d' } },
          { value: counts[5] || 0, name: labels[5] || '中性', itemStyle: { color: '#a0aec0' } }
        ].filter(item => item.value > 0)
      }
    ]
  }
  
  moodChart.setOption(option)
}

const initTrendChart = (data) => {
  if (!trendChartRef.value) return
  
  trendChart = echarts.init(trendChartRef.value)
  
  const dates = data?.dates || []
  const counts = data?.counts || []
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates.map(date => {
        const d = new Date(date)
        return `${d.getMonth() + 1}/${d.getDate()}`
      }),
      axisLabel: {
        rotate: 45,
        fontSize: 10
      }
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '日记数',
        type: 'bar',
        data: counts,
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#667eea' },
            { offset: 1, color: '#764ba2' }
          ])
        }
      }
    ]
  }
  
  trendChart.setOption(option)
}

const handleResize = () => {
  moodChart?.resize()
  trendChart?.resize()
}

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    user.value = JSON.parse(userStr)
  }
  loadStats()
  loadTodayTasks()
  loadRecentDiaries()
  loadCheckinData()
  loadChartData()
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  moodChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
.dashboard {
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
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28px;
}

.header-left h1 {
  font-size: 28px;
  font-weight: 700;
  color: #2d3748;
  margin-bottom: 6px;
}

.date-text {
  color: #718096;
  font-size: 15px;
}

.weather-card {
  display: flex;
  align-items: center;
  gap: 8px;
  background: white;
  padding: 12px 20px;
  border-radius: 25px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.weather-icon {
  font-size: 20px;
}

.weather-text {
  color: #4a5568;
  font-size: 14px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 28px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  position: relative;
  box-sizing: border-box;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.12);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  border-radius: 16px 16px 0 0;
}

.stat-card-diary::before {
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
}

.stat-card-task::before {
  background: linear-gradient(90deg, #11998e 0%, #38ef7d 100%);
}

.stat-card-checkin::before {
  background: linear-gradient(90deg, #f093fb 0%, #f5576c 100%);
}

.stat-card-friend::before {
  background: linear-gradient(90deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 14px;
  flex-shrink: 0;
}

.stat-card-diary .stat-icon-wrapper {
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
}

.stat-card-task .stat-icon-wrapper {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
}

.stat-card-checkin .stat-icon-wrapper {
  background: linear-gradient(135deg, #fce7f3 0%, #fbcfe8 100%);
}

.stat-card-friend .stat-icon-wrapper {
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
}

.stat-icon {
  font-size: 24px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1a202c;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #718096;
  margin-top: 4px;
}

.stat-trend {
  color: #a0aec0;
}

.trend-icon {
  font-size: 18px;
}

.section-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.section {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #2d3748;
}

.task-progress {
  font-size: 14px;
  color: #718096;
  font-weight: 500;
}

.progress-bar {
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  margin-bottom: 20px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.add-diary {
  font-size: 14px;
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.add-diary:hover {
  text-decoration: underline;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  border: 2px dashed #e2e8f0;
  border-radius: 12px;
  background: #f7fafc;
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.empty-state p {
  color: #718096;
  font-size: 14px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  background: #f7fafc;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.task-item:hover {
  background: #edf2f7;
  border-color: #e2e8f0;
}

.task-item.completed {
  opacity: 0.6;
}

.checkbox-wrapper {
  position: relative;
  margin-right: 14px;
}

.checkbox-wrapper input {
  opacity: 0;
  position: absolute;
  cursor: pointer;
}

.checkmark {
  width: 20px;
  height: 20px;
  border: 2px solid #cbd5e0;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.checkbox-wrapper input:checked + .checkmark {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
}

.checkbox-wrapper input:checked + .checkmark::after {
  content: '✓';
  color: white;
  font-size: 12px;
  font-weight: bold;
}

.task-content {
  flex: 1;
}

.task-title {
  display: block;
  font-size: 15px;
  color: #2d3748;
  font-weight: 500;
}

.task-item.completed .task-title {
  text-decoration: line-through;
}

.task-desc {
  display: block;
  font-size: 13px;
  color: #a0aec0;
  margin-top: 2px;
}

.task-item .priority {
  margin-left: 12px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.priority.high {
  background: #fed7d7;
  color: #c53030;
}

.priority.medium {
  background: #feebc8;
  color: #d69e2e;
}

.priority.low {
  background: #c6f6d5;
  color: #38a169;
}

.view-all {
  display: block;
  text-align: right;
  margin-top: 16px;
  font-size: 14px;
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.view-all:hover {
  text-decoration: underline;
}

.diary-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.diary-item {
  display: flex;
  gap: 14px;
  padding: 16px;
  background: #f7fafc;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.diary-item:hover {
  background: #edf2f7;
  border-color: #e2e8f0;
}

.diary-mood-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.diary-content {
  flex: 1;
  min-width: 0;
}

.diary-header {
  margin-bottom: 6px;
}

.diary-title {
  font-weight: 600;
  color: #2d3748;
  font-size: 15px;
}

.diary-text {
  color: #718096;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.diary-footer {
  display: flex;
  gap: 12px;
}

.diary-date {
  font-size: 12px;
  color: #a0aec0;
}

.diary-category {
  font-size: 12px;
  color: #667eea;
  background: #eef2ff;
  padding: 2px 8px;
  border-radius: 4px;
}

.bottom-section {
  margin-top: 24px;
}

.section-checkin {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
}

.streak-count {
  font-size: 14px;
  color: #f5576c;
  font-weight: 600;
  background: #fce7f3;
  padding: 4px 12px;
  border-radius: 20px;
}

.checkin-calendar {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin: 24px 0;
  padding: 24px 16px;
  background: linear-gradient(135deg, #fce7f3 0%, #fbcfe8 100%);
  border-radius: 14px;
  overflow: hidden;
}

.calendar-day {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.day-name {
  font-size: 12px;
  color: #718096;
}

.day-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #cbd5e0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.day-circle.checked {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  transform: scale(1.05);
}

.day-circle.today {
  border: 2px solid #f5576c;
}

.day-circle.today.checked {
  border-color: transparent;
}

.checkin-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(245, 87, 108, 0.3);
}

.checkin-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(245, 87, 108, 0.4);
}

.btn-icon {
  font-size: 20px;
}

.checked-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px;
  background: #d1fae5;
  border-radius: 12px;
  color: #38a169;
  font-weight: 500;
}

.checked-icon {
  font-size: 20px;
}

.charts-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-top: 24px;
}

.section-chart {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
}

.chart-container {
  height: 280px;
  width: 100%;
}

@media (max-width: 1024px) {
  .charts-section {
    grid-template-columns: 1fr;
  }
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .section-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .header {
    flex-direction: column;
    gap: 16px;
  }
}
</style>