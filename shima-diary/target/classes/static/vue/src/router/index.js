import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Login', component: () => import('../views/Login.vue'), meta: { requiresAuth: false } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { requiresAuth: false } },
  { path: '/dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { requiresAuth: true } },
  { path: '/diary', name: 'Diary', component: () => import('../views/Diary.vue'), meta: { requiresAuth: true } },
  { path: '/task', name: 'Task', component: () => import('../views/Task.vue'), meta: { requiresAuth: true } },
  { path: '/checkin', name: 'Checkin', component: () => import('../views/Checkin.vue'), meta: { requiresAuth: true } },
  { path: '/friends', name: 'Friends', component: () => import('../views/Friends.vue'), meta: { requiresAuth: true } },
  { path: '/categories', name: 'Categories', component: () => import('../views/Categories.vue'), meta: { requiresAuth: true } },
  { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue'), meta: { requiresAuth: true } },
  { path: '/archive', name: 'Archive', component: () => import('../views/Archive.vue'), meta: { requiresAuth: true } },
  { path: '/mood-analysis', name: 'MoodAnalysis', component: () => import('../views/MoodAnalysis.vue'), meta: { requiresAuth: true } },
  { path: '/share/:token', name: 'ShareDiary', component: () => import('../views/ShareDiary.vue'), meta: { requiresAuth: false } }
]

const router = createRouter({
  history: createWebHashHistory('/api/vue'),
  routes
})

router.beforeEach((to, from, next) => {
  const isLoggedIn = localStorage.getItem('user') !== null
  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/')
  } else if (!to.meta.requiresAuth && isLoggedIn) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router