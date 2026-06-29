const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export interface ApiResponse<T> {
  status: 'ok' | 'error'
  code: string
  data: T
  message: string | null
}

export function request<T>(path: string, options: Partial<UniApp.RequestOptions> = {}): Promise<T> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('miniapp_token') as string
    uni.request({
      ...options,
      url: `${API_BASE_URL}${path}`,
      header: {
        'Content-Type': 'application/json',
        ...(options.header || {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success: (response) => {
        const body = response.data as ApiResponse<T>
        if (response.statusCode === 401) {
          uni.removeStorageSync('miniapp_token')
          uni.removeStorageSync('miniapp_refresh_token')
          uni.removeStorageSync('miniapp_user')
        }
        if (response.statusCode >= 200 && response.statusCode < 300 && body?.status === 'ok') {
          resolve(body.data)
          return
        }
        reject(new Error(body?.message || '请求失败'))
      },
      fail: (error) => reject(new Error(error.errMsg || '网络连接失败')),
    })
  })
}
