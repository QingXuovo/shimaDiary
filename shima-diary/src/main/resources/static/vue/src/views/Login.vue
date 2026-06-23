<template>
  <div class="login-container">
    <div class="login-box">
      <div class="logo-section">
        <h1>📔 拾码日记</h1>
        <p>记录生活，打卡每一天</p>
      </div>
      
      <div class="error-message" v-if="errorMessage">{{ errorMessage }}</div>
      <div class="success-message" v-if="successMessage">{{ successMessage }}</div>
      
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">用户名</label>
          <input type="text" id="username" v-model="username" placeholder="请输入用户名" required>
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input type="password" id="password" v-model="password" placeholder="请输入密码" required>
        </div>
        <button type="submit" class="btn btn-primary">登录</button>
        <button type="button" class="btn btn-secondary" @click="goToRegister">注册新账号</button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../services/api'

const router = useRouter()
const username = ref('')
const password = ref('')
const errorMessage = ref('')
const successMessage = ref('')

const handleLogin = async () => {
  if (!username.value.trim() || !password.value) {
    errorMessage.value = '用户名和密码不能为空'
    successMessage.value = ''
    return
  }

  try {
    const response = await api.user.login({
      username: username.value.trim(),
      password: password.value
    })

    if (response.data.code === 200 && response.data.data) {
      localStorage.setItem('user', JSON.stringify(response.data.data))
      successMessage.value = '登录成功，正在跳转...'
      errorMessage.value = ''
      setTimeout(() => {
        router.push('/dashboard')
      }, 1500)
    } else {
      errorMessage.value = response.data.message || '登录失败'
      successMessage.value = ''
    }
  } catch (error) {
    console.error('登录错误:', error)
    errorMessage.value = '登录失败，请检查网络连接'
    successMessage.value = ''
  }
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  background: white;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  width: 100%;
  max-width: 400px;
}

.logo-section {
  text-align: center;
  margin-bottom: 30px;
}

.logo-section h1 {
  font-size: 28px;
  color: #333;
  margin-bottom: 8px;
}

.logo-section p {
  color: #666;
  font-size: 14px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #333;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.btn {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.3s;
}

.btn:hover {
  opacity: 0.9;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  margin-bottom: 10px;
}

.btn-secondary {
  background: #f5f5f5;
  color: #666;
}

.error-message {
  background: #ffebee;
  color: #c62828;
  padding: 10px 14px;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 14px;
}

.success-message {
  background: #e8f5e9;
  color: #2e7d32;
  padding: 10px 14px;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 14px;
}
</style>