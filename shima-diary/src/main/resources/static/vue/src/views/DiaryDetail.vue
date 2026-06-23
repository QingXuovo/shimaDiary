<template>
  <div class="diary-detail-page">
    <div class="page-header">
      <button @click="goBack" class="back-btn">← 返回搜索</button>
      <h1>日记详情</h1>
    </div>

    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="diary" class="diary-content">
      <div class="diary-header">
        <h2>{{ diary.title }}</h2>
        <div class="diary-meta">
          <span>📅 {{ formatDate(diary.diaryDate) }}</span>
          <span v-if="diary.mood">😀 {{ diary.mood }}</span>
          <span v-if="diary.weather">🌤️ {{ diary.weather }}</span>
          <span v-if="diary.location">📍 {{ diary.location }}</span>
        </div>
      </div>

      <div class="diary-body">
        <div class="diary-text" v-html="diary.content"></div>
      </div>

      <div v-if="diary.tags" class="diary-tags">
        <span class="tag">🏷️ {{ diary.tags }}</span>
      </div>

      <div class="diary-actions">
        <button @click="editDiary" class="btn btn-primary">编辑日记</button>
        <button @click="deleteDiary" class="btn btn-danger">删除日记</button>
      </div>
    </div>

    <div v-else class="no-data">
      <p>未找到日记详情</p>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import { useRouter } from 'vue-router';

export default {
  name: 'DiaryDetail',
  data() {
    return {
      diary: null,
      loading: true
    };
  },
  setup() {
    const router = useRouter();
    return { router };
  },
  mounted() {
    this.loadDiary();
  },
  methods: {
    async loadDiary() {
      this.loading = true;
      const diaryId = this.$route.params.id;
      
      try {
        const response = await axios.get('/api/diary/' + diaryId);
        if (response.data.success) {
          this.diary = response.data.data;
        } else {
          console.error('加载日记失败:', response.data.message);
        }
      } catch (error) {
        console.error('加载日记失败:', error);
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
    
    goBack() {
      // 返回上一页或搜索页面
      if (window.history.length > 1) {
        this.router.back();
      } else {
        this.router.push('/search');
      }
    },
    
    editDiary() {
      // 编辑日记
      this.$message.info('编辑功能开发中...');
    },
    
    async deleteDiary() {
      // 删除日记
      if (confirm('确定要删除这篇日记吗？')) {
        try {
          const response = await axios.delete('/api/diary/' + this.diary.id);
          if (response.data.success) {
            this.$message.success('删除成功');
            this.router.push('/search');
          } else {
            this.$message.error('删除失败');
          }
        } catch (error) {
          console.error('删除日记失败:', error);
          this.$message.error('删除失败，请重试');
        }
      }
    }
  }
};
</script>

<style scoped>
.diary-detail-page {
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

.diary-content {
  background: white;
  border-radius: 10px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.diary-header {
  border-bottom: 2px solid #f0f0f0;
  padding-bottom: 20px;
  margin-bottom: 20px;
}

.diary-header h2 {
  font-size: 24px;
  color: #333;
  margin-bottom: 15px;
}

.diary-meta {
  display: flex;
  gap: 20px;
  color: #666;
  font-size: 14px;
}

.diary-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.diary-body {
  padding: 20px 0;
}

.diary-text {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
}

.diary-tags {
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

.diary-actions {
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
