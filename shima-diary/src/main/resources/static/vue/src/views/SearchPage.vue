<template>
  <div class="search-page">
    <div class="search-header">
      <h1>全文搜索</h1>
      <p class="subtitle">搜索日记和任务，优先显示日期较近和优先级高的内容</p>
    </div>

    <!-- 搜索状态卡片 -->
    <div class="status-cards">
      <div class="status-card" :class="{ 'connected': searchStatus.available, 'disconnected': !searchStatus.available }">
        <div class="status-icon">
          <span v-if="searchStatus.available">✓</span>
          <span v-else>✗</span>
        </div>
        <div class="status-info">
          <h3>搜索引擎状态</h3>
          <p class="status-value">{{ searchStatus.available ? '运行中' : '不可用' }}</p>
          <small>{{ searchStatus.message }}</small>
        </div>
      </div>

      <div class="status-card">
        <div class="search-stats">
          <div class="stat-item">
            <span class="stat-value">{{ searchResult.total || 0 }}</span>
            <span class="stat-label">搜索结果</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ searchResult.diaryCount || 0 }}</span>
            <span class="stat-label">日记</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ searchResult.taskCount || 0 }}</span>
            <span class="stat-label">任务</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索框 -->
    <div class="search-box">
      <div class="search-input-wrapper">
        <input
          v-model="searchKeyword"
          type="text"
          class="search-input"
          placeholder="输入关键词搜索日记和任务..."
          @keyup.enter="doSearch"
          @input="onSearchInput"
        />
        <button @click="doSearch" class="search-btn" :disabled="loading">
          🔍 搜索
        </button>
      </div>

      <!-- 搜索类型切换 -->
      <div class="search-type-tabs">
        <button 
          @click="searchType = 'all'" 
          :class="{ active: searchType === 'all' }"
          class="type-tab"
        >
          全部
        </button>
        <button 
          @click="searchType = 'diary'" 
          :class="{ active: searchType === 'diary' }"
          class="type-tab"
        >
          📝 日记
        </button>
        <button 
          @click="searchType = 'task'" 
          :class="{ active: searchType === 'task' }"
          class="type-tab"
        >
          📋 任务
        </button>
      </div>

      <!-- 搜索建议 -->
      <div v-if="suggestions.length > 0" class="suggestions">
        <span
          v-for="suggestion in suggestions"
          :key="suggestion"
          class="suggestion-tag"
          @click="useSuggestion(suggestion)"
        >
          {{ suggestion }}
        </span>
      </div>
    </div>

    <!-- 搜索历史 -->
    <div v-if="searchHistory.length > 0 && !searchKeyword" class="search-history">
      <h3>搜索历史</h3>
      <div class="history-tags">
        <span
          v-for="history in searchHistory"
          :key="history"
          class="history-tag"
          @click="useHistory(history)"
        >
          ⏱️ {{ history }}
        </span>
        <button @click="clearHistory" class="clear-history">清除历史</button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>搜索中...</p>
    </div>

    <!-- 搜索结果 -->
    <div v-else-if="hasResults" class="search-results">
      <h2>搜索结果 ({{ searchResult.total || 0 }})</h2>
      
      <!-- 日记结果 -->
      <div v-if="searchResult.diaries && searchResult.diaries.length > 0" class="result-section">
        <div class="section-header">
          <h3>📝 日记 ({{ searchResult.diaryCount }})</h3>
          <span class="section-tip">按日期排序，最新的在前</span>
        </div>
        <div class="results-list">
          <div
            v-for="diary in searchResult.diaries"
            :key="'diary-' + diary.id"
            class="result-item diary"
            @click="viewDiary(diary)"
          >
            <div class="result-header">
              <h3 class="result-title" v-html="diary.title"></h3>
              <span class="result-date">{{ formatDate(diary.diaryDate) }}</span>
            </div>

            <div class="result-snippet" v-html="getSnippet(diary.content)"></div>

            <div class="result-meta">
              <span v-if="diary.mood" class="meta-tag">😀 {{ diary.mood }}</span>
              <span v-if="diary.weather" class="meta-tag">🌤️ {{ diary.weather }}</span>
              <span v-if="diary.tags" class="meta-tag">🏷️ {{ diary.tags }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 任务结果 -->
      <div v-if="searchResult.tasks && searchResult.tasks.length > 0" class="result-section">
        <div class="section-header">
          <h3>📋 任务 ({{ searchResult.taskCount }})</h3>
          <span class="section-tip">按优先级排序，高优先级优先</span>
        </div>
        <div class="results-list">
          <div
            v-for="task in searchResult.tasks"
            :key="'task-' + task.id"
            class="result-item task"
            :class="{ completed: task.status === 1 }"
            @click="viewTask(task)"
          >
            <div class="result-header">
              <h3 class="result-title" v-html="task.title"></h3>
              <div class="task-priority">
                <span class="priority-badge" :class="'priority-' + task.priority">
                  {{ getPriorityText(task.priority) }}
                </span>
                <span class="status-badge" :class="'status-' + task.status">
                  {{ getStatusText(task.status) }}
                </span>
              </div>
            </div>

            <div class="result-snippet" v-html="getSnippet(task.description)"></div>

            <div class="result-meta task-meta">
              <span v-if="task.dueDate" class="meta-tag due-date">
                ⏰ {{ formatDate(task.dueDate) }}
              </span>
              <span v-if="task.category" class="meta-tag">📁 {{ task.category }}</span>
              <span v-if="task.tags" class="meta-tag">🏷️ {{ task.tags }}</span>
              <span v-if="task.progress !== undefined" class="meta-tag">
                📊 进度: {{ task.progress }}%
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="searchResult.totalPages && searchResult.totalPages > 1">
        <button
          @click="changePage(searchResult.currentPage - 1)"
          :disabled="searchResult.currentPage <= 0"
          class="page-btn"
        >
          上一页
        </button>
        <span class="page-info">
          第 {{ searchResult.currentPage + 1 }} / {{ searchResult.totalPages }} 页
        </span>
        <button
          @click="changePage(searchResult.currentPage + 1)"
          :disabled="searchResult.currentPage >= searchResult.totalPages - 1"
          class="page-btn"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 无结果提示 -->
    <div v-else-if="searched && !hasResults" class="no-results">
      <div class="no-results-icon">🔍</div>
      <h3>未找到相关内容</h3>
      <p>没有找到包含 "{{ lastKeyword }}" 的日记或任务</p>
      <p class="tips">尝试使用其他关键词，或检查拼写</p>
    </div>

    <!-- 搜索说明 -->
    <div class="search-tips">
      <h2>📚 搜索技巧</h2>
      <div class="tips-grid">
        <div class="tip-card">
          <h4>多关键词搜索</h4>
          <p>使用多个关键词可以获得更精确的结果</p>
          <code>春天 旅行</code>
        </div>
        <div class="tip-card">
          <h4>标签搜索</h4>
          <p>可以搜索日记和任务的标签内容</p>
          <code>工作 学习</code>
        </div>
        <div class="tip-card">
          <h4>优先级搜索</h4>
          <p>任务搜索会优先显示高优先级的内容</p>
          <code>重要 紧急</code>
        </div>
        <div class="tip-card">
          <h4>高亮显示</h4>
          <p>搜索关键词会高亮显示</p>
          <code><em class="highlight">关键词</em></code>
        </div>
      </div>
    </div>

    <!-- 日记详情弹窗 -->
    <div v-if="showDiaryModal" class="modal" @click.self="closeDiaryModal">
      <div class="modal-content diary-modal">
        <button class="modal-close" @click="closeDiaryModal">×</button>
        <h2>{{ currentDiary.title }}</h2>
        <div class="diary-meta">
          <span>📅 {{ formatDate(currentDiary.diaryDate) }}</span>
          <span v-if="currentDiary.mood">😀 {{ currentDiary.mood }}</span>
          <span v-if="currentDiary.weather">🌤️ {{ currentDiary.weather }}</span>
          <span v-if="currentDiary.location">📍 {{ currentDiary.location }}</span>
        </div>
        <div class="diary-content" v-html="currentDiary.content"></div>
        <div v-if="currentDiary.tags" class="diary-tags">
          <span class="tag">🏷️ {{ currentDiary.tags }}</span>
        </div>
      </div>
    </div>

    <!-- 任务详情弹窗 -->
    <div v-if="showTaskModal" class="modal" @click.self="closeTaskModal">
      <div class="modal-content task-modal">
        <button class="modal-close" @click="closeTaskModal">×</button>
        <h2>{{ currentTask.title }}</h2>
        <div class="task-modal-header">
          <span class="priority-badge" :class="'priority-' + currentTask.priority">
            {{ getPriorityText(currentTask.priority) }}
          </span>
          <span class="status-badge" :class="'status-' + currentTask.status">
            {{ getStatusText(currentTask.status) }}
          </span>
        </div>
        <div class="task-meta-info">
          <span v-if="currentTask.dueDate">⏰ {{ formatDate(currentTask.dueDate) }}</span>
          <span v-if="currentTask.category">📁 {{ currentTask.category }}</span>
        </div>
        <div v-if="currentTask.description" class="task-description">
          <h3>描述</h3>
          <p>{{ currentTask.description }}</p>
        </div>
        <div v-if="currentTask.tags" class="task-tags">
          <span class="tag">🏷️ {{ currentTask.tags }}</span>
        </div>
        <div class="task-progress" v-if="currentTask.progress !== undefined">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: currentTask.progress + '%' }"></div>
          </div>
          <span>{{ currentTask.progress }}%</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import { useRouter } from 'vue-router';

export default {
  name: 'SearchPage',
  data() {
    return {
      searchKeyword: '',
      lastKeyword: '',
      loading: false,
      searched: false,
      suggestions: [],
      searchHistory: [],
      searchType: 'all', // 'all', 'diary', 'task'
      searchResult: {
        diaries: [],
        tasks: [],
        total: 0,
        diaryCount: 0,
        taskCount: 0,
        totalPages: 0,
        currentPage: 0,
        pageSize: 10
      },
      searchStatus: {
        available: false,
        message: '检查中...'
      },
      showDiaryModal: false,
      currentDiary: {},
      showTaskModal: false,
      currentTask: {}
    };
  },
  setup() {
    const router = useRouter();
    return { router };
  },
  computed: {
    hasResults() {
      return (this.searchResult.diaries && this.searchResult.diaries.length > 0) ||
             (this.searchResult.tasks && this.searchResult.tasks.length > 0);
    }
  },
  mounted() {
    this.loadSearchHistory();
    this.checkSearchStatus();
  },
  methods: {
    // 检查搜索引擎状态
    async checkSearchStatus() {
      try {
        const response = await axios.get('/api/search/status');
        if (response.data.success) {
          this.searchStatus = {
            available: response.data.data.available,
            message: response.data.data.message
          };
        }
      } catch (error) {
        this.searchStatus = {
          available: false,
          message: '搜索引擎不可用'
        };
      }
    },

    // 执行搜索
    async doSearch() {
      if (!this.searchKeyword.trim()) {
        return;
      }

      this.lastKeyword = this.searchKeyword;
      this.loading = true;
      this.searched = true;

      try {
        let url = '';
        let params = {
          keyword: this.searchKeyword,
          page: 0,
          size: this.searchResult.pageSize
        };

        if (this.searchType === 'diary') {
          url = '/api/search/diaries';
        } else if (this.searchType === 'task') {
          url = '/api/search/tasks';
        } else {
          url = '/api/search/all';
        }

        const response = await axios.get(url, { params });

        if (response.data.success) {
          const data = response.data.data;
          this.searchResult = {
            diaries: data.diaries || [],
            tasks: data.tasks || [],
            total: data.total || 0,
            diaryCount: data.diaryCount || (data.diaries ? data.diaries.length : 0),
            taskCount: data.taskCount || (data.tasks ? data.tasks.length : 0),
            totalPages: data.totalPages || 0,
            currentPage: data.currentPage || 0,
            pageSize: data.pageSize || 10
          };
          this.addToHistory(this.searchKeyword);
        }
      } catch (error) {
        console.error('搜索失败:', error);
        this.$message.error('搜索失败，请重试');
      } finally {
        this.loading = false;
      }
    },

    // 搜索输入处理（获取建议）
    async onSearchInput() {
      if (this.searchKeyword.length < 2) {
        this.suggestions = [];
        return;
      }

      try {
        const response = await axios.get('/api/search/suggestions', {
          params: {
            prefix: this.searchKeyword,
            limit: 5
          }
        });

        if (response.data.success) {
          this.suggestions = response.data.data;
        }
      } catch (error) {
        // 静默处理
      }
    },

    // 使用建议
    useSuggestion(suggestion) {
      this.searchKeyword = suggestion;
      this.suggestions = [];
      this.doSearch();
    },

    // 添加到搜索历史
    addToHistory(keyword) {
      let history = JSON.parse(localStorage.getItem('searchHistory') || '[]');
      history = history.filter(h => h !== keyword);
      history.unshift(keyword);
      history = history.slice(0, 10);
      localStorage.setItem('searchHistory', JSON.stringify(history));
      this.searchHistory = history;
    },

    // 加载搜索历史
    loadSearchHistory() {
      this.searchHistory = JSON.parse(localStorage.getItem('searchHistory') || '[]');
    },

    // 使用历史记录
    useHistory(keyword) {
      this.searchKeyword = keyword;
      this.doSearch();
    },

    // 清除历史
    clearHistory() {
      localStorage.removeItem('searchHistory');
      this.searchHistory = [];
    },

    // 翻页
    async changePage(page) {
      this.loading = true;

      try {
        let url = '';
        let params = {
          keyword: this.lastKeyword,
          page: page,
          size: this.searchResult.pageSize
        };

        if (this.searchType === 'diary') {
          url = '/api/search/diaries';
        } else if (this.searchType === 'task') {
          url = '/api/search/tasks';
        } else {
          url = '/api/search/all';
        }

        const response = await axios.get(url, { params });

        if (response.data.success) {
          const data = response.data.data;
          this.searchResult = {
            diaries: data.diaries || [],
            tasks: data.tasks || [],
            total: data.total || 0,
            diaryCount: data.diaryCount || (data.diaries ? data.diaries.length : 0),
            taskCount: data.taskCount || (data.tasks ? data.tasks.length : 0),
            totalPages: data.totalPages || 0,
            currentPage: data.currentPage || page,
            pageSize: data.pageSize || 10
          };
        }
      } catch (error) {
        console.error('翻页失败:', error);
      } finally {
        this.loading = false;
      }
    },

    // 获取摘要（高亮显示）
    getSnippet(content) {
      if (content) {
        const maxLength = 200;
        let text = content;
        if (text.length > maxLength) {
          text = text.substring(0, maxLength) + '...';
        }
        return text;
      }
      return '';
    },

    // 获取优先级文本
    getPriorityText(priority) {
      const priorityMap = {
        1: '高优先级',
        2: '中优先级',
        3: '低优先级'
      };
      return priorityMap[priority] || '未设置';
    },

    // 获取状态文本
    getStatusText(status) {
      const statusMap = {
        0: '待完成',
        1: '已完成',
        2: '进行中'
      };
      return statusMap[status] || '未知';
    },

    // 查看日记详情 - 跳转到日记详情页
    viewDiary(diary) {
      // 跳转到日记详情页面，URL格式：http://localhost:8080/api/vue/diary/:id
      this.router.push('/diary/' + diary.id);
    },

    // 关闭日记弹窗
    closeDiaryModal() {
      this.showDiaryModal = false;
      this.currentDiary = {};
    },

    // 查看任务详情 - 跳转到任务详情页
    viewTask(task) {
      // 跳转到任务详情页面，URL格式：http://localhost:8080/api/vue/task/:id
      this.router.push('/task/' + task.id);
    },

    // 关闭任务弹窗
    closeTaskModal() {
      this.showTaskModal = false;
      this.currentTask = {};
    },

    // 格式化日期
    formatDate(dateStr) {
      if (!dateStr) return '';
      const date = new Date(dateStr);
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    }
  }
};
</script>

<style scoped>
.search-page {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.search-header {
  text-align: center;
  margin-bottom: 30px;
}

.search-header h1 {
  font-size: 28px;
  color: #333;
  margin-bottom: 10px;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 15px;
  margin-bottom: 20px;
}

.status-card {
  background: white;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  display: flex;
  align-items: center;
  gap: 15px;
}

.status-card.connected {
  border-left: 4px solid #4CAF50;
}

.status-card.disconnected {
  border-left: 4px solid #f44336;
}

.status-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.connected .status-icon {
  background: #E8F5E9;
  color: #4CAF50;
}

.disconnected .status-icon {
  background: #FFEBEE;
  color: #f44336;
}

.status-info h3 {
  font-size: 14px;
  color: #666;
  margin: 0 0 5px 0;
}

.status-value {
  font-size: 18px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.status-info small {
  color: #999;
  font-size: 12px;
}

.search-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: bold;
  color: #2196F3;
}

.stat-label {
  font-size: 12px;
  color: #666;
}

.search-box {
  background: white;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-bottom: 20px;
}

.search-input-wrapper {
  display: flex;
  gap: 10px;
}

.search-input {
  flex: 1;
  padding: 12px 20px;
  border: 2px solid #e0e0e0;
  border-radius: 25px;
  font-size: 16px;
  outline: none;
  transition: border-color 0.3s;
}

.search-input:focus {
  border-color: #2196F3;
}

.search-btn {
  padding: 12px 30px;
  background: #2196F3;
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.3s;
}

.search-btn:hover:not(:disabled) {
  background: #1976D2;
}

.search-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.search-type-tabs {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.type-tab {
  padding: 8px 20px;
  background: #f5f5f5;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.type-tab:hover {
  background: #e0e0e0;
}

.type-tab.active {
  background: #2196F3;
  color: white;
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 15px;
}

.suggestion-tag {
  background: #E3F2FD;
  color: #1976D2;
  padding: 5px 15px;
  border-radius: 15px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s;
}

.suggestion-tag:hover {
  background: #BBDEFB;
}

.search-history {
  background: white;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-bottom: 20px;
}

.search-history h3 {
  font-size: 14px;
  color: #666;
  margin-bottom: 15px;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.history-tag {
  background: #F5F5F5;
  color: #666;
  padding: 5px 15px;
  border-radius: 15px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s;
}

.history-tag:hover {
  background: #E0E0E0;
}

.clear-history {
  background: none;
  border: none;
  color: #999;
  font-size: 12px;
  cursor: pointer;
}

.clear-history:hover {
  color: #f44336;
}

.loading {
  text-align: center;
  padding: 50px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #2196F3;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.search-results h2 {
  font-size: 18px;
  color: #333;
  margin-bottom: 20px;
}

.result-section {
  margin-bottom: 30px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-header h3 {
  font-size: 16px;
  color: #333;
  margin: 0;
}

.section-tip {
  font-size: 12px;
  color: #999;
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.result-item {
  background: white;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.result-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.result-item.diary {
  border-left: 4px solid #2196F3;
}

.result-item.task {
  border-left: 4px solid #FF9800;
}

.result-item.task.completed {
  opacity: 0.7;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.result-title {
  font-size: 18px;
  color: #333;
  margin: 0;
}

.result-date {
  color: #999;
  font-size: 14px;
}

.task-priority {
  display: flex;
  gap: 10px;
}

.priority-badge {
  padding: 3px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: bold;
}

.priority-1 {
  background: #FFEBEE;
  color: #f44336;
}

.priority-2 {
  background: #FFF3E0;
  color: #FF9800;
}

.priority-3 {
  background: #E8F5E9;
  color: #4CAF50;
}

.status-badge {
  padding: 3px 10px;
  border-radius: 10px;
  font-size: 12px;
}

.status-0 {
  background: #FFF3E0;
  color: #FF9800;
}

.status-1 {
  background: #E8F5E9;
  color: #4CAF50;
}

.status-2 {
  background: #E3F2FD;
  color: #1976D2;
}

.result-snippet {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 10px;
}

.result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.meta-tag {
  background: #F5F5F5;
  color: #666;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.meta-tag.due-date {
  background: #FFF3E0;
  color: #FF9800;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  margin-top: 30px;
}

.page-btn {
  padding: 8px 20px;
  background: #2196F3;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  transition: background 0.3s;
}

.page-btn:hover:not(:disabled) {
  background: #1976D2;
}

.page-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.page-info {
  color: #666;
  font-size: 14px;
}

.no-results {
  text-align: center;
  padding: 50px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.no-results-icon {
  font-size: 60px;
  margin-bottom: 20px;
}

.no-results h3 {
  font-size: 20px;
  color: #333;
  margin-bottom: 10px;
}

.no-results p {
  color: #666;
  margin: 5px 0;
}

.tips {
  color: #999 !important;
  font-size: 14px;
}

.search-tips {
  margin-top: 30px;
}

.search-tips h2 {
  font-size: 18px;
  color: #333;
  margin-bottom: 20px;
}

.tips-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.tip-card {
  background: white;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.tip-card h4 {
  font-size: 14px;
  color: #333;
  margin-bottom: 10px;
}

.tip-card p {
  font-size: 12px;
  color: #666;
  margin-bottom: 10px;
}

.tip-card code {
  background: #F5F5F5;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.highlight {
  background: #FFEB3B;
  color: #333;
  padding: 0 2px;
  border-radius: 2px;
}

/* 模态框 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 15px;
  padding: 30px;
  max-width: 600px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  position: relative;
}

.modal-close {
  position: absolute;
  top: 15px;
  right: 15px;
  background: none;
  border: none;
  font-size: 30px;
  color: #999;
  cursor: pointer;
}

.modal-close:hover {
  color: #333;
}

.diary-modal h2 {
  margin-bottom: 15px;
  padding-right: 30px;
}

.diary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.diary-content {
  color: #333;
  line-height: 1.8;
  white-space: pre-wrap;
}

.diary-tags {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.tag {
  background: #E3F2FD;
  color: #1976D2;
  padding: 5px 15px;
  border-radius: 15px;
  font-size: 14px;
}

/* 任务弹窗样式 */
.task-modal h2 {
  margin-bottom: 15px;
  padding-right: 30px;
}

.task-modal-header {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.task-meta-info {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.task-description {
  margin-bottom: 20px;
}

.task-description h3 {
  font-size: 14px;
  color: #333;
  margin-bottom: 10px;
}

.task-description p {
  color: #666;
  line-height: 1.6;
}

.task-tags {
  margin-bottom: 20px;
}

.task-progress {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.progress-bar {
  flex: 1;
  height: 20px;
  background: #f5f5f5;
  border-radius: 10px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #2196F3;
  border-radius: 10px;
  transition: width 0.3s;
}

.task-progress span {
  font-weight: bold;
  color: #333;
}
</style>