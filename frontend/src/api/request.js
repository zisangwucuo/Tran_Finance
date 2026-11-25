import axios from 'axios'
import { ElMessage } from 'element-plus'

// Unified API client. Use the backend address directly to avoid dev-proxy ambiguity.
const service = axios.create({
  baseURL: 'http://localhost:8080/api',
  withCredentials: true,
  timeout: 10000
})

service.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result?.code !== 200) {
      ElMessage.error(result?.message || '请求失败')
      return Promise.reject(new Error(result?.message || '请求失败'))
    }
    return result.data
  },
  (error) => {
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请确认模型服务或AI服务是否响应较慢')
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.message || '网络或服务异常')
    return Promise.reject(error)
  }
)

export default service
