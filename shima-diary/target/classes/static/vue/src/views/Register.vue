<template>
  <div class="register-container">
    <div class="register-box">
      <div class="logo-section">
        <h1>📔 拾码日记</h1>
        <p>注册新账号</p>
      </div>
      
      <div class="error-message" v-if="errorMessage">{{ errorMessage }}</div>
      <div class="success-message" v-if="successMessage">{{ successMessage }}</div>
      
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label for="username">用户名</label>
          <input type="text" id="username" v-model="username" placeholder="请输入用户名（至少3位）" required>
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input type="password" id="password" v-model="password" placeholder="请输入密码（至少6位）" required>
        </div>
        <div class="form-group">
          <label for="nickname">昵称</label>
          <input type="text" id="nickname" v-model="nickname" placeholder="请输入昵称（可选）">
        </div>
        <button type="submit" class="btn btn-primary">注册</button>
        <button type="button" class="btn btn-secondary" @click="goToLogin">返回登录</button>
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
const nickname = ref('')
const errorMessage = ref('')
const successMessage = ref('')

const handleRegister = async () => {
  if (!username.value.trim()) {
    errorMessage.value = '用户名不能为空'
    return
  }
  if (username.value.trim().length < 3) {
    errorMessage.value = '用户名长度不能少于3位'
    return
  }
  if (!password.value) {
    errorMessage.value = '密码不能为空'
    return
  }
  if (password.value.length < 6) {
    errorMessage.value = '密码长度不能少于6位'
    return
  }

  try {
    const response = await api.user.register({
      username: username.value.trim(),
      password: password.value,
      nickname: nickname.value.trim() || username.value.trim()
    })

    if (response.data.code === 200) {
      successMessage.value = '注册成功，正在跳转登录...'
      errorMessage.value = ''
      setTimeout(() => {
        router.push('/')
      }, 1500)
    } else {
      errorMessage.value = response.data.message || '注册失败'
      successMessage.value = ''
    }
  } catch (error) {
    console.error('注册错误:', error)
    errorMessage.value = '注册失败，请检查网络连接'
    successMessage.value = ''
  }
}

const goToLogin = () => {
  router.push('/')
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-box {
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