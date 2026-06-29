const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://example.com/api'

export function request<T>(path: string, options: UniApp.RequestOptions = {}): Promise<T> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('miniapp_token')
    uni.request({
      ...options,
      url: `${API_BASE_URL}${path}`,
      header: {
        ...(options.header || {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success: (response) => resolve(response.data as T),
      fail: reject,
    })
  })
}
