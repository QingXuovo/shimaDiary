<template>
  <div class="checkin-page">
    <Sidebar :currentPage="'checkin'" />
    <main class="main-content">
      <header class="page-header">
        <h1>📅 打卡记录</h1>
        <div class="header-actions">
          <button 
            v-if="!todayChecked" 
            class="btn btn-primary" 
            @click="handleCheckin"
            :disabled="isCheckingIn"
          >
            <span v-if="isCheckingIn">📍 获取位置中...</span>
            <span v-else>今日打卡</span>
          </button>
          <span v-else class="checked-badge">✓ 今日已打卡</span>
        </div>
      </header>

      <div class="stats-section">
        <div class="stat-card streak-card">
          <div class="stat-icon">🔥</div>
          <div class="stat-info">
            <div class="stat-value">{{ currentStreak }}</div>
            <div class="stat-label">连续打卡天数</div>
          </div>
        </div>
        <div class="stat-card total-card">
          <div class="stat-icon">📊</div>
          <div class="stat-info">
            <div class="stat-value">{{ totalCheckins }}</div>
            <div class="stat-label">累计打卡天数</div>
          </div>
        </div>
        <div class="stat-card month-card">
          <div class="stat-icon">🗓️</div>
          <div class="stat-info">
            <div class="stat-value">{{ monthCheckins }}</div>
            <div class="stat-label">本月打卡天数</div>
          </div>
        </div>
      </div>

      <div class="calendar-section">
        <h2>📆 本月打卡日历</h2>
        <div class="calendar-header">
          <button class="nav-btn" @click="prevMonth">◀</button>
          <span class="current-month">{{ calendarTitle }}</span>
          <button class="nav-btn" @click="nextMonth">▶</button>
        </div>
        <div class="calendar-grid">
          <div class="calendar-weekdays">
            <span v-for="day in weekdays" :key="day">{{ day }}</span>
          </div>
          <div class="calendar-days">
            <div 
              v-for="(day, index) in calendarDays" 
              :key="index"
              class="calendar-day"
              :class="{ 
                'other-month': !day.currentMonth,
                'checked': day.checked,
                'today': day.isToday,
                'empty': !day.day
              }"
            >
              <span v-if="day.day">{{ day.day }}</span>
              <span v-if="day.checked" class="check-mark">✓</span>
            </div>
          </div>
        </div>
        <div class="calendar-legend">
          <span class="legend-item"><span class="legend-dot checked"></span> 已打卡</span>
          <span class="legend-item"><span class="legend-dot today"></span> 今天</span>
        </div>
      </div>

      <div class="history-section">
        <h2>📝 打卡历史</h2>
        <div v-if="checkinHistory.length === 0" class="empty-state">
          <div class="empty-icon">📋</div>
          <p>暂无打卡记录</p>
          <p class="empty-hint">点击上方「今日打卡」开始记录</p>
        </div>
        <div v-else class="history-list">
          <div v-for="checkin in checkinHistory" :key="checkin.id" class="history-item">
            <div class="history-date-wrapper">
              <span class="history-date">{{ formatDate(checkin.checkDate) }}</span>
              <span class="history-time" v-if="checkin.createTime">{{ formatTime(checkin.createTime) }}</span>
            </div>
            <div class="history-location" v-if="checkin.location">
              <span class="location-icon">📍</span>
              <span>{{ checkin.location }}</span>
            </div>
            <div class="history-check" v-else>
              <span>无定位记录</span>
            </div>
          </div>
        </div>
      </div>

      <div class="location-tip">
        <span class="tip-icon">💡</span>
        <span>打卡时将自动获取您的位置信息，用于记录打卡地点</span>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import { api } from '../services/api'

const checkins = ref([])
const currentStreak = ref(0)
const totalCheckins = ref(0)
const monthCheckins = ref(0)
const todayChecked = ref(false)
const isCheckingIn = ref(false)

const currentDate = new Date()
const currentYear = ref(currentDate.getFullYear())
const currentMonth = ref(currentDate.getMonth())

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

const calendarTitle = computed(() => {
  return `${currentYear.value}年${currentMonth.value + 1}月`
})

const calendarDays = computed(() => {
  const days = []
  const firstDay = new Date(currentYear.value, currentMonth.value, 1)
  const lastDay = new Date(currentYear.value, currentMonth.value + 1, 0)
  const today = new Date()
  
  const startPadding = firstDay.getDay()
  
  const prevMonthLastDay = new Date(currentYear.value, currentMonth.value, 0).getDate()
  for (let i = startPadding - 1; i >= 0; i--) {
    days.push({
      day: prevMonthLastDay - i,
      currentMonth: false,
      checked: false,
      isToday: false
    })
  }
  
  for (let i = 1; i <= lastDay.getDate(); i++) {
    const dateStr = `${currentYear.value}-${String(currentMonth.value + 1).padStart(2, '0')}-${String(i).padStart(2, '0')}`
    const isChecked = checkins.value.some(c => c.checkDate === dateStr)
    const isTodayDate = today.getFullYear() === currentYear.value && 
                        today.getMonth() === currentMonth.value && 
                        today.getDate() === i
    
    days.push({
      day: i,
      currentMonth: true,
      checked: isChecked,
      isToday: isTodayDate
    })
  }
  
  const remainingDays = 42 - days.length
  for (let i = 1; i <= remainingDays; i++) {
    days.push({
      day: i,
      currentMonth: false,
      checked: false,
      isToday: false
    })
  }
  
  return days
})

const checkinHistory = computed(() => {
  return [...checkins.value].sort((a, b) => new Date(b.checkDate + ' ' + (b.createTime || '00:00')) - new Date(a.checkDate + ' ' + (a.createTime || '00:00')))
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' })
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const getCurrentLocation = () => {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('浏览器不支持定位'))
      return
    }
    
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude } = position.coords
        resolve({ latitude, longitude })
      },
      (error) => {
        reject(error)
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      }
    )
  })
}

const getAddressFromCoords = async (latitude, longitude) => {
  try {
    const response = await fetch(
      `https://apis.map.qq.com/ws/geocoder/v1/?location=${latitude},${longitude}&key=OB4BZ-D4W3U-B7VVO-4PJWW-6TKDJ-WPB77`
    )
    const data = await response.json()
    if (data.status === 0 && data.result) {
      return data.result.address
    }
    return `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`
  } catch (error) {
    console.error('获取地址失败:', error)
    return `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`
  }
}

const prevMonth = () => {
  if (currentMonth.value === 0) {
    currentMonth.value = 11
    currentYear.value--
  } else {
    currentMonth.value--
  }
  loadCheckinsForMonth()
}

const nextMonth = () => {
  if (currentMonth.value === 11) {
    currentMonth.value = 0
    currentYear.value++
  } else {
    currentMonth.value++
  }
  loadCheckinsForMonth()
}

const loadCheckins = async () => {
  try {
    const [checkinsRes, statsRes] = await Promise.all([
      api.checkin.list(),
      api.checkin.stats()
    ])
    
    checkins.value = checkinsRes.data.data || []
    const stats = statsRes.data.data || {}
    
    currentStreak.value = stats.currentStreak || 0
    totalCheckins.value = stats.totalCheckins || 0
    monthCheckins.value = stats.monthCheckins || 0
    
    const today = new Date().toISOString().split('T')[0]
    todayChecked.value = checkins.value.some(c => c.checkDate === today)
  } catch (error) {
    console.error('加载打卡数据失败:', error)
  }
}

const loadCheckinsForMonth = async () => {
  try {
    const response = await api.checkin.list()
    const allCheckins = response.data.data || []
    
    checkins.value = allCheckins.filter(c => {
      if (!c.checkDate) return false
      const date = new Date(c.checkDate)
      return date.getFullYear() === currentYear.value && date.getMonth() === currentMonth.value
    })
  } catch (error) {
    console.error('加载月度打卡数据失败:', error)
  }
}

const handleCheckin = async () => {
  isCheckingIn.value = true
  
  let location = null
  try {
    const coords = await getCurrentLocation()
    location = await getAddressFromCoords(coords.latitude, coords.longitude)
  } catch (error) {
    console.warn('获取位置失败:', error)
  }
  
  try {
    const response = await api.checkin.create({
      checkType: 'daily',
      checkName: '每日打卡',
      location: location || ''
    })
    
    if (response.data.code === 200) {
      todayChecked.value = true
      loadCheckins()
      const msg = location ? `打卡成功！📍 ${location}` : '打卡成功！'
      alert(msg)
    } else {
      alert(response.data.message || '打卡失败')
    }
  } catch (error) {
    console.error('打卡失败:', error)
    alert('打卡失败，请重试')
  } finally {
    isCheckingIn.value = false
  }
}

onMounted(() => {
  loadCheckins()
})
</script>

<style scoped>
.checkin-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  padding-top: 100px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 28px;
  color: white;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.header-actions {
  display: flex;
  align-items: center;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 25px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-primary {
  background: white;
  color: #667eea;
}

.checked-badge {
  background: rgba(255,255,255,0.2);
  color: white;
  padding: 12px 24px;
  border-radius: 25px;
  font-weight: 600;
  backdrop-filter: blur(10px);
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
}

.stat-icon {
  font-size: 32px;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}

.streak-card .stat-icon {
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
}

.total-card .stat-icon {
  background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%);
}

.month-card .stat-icon {
  background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #333;
}

.stat-label {
  color: #999;
  font-size: 14px;
}

.calendar-section,
.history-section {
  background: white;
  padding: 24px;
  border-radius: 16px;
  margin-bottom: 24px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
}

.calendar-section h2,
.history-section h2 {
  font-size: 18px;
  color: #333;
  margin-bottom: 20px;
  font-weight: 600;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.nav-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: #f5f5f5;
  border-radius: 10px;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.2s;
}

.nav-btn:hover {
  background: #e8e8e8;
  transform: scale(1.1);
}

.current-month {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.calendar-grid {
  width: 100%;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  margin-bottom: 8px;
}

.calendar-weekdays span {
  text-align: center;
  color: #999;
  font-size: 14px;
  padding: 10px;
  font-weight: 500;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.calendar-day {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  border-radius: 12px;
  position: relative;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 15px;
  font-weight: 500;
}

.calendar-day:hover {
  background: #e8f5e9;
  transform: scale(1.05);
}

.calendar-day.other-month {
  opacity: 0.3;
}

.calendar-day.checked {
  background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%);
  color: white;
}

.calendar-day.today {
  border: 3px solid #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
}

.calendar-day.empty {
  background: transparent;
  cursor: default;
}

.check-mark {
  position: absolute;
  bottom: 4px;
  font-size: 12px;
  font-weight: bold;
}

.calendar-day.checked .check-mark {
  color: white;
}

.calendar-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.legend-dot.checked {
  background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%);
}

.legend-dot.today {
  border: 2px solid #667eea;
  background: transparent;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
  transition: background 0.2s;
}

.history-item:hover {
  background: #f0f1f2;
}

.history-date-wrapper {
  display: flex;
  flex-direction: column;
}

.history-date {
  font-weight: 600;
  color: #333;
  font-size: 15px;
}

.history-time {
  color: #999;
  font-size: 13px;
  margin-top: 2px;
}

.history-location {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #667eea;
  font-size: 14px;
  background: #e8e8ff;
  padding: 8px 12px;
  border-radius: 20px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-icon {
  flex-shrink: 0;
}

.history-check {
  color: #999;
  font-size: 14px;
}

.empty-state {
  text-align: center;
  padding: 40px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state p {
  color: #999;
  margin: 8px 0;
}

.empty-hint {
  font-size: 14px;
  color: #667eea !important;
}

.location-tip {
  background: rgba(255,255,255,0.9);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 24px;
  border-radius: 12px;
  color: #666;
  font-size: 14px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
}

.tip-icon {
  font-size: 18px;
}

@media (max-width: 768px) {
  .main-content {
    padding-left: 84px;
    padding-top: 80px;
  }
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
  .page-header h1 {
    font-size: 22px;
  }
  .btn {
    padding: 10px 16px;
    font-size: 13px;
  }
  .history-location {
    max-width: 120px;
  }
}
</style>