<template>
  <div class="stats">
    <Sidebar :currentPage="'stats'" />
    <main class="main-content">
      <!-- 年度统计卡片 -->
      <div class="yearly-section">
        <h2>年度统计</h2>
        <div class="yearly-grid">
          <div class="yearly-card">
            <div class="yearly-value">{{ yearlyStats.totalDiaries }}</div>
            <div class="yearly-label">年度日记</div>
          </div>
          <div class="yearly-card">
            <div class="yearly-value">{{ yearlyStats.totalCheckins }}</div>
            <div class="yearly-label">年度打卡</div>
          </div>
          <div class="yearly-card">
            <div class="yearly-value">{{ yearlyStats.totalTasks }}</div>
            <div class="yearly-label">年度任务</div>
          </div>
          <div class="yearly-card">
            <div class="yearly-value">{{ yearlyStats.completedTasks }}</div>
            <div class="yearly-label">已完成任务</div>
          </div>
        </div>
      </div>

      <!-- 月度趋势 -->
      <div class="trend-section">
        <h2>月度趋势</h2>
        <div class="trend-chart">
          <div class="chart-bars">
            <div v-for="(month, index) in monthlyData" :key="index" class="bar-item">
              <div class="bar-container">
                <div 
                  class="bar diary-bar" 
                  :style="{ height: (month.diaries / maxDiaries * 100) + '%' }"
                  :title="month.name + ': ' + month.diaries + '篇日记'"
                ></div>
                <div 
                  class="bar checkin-bar" 
                  :style="{ height: (month.checkins / maxCheckins * 100) + '%' }"
                  :title="month.name + ': ' + month.checkins + '次打卡'"
                ></div>
              </div>
              <div class="bar-label">{{ month.name }}</div>
            </div>
          </div>
          <div class="chart-legend">
            <div class="legend-item">
              <span class="legend-color diary-color"></span>
              <span>日记</span>
            </div>
            <div class="legend-item">
              <span class="legend-color checkin-color"></span>
              <span>打卡</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分类统计 -->
      <div class="category-section">
        <h2>分类统计</h2>
        <div v-if="categoryStats.length === 0" class="empty-state">暂无分类统计数据</div>
        <div v-else class="category-list">
          <div v-for="stat in categoryStats" :key="stat.id" class="category-stat">
            <div class="category-header">
              <span class="category-dot" :style="{ background: stat.color }"></span>
              <span class="category-name">{{ stat.name }}</span>
            </div>
            <div class="category-bars">
              <div class="bar-wrapper">
                <div 
                  class="category-bar" 
                  :style="{ width: (stat.diaryCount / maxCategoryCount * 100) + '%', background: stat.color }"
                ></div>
                <span class="bar-count">{{ stat.diaryCount }}篇日记</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 打卡配置统计 -->
      <div class="checkin-section">
        <h2>打卡类型统计</h2>
        <div v-if="checkinStats.length === 0" class="empty-state">暂无打卡配置</div>
        <div v-else class="checkin-grid">
          <div v-for="stat in checkinStats" :key="stat.id" class="checkin-card">
            <div class="checkin-icon">✅</div>
            <div class="checkin-info">
              <div class="checkin-name">{{ stat.checkName }}</div>
              <div class="checkin-count">已打卡 {{ stat.count }} 次</div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../services/api'
import Sidebar from '../components/Sidebar.vue'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const yearlyStats = ref({
  totalDiaries: 0,
  totalCheckins: 0,
  totalTasks: 0,
  completedTasks: 0
})
const monthlyData = ref([])
const categoryStats = ref([])
const checkinStats = ref([])

const maxDiaries = computed(() => {
  const max = Math.max(...monthlyData.value.map(m => m.diaries), 1)
  return max
})

const maxCheckins = computed(() => {
  const max = Math.max(...monthlyData.value.map(m => m.checkins), 1)
  return max
})

const maxCategoryCount = computed(() => {
  const max = Math.max(...categoryStats.value.map(c => c.diaryCount), 1)
  return max
})

async function loadStats() {
  try {
    const [diaryRes, checkinRes, taskRes, categoryRes, checkinConfigRes] = await Promise.all([
      api.diary.list(),
      api.checkin.list(),
      api.task.list(),
      api.category.list(),
      api.checkin.config.list()
    ])

    const diaries = diaryRes.data.data || []
    const checkins = checkinRes.data.data || []
    const tasks = taskRes.data.data || []
    const categories = categoryRes.data.data || []
    const configs = checkinConfigRes.data.data || []

    // 年度统计
    yearlyStats.value = {
      totalDiaries: diaries.length,
      totalCheckins: checkins.length,
      totalTasks: tasks.length,
      completedTasks: tasks.filter(t => t.status === 1).length
    }

    // 月度数据
    const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
    const currentYear = new Date().getFullYear()
    
    monthlyData.value = months.map((name, index) => {
      const monthStr = String(index + 1).padStart(2, '0')
      return {
        name,
        diaries: diaries.filter(d => d.diaryDate?.startsWith(`${currentYear}-${monthStr}`)).length,
        checkins: checkins.filter(c => c.checkDate?.startsWith(`${currentYear}-${monthStr}`)).length
      }
    })

    // 分类统计
    categoryStats.value = categories.map(cat => ({
      id: cat.id,
      name: cat.name,
      color: cat.color || '#667eea',
      diaryCount: diaries.filter(d => d.categoryId === cat.id).length
    })).filter(c => c.diaryCount > 0)

    // 打卡类型统计
    checkinStats.value = configs.map(config => ({
      id: config.id,
      checkName: config.checkName,
      count: checkins.filter(c => c.checkType === config.checkType).length
    }))
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
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

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.stats {
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

.yearly-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.yearly-section h2 {
  font-size: 18px;
  margin-bottom: 20px;
  color: #333;
}

.yearly-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.yearly-card {
  text-align: center;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 10px;
}

.yearly-value {
  font-size: 32px;
  font-weight: 700;
  color: #667eea;
  margin-bottom: 8px;
}

.yearly-label {
  font-size: 14px;
  color: #666;
}

.trend-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.trend-section h2 {
  font-size: 18px;
  margin-bottom: 20px;
  color: #333;
}

.trend-chart {
  padding: 20px;
}

.chart-bars {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  height: 200px;
  gap: 12px;
}

.bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.bar-container {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 4px;
}

.bar {
  width: 16px;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s;
}

.diary-bar {
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
}

.checkin-bar {
  background: linear-gradient(180deg, #10b981 0%, #059669 100%);
}

.bar-label {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 20px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #666;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 4px;
}

.diary-color {
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
}

.checkin-color {
  background: linear-gradient(180deg, #10b981 0%, #059669 100%);
}

.category-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.category-section h2 {
  font-size: 18px;
  margin-bottom: 20px;
  color: #333;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.category-stat {
  padding: 12px 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.category-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.category-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.category-bars {
  display: flex;
  align-items: center;
}

.bar-wrapper {
  flex: 1;
  height: 12px;
  background: #e9ecef;
  border-radius: 6px;
  overflow: hidden;
  position: relative;
}

.category-bar {
  height: 100%;
  border-radius: 6px;
  transition: width 0.3s;
}

.bar-count {
  margin-left: 12px;
  font-size: 12px;
  color: #999;
}

.checkin-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
}

.checkin-section h2 {
  font-size: 18px;
  margin-bottom: 20px;
  color: #333;
}

.checkin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.checkin-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 10px;
}

.checkin-icon {
  font-size: 28px;
}

.checkin-info {
  flex: 1;
}

.checkin-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.checkin-count {
  font-size: 12px;
  color: #999;
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

.btn-secondary {
  background: rgba(255,255,255,0.2);
  color: white;
}

.btn-secondary:hover {
  background: rgba(255,255,255,0.3);
}
</style>
