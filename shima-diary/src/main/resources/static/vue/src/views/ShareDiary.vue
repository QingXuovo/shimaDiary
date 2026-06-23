<template>
  <div class="share-container">
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
    </div>
    
    <div v-else-if="error" class="error-page">
      <div class="error-icon">❌</div>
      <div class="error-title">链接无效或已过期</div>
      <div class="error-desc">{{ error }}</div>
      <button class="back-btn" @click="goHome">返回首页</button>
    </div>
    
    <div v-else-if="diary" class="diary-card">
      <div class="card-header">
        <div class="mood-weather">
          <span class="mood-icon">{{ getMoodIcon(diary.mood) }}</span>
          <span class="weather-icon">{{ getWeatherIcon(diary.weather) }}</span>
        </div>
        <div class="share-label">🔗 分享日记</div>
      </div>
      
      <div class="card-title">{{ diary.title }}</div>
      
      <div class="card-meta">
        <span class="date">{{ formatDate(diary.diaryDate) }}</span>
        <span v-if="diary.location" class="location">📍 {{ diary.location }}</span>
      </div>
      
      <div class="card-content">
        {{ diary.content }}
      </div>
      
      <div v-if="diary.tags" class="card-tags">
        <span v-for="tag in diary.tags.split(',')" :key="tag" class="tag">{{ tag.trim() }}</span>
      </div>
      
      <div class="card-footer">
        <div class="expire-info">⏱️ 此链接将于 {{ formatDateTime(expireTime) }} 过期</div>
        <button class="copy-btn" @click="copyLink">📋 复制链接</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const diary = ref(null)
const loading = ref(true)
const error = ref('')
const expireTime = ref('')

const getMoodIcon = (mood) => {
  const icons = { happy: '😊', sad: '😢', normal: '😐', excited: '🤩', tired: '😴' }
  return icons[mood] || '😐'
}

const getWeatherIcon = (weather) => {
  const icons = { sunny: '☀️', cloudy: '☁️', rainy: '🌧️', snowy: '❄️' }
  return icons[weather] || '☀️'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

const copyLink = async () => {
  await navigator.clipboard.writeText(window.location.href)
  alert('链接已复制到剪贴板！')
}

const goHome = () => {
  window.location.href = '/api/vue/'
}

const loadDiary = async () => {
  const token = window.location.pathname.split('/').pop()
  if (!token) {
    error.value = '无效的分享链接'
    loading.value = false
    return
  }
  
  try {
    const response = await axios.get(`/api/share/${token}`)
    if (response.data.code === 200 && response.data.data) {
      diary.value = response.data.data
      const shareInfo = response.data.data.shareInfo
      if (shareInfo && shareInfo.expireTime) {
        expireTime.value = shareInfo.expireTime
      }
    } else {
      error.value = response.data.message || '分享链接无效'
    }
  } catch (err) {
    error.value = err.response?.data?.message || '链接无效或已过期'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDiary()
})
</script>

<style scoped>
.share-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-page {
  text-align: center;
  color: #fff;
  padding: 40px;
}

.error-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.error-title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 10px;
}

.error-desc {
  font-size: 14px;
  opacity: 0.8;
  margin-bottom: 30px;
}

.back-btn {
  padding: 12px 30px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
  border-radius: 25px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.diary-card {
  background: #fff;
  border-radius: 16px;
  padding: 30px;
  max-width: 600px;
  width: 100%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.mood-weather {
  font-size: 24px;
}

.mood-icon, .weather-icon {
  margin-right: 10px;
}

.share-label {
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
  padding: 6px 12px;
  border-radius: 12px;
}

.card-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 15px;
}

.card-meta {
  display: flex;
  gap: 15px;
  font-size: 14px;
  color: #999;
  margin-bottom: 20px;
}

.card-content {
  font-size: 16px;
  line-height: 1.8;
  color: #555;
  white-space: pre-wrap;
  margin-bottom: 20px;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 25px;
}

.tag {
  font-size: 12px;
  color: #667eea;
  background: #e8ecf5;
  padding: 4px 12px;
  border-radius: 10px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.expire-info {
  font-size: 12px;
  color: #999;
}

.copy-btn {
  padding: 10px 20px;
  background: #667eea;
  color: #fff;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.copy-btn:hover {
  background: #5568d3;
  transform: translateY(-2px);
}
</style>