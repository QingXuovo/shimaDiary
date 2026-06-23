<template>
  <header class="navbar">
    <div class="navbar-brand">
      <h1>📔 拾码日记</h1>
    </div>
    
    <nav class="navbar-nav">
      <router-link 
        to="/dashboard" 
        class="nav-item"
        :class="{ active: currentPage === 'dashboard' }"
      >
        <span class="nav-icon">🏠</span>
        <span class="nav-text">首页</span>
      </router-link>
      <router-link 
        to="/diary" 
        class="nav-item"
        :class="{ active: currentPage === 'diary' }"
      >
        <span class="nav-icon">📝</span>
        <span class="nav-text">日记</span>
      </router-link>
      <router-link 
        to="/task" 
        class="nav-item"
        :class="{ active: currentPage === 'task' }"
      >
        <span class="nav-icon">✅</span>
        <span class="nav-text">任务</span>
      </router-link>
      <router-link 
        to="/checkin" 
        class="nav-item"
        :class="{ active: currentPage === 'checkin' }"
      >
        <span class="nav-icon">📅</span>
        <span class="nav-text">打卡</span>
      </router-link>
      <router-link 
        to="/friends" 
        class="nav-item"
        :class="{ active: currentPage === 'friends' }"
      >
        <span class="nav-icon">👥</span>
        <span class="nav-text">好友</span>
      </router-link>
      <router-link 
        to="/categories" 
        class="nav-item"
        :class="{ active: currentPage === 'categories' }"
      >
        <span class="nav-icon">📁</span>
        <span class="nav-text">分类</span>
      </router-link>
      <router-link 
        to="/archive" 
        class="nav-item"
        :class="{ active: currentPage === 'archive' }"
      >
        <span class="nav-icon">📦</span>
        <span class="nav-text">归档</span>
      </router-link>
      <router-link 
        to="/mood-analysis" 
        class="nav-item"
        :class="{ active: currentPage === 'mood-analysis' }"
      >
        <span class="nav-icon">💭</span>
        <span class="nav-text">心情分析</span>
      </router-link>
      <router-link 
        to="/profile" 
        class="nav-item"
        :class="{ active: currentPage === 'profile' }"
      >
        <span class="nav-icon">👤</span>
        <span class="nav-text">个人中心</span>
      </router-link>
    </nav>
    
    <div class="navbar-actions">
      <button class="logout-btn" @click="handleLogout">
        <span>🚪</span>
        <span>退出登录</span>
      </button>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { api } from '../services/api'

defineProps({
  currentPage: {
    type: String,
    required: true
  }
})

const router = useRouter()

const handleLogout = async () => {
  try {
    await api.user.logout()
  } catch (error) {
    console.error('退出登录失败:', error)
  } finally {
    localStorage.removeItem('user')
    router.push('/')
  }
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 0 24px;
  height: 64px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.navbar-brand h1 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.navbar-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  border-radius: 8px;
  color: white;
  text-decoration: none;
  transition: background 0.3s;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.nav-item.active {
  background: rgba(255, 255, 255, 0.2);
}

.nav-icon {
  font-size: 16px;
  margin-right: 8px;
}

.nav-text {
  font-size: 14px;
  font-weight: 500;
}

.navbar-actions {
  display: flex;
  align-items: center;
}

.logout-btn {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
  transition: background 0.3s;
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.logout-btn span:first-child {
  margin-right: 8px;
}

@media (max-width: 768px) {
  .navbar {
    padding: 0 12px;
    height: 56px;
  }
  
  .navbar-brand h1 {
    font-size: 16px;
  }
  
  .nav-text {
    display: none;
  }
  
  .nav-item {
    padding: 8px 12px;
  }
  
  .nav-icon {
    margin-right: 0;
    font-size: 18px;
  }
  
  .logout-btn span:last-child {
    display: none;
  }
}
</style>