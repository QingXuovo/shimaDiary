<template>
  <div class="mood-analysis">
    <Sidebar :currentPage="'mood-analysis'" />
    <main class="main-content">
      <header class="header">
        <h1>💭 AI心情分析</h1>
        <p class="header-desc">基于您的日记内容进行情绪分析，帮助您更好地了解自己</p>
      </header>

      <div class="suggestion-card" v-if="suggestionData">
        <div class="suggestion-header">
          <span class="suggestion-icon">{{ getMoodIcon(suggestionData.mood) }}</span>
          <h2>情绪状态</h2>
        </div>
        <p class="suggestion-text">{{ suggestionData.suggestion }}</p>
        <div class="tips-list">
          <div v-for="(tip, index) in suggestionData.tips" :key="index" class="tip-item">
            <span class="tip-icon">💡</span>
            <span>{{ tip }}</span>
          </div>
        </div>
      </div>

      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-value">{{ suggestionData?.diaryCount || 0 }}</div>
          <div class="stat-label">最近7天日记</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ trendSummary?.positive || 0 }}</div>
          <div class="stat-label">积极情绪</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ trendSummary?.negative || 0 }}</div>
          <div class="stat-label">消极情绪</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ trendSummary?.calm || 0 }}</div>
          <div class="stat-label">平静情绪</div>
        </div>
      </div>

      <div class="charts-section">
        <div class="chart-card">
          <div class="chart-header">
            <h2>📈 情绪趋势（近30天）</h2>
          </div>
          <div ref="trendChartRef" class="chart-container"></div>
        </div>
        <div class="chart-card">
          <div class="chart-header">
            <h2>📊 情绪分布</h2>
          </div>
          <div ref="distributionChartRef" class="chart-container"></div>
        </div>
      </div>

      <div class="analysis-section">
        <h2>📝 近期情绪记录</h2>
        <div v-if="trendData.length === 0" class="empty-state">
          <div class="empty-icon">📭</div>
          <p>暂无足够的日记数据进行分析</p>
          <p class="empty-hint">多写几篇日记，AI就能帮您分析心情啦！</p>
        </div>
        <div v-else class="analysis-list">
          <div v-for="item in trendData" :key="item.date" class="analysis-item">
            <div class="analysis-date">
              <span>{{ formatDate(item.date) }}</span>
            </div>
            <div class="analysis-info">
              <span class="analysis-title">{{ item.title || '无标题' }}</span>
              <div class="analysis-meta">
                <span class="mood-badge" :class="item.sentiment">
                  {{ getSentimentLabel(item.sentiment) }}
                </span>
                <span class="score">{{ Math.round(item.score) }}分</span>
              </div>
            </div>
            <div class="score-bar">
              <div class="score-fill" :style="{ width: item.score + '%', background: getScoreColor(item.score) }"></div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import Sidebar from '../components/Sidebar.vue'
import { api } from '../services/api'

const suggestionData = ref(null)
const trendData = ref([])
const trendSummary = ref(null)
const trendChartRef = ref(null)
const distributionChartRef = ref(null)
let trendChart = null
let distributionChart = null

const getMoodIcon = (mood) => {
  const icons = { positive: '😊', negative: '😢', balanced: '😌', neutral: '😐' }
  return icons[mood] || '😐'
}

const getSentimentLabel = (sentiment) => {
  const labels = { positive: '积极', negative: '消极', neutral: '中性' }
  return labels[sentiment] || '未知'
}

const getScoreColor = (score) => {
  if (score >= 70) return 'linear-gradient(90deg, #38ef7d, #11998e)'
  if (score >= 40) return 'linear-gradient(90deg, #4facfe, #00f2fe)'
  return 'linear-gradient(90deg, #f5576c, #f093fb)'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const loadSuggestion = async () => {
  try {
    const response = await api.diary.ai.suggestion()
    suggestionData.value = response.data.data
  } catch (error) {
    console.error('获取情绪建议失败:', error)
  }
}

const loadTrend = async () => {
  try {
    const response = await api.diary.ai.trend(30)
    trendData.value = response.data.data?.trend || []
    trendSummary.value = response.data.data?.summary || {}
    nextTick(() => {
      initTrendChart(response.data.data)
      initDistributionChart(response.data.data?.summary)
    })
  } catch (error) {
    console.error('获取情绪趋势失败:', error)
  }
}

const initTrendChart = (data) => {
  if (!trendChartRef.value || !data) return
  
  trendChart = echarts.init(trendChartRef.value)
  
  const trend = data.trend || []
  const dates = trend.map(item => {
    const d = new Date(item.date)
    return `${d.getMonth() + 1}/${d.getDate()}`
  })
  const scores = trend.map(item => item.score || 50)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const item = trend[params[0].dataIndex]
        return `${item.date}<br/>情绪: ${getSentimentLabel(item.sentiment)}<br/>分数: ${item.score}`
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
      data: dates,
      axisLabel: {
        rotate: 45,
        fontSize: 10
      }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: {
        formatter: '{value}分'
      }
    },
    series: [
      {
        name: '情绪分数',
        type: 'line',
        smooth: true,
        data: scores,
        lineStyle: {
          width: 3,
          color: '#667eea'
        },
        itemStyle: {
          color: '#667eea'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(102, 126, 234, 0.4)' },
            { offset: 1, color: 'rgba(102, 126, 234, 0.05)' }
          ])
        }
      }
    ]
  }
  
  trendChart.setOption(option)
}

const initDistributionChart = (summary) => {
  if (!distributionChartRef.value || !summary) return
  
  distributionChart = echarts.init(distributionChartRef.value)
  
  const data = [
    { value: summary.positive || 0, name: '积极', itemStyle: { color: '#38ef7d' } },
    { value: summary.negative || 0, name: '消极', itemStyle: { color: '#f5576c' } },
    { value: summary.calm || 0, name: '平静', itemStyle: { color: '#4facfe' } }
  ].filter(item => item.value > 0)
  
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
        name: '情绪分布',
        type: 'pie',
        radius: ['40%', '70%'],
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 18,
            fontWeight: 'bold'
          }
        },
        data: data
      }
    ]
  }
  
  distributionChart.setOption(option)
}

const handleResize = () => {
  trendChart?.resize()
  distributionChart?.resize()
}

onMounted(() => {
  loadSuggestion()
  loadTrend()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  distributionChart?.dispose()
})
</script>

<style scoped>
.mood-analysis {
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

.header-desc {
  color: #718096;
  font-size: 15px;
  margin: 8px 0 0 0;
}

.suggestion-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 24px;
  color: white;
  margin-bottom: 24px;
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.3);
}

.suggestion-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.suggestion-icon {
  font-size: 32px;
}

.suggestion-header h2 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.suggestion-text {
  font-size: 15px;
  line-height: 1.7;
  margin: 0 0 16px 0;
  opacity: 0.95;
}

.tips-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 14px;
  opacity: 0.9;
}

.tip-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #2d3748;
}

.stat-label {
  font-size: 13px;
  color: #718096;
  margin-top: 4px;
}

.charts-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.chart-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
}

.chart-header {
  margin-bottom: 16px;
}

.chart-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #2d3748;
  margin: 0;
}

.chart-container {
  height: 250px;
  width: 100%;
}

.analysis-section {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.06);
}

.analysis-section h2 {
  font-size: 18px;
  font-weight: 600;
  color: #2d3748;
  margin: 0 0 20px 0;
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

.analysis-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.analysis-item {
  padding: 16px;
  background: #f7fafc;
  border-radius: 12px;
}

.analysis-date {
  font-size: 13px;
  color: #a0aec0;
  margin-bottom: 8px;
}

.analysis-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.analysis-title {
  font-size: 15px;
  font-weight: 500;
  color: #2d3748;
}

.analysis-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mood-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.mood-badge.positive {
  background: #d1fae5;
  color: #38a169;
}

.mood-badge.negative {
  background: #fed7d7;
  color: #c53030;
}

.mood-badge.neutral {
  background: #e2e8f0;
  color: #4a5568;
}

.score {
  font-size: 14px;
  font-weight: 600;
  color: #667eea;
}

.score-bar {
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}

.score-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}

@media (max-width: 1024px) {
  .charts-section {
    grid-template-columns: 1fr;
  }
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .main-content {
    padding: 16px;
  }
}
</style>