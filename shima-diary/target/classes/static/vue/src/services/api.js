import axios from 'axios'

const API_BASE = '/api'

const instance = axios.create({
  baseURL: API_BASE,
  withCredentials: true
})

instance.interceptors.request.use(
  config => {
    const user = localStorage.getItem('user')
    if (user) {
      const userObj = JSON.parse(user)
      config.headers['X-User-Id'] = userObj.id
    }
    return config
  },
  error => Promise.reject(error)
)

instance.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('user')
      window.location.href = '/api/vue/'
    }
    return Promise.reject(error)
  }
)

export const api = {
  user: {
    login: data => instance.post('/user/login', data),
    register: data => instance.post('/user/register', data),
    getCurrent: () => instance.get('/user/current'),
    update: (id, data) => instance.put(`/user/${id}`, data),
    updatePassword: (id, data) => instance.put(`/user/${id}/password`, data),
    logout: () => instance.post('/user/logout')
  },
  diary: {
    list: () => instance.get('/diary'),
    getById: id => instance.get(`/diary/${id}`),
    create: data => instance.post('/diary', data),
    update: (id, data) => instance.put(`/diary/${id}`, data),
    delete: id => instance.delete(`/diary/${id}`),
    search: keyword => instance.get(`/diary/search?keyword=${keyword}`),
    count: () => instance.get('/diary/count'),
    byMood: mood => instance.get(`/diary/mood/${mood}`),
    byCategory: categoryId => instance.get(`/diary/category/${categoryId}`),
    byDateRange: (startDate, endDate) => instance.get(`/diary/range?startDate=${startDate}&endDate=${endDate}`),
    share: (diaryId, days) => instance.post(`/share/diary/${diaryId}`, { days }),
    cancelShare: diaryId => instance.delete(`/share/diary/${diaryId}`),
    viewShare: token => instance.get(`/share/${token}`),
    archive: {
      list: () => instance.get('/diary/archive/list'),
      archive: id => instance.post(`/diary/archive/${id}/archive`),
      unarchive: id => instance.post(`/diary/archive/${id}/unarchive`),
      recycle: () => instance.get('/diary/archive/recycle'),
      restore: id => instance.post(`/diary/archive/${id}/restore`),
      permanentDelete: id => instance.delete(`/diary/archive/${id}/permanent`)
    },
    ai: {
      analyze: id => instance.get(`/diary/ai/analyze/${id}`),
      trend: days => instance.get(`/diary/ai/trend?days=${days}`),
      suggestion: () => instance.get('/diary/ai/suggestion')
    }
  },
  task: {
    list: () => instance.get('/task'),
    getById: id => instance.get(`/task/${id}`),
    create: data => instance.post('/task', data),
    update: (id, data) => instance.put(`/task/${id}`, data),
    delete: id => instance.delete(`/task/${id}`),
    updateStatus: (id, status) => instance.put(`/task/${id}/status`, { status }),
    updateProgress: (id, progress) => instance.put(`/task/${id}/progress?progress=${progress}`),
    count: () => instance.get('/task/count'),
    stats: () => instance.get('/task/stats'),
    today: () => instance.get('/task/today'),
    overdue: () => instance.get('/task/overdue'),
    search: keyword => instance.get(`/task/search?keyword=${keyword}`),
    byPriority: priority => instance.get(`/task/priority/${priority}`),
    getSubTasks: parentId => instance.get(`/task/${parentId}/subtasks`)
  },
  checkin: {
    list: () => instance.get('/checkin'),
    create: data => instance.post('/checkin', data),
    delete: id => instance.delete(`/checkin/${id}`),
    today: () => instance.get('/checkin/today'),
    stats: () => instance.get('/checkin/stats'),
    count: () => instance.get('/checkin/count'),
    continuous: type => instance.get(`/checkin/continuous/${type}`),
    recent: days => instance.get(`/checkin/recent?days=${days}`),
    config: {
      list: () => instance.get('/checkin/config'),
      create: data => instance.post('/checkin/config', data),
      update: data => instance.put('/checkin/config', data),
      delete: id => instance.delete(`/checkin/config/${id}`)
    }
  },
  category: {
    list: () => instance.get('/category/list'),
    create: data => instance.post('/category', data),
    update: (id, data) => instance.put(`/category/${id}`, data),
    delete: id => instance.delete(`/category/${id}`)
  },
  friend: {
    list: () => instance.get('/friend/list'),
    search: keyword => instance.get(`/friend/search?keyword=${keyword}`),
    add: friendId => instance.post(`/friend/request/${friendId}`),
    accept: friendId => instance.post(`/friend/accept/${friendId}`),
    reject: friendId => instance.post(`/friend/reject/${friendId}`),
    delete: friendId => instance.delete(`/friend/${friendId}`),
    requests: () => instance.get('/friend/requests'),
    check: friendId => instance.get(`/friend/check/${friendId}`)
  },
  stats: {
    dashboard: () => instance.get('/stats/dashboard'),
    dashboardFull: () => instance.get('/stats/dashboard-full'),
    yearReview: year => instance.get(`/stats/year-review?year=${year}`),
    monthly: year => instance.get(`/stats/monthly?year=${year}`),
    moodDistribution: () => instance.get('/stats/mood-distribution'),
    diaryTrend: days => instance.get(`/stats/diary-trend?days=${days}`)
  }
}

export default instance