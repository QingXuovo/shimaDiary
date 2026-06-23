<template>
  <div class="task-detail-page">
    <div class="page-header">
      <button @click="goBack" class="back-btn">← 返回搜索</button>
      <h1>任务详情</h1>
    </div>

    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="task" class="task-content">
      <div class="task-header">
        <h2>{{ task.title }}</h2>
        <div class="task-badges">
          <span class="priority-badge" :class="'priority-' + task.priority">
            {{ getPriorityText(task.priority) }}
          </span>
          <span class="status-badge" :class="'status-' + task.status">
            {{ getStatusText(task.status) }}
          </span>
        </div>
      </div>

      <div class="task-meta">
        <div v-if="task.dueDate" class="meta-item">
          <span>⏰ 截止日期：{{ formatDate(task.dueDate) }}</span>
        </div>
        <div v-if="task.category" class="meta-item">
          <span>📁 分类：{{ task.category }}</span>
        </div>
        <div v-if="task.progress !== undefined" class="meta-item">
          <span>📊 进度：{{ task.progress }}%</span>
        </div>
      </div>

      <div v-if="task.description" class="task-body">
        <h3>任务描述</h3>
        <div class="task-text">{{ task.description }}</div>
      </div>

      <div v-if="task.tags" class="task-tags">
        <span class="tag">🏷️ {{ task.tags }}</span>
      </div>

      <div v-if="task.progress !== undefined" class="task-progress">
        <h3>完成进度</h3>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: task.progress + '%' }"></div>
        </div>
        <span class="progress-text">{{ task.progress }}%</span>
      </div>

      <div class="task-actions">
        <button @click="completeTask" v-if="task.status !== 1" class="btn btn-success">完成任务</button>
        <button @click="editTask" class="btn btn-primary">编辑任务</button>
        <button @click="deleteTask" class="btn btn-danger">删除任务</button>
      </div>
    </div>

    <div v-else class="no-data">
      <p>未找到任务详情</p>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import { useRouter } from 'vue-router';

export default {
  name: 'TaskDetail',
  data() {
    return {
      task: null,
      loading: true
    };
  },
  setup() {
    const router = useRouter();
    return { router };
  },
  mounted() {
    this.loadTask();
  },
  methods: {
    async loadTask() {
      this.loading = true;
      const taskId = this.$route.params.id;
      
      try {
        const response = await axios.get('/api/task/' + taskId);
        if (response.data.success) {
          this.task = response.data.data;
        } else {
          console.error('加载任务失败:', response.data.message);
        }
      } catch (error) {
        console.error('加载任务失败:', error);
      } finally {
        this.loading = false;
      }
    },
    
    formatDate(dateStr) {
      if (!dateStr) return '';
      const date = new Date(dateStr);
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    },
    
    getPriorityText(priority) {
      const priorityMap = {
        1: '高优先级',
        2: '中优先级',
        3: '低优先级'
      };
      return priorityMap[priority] || '未设置';
    },
    
    getStatusText(status) {
      const statusMap = {
        0: '待完成',
        1: '已完成',
        2: '进行中'
      };
      return statusMap[status] || '未知';
    },
    
    goBack() {
      // 返回上一页或搜索页面
      if (window.history.length > 1) {
        this.router.back();
      } else {
        this.router.push('/search');
      }
    },
    
    async completeTask() {
      try {
        const response = await axios.put('/api/task/' + this.task.id + '/complete');
        if (response.data.success) {
          this.$message.success('任务已完成');
          this.loadTask(); // 重新加载任务
        } else {
          this.$message.error('操作失败');
        }
      } catch (error) {
        console.error('完成任务失败:', error);
        this.$message.error('操作失败，请重试');
      }
    },
    
    editTask() {
      // 编辑任务
      this.$message.info('编辑功能开发中...');
    },
    
    async deleteTask() {
      // 删除任务
      if (confirm('确定要删除这个任务吗？')) {
        try {
          const response = await axios.delete('/api/task/' + this.task.id);
          if (response.data.success) {
            this.$message.success('删除成功');
            this.router.push('/search');
          } else {
            this.$message.error('删除失败');
          }
        } catch (error) {
          console.error('删除任务失败:', error);
          this.$message.error('删除失败，请重试');
        }
      }
    }
  }
};
</script>

<style scoped>
.task-detail-page {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
}

.back-btn {
  background: #f5f5f5;
  border: none;
  padding: 10px 20px;
  border-radius: 5px;
  cursor: pointer;
  margin-bottom: 10px;
  font-size: 14px;
}

.back-btn:hover {
  background: #e0e0e0;
}

.page-header h1 {
  font-size: 28px;
  color: #333;
}

.loading {
  text-align: center;
  padding: 50px;
}

.spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.task-content {
  background: white;
  border-radius: 10px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.task-header {
  border-bottom: 2px solid #f0f0f0;
  padding-bottom: 20px;
  margin-bottom: 20px;
}

.task-header h2 {
  font-size: 24px;
  color: #333;
  margin-bottom: 15px;
}

.task-badges {
  display: flex;
  gap: 10px;
}

.priority-badge, .status-badge {
  padding: 5px 15px;
  border-radius: 15px;
  font-size: 12px;
  font-weight: 600;
}

.priority-1 {
  background: #ffebee;
  color: #f44336;
}

.priority-2 {
  background: #fff3e0;
  color: #ff9800;
}

.priority-3 {
  background: #e8f5e9;
  color: #4caf50;
}

.status-0 {
  background: #fff3e0;
  color: #ff9800;
}

.status-1 {
  background: #e8f5e9;
  color: #4caf50;
}

.status-2 {
  background: #e3f2fd;
  color: #2196f3;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 20px;
}

.meta-item {
  font-size: 14px;
  color: #666;
}

.task-body {
  padding: 20px 0;
}

.task-body h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 10px;
}

.task-text {
  font-size: 15px;
  line-height: 1.8;
  color: #555;
}

.task-tags {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.tag {
  display: inline-block;
  background: #e3f2fd;
  color: #1976D2;
  padding: 5px 15px;
  border-radius: 15px;
  font-size: 14px;
  margin-right: 10px;
}

.task-progress {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.task-progress h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 10px;
}

.progress-bar {
  height: 20px;
  background: #e0e0e0;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 5px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 14px;
  color: #666;
}

.task-actions {
  margin-top: 30px;
  display: flex;
  gap: 15px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover {
  background: #5568d3;
}

.btn-success {
  background: #4caf50;
  color: white;
}

.btn-success:hover {
  background: #43a047;
}

.btn-danger {
  background: #f44336;
  color: white;
}

.btn-danger:hover {
  background: #d32f2f;
}

.no-data {
  text-align: center;
  padding: 50px;
  color: #666;
}
</style>
