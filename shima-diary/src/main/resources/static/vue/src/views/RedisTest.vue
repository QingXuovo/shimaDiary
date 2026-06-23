<template>
  <div class="redis-test">
    <div class="header">
      <h1>Redis 缓存测试中心</h1>
      <p class="subtitle">使用两种持久化策略保存用户数据</p>
    </div>

    <!-- 连接状态卡片 -->
    <div class="status-cards">
      <div class="status-card" :class="{ 'connected': redisStatus.connected, 'disconnected': !redisStatus.connected }">
        <div class="status-icon">
          <span v-if="redisStatus.connected">✓</span>
          <span v-else>✗</span>
        </div>
        <div class="status-info">
          <h3>Redis 连接状态</h3>
          <p class="status-value">{{ redisStatus.connected ? '已连接' : '未连接' }}</p>
          <small>{{ redisStatus.message }}</small>
        </div>
      </div>

      <div class="status-card strategy">
        <div class="strategy-item">
          <h4>RDB 策略</h4>
          <p>{{ redisStatus.rdbStrategy }}</p>
          <span class="badge">快照持久化</span>
        </div>
        <div class="strategy-item">
          <h4>AOF 策略</h4>
          <p>{{ redisStatus.aofStrategy }}</p>
          <span class="badge">日志持久化</span>
        </div>
      </div>
    </div>

    <!-- 两种持久化策略说明 -->
    <div class="strategy-explanation">
      <h2>📚 持久化策略说明</h2>
      <div class="strategy-grid">
        <div class="strategy-card rdb">
          <h3>RDB (Redis Database)</h3>
          <p>定期生成数据快照，适合大量操作数据，性能优先</p>
          <ul>
            <li>操作日志缓存</li>
            <li>日记草稿缓存</li>
            <li>打卡记录缓存</li>
            <li>任务状态缓存</li>
          </ul>
          <div class="expire-tag">过期时间: 24小时</div>
        </div>
        <div class="strategy-card aof">
          <h3>AOF (Append Only File)</h3>
          <p>实时追加操作日志，数据安全优先，可靠性高</p>
          <ul>
            <li>用户会话缓存</li>
            <li>偏好设置缓存</li>
            <li>好友关系缓存</li>
            <li>关键配置缓存</li>
          </ul>
          <div class="expire-tag">过期时间: 1小时 ~ 7天</div>
        </div>
      </div>
    </div>

    <!-- 测试操作区 -->
    <div class="test-operations">
      <h2>🧪 缓存操作测试</h2>
      
      <div class="operation-tabs">
        <button 
          v-for="tab in tabs" 
          :key="tab.key"
          :class="['tab-btn', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          {{ tab.icon }} {{ tab.name }}
        </button>
      </div>

      <!-- AOF策略测试 -->
      <div v-if="activeTab === 'session'" class="operation-panel">
        <h3>用户会话缓存 (AOF策略)</h3>
        <p class="description">保存用户登录会话信息，确保重新登录时数据不丢失</p>
        <div class="action-buttons">
          <button @click="saveSession" class="btn btn-primary" :disabled="loading">
            💾 保存会话
          </button>
          <button @click="getSession" class="btn btn-secondary" :disabled="loading">
            📖 获取会话
          </button>
        </div>
        <div v-if="sessionData" class="result-panel">
          <h4>会话数据</h4>
          <pre>{{ JSON.stringify(sessionData, null, 2) }}</pre>
        </div>
      </div>

      <div v-if="activeTab === 'preference'" class="operation-panel">
        <h3>偏好设置缓存 (AOF策略)</h3>
        <p class="description">保存用户个性化设置，如主题、通知等</p>
        <div class="preference-form">
          <label>
            主题:
            <select v-model="preferenceForm.theme">
              <option value="light">浅色</option>
              <option value="dark">深色</option>
            </select>
          </label>
          <label>
            通知: 
            <input type="checkbox" v-model="preferenceForm.notifications" />
          </label>
        </div>
        <div class="action-buttons">
          <button @click="savePreference" class="btn btn-primary" :disabled="loading">
            💾 保存偏好
          </button>
          <button @click="getPreference" class="btn btn-secondary" :disabled="loading">
            📖 获取偏好
          </button>
        </div>
        <div v-if="preferenceData" class="result-panel">
          <h4>偏好数据</h4>
          <pre>{{ JSON.stringify(preferenceData, null, 2) }}</pre>
        </div>
      </div>

      <div v-if="activeTab === 'friend'" class="operation-panel">
        <h3>好友关系缓存 (AOF策略)</h3>
        <p class="description">缓存好友关系数据，提高查询效率</p>
        <div class="friend-form">
          <input v-model="friendForm.friendId" type="number" placeholder="好友ID" />
          <input v-model="friendForm.friendName" placeholder="好友名称" />
        </div>
        <div class="action-buttons">
          <button @click="saveFriend" class="btn btn-primary" :disabled="loading">
            💾 保存好友
          </button>
          <button @click="getFriend" class="btn btn-secondary" :disabled="loading">
            📖 获取好友
          </button>
        </div>
        <div v-if="friendData" class="result-panel">
          <h4>好友数据</h4>
          <pre>{{ JSON.stringify(friendData, null, 2) }}</pre>
        </div>
      </div>

      <!-- RDB策略测试 -->
      <div v-if="activeTab === 'diary'" class="operation-panel">
        <h3>日记草稿缓存 (RDB策略)</h3>
        <p class="description">自动保存未完成的日记，防止数据丢失</p>
        <div class="diary-form">
          <input v-model="diaryForm.title" placeholder="日记标题" />
          <textarea v-model="diaryForm.content" placeholder="日记内容" rows="4"></textarea>
        </div>
        <div class="action-buttons">
          <button @click="saveDiaryDraft" class="btn btn-success" :disabled="loading">
            💾 保存草稿
          </button>
          <button @click="getDiaryDraft" class="btn btn-secondary" :disabled="loading">
            📖 获取草稿
          </button>
          <button @click="deleteDiaryDraft" class="btn btn-danger" :disabled="loading">
            🗑️ 删除草稿
          </button>
        </div>
        <div v-if="diaryData" class="result-panel">
          <h4>草稿数据</h4>
          <pre>{{ JSON.stringify(diaryData, null, 2) }}</pre>
        </div>
      </div>

      <div v-if="activeTab === 'checkin'" class="operation-panel">
        <h3>打卡记录缓存 (RDB策略)</h3>
        <p class="description">缓存用户打卡记录，提高系统性能</p>
        <div class="checkin-form">
          <input v-model="checkinForm.type" placeholder="打卡类型" />
          <input v-model="checkinForm.location" placeholder="打卡地点" />
          <input v-model="checkinForm.note" placeholder="打卡备注" />
        </div>
        <div class="action-buttons">
          <button @click="saveCheckin" class="btn btn-success" :disabled="loading">
            💾 保存打卡
          </button>
          <button @click="getCheckin" class="btn btn-secondary" :disabled="loading">
            📖 获取打卡
          </button>
        </div>
        <div v-if="checkinData" class="result-panel">
          <h4>打卡数据</h4>
          <pre>{{ JSON.stringify(checkinData, null, 2) }}</pre>
        </div>
      </div>

      <div v-if="activeTab === 'operation'" class="operation-panel">
        <h3>操作日志缓存 (RDB策略)</h3>
        <p class="description">记录用户的所有操作行为，便于审计和分析</p>
        <div class="action-buttons">
          <button @click="getOperationLogs" class="btn btn-success" :disabled="loading">
            📋 查看日志
          </button>
          <button @click="clearOperationLogs" class="btn btn-danger" :disabled="loading">
            🗑️ 清除日志
          </button>
        </div>
        <div v-if="operationLogs.length > 0" class="result-panel logs-panel">
          <h4>操作日志</h4>
          <div v-for="(log, index) in operationLogs" :key="index" class="log-item">
            <span class="log-time">{{ log.timestamp }}</span>
            <span class="log-type">{{ log.operationType }}</span>
            <span class="log-desc">{{ log.description }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 实时数据监控 -->
    <div class="monitor-section">
      <h2>📊 Redis 实时数据监控</h2>
      <div class="monitor-controls">
        <button @click="refreshKeys" class="btn btn-info" :disabled="loading">
          🔄 刷新数据
        </button>
        <input v-model="searchKey" placeholder="搜索键..." class="search-input" />
      </div>
      
      <div class="keys-list">
        <div v-if="filteredKeys.length === 0" class="no-data">
          暂无缓存数据
        </div>
        <div 
          v-for="key in filteredKeys" 
          :key="key" 
          class="key-item"
          @click="viewKeyDetail(key)"
        >
          <div class="key-header">
            <span class="key-name">{{ key }}</span>
            <span class="key-type">{{ getKeyType(key) }}</span>
          </div>
          <div class="key-value">{{ getKeyValue(key) }}</div>
          <div class="key-actions">
            <button @click.stop="deleteKey(key)" class="btn-icon btn-danger-icon">🗑️</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 操作日志 -->
    <div class="message-log">
      <div 
        v-for="(msg, index) in messages" 
        :key="index" 
        :class="['message', msg.type]"
      >
        {{ msg.text }}
      </div>
    </div>

    <!-- 模态框 -->
    <div v-if="showModal" class="modal-overlay" @click="showModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ modalTitle }}</h3>
          <button @click="showModal = false" class="modal-close">×</button>
        </div>
        <div class="modal-body">
          <pre>{{ modalContent }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'RedisTest',
  data() {
    return {
      redisStatus: {
        connected: false,
        message: '正在连接...',
        rdbStrategy: '未启用',
        aofStrategy: '未启用'
      },
      activeTab: 'session',
      loading: false,
      
      tabs: [
        { key: 'session', name: '会话', icon: '🔐' },
        { key: 'preference', name: '偏好', icon: '⚙️' },
        { key: 'friend', name: '好友', icon: '👥' },
        { key: 'diary', name: '草稿', icon: '📝' },
        { key: 'checkin', name: '打卡', icon: '📍' },
        { key: 'operation', name: '日志', icon: '📋' }
      ],
      
      sessionData: null,
      preferenceData: null,
      preferenceForm: {
        theme: 'light',
        notifications: true
      },
      friendData: null,
      friendForm: {
        friendId: '',
        friendName: ''
      },
      diaryData: null,
      diaryForm: {
        title: '',
        content: ''
      },
      checkinData: null,
      checkinForm: {
        type: '',
        location: '',
        note: ''
      },
      operationLogs: [],
      
      keys: [],
      searchKey: '',
      showModal: false,
      modalTitle: '',
      modalContent: '',
      messages: []
    };
  },
  computed: {
    filteredKeys() {
      if (!this.searchKey) return this.keys;
      return this.keys.filter(key => key.includes(this.searchKey));
    }
  },
  mounted() {
    this.checkRedisStatus();
    this.refreshKeys();
    
    // 每30秒自动刷新状态
    setInterval(() => {
      this.checkRedisStatus();
      this.refreshKeys();
    }, 30000);
  },
  methods: {
    // 检查Redis状态
    async checkRedisStatus() {
      try {
        const response = await axios.get('/api/cache/status');
        if (response.data.success) {
          const data = response.data.data;
          this.redisStatus = {
            connected: data.redisEnabled,
            message: data.description,
            rdbStrategy: data.rdbStrategy,
            aofStrategy: data.aofStrategy
          };
        }
      } catch (error) {
        this.redisStatus = {
          connected: false,
          message: '连接失败: ' + (error.message || '未知错误'),
          rdbStrategy: '未启用',
          aofStrategy: '未启用'
        };
        this.addMessage('error', 'Redis状态检查失败');
      }
    },

    // 添加消息
    addMessage(type, text) {
      this.messages.unshift({ type, text });
      if (this.messages.length > 5) {
        this.messages.pop();
      }
      setTimeout(() => {
        this.messages = this.messages.filter(m => m !== this.messages[0]);
      }, 5000);
    },

    // 会话操作
    async saveSession() {
      this.loading = true;
      try {
        const response = await axios.post('/api/cache/session/save');
        this.sessionData = response.data.data;
        this.addMessage('success', '会话保存成功 (AOF策略)');
      } catch (error) {
        this.addMessage('error', '保存会话失败: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },

    async getSession() {
      this.loading = true;
      try {
        const response = await axios.get('/api/cache/session/get');
        this.sessionData = response.data.data;
        this.addMessage('success', '会话获取成功');
      } catch (error) {
        this.addMessage('error', '获取会话失败: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },

    // 偏好设置操作
    async savePreference() {
      this.loading = true;
      try {
        const response = await axios.post('/api/cache/preference/save', this.preferenceForm);
        this.preferenceData = response.data.data;
        this.addMessage('success', '偏好设置保存成功 (AOF策略)');
      } catch (error) {
        this.addMessage('error', '保存偏好失败: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },

    async getPreference() {
      this.loading = true;
      try {
        const response = await axios.get('/api/cache/preference/get');
        this.preferenceData = response.data.data;
        this.addMessage('success', '偏好设置获取成功');
      } catch (error) {
        this.addMessage('error', '获取偏好失败: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },

    // 好友关系操作
    async saveFriend() {
      this.loading = true;
      try {
        const response = await axios.post('/api/cache/friend/save', this.friendForm);
        this.friendData = response.data.data;
        this.addMessage('success', '好友关系保存成功 (AOF策略)');
      } catch (error) {
        this.addMessage('error', '保存好友失败: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },

    async getFriend() {
      this.loading = true;
      try {
        const response = await axios.get('/api/cache/friend/get');
        this.friendData = response.data.data;
        this.addMessage('success', '好友关系获取成功');
      } catch (error) {
        this.addMessage('error', '获取好友失败: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },

    // 日记草稿操作
    async saveDiaryDraft() {
      this.loading = true;
      try {
        const response = await axios.post('/api/cache/diary/draft/save', this.diaryForm);
        this.diaryData = response.data.data;
        this.addMessage('success', '日记草稿保存成功 (RDB策略)');
      } catch (error) {
        this.addMessage('error', '保存草稿失败: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },

    async getDiaryDraft() {
      this.loading = true;
      try {
        const response = await axios.get('/api/cache/diary/draft/get');
        this.diaryData = response.data.data;
        this.addMessage('success', '日记草稿获取成功');
      } catch (error) {
        this.addMessage('error', '获取草稿失败: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },

    async deleteDiaryDraft() {
      this.loading = true;
      try {
        await axios.delete('/api/cache/diary/draft/delete');
        this.diaryData = null;
        this.diaryForm = { title: '', content: '' };
        this.addMessage('success', '日记草稿删除成功');
      } catch (error) {
        this.addMessage('error', '删除草稿失败: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },

    // 打卡记录操作
    async saveCheckin() {
      this.loading = true;
      try {
        const response = await axios.post('/api/cache/checkin/save', this.checkinForm);
        this.checkinData = response.data.data;
        this.addMessage('success', '打卡记录保存成功 (RDB策略)');
      } catch (error) {
        this.addMessage('error', '保存打卡失败: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },

    async getCheckin() {
      this.loading = true;
      try {
        const response = await axios.get('/api/cache/checkin/get');
        this.checkinData = response.data.data;
        this.addMessage('success', '打卡记录获取成功');
      } catch (error) {
        this.addMessage('error', '获取打卡失败: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },

    // 操作日志
    async getOperationLogs() {
      this.loading = true;
      try {
        const response = await axios.get('/api/cache/operation/list');
        this.operationLogs = response.data.data || [];
        this.addMessage('success', '操作日志获取成功');
      } catch (error) {
        this.addMessage('error', '获取日志失败: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },

    async clearOperationLogs() {
      this.loading = true;
      try {
        await axios.delete('/api/cache/operation/clear');
        this.operationLogs = [];
        this.addMessage('success', '操作日志清除成功');
      } catch (error) {
        this.addMessage('error', '清除日志失败: ' + (error.response?.data?.message || error.message));
      }
      this.loading = false;
    },

    // 刷新键列表
    async refreshKeys() {
      try {
        const response = await axios.get('/api/cache/keys');
        this.keys = response.data.data || [];
      } catch (error) {
        console.error('获取键列表失败', error);
      }
    },

    getKeyType(key) {
      if (key.startsWith('user:session:')) return '会话';
      if (key.startsWith('user:pref:')) return '偏好';
      if (key.startsWith('user:friend:')) return '好友';
      if (key.startsWith('user:diary:draft:')) return '草稿';
      if (key.startsWith('user:checkin:')) return '打卡';
      if (key.startsWith('user:op:')) return '日志';
      return '其他';
    },

    getKeyValue(key) {
      // 这里应该调用API获取值，简化处理
      return '点击查看详情...';
    },

    viewKeyDetail(key) {
      this.modalTitle = '键详情: ' + key;
      this.modalContent = '正在加载...';
      this.showModal = true;
      
      // 获取键详情
      axios.get('/api/cache/key/' + encodeURIComponent(key))
        .then(response => {
          this.modalContent = JSON.stringify(response.data.data, null, 2);
        })
        .catch(error => {
          this.modalContent = '获取失败: ' + error.message;
        });
    },

    deleteKey(key) {
      if (confirm('确定要删除这个缓存键吗?')) {
        axios.delete('/api/cache/key/' + encodeURIComponent(key))
          .then(() => {
            this.addMessage('success', '键删除成功');
            this.refreshKeys();
          })
          .catch(error => {
            this.addMessage('error', '删除失败: ' + error.message);
          });
      }
    }
  }
};
</script>

<style scoped>
.redis-test {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
}

.header {
  text-align: center;
  color: white;
  margin-bottom: 30px;
}

.header h1 {
  font-size: 2.5em;
  margin-bottom: 10px;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
}

.subtitle {
  font-size: 1.2em;
  opacity: 0.9;
}

/* 状态卡片 */
.status-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.status-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
  transition: transform 0.3s;
}

.status-card:hover {
  transform: translateY(-5px);
}

.status-card.connected {
  border-left: 4px solid #52c41a;
}

.status-card.disconnected {
  border-left: 4px solid #ff4d4f;
}

.status-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  background: #f0f0f0;
}

.connected .status-icon {
  background: #d4edda;
  color: #52c41a;
}

.disconnected .status-icon {
  background: #f8d7da;
  color: #ff4d4f;
}

.status-info h3 {
  margin: 0 0 5px 0;
  color: #333;
}

.status-value {
  font-size: 1.5em;
  font-weight: bold;
  color: #667eea;
  margin: 5px 0;
}

.status-card.strategy {
  display: block;
}

.strategy-item {
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.strategy-item:last-child {
  border-bottom: none;
}

.strategy-item h4 {
  margin: 0 0 5px 0;
  color: #667eea;
}

.strategy-item p {
  margin: 0;
  font-size: 0.9em;
  color: #666;
}

.badge {
  display: inline-block;
  padding: 2px 8px;
  background: #667eea;
  color: white;
  border-radius: 10px;
  font-size: 0.75em;
  margin-top: 5px;
}

/* 策略说明 */
.strategy-explanation {
  background: white;
  border-radius: 16px;
  padding: 30px;
  margin-bottom: 30px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
}

.strategy-explanation h2 {
  text-align: center;
  color: #333;
  margin-bottom: 20px;
}

.strategy-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.strategy-card {
  padding: 20px;
  border-radius: 12px;
  border: 2px solid transparent;
}

.strategy-card.rdb {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-color: #0ea5e9;
}

.strategy-card.aof {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border-color: #22c55e;
}

.strategy-card h3 {
  margin: 0 0 10px 0;
  color: #333;
}

.strategy-card p {
  color: #666;
  margin-bottom: 15px;
}

.strategy-card ul {
  list-style: none;
  padding: 0;
  margin: 0 0 15px 0;
}

.strategy-card li {
  padding: 5px 0;
  color: #555;
}

.strategy-card li::before {
  content: '✓ ';
  color: #22c55e;
}

.rdb li::before {
  color: #0ea5e9;
}

.expire-tag {
  padding: 5px 10px;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 8px;
  font-size: 0.85em;
  color: #667eea;
  display: inline-block;
}

/* 测试操作区 */
.test-operations {
  background: white;
  border-radius: 16px;
  padding: 30px;
  margin-bottom: 30px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
}

.test-operations h2 {
  text-align: center;
  color: #333;
  margin-bottom: 20px;
}

.operation-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.tab-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 25px;
  background: #f0f0f0;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 0.95em;
}

.tab-btn:hover {
  background: #e0e0e0;
}

.tab-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.operation-panel {
  padding: 20px;
  background: #fafafa;
  border-radius: 12px;
}

.operation-panel h3 {
  margin: 0 0 10px 0;
  color: #333;
}

.description {
  color: #666;
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 1em;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #5568d3;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background: #5a6268;
}

.btn-success {
  background: #52c41a;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #3d8b1f;
}

.btn-danger {
  background: #ff4d4f;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #d9363e;
}

.btn-info {
  background: #1890ff;
  color: white;
}

.result-panel {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 20px;
  border-radius: 8px;
  margin-top: 20px;
}

.result-panel h4 {
  color: #569cd6;
  margin: 0 0 10px 0;
}

.result-panel pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.preference-form,
.friend-form,
.diary-form,
.checkin-form {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.preference-form label,
.friend-form input,
.diary-form input,
.diary-form textarea,
.checkin-form input {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1em;
}

.diary-form textarea,
.checkin-form input {
  flex: 1;
  min-width: 200px;
}

.logs-panel {
  max-height: 400px;
  overflow-y: auto;
}

.log-item {
  display: flex;
  gap: 15px;
  padding: 10px;
  border-bottom: 1px solid #333;
}

.log-item:last-child {
  border-bottom: none;
}

.log-time {
  color: #608b4e;
  white-space: nowrap;
}

.log-type {
  color: #ce9178;
  white-space: nowrap;
}

.log-desc {
  color: #d4d4d4;
}

/* 监控区 */
.monitor-section {
  background: white;
  border-radius: 16px;
  padding: 30px;
  margin-bottom: 30px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
}

.monitor-section h2 {
  text-align: center;
  color: #333;
  margin-bottom: 20px;
}

.monitor-controls {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.search-input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1em;
}

.keys-list {
  display: grid;
  gap: 15px;
}

.no-data {
  text-align: center;
  padding: 40px;
  color: #999;
}

.key-item {
  background: #f9f9f9;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 15px;
  cursor: pointer;
  transition: all 0.3s;
}

.key-item:hover {
  background: #f0f0f0;
  border-color: #667eea;
}

.key-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.key-name {
  font-weight: bold;
  color: #667eea;
  word-break: break-all;
}

.key-type {
  padding: 2px 10px;
  background: #667eea;
  color: white;
  border-radius: 10px;
  font-size: 0.8em;
}

.key-value {
  color: #666;
  font-size: 0.9em;
}

.key-actions {
  margin-top: 10px;
}

.btn-icon {
  padding: 5px 10px;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  background: transparent;
}

.btn-danger-icon:hover {
  background: #fff1f0;
}

/* 消息提示 */
.message-log {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message {
  padding: 15px 20px;
  border-radius: 8px;
  background: white;
  box-shadow: 0 5px 20px rgba(0,0,0,0.15);
  animation: slideIn 0.3s ease;
  max-width: 350px;
}

.message.success {
  background: #f6ffed;
  border-left: 4px solid #52c41a;
}

.message.error {
  background: #fff2f0;
  border-left: 4px solid #ff4d4f;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  max-width: 600px;
  width: 90%;
  max-height: 80vh;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.modal-close {
  background: none;
  border: none;
  font-size: 2em;
  cursor: pointer;
  color: #999;
}

.modal-close:hover {
  color: #333;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
  max-height: calc(80vh - 80px);
}

.modal-body pre {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 15px;
  border-radius: 8px;
  overflow-x: auto;
}
</style>
