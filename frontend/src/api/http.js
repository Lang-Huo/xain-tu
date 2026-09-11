import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截：携带 JWT
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：401 跳登录
http.interceptors.response.use(
  (resp) => resp,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      location.href = '/'
    }
    return Promise.reject(error)
  }
)

export default http
